# Tasks: Switch DoclingExtractor to Async Docling Serve API

**Status:** Approved

## Checklist

- [x] 1. Update `DoclingExtractor` to use async API
  - Change `extract(Path)` to return `Uni<ExtractionResult>`, call
    `convertFilesAsync` wrapped in `Uni.createFrom().completionStage()`
  - Change `extractAndChunk(Path)` to return `Uni<List<TextSegment>>`,
    call `chunkFilesWithHybridChunkerAsync` wrapped in
    `Uni.createFrom().completionStage()`
  - Add `io.smallrye.mutiny.Uni` import

- [x] 2. Update `IngestionPipeline` interface
  - Change `processAndStore(Path)` return type to `Uni<List<TextSegment>>`
  - Add `io.smallrye.mutiny.Uni` import

- [x] 3. Update `AbstractIngestionPipeline`
  - Change `buildSegments(Path)` return type to `Uni<List<TextSegment>>`
  - Chain `processAndStore` reactively: `buildSegments(path).invoke(segments -> embed/store)`
  - Add `io.smallrye.mutiny.Uni` import

- [x] 4. Update pipeline implementations
  - `DoclingNaiveIngestionPipeline`: chain `.map()` on `extract()` Uni
  - `DoclingHybridIngestionPipeline`: return `extractAndChunk()` directly
  - `TikaNaiveIngestionPipeline`: wrap sync Tika call in
    `Uni.createFrom().item()`
  - Add `io.smallrye.mutiny.Uni` import to each

- [x] 5. Update `IngestionStartup`
  - Change `runPipeline` to return `Uni<Void>`
  - Parallel mode: map pipelines to their `Uni<Void>`, join with
    `Uni.join().all().andCollectFailures()`
  - Sequential mode: await each pipeline's `Uni` with
    `.await().atMost(Duration.ofMinutes(5))`
  - Remove `io.smallrye.mutiny.infrastructure.Infrastructure` import
    (no longer needed — async calls already run on worker pool
    internally)

- [x] 6. Update WireMock stubs for async 3-step flow
  - Replace `mappings/docling-convert.json` with async submit stub
    (`POST /v1/convert/source/async`)
  - Replace `mappings/docling-chunk-hybrid.json` with async submit stub
    (`POST /v1/chunk/hybrid/source/async`)
  - Add `mappings/docling-task-poll-convert.json`
    (`GET /v1/status/poll/convert-task-1`)
  - Add `mappings/docling-task-poll-chunk.json`
    (`GET /v1/status/poll/chunk-task-1`)
  - Add `mappings/docling-convert-result.json`
    (`GET /v1/result/convert-task-1`)
  - Add `mappings/docling-chunk-result.json`
    (`GET /v1/result/chunk-task-1`)
  - Add `__files/docling-convert-task-submit.json` and
    `__files/docling-chunk-task-submit.json` (task status responses)

- [x] 7. Update `CaptureDoclingResponsesTest`
  - Switch to `convertFilesAsync` / `chunkFilesWithHybridChunkerAsync`
  - Block on `CompletionStage` with `.toCompletableFuture().join()`

- [x] 8. Update test classes for `Uni` return types
  - `DoclingExtractorTest` — add `.await().atMost(Duration.ofMinutes(5))`
  - `DoclingHybridChunkingTest` — same
  - `ChunkSizeValidationTest` — two calls updated
  - `ChunkSizeSimulationTest` — two calls updated
  - `ModeAvsModeBTest` — one call updated
  - Add `java.time.Duration` import to each

- [x] 9. Verify: `./mvnw verify -Duse.wiremock.docling=true` passes
