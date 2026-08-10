# Spec 010: Split DoclingExtractor into Mode-Specific Extractors

**Status:** Approved

## Summary

Refactor the monolithic `DoclingExtractor` class into two mode-specific
extractors — `DoclingNaiveExtractor` (Mode B) and
`DoclingHybridExtractor` (Mode C).

## Motivation

`DoclingExtractor` currently houses two completely independent code
paths behind `extract` (Mode B) and `extractAndChunk` (Mode C). They
call different Docling Serve endpoints, use different request/response
types, return different result types, and share no private helpers. The
only reason they coexist in one class is history — they were built
incrementally.

Splitting them makes the codebase clearer: each pipeline depends on
exactly the extractor it uses, and a reader can see the full extraction
logic for a mode in one file without mentally filtering out the other
mode's code.

## Requirements

1. `DoclingExtractor` is replaced by two classes:
   - `DoclingNaiveExtractor` — contains the current `extract(Path)`
     method and its private helpers (`buildFullText`, `buildProvenance`,
     `toProvenanceEntry`, `tableToText`).
   - `DoclingHybridExtractor` — contains the current
     `extractAndChunk(Path)` method and its private helper
     (`buildPictureSegment`).
2. Only introduce a shared base interface if there is a real polymorphic
   use case (i.e., a caller that needs to hold a reference to either
   extractor interchangeably). Do not force a shared abstraction when
   the two extractors have different signatures and return types.
3. Both new classes are `@ApplicationScoped` CDI beans with constructor-
   injected `DoclingServeApi`.
4. `DoclingNaiveIngestionPipeline` injects `DoclingNaiveExtractor`
   instead of `DoclingExtractor`.
5. `DoclingHybridIngestionPipeline` injects `DoclingHybridExtractor`
   instead of `DoclingExtractor`.
6. The original `DoclingExtractor` class is deleted.
7. All existing tests continue to pass with no behavioral changes.
8. New or updated tests verify each extractor independently.

## Out of scope

- Changing the Docling Serve API calls or request/response types.
- Modifying `DocItemIndex`, `ExtractionResult`, `ProvenanceEntry`, or
  any other supporting type.
- Refactoring the pipeline hierarchy or `IngestionStartup`.
- Any behavioral changes to extraction or chunking logic.

## Open questions

None.
