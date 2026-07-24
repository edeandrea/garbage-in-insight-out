# Spec 004: Chunk Metadata Enrichment — Implementation Notes

## Task 1: Update CaptureDoclingResponsesTest and re-capture WireMock stub

Add `.includeConvertedDoc(true)` to the `HybridChunkDocumentRequest`
in `captureChunkingResponse()`. Run with
`-Dcapture.docling.responses=true` to regenerate the stub.
Captured 1,135,589 bytes. Verified: 568 texts, 5 tables, caption
`$ref` pointers present.

## Task 2: Create DocItemIndex ref resolution utility

Package-private record in the extraction package. Factory method builds
maps keyed by `getSelfRef()`. `labelFor` returns the label string.
`captionTextFor` resolves a table's first caption ref through the text
map. `DocItemLabel.toString()` returns uppercase (e.g., "PARAGRAPH"),
matching existing behavior in `DoclingExtractor.buildProvenance()`.

## Task 3: Fix Mode B elementLabel in DoclingExtractor

Add `elementLabel` param to `toProvenanceEntry()`. In
`buildProvenance()`, build a `DocItemIndex`, then: caption text items
pass their own text, table items resolve via `captionTextFor()`, all
others pass null. Docling uses `"text"` label (not `"paragraph"`) for
ordinary text items — `DocItemLabel.TEXT.toString()` returns `"TEXT"`.

## Task 5: Enrich Mode C with elementType

Set `includeConvertedDoc(true)` on the request. Extract DoclingDocument
from response, build DocItemIndex, resolve first docItem per chunk.
