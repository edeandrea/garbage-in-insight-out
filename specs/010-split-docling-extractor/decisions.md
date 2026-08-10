# Spec 010: Split DoclingExtractor — Decisions

## 1. [2026-08-10 15:45 EDT]: No shared interface between the two extractors

**Question:** Should `DoclingNaiveExtractor` and `DoclingHybridExtractor`
share a base interface?

**Options considered:**
- A shared interface with a generic type parameter (e.g.,
  `DoclingExtractor<T>` with `Uni<T> extract(Path)`)
- A marker interface with no methods
- No shared interface

**Decision:** No shared interface. The two methods have different
signatures (`Uni<ExtractionResult>` vs `Uni<List<TextSegment>>`) and
no caller needs to hold either interchangeably. A shared interface
would be either an empty marker or force an awkward generic — neither
adds value.

## 2. [2026-08-10 15:45 EDT]: Duplicate JSON_OPTIONS rather than share it

**Question:** The `JSON_OPTIONS` constant (`ConvertDocumentOptions` with
`OutputFormat.JSON`) is currently used by both `extract` and
`extractAndChunk`. How should it be handled after the split?

**Options considered:**
- Extract to a shared utility class
- Duplicate the one-liner in each new class

**Decision:** Duplicate in each class. It's a single-line constant, and
the two classes may diverge in options over time. A shared utility for
one constant is over-engineering.

## 3. [2026-08-10 15:50 EDT]: Rename both test classes to match extractors

**Question:** `DoclingExtractorTest` only tests the naive path, and
`DoclingHybridChunkingTest` tests the hybrid path. Should the test
class names align with the new extractor names?

**Decision:** Yes. Rename `DoclingExtractorTest` to
`DoclingNaiveExtractorTest` and `DoclingHybridChunkingTest` to
`DoclingHybridExtractorTest` for consistent naming.
