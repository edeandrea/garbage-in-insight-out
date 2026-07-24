# Spec 004: Chunk Metadata Enrichment — Decisions

## 1. [2026-07-24 15:26 EDT]: Full caption text for elementLabel

**Question:** Should `elementLabel` store the full caption text (e.g.,
"Table 2: Prediction performance of object detection networks...") or
just the prefix (e.g., "Table 2")?

**Options considered:**
1. Full caption text — more informative, grid column is resizable.
2. Prefix only — short and scannable, requires parsing logic.

**Decision:** Full caption text. The grid column is resizable (spec 002),
so users can expand it to see the full caption when needed.

---

## 2. [2026-07-24 15:26 EDT]: Investigate multiple docItems before deciding

**Question:** When a hybrid chunk references multiple doc items (e.g., a
heading + a paragraph), which element type should we use?

**Decision:** Investigate first. Run a simulation to see how often
multiple `docItems` references occur per chunk and what the typical
patterns are, then decide based on the actual data.

---

## 3. [2026-07-24 15:26 EDT]: Investigate caption overlap patterns before deciding

**Question:** In Mode B, when a naive chunk overlaps a caption text item,
should the chunk inherit that caption's text as its `elementLabel`?

**Decision:** Investigate first, then provide a recommendation based on
the actual overlap patterns in the test document.

---

## 4. [2026-07-24 15:40 EDT]: Validate includeConvertedDoc before building resolver

**Question:** Requirement 2 assumes `includeConvertedDoc(true)` returns
the full `DoclingDocument` alongside hybrid chunks. This is based on the
API model, but hasn't been tested against a live Docling Serve instance.
Should the spec include an explicit validation step?

**Options considered:**
1. Add a separate validation requirement — test the API option with a
   real call before building the cross-referencing logic.
2. Rely on requirement 3 (validate with real data) to cover this
   implicitly.

**Decision:** Add a separate validation requirement (req 3 in the spec).
Test `includeConvertedDoc(true)` with a real Docling Serve call before
building the resolver, since the entire Mode C enrichment depends on it.

---

## 5. [2026-07-24 15:40 EDT]: Mode C elementLabel enrichment from DoclingDocument

**Question:** Mode C already populates `elementLabel` from
`chunk.getCaptions()`. Should the spec also cover resolving table
captions from the included `DoclingDocument` for potentially richer data?

**Options considered:**
1. Mode C is already fine — `chunk.getCaptions()` is sufficient.
2. Investigate during implementation — check if the included
   `DoclingDocument` provides better caption data.

**Decision:** Investigate during implementation. Compare what
`chunk.getCaptions()` returns versus what the `DoclingDocument` table
caption resolution would provide.

---

## 6. [2026-07-24 15:57 EDT]: First doc\_item for Mode C elementType

**Question:** When a hybrid chunk references multiple doc items, which
element type should be used?

**Analysis:** Examined all 74 chunks in `docling-chunk-response.json`:
- 47 chunks (64%) have exactly 1 doc\_item
- 12 chunks have 2 doc\_items
- 8 chunks have 3 doc\_items
- 4 chunks have 4 doc\_items
- 3 chunks have 7 doc\_items
- Doc\_item references use only `#/texts/N` and `#/tables/N` prefixes
- Label distribution when resolved: 30 pure `text`, 22 pure `table`,
  9 `caption+text`, 6 pure `list_item`, plus 6 mixed combos
- First doc\_item's label agrees with majority-vote label in **100%**
  of cases — zero disagreements

**Decision:** Use the first doc\_item's label. Simple, O(1),
deterministic, and produces the same result as majority-vote in
practice.

---

## 7. [2026-07-24 15:57 EDT]: Caption text items in Mode B

**Question:** When a naive chunk overlaps a caption text item
(`label=CAPTION`), should the chunk inherit that caption's text as
its `elementLabel`?

**Analysis:** Caption text items (`label=CAPTION`) are regular entries
in `doc.getTexts()`. They have distinct positions in the concatenated
full text. When `buildProvenance()` iterates text items, captions
already get their own provenance entries with `elementType = "CAPTION"`.
The `NaiveChunker.attachMetadata()` overlap logic already copies
whatever `elementLabel` is set on the overlapping provenance entry.
No special branch or override is needed — the caption just participates
in the same provenance flow as paragraphs and section headers.

In the test document (`docling-convert-response.json`), caption text
items contain text like "Table 1: DocLayNet dataset overview...",
"Table 2: Prediction performance...", etc.

**Decision:** Yes. For any text item with `label == CAPTION`, use
`item.getText()` as `elementLabel`. This falls naturally out of the
general fix with no special handling.

---

## 8. [2026-07-24 15:57 EDT]: Mode C label enrichment not needed

**Question:** Does the DoclingDocument provide richer caption data than
`chunk.getCaptions()` for Mode C?

**Analysis:** The hybrid chunker API's `Chunk.getCaptions()` returns
`List<String>` — already-resolved caption text. The DoclingDocument's
`TableItem.getCaptions()` returns `List<RefItem>` with `$ref` pointers
(e.g., `#/texts/367`) to caption text items. When resolved, these
produce the exact same text strings that `chunk.getCaptions()` already
provides. Verified by cross-referencing the test data: the caption
strings in `docling-chunk-response.json` match the resolved text from
caption text items in `docling-convert-response.json`.

**Decision:** No enrichment needed. `chunk.getCaptions()` already
returns the same data pre-resolved. The DoclingDocument adds nothing
for Mode C `elementLabel`.

---

## 9. [2026-07-24 15:57 EDT]: No fallback if includeConvertedDoc fails

**Question:** If `includeConvertedDoc(true)` doesn't work with the
current Docling Serve version, should we fall back to a separate
`convertFiles()` call?

**Decision:** No. If it doesn't work, fail the validation step (req 3)
and reassess. Don't add complexity for a hypothetical failure.
