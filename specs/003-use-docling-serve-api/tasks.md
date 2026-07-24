# Tasks: Switch from DoclingService to DoclingServeApi

**Status:** Approved

## Checklist

- [x] 1. Update `DoclingExtractor` to use `DoclingServeApi`
  - Change constructor injection from `DoclingService` to `DoclingServeApi`
  - Rewrite `extract(Path)` to build a `ConvertDocumentRequest` with
    `OutputFormat.JSON` and call `convertFiles(request, documentPath)`;
    remove the `try/catch (IOException)` block
  - Rewrite `extractAndChunk(Path)` to build a
    `HybridChunkDocumentRequest` with `OutputFormat.JSON` and call
    `chunkFilesWithHybridChunker(request, documentPath)`; remove the
    `try/catch (IOException)` block
  - Update imports accordingly

- [x] 2. Update `CaptureDoclingResponsesTest` to use `DoclingServeApi`
  - Change `@Inject DoclingService` field to `@Inject DoclingServeApi`
  - Rewrite `captureConversionResponse()` to build a
    `ConvertDocumentRequest` and call `convertFiles(request, FIXTURE)`
  - Rewrite `captureChunkingResponse()` to build a
    `HybridChunkDocumentRequest` and call
    `chunkFilesWithHybridChunker(request, FIXTURE)`
  - Update imports accordingly

- [x] 3. Run `./mvnw verify` and confirm all tests pass

- [x] 4. Verify no remaining `DoclingService` references:
  `grep -r "DoclingService" src/`