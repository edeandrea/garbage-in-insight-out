# Spec 006: Orphaned Chart Text in Hybrid Chunks

**Status:** Approved

## Summary

Mode C's hybrid chunker extracts chart/figure text labels (e.g., pie
chart percentages like "Patents 8%") via Docling but drops them during
chunking. These orphaned text items should be included so Mode C can
answer questions about figure data.

## Motivation

During planted question testing, the question "What percentage of
DocLayNet pages are Patents?" revealed that Modes A and B answer
correctly (8%) while Mode C cannot. Investigation showed:

- Docling's extraction step DOES capture the chart text — the
  `DoclingDocument` contains individual text items for each pie chart
  label ("Patents", "8%", "Scientific", "17%", etc.)
- The hybrid chunker (Docling Serve server-side) does NOT include
  these items in any chunk — they are too small and semantically
  disconnected to be grouped with surrounding content
- Modes A/B handle this because the naive chunker concatenates ALL
  extracted text (including chart labels) before chunking, so the
  labels end up in chunks alongside the Figure 2 caption

Since `includeConvertedDoc(true)` is already set on the hybrid chunk
request (spec 004), the full `DoclingDocument` is available alongside
the chunks. We can detect which text items are not referenced by any
chunk's `docItems` and include them.

## Requirements

1. **Detect orphaned text items:** After receiving the hybrid chunk
   response, compare the `docItems` references across all chunks
   against all text items in the `DoclingDocument`. Identify text items
   whose `selfRef` is not referenced by any chunk.

2. **Include orphaned items:** Append orphaned text items to the nearest
   relevant chunk, or create synthetic segments for them so they are
   ingested into Qdrant and available for retrieval.

3. **Validate with figure questions:** After the fix, Mode C should be
   able to answer "What percentage of DocLayNet pages are Patents?" and
   similar figure/chart questions.

## Out of scope

- Changing the Docling Serve hybrid chunker behavior (server-side)
- Handling orphaned picture/image items (only text items)
- Changing Mode A or B behavior

## Open questions

1. **Inclusion strategy:** Should orphaned items be appended to the
   nearest chunk (by position or page number), or created as standalone
   synthetic segments? Appending keeps the segment count stable;
   standalone segments might rank better for direct questions about
   chart data.

2. **Filtering:** Should all orphaned text items be included, or only
   those with specific labels (e.g., `CAPTION`, chart-related items)?
   Including everything could add noise; filtering might miss relevant
   items.
