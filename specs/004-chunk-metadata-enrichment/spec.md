# Spec 004: Chunk Metadata Enrichment

**Status:** Approved

## Summary

Fix the Mode B `elementLabel` bug and enrich Mode C chunks with
`elementType` metadata, so the UI's chunk grid shows accurate structural
information across all three modes.

## Motivation

[Spec 002 decision #15](../002-ui-polish/decisions.md#15-2026-07-24-1503-edt-defer-metadata-labeltype-investigation-to-separate-spec)
deferred this work from UI polish. Investigation revealed three metadata
gaps:

| Field | Mode A (Naive) | Mode B (Docling + Naive) | Mode C (Docling + Hybrid) |
|-------|:-:|:-:|:-:|
| `pageNumber` | always null | populated | populated |
| `elementType` | always null | populated | **always null** |
| `elementLabel` | always null | **always null (bug)** | populated (from captions) |

- **Mode B `elementLabel` bug:**
  `DoclingExtractor.toProvenanceEntry()` (line 105) hardcodes
  `elementLabel` to `null`. The Docling document model has caption data
  available on `TableItem.getCaptions()` (resolved via `$ref` to text
  items with `label=CAPTION`), but it's never wired through. This was
  called for in [spec 001 decision #6](../001-three-mode-rag-demo/decisions.md#6-2026-07-21-edt-segment-metadata-page_number-element_type-element_label)
  but not implemented.

- **Mode C missing `elementType`:** `extractAndChunk()` never writes
  `element_type` metadata. The `Chunk` response from the hybrid chunker
  API has no dedicated element type field, but it does have a `docItems`
  field (`List<String>`) containing `$ref`-style references (e.g.,
  `#/texts/3`, `#/tables/0`) back into the `DoclingDocument`. By setting
  `includeConvertedDoc(true)` on the `HybridChunkDocumentRequest`, the
  full `DoclingDocument` is returned alongside the chunks, allowing
  element type lookup via `docItems` cross-referencing.

- **Mode A** has no metadata by design — Tika extraction produces no
  provenance.

## Requirements

1. **Fix Mode B `elementLabel`:** For table items, resolve
   `table.getCaptions()` `$ref` references against `doc.getTexts()` to
   populate `elementLabel` with the caption text (e.g., "Table 2:
   Prediction performance..."). For text items that are themselves
   captions (`label=CAPTION`), the text IS the caption — consider
   whether these should populate `elementLabel` on chunks that overlap
   them.

2. **Enrich Mode C with `elementType`:** Set
   `includeConvertedDoc(true)` on the `HybridChunkDocumentRequest`.
   Resolve each `chunk.getDocItems()` reference against the returned
   `DoclingDocument` to look up the element's `label` (a `DocItemLabel`
   enum: `PARAGRAPH`, `TABLE`, `SECTION_HEADER`, etc.) and store it as
   `element_type` metadata on the `TextSegment`.

3. **Validate `includeConvertedDoc(true)` with real Docling Serve:**
   Before building the cross-referencing logic for req 2, verify that
   `includeConvertedDoc(true)` on `HybridChunkDocumentRequest` actually
   returns the `DoclingDocument` alongside chunks with the current
   Docling Serve version. This is based on the API model having the
   field — it hasn't been tested against a live server.

4. **Validate with real data:** Run the ingestion pipeline against the
   actual test document and verify that `elementLabel` and `elementType`
   are populated correctly for Modes B and C. This can use the existing
   gated simulation tests or a new targeted test.

## Out of scope

- Changing Mode A (Tika) metadata — no provenance is available.
- Changing the UI grid columns — spec 002 already handles display.
- Changing the chunking strategy or chunk sizes.
- Adding new metadata fields beyond `elementType` and `elementLabel`.

## Resolved questions

1. **Caption text length:** Store the full caption text. The grid column
   is resizable (spec 002), so users can expand it.

## Open questions (require investigation)

1. **Multiple `docItems` per chunk (Mode C):** A hybrid chunk may
   reference multiple doc items. Need to run a simulation to see how
   often this happens and what the typical patterns are before deciding
   how to pick the element type.

2. **Caption overlap in Mode B:** When a naive chunk overlaps a caption
   text item, should the chunk inherit that caption's text as its
   `elementLabel`? Need to investigate the actual overlap patterns to
   make a recommendation.

3. **Mode C label enrichment:** Mode C already populates `elementLabel`
   from `chunk.getCaptions()`. Investigate during implementation whether
   the included `DoclingDocument` provides richer caption data.
