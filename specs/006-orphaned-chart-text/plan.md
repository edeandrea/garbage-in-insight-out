# Spec 006: Orphaned Chart Text in Hybrid Chunks — Technical Plan

**Status:** Approved

## Investigation Findings

Analysis of the re-captured `docling-chunk-response.json` (Docling
`quarkus-docling 1.4.1`) reveals 442 of 543 text items (81.4%) are
orphaned. The key structural finding: **orphaned items whose parent
is a `PictureItem` account for 413 of them** (93%). The remaining 29
are section headers (already used as chunk headings) and page headers
(boilerplate).

The parent `$ref` cleanly distinguishes picture-children from body
text — no hardcoded page numbers needed.

| Picture | Page | Caption | Orphaned Children |
|---------|------|---------|-------------------|
| `#/pictures/0` | 1 | Figure 1: Four examples of complex page layouts... | 305 (OCR of embedded example documents) |
| `#/pictures/1` | 3 | Figure 2: Distribution of DocLayNet pages... | 12 (pie chart labels: "Patents 8%", etc.) |
| `#/pictures/2` | 4 | Figure 3: Corpus Conversion Service annotation... | 22 (UI element labels) |
| `#/pictures/3` | 5 | (none) | 12 (figure sub-labels, hash strings) |
| `#/pictures/4` | 6 | Figure 5: Prediction performance (mAP)... | 20 (axis labels, tick values) |
| `#/pictures/5` | 9 | Figure 6: Example layout predictions... | 42 (OCR from prediction example images) |

All content is legitimate — even the OCR from embedded document
screenshots is real text a user might ask about (e.g., "Which figure
depicts looking back on 175 years of looking forward?"). No filtering
needed — let vector search rank by relevance.

## Architecture

Create **one synthetic segment per picture** that has orphaned
children. Each segment contains the picture's caption (if any)
followed by the concatenated orphaned children text, separated by
newlines. This gives the embedding model context (from the caption) to
rank the segment appropriately for questions.

In `DoclingExtractor.extractAndChunk()`, after building the chunk
segments from the hybrid chunker response:

1. Collect all `docItems` refs across all chunks into a `Set<String>`
2. For each `PictureItem` in the `DoclingDocument`, find its orphaned
   text children (parent `$ref` matches the picture's `selfRef`, and
   the child's `selfRef` is not in the referenced set)
3. If orphaned children exist, create a synthetic `TextSegment` with:
   - Text: caption text (if any) + newline + orphaned children text
     joined by spaces
   - Metadata: `mode=DOCLING_HYBRID_CHUNK`, `page_number` (from
     picture's provenance), `element_type=PICTURE`
4. Add synthetic segments to the returned list

## Files to modify

| File | Summary |
|------|---------|
| `DocItemIndex.java` | Add method to find orphaned children of a picture item. |
| `DoclingExtractor.java` | Add post-processing in `extractAndChunk()` to create synthetic picture segments. |
| `DoclingHybridChunkingTest.java` | Add test verifying synthetic segments contain chart label text. |

## Approach

### DocItemIndex additions

Add a method:

```java
List<BaseTextItem> orphanedChildrenOf(String pictureRef,
                                       Set<String> referencedRefs)
```

Returns text items whose parent `$ref` equals `pictureRef` and whose
`selfRef` is not in `referencedRefs`.

Refactor `captionTextFor(TableItem)` to accept `List<RefItem>` instead,
so it works for both tables and pictures:

```java
Optional<String> captionTextFor(List<RefItem> captions)
```

Callers change from `captionTextFor(table)` to
`captionTextFor(table.getCaptions())`. The existing call site in
`DoclingExtractor.buildProvenance()` and `resolvedCaptionFor()` will
need updating to pass `table.getCaptions()` instead of the table
itself.

### DoclingExtractor changes

After the existing chunk-to-segment mapping in `extractAndChunk()`:

1. Build `referencedRefs` set from all chunks' `docItems`
2. Iterate `doc.getPictures()`
3. For each picture, call `index.orphanedChildrenOf(pic.selfRef, refs)`
4. If non-empty, resolve the caption text (via `index.captionTextFor`)
   and build a synthetic `TextSegment`
5. Add to the segment list

### What about non-picture orphans?

The 29 body-parented orphans (18 section headers + 11 page headers)
are intentionally excluded:
- **Section headers** are already used as chunk `headings` metadata
- **Page headers** are boilerplate running headers

No action needed for these.

## Test approach

- `DocItemIndexTest`: Add test for `orphanedChildrenOf()` method
- `DoclingHybridChunkingTest`: Assert that at least one segment
  contains "Patents" or "8%" (Figure 2 chart labels) and has
  `element_type=PICTURE`

## Alternatives considered

| Alternative | Why rejected |
|-------------|-------------|
| Filter by child count heuristic | Arbitrary threshold, not generalizable |
| Hardcode excluded pages | Breaks for different documents |
| Filter by label type | All orphans have label `text` — can't distinguish |
| Append to nearest existing chunk | Mutates chunk text, changes embeddings |

## Verification

1. Run tests with WireMock stubs
2. Start the app, re-ingest, test "What percentage of DocLayNet pages
   are Patents?" — Mode C should now answer correctly
