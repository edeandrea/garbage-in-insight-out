# Spec 006: Orphaned Chart Text in Hybrid Chunks — Tasks

**Status:** Approved

## Tasks

- [x] 1. **Refactor `captionTextFor` to accept `List<RefItem>` ([req 2](spec.md#requirements))**
  Change `DocItemIndex.captionTextFor(TableItem)` to
  `captionTextFor(List<RefItem>)`. Update callers in
  `DoclingExtractor.buildProvenance()` and `resolvedCaptionFor()` to
  pass `table.getCaptions()` instead of the table itself. Update
  existing `DocItemIndexTest` tests for the new signature. Run tests
  to confirm nothing breaks.

- [x] 2. **Add `orphanedChildrenOf` to DocItemIndex ([req 1](spec.md#requirements))**
  Add `List<BaseTextItem> orphanedChildrenOf(String pictureRef, Set<String> referencedRefs)` — returns text items whose parent `$ref`
  equals `pictureRef` and whose `selfRef` is not in `referencedRefs`.
  Add unit tests: items with matching parent and unreferenced selfRef
  are returned; referenced items are excluded; items with different
  parent are excluded.

- [x] 3. **Create synthetic picture segments in `extractAndChunk()` ([req 2](spec.md#requirements))**
  After building the chunk segments, collect all `docItems` refs into a
  `Set<String>`. Iterate `doc.getPictures()`, call
  `orphanedChildrenOf(pic.getSelfRef(), refs)` for each. If non-empty,
  create a synthetic `TextSegment` with: caption text (resolved via
  `captionTextFor(pic.getCaptions())`) + newline + orphaned children
  text joined by spaces. Metadata: `mode=DOCLING_HYBRID_CHUNK`,
  `page_number`, `element_type=PICTURE`.

- [x] 4. **Add integration test for synthetic picture segments ([req 3](spec.md#requirements))**
  Add `DoclingHybridChunkingTest` test: assert at least one segment
  contains "Patents" or "8%" (Figure 2 chart labels) and has
  `element_type` equal to `PICTURE`. Run full test suite to confirm
  all tests pass.

- [x] 5. **Browser verification ([req 3](spec.md#requirements))**
  Start the app, re-ingest, test both figure questions across all
  modes:
  - Verify all previous questions still answer correctly
  - "What percentage of DocLayNet pages are Patents?" (Mode C should
    answer 8%)
  - "According to Figure 2, which document category is the largest
    in DocLayNet?" (Mode C should answer Financial Reports at 32%)
