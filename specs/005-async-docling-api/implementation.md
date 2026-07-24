# Implementation: Switch DoclingExtractor to Async Docling Serve API

**Status:** Complete

## Task 1: Update `DoclingExtractor` to use async API

Changed both public methods to return `Uni<T>`:

- `extract(Path)` → `Uni<ExtractionResult>`: wraps `convertFilesAsync()`
  in `Uni.createFrom().completionStage(() -> ...)`, maps the
  `ConvertDocumentResponse` to `ExtractionResult` inside `.map()`.

- `extractAndChunk(Path)` → `Uni<List<TextSegment>>`: wraps
  `chunkFilesWithHybridChunkerAsync()` the same way, maps chunks to
  `TextSegment` list inside `.map()`.

The `Supplier<CompletionStage>` form of `Uni.createFrom().completionStage()`
ensures the async task is submitted lazily — only when the `Uni` is
subscribed to.

## Tasks 2–4: Update pipeline interface and implementations

`IngestionPipeline.processAndStore()` and
`AbstractIngestionPipeline.buildSegments()` both changed to return
`Uni<List<TextSegment>>`. The embed/store step in `processAndStore`
chains as `.invoke()` on the `Uni`.

Each pipeline implementation adapted:
- **DoclingNaiveIngestionPipeline:** chains `.map()` on the `Uni` from
  `extract()` to apply the naive chunker.
- **DoclingHybridIngestionPipeline:** returns `extractAndChunk()` directly
  (already a `Uni`).
- **TikaNaiveIngestionPipeline:** wraps its synchronous Tika + chunker
  call in `Uni.createFrom().item(() -> { ... })`.

## Task 5: Update `IngestionStartup`

`runPipeline` now returns `Uni<Void>`. For the skip-if-exists case, it
returns `Uni.createFrom().voidItem()`. Otherwise it chains
`pipeline.processAndStore(path).invoke(logSegments).replaceWithVoid()`.

Parallel mode maps each pipeline directly to its `Uni<Void>` and joins
with `Uni.join().all().andCollectFailures()`. The
`runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` wrapper was
removed because the async Docling calls already run on the Mutiny worker
pool internally (in `QuarkusDoclingServeApi.executeAsync()`).

Sequential mode awaits each pipeline's `Uni` with
`.await().atMost(Duration.ofMinutes(5))`.

## Task 6: Update WireMock stubs

Replaced the two sync stubs with six async stubs modeling the 3-step flow:

| Stub file | Endpoint | Response |
|-----------|----------|----------|
| `docling-convert.json` | `POST /v1/convert/source/async` | `task_id: "convert-task-1"`, `task_status: "started"` |
| `docling-chunk-hybrid.json` | `POST /v1/chunk/hybrid/source/async` | `task_id: "chunk-task-1"`, `task_status: "started"` |
| `docling-task-poll-convert.json` | `GET /v1/status/poll/convert-task-1` | `task_status: "success"` |
| `docling-task-poll-chunk.json` | `GET /v1/status/poll/chunk-task-1` | `task_status: "success"` |
| `docling-convert-result.json` | `GET /v1/result/convert-task-1` | existing `docling-convert-response.json` |
| `docling-chunk-result.json` | `GET /v1/result/chunk-task-1` | existing `docling-chunk-response.json` |

Key insight during implementation: the poll response's `task_id` field is
used by the library to construct the result retrieval URL. A generic poll
stub returning a static `task_id` caused 404s on the result endpoint.
Fixed by using per-task poll stubs that echo back the correct `task_id`.

## Tasks 7–8: Update test classes

`CaptureDoclingResponsesTest` switched to `convertFilesAsync` /
`chunkFilesWithHybridChunkerAsync`, blocking with
`.toCompletableFuture().join()`.

Five test classes updated to subscribe to the `Uni` with
`.await().atMost(Duration.ofMinutes(5))` — matching the standard timeout
used across all other tests in the project.

## Task 9: Verification

`./mvnw verify -Duse.wiremock.docling=true` — 58 tests, 0 failures,
17 expected skips (gated tests). Logs confirm async flow:
"Started async conversion with task ID: convert-task-1" →
"Task convert-task-1 completed successfully".
