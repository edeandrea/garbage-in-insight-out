# Spec 003: Switch from DoclingService to DoclingServeApi

**Status:** Approved

## Summary

Replace the `DoclingService` wrapper (from `quarkus-docling`) with direct
usage of the `DoclingServeApi` interface (from `docling-serve-api`) in
`DoclingExtractor` and `CaptureDoclingResponsesTest`. The `quarkus-docling`
extension remains as the CDI provider of the `DoclingServeApi` bean and
for dev services — only the higher-level wrapper is bypassed.

## Motivation

`DoclingService` is a convenience wrapper that adds a layer of indirection
between this project's code and the underlying `DoclingServeApi`. The
project only uses two methods (`convertFile` and `chunkFileHybrid`), both
of which map directly to `DoclingServeApi` convenience methods that accept
`Path` arguments and handle file I/O internally. Removing the wrapper
simplifies the dependency chain and gives the project direct control over
request construction (e.g., chunking options, output format) without going
through an intermediary.

## Requirements

1. `DoclingExtractor` must inject `DoclingServeApi` instead of
   `DoclingService`.

2. `DoclingExtractor.extract(Path)` must call
   `DoclingServeApi.convertFiles(ConvertDocumentRequest, Path...)` with
   `OutputFormat.JSON` set in the request options, producing the same
   `InBodyConvertDocumentResponse` result as today.

3. `DoclingExtractor.extractAndChunk(Path)` must call
   `DoclingServeApi.chunkFilesWithHybridChunker(HybridChunkDocumentRequest, Path...)`
   with `OutputFormat.JSON` set in the request options, producing the
   same `ChunkDocumentResponse` result as today.

4. `CaptureDoclingResponsesTest` must inject `DoclingServeApi` instead
   of `DoclingService` and use the same API methods as `DoclingExtractor`.

5. All existing tests (`DoclingExtractorTest`,
   `DoclingHybridChunkingTest`, `ChunkSizeValidationTest`,
   `ChunkSizeSimulationTest`, `ModeAvsModeBTest`) must continue to pass
   with no changes to their assertions or test logic.

6. The `quarkus-docling` Maven dependency must remain — it provides the
   CDI-managed `DoclingServeApi` bean and Docling dev services.

7. No behavioral change to the ingestion pipelines or chat functionality.

## Out of scope

- Removing the `quarkus-docling` extension entirely.
- Changing chunking options (e.g., `maxTokens`, `mergePeers`) — the
  current defaults remain.
- Adding new Docling API features (async conversion, batch processing,
  hierarchical chunking).
- Changing WireMock stub mappings or captured response files — the
  HTTP endpoints and payloads are unchanged.

## Open questions

None.