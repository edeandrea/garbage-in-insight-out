# Plan: Switch DoclingExtractor to Async Docling Serve API

**Status:** Approved

## Approach

Replace the synchronous `DoclingServeApi` calls with their async variants,
wrap the returned `CompletionStage` in Mutiny `Uni`, and propagate the
reactive type through the pipeline interface up to `IngestionStartup`.

The `docling-serve-api` library's async methods use a task-based model:
1. **Submit:** `POST /v1/convert/source/async` (or `.../chunk/hybrid/source/async`) → returns a `TaskStatusPollResponse` with a `task_id`
2. **Poll:** `GET /v1/status/poll/{taskId}?wait=0` → returns `TaskStatusPollResponse` with `task_status`
3. **Result:** `GET /v1/result/{taskId}` → returns the `ConvertDocumentResponse` or `ChunkDocumentResponse`

The `QuarkusDoclingServeApi` implementation handles the submit/poll/result
lifecycle internally on the Mutiny worker pool, returning a
`CompletionStage` that completes when the result is available.

## Files to change

### `DoclingExtractor.java`

**`extract(Path)`:**
- Wrap `convertFilesAsync(request, documentPath)` in
  `Uni.createFrom().completionStage(() -> ...)`.
- Map the response to `ExtractionResult` inside `.map()`.
- Return type changes from `ExtractionResult` to `Uni<ExtractionResult>`.

**`extractAndChunk(Path)`:**
- Wrap `chunkFilesWithHybridChunkerAsync(request, documentPath)` in
  `Uni.createFrom().completionStage(() -> ...)`.
- Map the response to `List<TextSegment>` inside `.map()`.
- Return type changes from `List<TextSegment>` to `Uni<List<TextSegment>>`.

### `IngestionPipeline.java`

- `processAndStore(Path)` return type changes to `Uni<List<TextSegment>>`.

### `AbstractIngestionPipeline.java`

- `buildSegments(Path)` return type changes to `Uni<List<TextSegment>>`.
- `processAndStore(Path)` chains `buildSegments(path).invoke(segments -> embed/store)`.

### `DoclingNaiveIngestionPipeline.java`

- `buildSegments` chains `.map(result -> naiveChunker.chunk(result, mode))` on
  the `Uni` from `doclingExtractor.extract()`.

### `DoclingHybridIngestionPipeline.java`

- `buildSegments` returns `doclingExtractor.extractAndChunk(documentPath)` directly.

### `TikaNaiveIngestionPipeline.java`

- `buildSegments` wraps the synchronous Tika extraction in
  `Uni.createFrom().item(() -> { ... })`.

### `IngestionStartup.java`

- `runPipeline` returns `Uni<Void>` instead of being void.
- Parallel mode: maps each pipeline to its `Uni<Void>`, joins with
  `Uni.join().all().andCollectFailures()`.
- Sequential mode: awaits each pipeline's `Uni` with
  `.await().atMost(Duration.ofMinutes(5))`.

### WireMock stubs

**Remove:**
- `mappings/docling-convert.json` (sync `POST /v1/convert/source`)
- `mappings/docling-chunk-hybrid.json` (sync `POST /v1/chunk/hybrid/source`)

**Add submit stubs:**
- `mappings/docling-convert.json` → `POST /v1/convert/source/async` responds with `task_id: "convert-task-1"`, `task_status: "started"`
- `mappings/docling-chunk-hybrid.json` → `POST /v1/chunk/hybrid/source/async` responds with `task_id: "chunk-task-1"`, `task_status: "started"`

**Add poll stubs (per task ID — the result endpoint uses the task ID from the poll response):**
- `mappings/docling-task-poll-convert.json` → `GET /v1/status/poll/convert-task-1` responds with `task_status: "success"`
- `mappings/docling-task-poll-chunk.json` → `GET /v1/status/poll/chunk-task-1` responds with `task_status: "success"`

**Add result stubs (keyed by task ID):**
- `mappings/docling-convert-result.json` → `GET /v1/result/convert-task-1` responds with existing `docling-convert-response.json`
- `mappings/docling-chunk-result.json` → `GET /v1/result/chunk-task-1` responds with existing `docling-chunk-response.json`

### `CaptureDoclingResponsesTest.java`

- Switch to `convertFilesAsync` / `chunkFilesWithHybridChunkerAsync`,
  block on the `CompletionStage` with `.toCompletableFuture().join()`.

### Test classes calling `DoclingExtractor`

- `DoclingExtractorTest`, `DoclingHybridChunkingTest`,
  `ChunkSizeValidationTest`, `ChunkSizeSimulationTest`,
  `ModeAvsModeBTest` — add `.await().atMost(Duration.ofMinutes(5))`
  after each `Uni`-returning call.

## No changes required

- **`pom.xml`** — Mutiny is already a transitive dependency.
- **`application.yml`** — async poll/timeout defaults are sufficient.
- **`TikaExtractor.java`** — Tika has no async variant.
- **`NaiveChunker.java`** — chunking is CPU-only, stays sync.
- **Chat-layer code** — only reads from Qdrant at chat time.
- **Existing `__files/` response bodies** — the async result endpoint
  returns the same format as the sync endpoint.

## Verification

1. `./mvnw verify -Duse.wiremock.docling=true` — full test suite with
   WireMock Docling stubs.
2. Confirm async flow in logs: "Started async conversion with task ID:
   convert-task-1" → "Task convert-task-1 completed successfully".
3. `grep -rn "indefinitely" src/` returns zero results.
4. `grep -rn "convertFiles\b\|chunkFilesWithHybridChunker\b" src/main/`
   returns zero results (only async variants remain in production code).
