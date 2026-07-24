# Spec 005: Switch DoclingExtractor to Async Docling Serve API

**Status:** Approved

## Summary

Switch `DoclingExtractor` from the synchronous `DoclingServeApi` methods
(`convertFiles`, `chunkFilesWithHybridChunker`) to their async variants
(`convertFilesAsync`, `chunkFilesWithHybridChunkerAsync`). Make the entire
ingestion pipeline reactive by propagating `Uni` return types through
`IngestionPipeline`, `AbstractIngestionPipeline`, and all three pipeline
implementations.

## Motivation

The synchronous Docling API holds an HTTP connection open for the entire
duration of document conversion or chunking. For large documents, this
ties up a thread and an HTTP connection while waiting for server-side
processing.

The async API uses Docling Serve's task-based model: submit a task
(receives a task ID immediately), poll for completion, then retrieve the
result. This frees the HTTP connection during processing and is more
resilient for long-running conversions. The `docling-serve-api` library
handles the submit/poll/retrieve lifecycle internally, returning a
`CompletionStage` that completes when the result is ready.

Making the pipeline fully reactive with Mutiny `Uni` is idiomatic for
Quarkus and eliminates the `Uni.createFrom().voidItem().invoke(syncCall)`
workaround that was previously used in `IngestionStartup` to achieve
parallelism.

## Requirements

1. `DoclingExtractor.extract(Path)` must return `Uni<ExtractionResult>`
   instead of `ExtractionResult`, calling `convertFilesAsync` internally.

2. `DoclingExtractor.extractAndChunk(Path)` must return
   `Uni<List<TextSegment>>` instead of `List<TextSegment>`, calling
   `chunkFilesWithHybridChunkerAsync` internally.

3. `IngestionPipeline.processAndStore(Path)` must return
   `Uni<List<TextSegment>>` instead of `List<TextSegment>`.

4. `AbstractIngestionPipeline.buildSegments(Path)` must return
   `Uni<List<TextSegment>>`. The embed/store step in `processAndStore`
   must chain reactively via `.invoke()`.

5. `DoclingNaiveIngestionPipeline` must chain `.map()` on the `Uni` from
   `extract()` to apply the naive chunker.

6. `DoclingHybridIngestionPipeline` must return the `Uni` from
   `extractAndChunk()` directly.

7. `TikaNaiveIngestionPipeline` must wrap its synchronous Tika call in
   `Uni.createFrom().item()`.

8. `IngestionStartup` must subscribe to the pipeline `Uni` natively
   instead of wrapping synchronous calls.

9. WireMock stubs must be updated from the single-endpoint sync flow to
   the three-endpoint async flow (submit → poll → result).

10. `CaptureDoclingResponsesTest` must use the async API methods.

11. All test classes that call `DoclingExtractor` must subscribe to the
    returned `Uni` with `.await().atMost(Duration.ofMinutes(5))`.

12. All existing tests must continue to pass with no changes to their
    assertions.

## Out of scope

- Batch conversion API (`convertSourceBatchAsync`).
- Hierarchical chunking API.
- Configuring `asyncPollInterval` or `asyncTimeout` — defaults from
  `DoclingRuntimeConfig` are sufficient (2s poll, 5m timeout).
- Making Tika extraction async — Tika has no async API.
- Changes to the chat-layer or Vaadin UI code.

## Open questions

None.
