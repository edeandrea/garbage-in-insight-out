# Tasks 010: Split DoclingExtractor into Mode-Specific Extractors

**Status:** Approved

- [ ] 1. Create `DoclingNaiveExtractor` with `extract(Path)` and its private helpers (`buildFullText`, `buildProvenance`, `toProvenanceEntry`, `tableToText`), copied from `DoclingExtractor`. `@ApplicationScoped`, constructor-injected `DoclingServeApi`, own `JSON_OPTIONS` constant.
- [ ] 2. Create `DoclingHybridExtractor` with `extractAndChunk(Path)` and its private helper (`buildPictureSegment`), copied from `DoclingExtractor`. `@ApplicationScoped`, constructor-injected `DoclingServeApi`, own `JSON_OPTIONS` constant.
- [ ] 3. Update `DoclingNaiveIngestionPipeline` to inject `DoclingNaiveExtractor` instead of `DoclingExtractor`.
- [ ] 4. Update `DoclingHybridIngestionPipeline` to inject `DoclingHybridExtractor` instead of `DoclingExtractor`.
- [ ] 5. Delete `DoclingExtractor.java`.
- [ ] 6. Rename `DoclingExtractorTest` to `DoclingNaiveExtractorTest` and update it to inject `DoclingNaiveExtractor`.
- [ ] 7. Rename `DoclingHybridChunkingTest` to `DoclingHybridExtractorTest` and update it to inject `DoclingHybridExtractor`.
- [ ] 8. Update `ChunkSizeValidationTest`, `ModeAvsModeBTest`, and `ChunkSizeSimulationTest` to inject `DoclingNaiveExtractor` instead of `DoclingExtractor`.
- [ ] 9. Run full test suite (`./mvnw test -Duse.wiremock.docling=true`) and verify all tests pass.
