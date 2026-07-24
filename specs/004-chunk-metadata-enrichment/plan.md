# Spec 004: Chunk Metadata Enrichment — Technical Plan

**Status:** Approved

## Investigation Findings (resolving the 3 open questions)

### Multiple `docItems` per chunk (Mode C)

Analysis of the existing `docling-chunk-response.json` (74 chunks):
- 47 chunks (64%) have exactly 1 doc\_item
- The remaining have 2–7 doc\_items each
- The first doc\_item's label and the majority-vote label agree in
  100% of cases in this test data

**Recommendation:** Use the first doc\_item's label. Simple, O(1),
deterministic, and produces the same result as majority-vote in
practice.

### Caption overlap in Mode B

Caption text items (`label=CAPTION`) in the Docling document model are
regular text items that appear in `doc.getTexts()`. When
`buildProvenance()` iterates text items, captions get their own
provenance entries with `elementType = "CAPTION"`. The fix naturally
extends to these: for any text item where `label == CAPTION`, set
`elementLabel` to the item's own text. No special handling needed —
it falls out of the general approach.

### Mode C label enrichment from DoclingDocument

`chunk.getCaptions()` returns pre-resolved caption text strings
(`List<String>`). The DoclingDocument's table captions are `$ref`
references to text items whose text matches what `chunk.getCaptions()`
returns. The data is equivalent — no enrichment needed for
`elementLabel` in Mode C.

## Architecture

### DocItemIndex — shared ref resolution utility

Both fixes require resolving `$ref`-style paths (e.g., `#/texts/367`,
`#/tables/0`) against a `DoclingDocument`. Create a package-private
record `DocItemIndex` in the extraction package:

```java
record DocItemIndex(
    Map<String, BaseTextItem> textsByRef,
    Map<String, TableItem> tablesByRef
) {
    static DocItemIndex of(DoclingDocument doc) { ... }
    Optional<String> labelFor(String ref) { ... }
    Optional<String> captionTextFor(String tableRef) { ... }
}
```

- `of(doc)` — builds both maps from `doc.getTexts()` and
  `doc.getTables()`, keyed by `getSelfRef()`
- `labelFor(ref)` — returns the element's label as a string (from
  `BaseTextItem.getLabel().toString()` or `TableItem.getLabel()`)
- `captionTextFor(tableRef)` — resolves a table's `getCaptions()` refs
  through `textsByRef` to get the first caption's text

Per-document scope (created via factory method), not a CDI bean —
the index is tied to one specific `DoclingDocument` and short-lived.

## Files to modify

| File | Summary |
|------|---------|
| `DocItemIndex.java` **(new)** | Package-private record with ref resolution utilities. |
| `DoclingExtractor.java` | Mode B: add `elementLabel` param to `toProvenanceEntry()`, resolve table captions and caption text items via `DocItemIndex`. Mode C: set `includeConvertedDoc(true)`, resolve `chunk.getDocItems()` for element type. |
| `CaptureDoclingResponsesTest.java` | Update `captureChunkingResponse()` to set `includeConvertedDoc(true)`. |
| `docling-chunk-response.json` | Re-captured with `includeConvertedDoc=true` (will include embedded `DoclingDocument`). |
| `DoclingExtractorTest.java` | Add assertions for `elementLabel` on table and caption provenance entries. |
| `DoclingHybridChunkingTest.java` | Add assertions for `element_type` metadata on segments. |
| `NaiveChunkerTest.java` | Update test data to use non-null `elementLabel` in `ProvenanceEntry`. |

**No changes needed:** `ProvenanceEntry.java` (already has
`elementLabel` field), `NaiveChunker.java` (already copies
`elementLabel` when non-null), `ChunkMapper.java`, `ChunkMetadata.java`,
pipeline classes, WireMock mapping stubs, `pom.xml`.

## Approach per requirement

### Req 1: Fix Mode B `elementLabel`

Add `elementLabel` parameter to `toProvenanceEntry()`:

```java
private Optional<ProvenanceEntry> toProvenanceEntry(
    String itemText, String elementType, String elementLabel,
    List<ProvenanceItem> provItems, String fullText)
```

In `buildProvenance()`:
- Build a `DocItemIndex` from the `DoclingDocument`
- **Text items with `label == CAPTION`:** pass `item.getText()` as
  `elementLabel` — the item IS the caption
- **Table items:** resolve `table.getCaptions()` refs via
  `DocItemIndex.captionTextFor()` to get caption text
- **All other text items:** pass `null` — paragraphs, section headers,
  etc. have no caption

### Req 2: Enrich Mode C with `elementType`

In `extractAndChunk()`:
1. Set `includeConvertedDoc(true)` on `HybridChunkDocumentRequest`
2. Extract `DoclingDocument` from
   `response.getDocuments().getFirst().getContent().getJsonContent()`
3. Build a `DocItemIndex` from the document
4. For each chunk, resolve the first `chunk.getDocItems()` ref via
   `DocItemIndex.labelFor()` and store as `element_type` metadata

### Req 3: Validate `includeConvertedDoc(true)`

Update `CaptureDoclingResponsesTest.captureChunkingResponse()` to set
`includeConvertedDoc(true)`. Run against a live Docling Serve instance
to generate the updated WireMock stub. Verify the response contains
`documents[0].content.json_content` with texts and tables.

### Req 4: Validate with real data

Run the full ingestion pipeline against the test document (with real
Docling Serve, not WireMock) and verify that:
- Mode B chunks have non-null `elementLabel` for table-related and
  caption-related chunks
- Mode C chunks have non-null `elementType`
- Mode A remains unchanged (all null — by design)

## Implementation order

| Step | Req | Description |
|------|-----|-------------|
| 1 | 3 | Update `CaptureDoclingResponsesTest` to use `includeConvertedDoc(true)`. Re-capture WireMock stub. |
| 2 | — | Create `DocItemIndex` record with ref resolution utilities. |
| 3 | 1 | Fix `toProvenanceEntry()` and `buildProvenance()` for Mode B `elementLabel`. |
| 4 | 1 | Update `NaiveChunkerTest` and `DoclingExtractorTest` for Mode B fix. |
| 5 | 2 | Update `extractAndChunk()` for Mode C `elementType`. |
| 6 | 2 | Update `DoclingHybridChunkingTest` for Mode C fix. |
| 7 | 4 | Full test suite pass. End-to-end validation with real Docling Serve. |

## Test approach

**Unit tests:**
- `NaiveChunkerTest`: update `attachesProvenanceMetadataWhenPresent()`
  to use a `ProvenanceEntry` with non-null `elementLabel`. Verify it
  flows through to segment metadata.

**Integration tests (WireMock):**
- `DoclingExtractorTest`: assert that provenance entries for table items
  have non-null `elementLabel` (caption text). Assert caption text items
  have `elementLabel` equal to their own text. Assert ordinary text items
  have null `elementLabel`.
- `DoclingHybridChunkingTest`: assert segments have `element_type`
  metadata. Verify expected distribution of labels (TEXT, TABLE,
  LIST\_ITEM, CAPTION, etc.).

**Existing tests that must continue to pass unchanged:**
- `ChunkMapperTest`, `ChunkSizeValidationTest`

## Alternatives considered

| Alternative | Why rejected |
|-------------|-------------|
| Majority-vote for Mode C elementType | Agrees with first-item in 100% of test data. First-item is simpler and O(1). |
| Static methods on DoclingExtractor instead of DocItemIndex record | Scatters map-building logic. Record encapsulates the two maps as a cohesive unit. |
| Separate WireMock stub for with/without includeConvertedDoc | Only one endpoint path; production code will always use `includeConvertedDoc=true`. Two stubs adds complexity with no benefit. |
| Enriching Mode C elementLabel from DoclingDocument | `chunk.getCaptions()` already returns the same data pre-resolved. No improvement possible. |
