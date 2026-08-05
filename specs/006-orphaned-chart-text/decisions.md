# Spec 006: Orphaned Chart Text — Decisions

## 1. [2026-08-05 14:08 EDT]: No filtering — include all picture-children orphans

**Question:** Should we filter orphaned text items by label, child
count, page number, or content layer to separate useful chart labels
from OCR noise?

**Analysis:** With `quarkus-docling 1.4.1`, all 413 picture-parented
orphans have `label=text` and `contentLayer=body`. There is no
structural property that distinguishes "Patents 8%" (useful chart data)
from "eyepiece adjustment for viewing" (OCR from an embedded screenshot).
Filtering heuristics (child count thresholds, page exclusions) would be
arbitrary and break for different documents.

**Decision:** Include ALL orphaned picture-children. The "noise" is
actually legitimate content — someone might ask about text visible in
embedded document screenshots (e.g., "Which figure depicts looking back
on 175 years of looking forward?"). Vector search at retrieval time
will rank relevant content higher. No filtering, no hardcoding.

---

## 2. [2026-08-05 14:08 EDT]: One synthetic segment per picture

**Question:** How should orphaned items be included — appended to
existing chunks, or as standalone synthetic segments?

**Decision:** One synthetic segment per picture, containing the
picture's caption (prepended for embedding context) + all orphaned
children text. This keeps existing chunk embeddings unchanged and
groups related figure data together. The caption provides semantic
context so the embedding model can rank the segment appropriately.

---

## 3. [2026-08-05 14:08 EDT]: Use parent $ref for filtering, not hardcoded pages

**Question:** How to distinguish OCR'd figure text from regular
document text without hardcoding page numbers?

**Analysis:** With `quarkus-docling 1.4.1`, orphaned text items whose
parent `$ref` points to a `PictureItem` (`#/pictures/N`) account for
413 of 442 orphans (93%). The remaining 29 are body-parented section
headers (already in chunk headings) and page headers (boilerplate).

**Decision:** Filter by parent `$ref` — items with parent
`#/pictures/*` are figure-related. No page number hardcoding needed.
This is generalizable to any document.
