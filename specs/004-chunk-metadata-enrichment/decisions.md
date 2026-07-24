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
