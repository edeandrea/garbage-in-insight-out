# Spec 004: Chunk Metadata Enrichment — Tasks

**Status:** Approved

## Tasks

- [x] 1. **Update CaptureDoclingResponsesTest and re-capture WireMock stub ([req 3](spec.md#requirements))**
  Update `captureChunkingResponse()` to set `includeConvertedDoc(true)`
  on the `HybridChunkDocumentRequest`. Run the capture test against a
  live Docling Serve instance (`-Dcapture.docling.responses=true`) to
  regenerate `docling-chunk-response.json` with the embedded
  `DoclingDocument`. Verify the captured response contains
  `documents[0].content.json_content` with texts and tables. If it
  doesn't, stop — the Mode C enrichment (req 2) depends on this.

- [x] 2. **Create DocItemIndex ref resolution utility**
  Create a package-private record `DocItemIndex` in the
  `ai.ingestion.extraction` package. Factory method `of(DoclingDocument)`
  builds lookup maps from `doc.getTexts()` (keyed by `getSelfRef()`)
  and `doc.getTables()` (keyed by `getSelfRef()`). Methods:
  `labelFor(String ref)` returns the element's label as a string,
  `captionTextFor(TableItem table)` resolves the table's first
  `getCaptions()` ref through the text map to get caption text. Add unit
  tests for `DocItemIndex` covering: label lookup for text items, label
  lookup for table items, caption text resolution, unknown ref returns
  empty.

- [x] 3. **Fix Mode B `elementLabel` in DoclingExtractor ([req 1](spec.md#requirements))**
  Add `elementLabel` parameter to `toProvenanceEntry()`. Update
  `buildProvenance()` to build a `DocItemIndex` from the document, then:
  for text items with `label == CAPTION`, pass `item.getText()` as
  `elementLabel`; for table items, resolve caption text via
  `DocItemIndex.captionTextFor(table)`; for all other text items, pass
  `null`.

- [x] 4. **Update Mode B tests ([req 1](spec.md#requirements))**
  Update `NaiveChunkerTest.attachesProvenanceMetadataWhenPresent()` to
  use a `ProvenanceEntry` with non-null `elementLabel` and verify it
  flows through to segment metadata. Update `DoclingExtractorTest` to
  assert: table provenance entries have non-null `elementLabel` (caption
  text), caption text items have `elementLabel` equal to their own text,
  ordinary text items have null `elementLabel`. Run tests to verify.

- [x] 5. **Enrich Mode C with `elementType` in DoclingExtractor ([req 2](spec.md#requirements))**
  Set `includeConvertedDoc(true)` on `HybridChunkDocumentRequest` in
  `extractAndChunk()`. Extract the `DoclingDocument` from
  `response.getDocuments().getFirst().getContent().getJsonContent()`.
  Build a `DocItemIndex`. For each chunk, resolve the first
  `chunk.getDocItems()` ref via `DocItemIndex.labelFor()` and store as
  `element_type` metadata on the `TextSegment`.

- [x] 6. **Update Mode C tests ([req 2](spec.md#requirements))**
  Update `DoclingHybridChunkingTest` to assert segments have
  `element_type` metadata populated. Verify expected labels appear
  (TEXT, TABLE, LIST\_ITEM, CAPTION, etc.). Run tests to verify.

- [x] 7. **Full test suite pass ([req 4](spec.md#requirements))**
  Run `./mvnw test` and confirm all tests pass. Fix any failures.
