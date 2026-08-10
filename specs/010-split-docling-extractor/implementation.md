# Spec 010: Split DoclingExtractor — Implementation Notes

## Task 1: Create DoclingNaiveExtractor

Copy `extract(Path)` and its private helpers (`buildFullText`,
`buildProvenance`, `toProvenanceEntry`, `tableToText`) from
`DoclingExtractor` into a new `DoclingNaiveExtractor` class. Own
`JSON_OPTIONS` constant, `@ApplicationScoped`, constructor-injected
`DoclingServeApi`.

## Task 2: Create DoclingHybridExtractor

Copy `extractAndChunk(Path)` and its private helper
(`buildPictureSegment`) from `DoclingExtractor` into a new
`DoclingHybridExtractor` class. Own `JSON_OPTIONS` constant,
`@ApplicationScoped`, constructor-injected `DoclingServeApi`.

## Tasks 3–5: Update pipelines and delete DoclingExtractor

Mechanical: updated imports and field types in both pipeline classes,
then deleted the original `DoclingExtractor.java`.

## Tasks 6–7: Rename test classes

Renamed `DoclingExtractorTest` → `DoclingNaiveExtractorTest` and
`DoclingHybridChunkingTest` → `DoclingHybridExtractorTest`. Updated
class names and injected types.

## Task 8: Update simulation/validation tests

`ChunkSizeValidationTest` needed both extractors — it tests Mode B
(`extract`) and Mode C (`extractAndChunk`) in the same class. Split
the single `doclingExtractor` field into `doclingNaiveExtractor` and
`doclingHybridExtractor`. `ModeAvsModeBTest` and
`ChunkSizeSimulationTest` only use `extract`, so a straight rename.

## Task 9: Full test suite

82 tests pass, 9 skipped (gated). No stale `DoclingExtractor`
references remain in src/.
