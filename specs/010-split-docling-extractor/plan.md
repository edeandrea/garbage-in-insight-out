# Plan 010: Split DoclingExtractor into Mode-Specific Extractors

**Status:** Approved

## Approach

Mechanical split — move each public method and its private helpers into
a dedicated class, update all injection sites. No behavioral changes,
no new abstractions.

The two code paths (`extract` / `extractAndChunk`) share no private
helpers and return different types (`Uni<ExtractionResult>` vs
`Uni<List<TextSegment>>`). No caller uses them interchangeably, so
there is no polymorphic use case and no shared interface is needed.

The `JSON_OPTIONS` constant (`ConvertDocumentOptions` with
`OutputFormat.JSON`) is used by both methods. Rather than duplicating
it or extracting a shared utility, each new class defines its own
private copy — it's a one-liner and the two classes may diverge in
options over time.

## Files to create

| File | Purpose |
|---|---|
| `src/main/java/.../extraction/DoclingNaiveExtractor.java` | `extract(Path)` + `buildFullText`, `buildProvenance`, `toProvenanceEntry`, `tableToText` |
| `src/main/java/.../extraction/DoclingHybridExtractor.java` | `extractAndChunk(Path)` + `buildPictureSegment` |

## Files to modify

| File | Change |
|---|---|
| `src/main/java/.../pipeline/DoclingNaiveIngestionPipeline.java` | Inject `DoclingNaiveExtractor` instead of `DoclingExtractor` |
| `src/main/java/.../pipeline/DoclingHybridIngestionPipeline.java` | Inject `DoclingHybridExtractor` instead of `DoclingExtractor` |
| `src/test/.../extraction/DoclingExtractorTest.java` | Rename to `DoclingNaiveExtractorTest.java`, inject `DoclingNaiveExtractor` |
| `src/test/.../extraction/DoclingHybridChunkingTest.java` | Rename to `DoclingHybridExtractorTest.java`, inject `DoclingHybridExtractor` |
| `src/test/.../pipeline/ChunkSizeValidationTest.java` | Inject `DoclingNaiveExtractor` (calls `extract`) |
| `src/test/.../pipeline/ModeAvsModeBTest.java` | Inject `DoclingNaiveExtractor` (calls `extract`) |
| `src/test/.../pipeline/ChunkSizeSimulationTest.java` | Inject `DoclingNaiveExtractor` (calls `extract`) |

## Files to delete

| File | Reason |
|---|---|
| `src/main/java/.../extraction/DoclingExtractor.java` | Replaced by the two new classes |

## Files unchanged

- `CaptureDoclingResponsesTest.java` — injects `DoclingServeApi`
  directly, not `DoclingExtractor`. No changes needed.
- `DocItemIndex.java`, `ExtractionResult.java`, `ProvenanceEntry.java`,
  `TikaExtractor.java`, `NaiveChunker.java` — untouched per spec.

## Tradeoffs considered

1. **Shared interface vs none** — The two methods have different
   signatures (`Uni<ExtractionResult>` vs `Uni<List<TextSegment>>`) and
   no caller needs to hold either interchangeably. A shared interface
   would be an empty marker or force an awkward generic. Omitting it.
2. **Shared `JSON_OPTIONS` constant** — Could extract to a utility
   class. Not worth it for a one-line constant used in two places.
   Each class gets its own private copy.
3. **Rename test classes** — `DoclingExtractorTest` becomes
   `DoclingNaiveExtractorTest` and `DoclingHybridChunkingTest` becomes
   `DoclingHybridExtractorTest`, so both test classes match their
   corresponding extractor.
