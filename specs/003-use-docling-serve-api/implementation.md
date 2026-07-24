# Implementation: Switch from DoclingService to DoclingServeApi

## Task 1: Update `DoclingExtractor` to use `DoclingServeApi`

**Approach:**
- Replace `DoclingService` constructor parameter with `DoclingServeApi`.
- In `extract(Path)`: build `ConvertDocumentRequest` with
  `ConvertDocumentOptions.builder().toFormat(OutputFormat.JSON).build()`,
  call `doclingServeApi.convertFiles(request, documentPath)`, cast to
  `InBodyConvertDocumentResponse`. Remove try/catch — the API uses
  unchecked exceptions.
- In `extractAndChunk(Path)`: build `HybridChunkDocumentRequest` with
  same options pattern, call
  `doclingServeApi.chunkFilesWithHybridChunker(request, documentPath)`.
  Remove try/catch.
- Remove unused imports (`IOException`, `UncheckedIOException`,
  `DoclingService`), add new ones (`DoclingServeApi`,
  `ConvertDocumentRequest`, `ConvertDocumentOptions`,
  `HybridChunkDocumentRequest`).
- Extracted `ConvertDocumentOptions` as a `private static final` constant
  (`JSON_OPTIONS`) since both methods use the same options object.

## Task 2: Update `CaptureDoclingResponsesTest` to use `DoclingServeApi`

**Approach:**
- Replace `@Inject DoclingService` with `@Inject DoclingServeApi`.
- In both test methods: build the same request objects as
  `DoclingExtractor` and call the corresponding `DoclingServeApi`
  convenience methods.
- Keep `throws IOException` on both methods — still needed for
  `Files.writeString`.
- Remove `DoclingService` import, add `DoclingServeApi`,
  `ConvertDocumentRequest`, `ConvertDocumentOptions`,
  `HybridChunkDocumentRequest`.