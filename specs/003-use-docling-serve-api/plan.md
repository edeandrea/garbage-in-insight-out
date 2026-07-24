# Plan: Switch from DoclingService to DoclingServeApi

**Status:** Approved

## Approach

Replace `DoclingService` (the `quarkus-docling` wrapper) with direct
`DoclingServeApi` injection in two files. Use the `DoclingServeApi`
convenience methods that accept `Path` arguments — these handle file
reading and base64-encoding internally, keeping `DoclingExtractor` code
concise.

The convenience methods (`convertFiles`, `chunkFilesWithHybridChunker`)
do not declare checked exceptions. File I/O failures surface as unchecked
`RuntimeException` wrapping `IOException` (from `FileUtils.createFileSources`).
This changes the exception handling in `DoclingExtractor`.

## Files to change

### `src/main/java/dev/ericdeandrea/docling/ai/ingestion/extraction/DoclingExtractor.java`

**Constructor:** Change injection from `DoclingService` to `DoclingServeApi`.

**`extract(Path)`:**
- Build a `ConvertDocumentRequest` with `OutputFormat.JSON` in its options.
- Call `doclingServeApi.convertFiles(request, documentPath)`.
- Cast result to `InBodyConvertDocumentResponse` (unchanged).
- Remove the `try/catch (IOException)` block — the convenience method
  wraps I/O failures in unchecked `RuntimeException`, so no catch is
  needed. The `UncheckedIOException` this method currently throws is
  equally unchecked, so callers are unaffected.

**`extractAndChunk(Path)`:**
- Build a `HybridChunkDocumentRequest` with `OutputFormat.JSON` in its
  options.
- Call `doclingServeApi.chunkFilesWithHybridChunker(request, documentPath)`.
- Remove the `try/catch (IOException)` block — same rationale as above.

**Imports:** Remove `DoclingService`, `UncheckedIOException`, `IOException`.
Add `DoclingServeApi`, `ConvertDocumentRequest`,
`ConvertDocumentOptions`, `HybridChunkDocumentRequest`.

### `src/test/java/dev/ericdeandrea/docling/ai/ingestion/extraction/CaptureDoclingResponsesTest.java`

**Field:** Change `@Inject DoclingService` to `@Inject DoclingServeApi`.

**`captureConversionResponse()`:**
- Build a `ConvertDocumentRequest` with `OutputFormat.JSON` and call
  `doclingServeApi.convertFiles(request, FIXTURE)`.
- `throws IOException` stays on the method — `Files.writeString` still
  needs it.

**`captureChunkingResponse()`:**
- Build a `HybridChunkDocumentRequest` with `OutputFormat.JSON` and call
  `doclingServeApi.chunkFilesWithHybridChunker(request, FIXTURE)`.
- `throws IOException` stays — same reason.

**Imports:** Remove `DoclingService`. Add `DoclingServeApi`,
`ConvertDocumentRequest`, `ConvertDocumentOptions`,
`HybridChunkDocumentRequest`.

## No changes required

- **WireMock stubs** — the convenience methods delegate to
  `convertSource` and `chunkSourceWithHybridChunker`, which hit the same
  HTTP endpoints (`/v1/convert/source`, `/v1/chunk/hybrid/source`).
- **Test classes** (`DoclingExtractorTest`, `DoclingHybridChunkingTest`,
  `ChunkSizeValidationTest`, etc.) — they inject `DoclingExtractor`, not
  `DoclingService`, so they're unaffected.
- **Ingestion pipelines** — they depend on `DoclingExtractor`, not on the
  Docling client directly.
- **`pom.xml`** — `quarkus-docling` dependency stays.
- **`application.yml`** — no Docling configuration changes.

## Verification

1. Run the full test suite: `./mvnw verify`.
2. Confirm `DoclingExtractorTest`, `DoclingHybridChunkingTest`, and
   `ChunkSizeValidationTest` pass (these exercise both `extract` and
   `extractAndChunk` against WireMock stubs).
3. Verify no remaining references to `DoclingService` in project source:
   `grep -r "DoclingService" src/`.
