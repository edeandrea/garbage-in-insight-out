# Implementation 001: Three-Mode RAG Demo

Implementation details per task, written before each task is
implemented and updated if the approach changes during implementation.

---

## Task 1: Add POM dependencies

### Version properties to add

| Property | Value | Source |
|----------|-------|--------|
| `vaadin.version` | `25.2.3` | [Maven Central](https://central.sonatype.com/artifact/com.vaadin/vaadin-bom) |
| `quarkus-docling.version` | `1.4.0` | [Maven Central](https://central.sonatype.com/artifact/io.quarkiverse.docling/quarkus-docling) |
| `mapstruct.version` | `1.6.3` | [Maven Central](https://central.sonatype.com/artifact/org.mapstruct/mapstruct) |
| `browserless-test.version` | `1.1.1` | [Maven Central](https://central.sonatype.com/artifact/com.vaadin/browserless-test-quarkus) |

### BOMs to add to dependencyManagement

Order: quarkus-bom (existing), then langchain4j-bom, then vaadin-bom,
then browserless-test-bom.

| groupId | artifactId | version | notes |
|---------|-----------|---------|-------|
| `io.quarkus.platform` | `quarkus-langchain4j-bom` | `${quarkus.platform.version}` | Manages all `io.quarkiverse.langchain4j` and `dev.langchain4j` deps |
| `com.vaadin` | `vaadin-bom` | `${vaadin.version}` | Manages Vaadin deps |
| `com.vaadin` | `browserless-test-bom` | `${browserless-test.version}` | Manages browserless test deps |

### Dependencies to add

| groupId | artifactId | version | scope | notes |
|---------|-----------|---------|-------|-------|
| `io.quarkiverse.langchain4j` | `quarkus-langchain4j-openai` | (BOM) | compile | OpenAI-compatible LLM/embedding |
| `io.quarkiverse.langchain4j` | `quarkus-langchain4j-pgvector` | (BOM) | compile | pgvector embedding store |
| `io.quarkiverse.langchain4j` | `quarkus-langchain4j-ollama` | (BOM) | **provided** | Dev Services only |
| `dev.langchain4j` | `langchain4j-document-parser-apache-tika` | (BOM) | compile | Mode A extraction |
| `io.quarkiverse.docling` | `quarkus-docling` | `${quarkus-docling.version}` | compile | Modes B/C extraction |
| `com.vaadin` | `vaadin-quarkus-extension` | (BOM) | compile | Vaadin UI |
| `io.quarkus` | `quarkus-config-yaml` | (BOM) | compile | YAML config |
| `org.mapstruct` | `mapstruct` | `${mapstruct.version}` | compile | Type-safe mapping |
| `com.vaadin` | `browserless-test-quarkus` | (BOM) | **test** | Vaadin UI testing |

### maven-compiler-plugin changes

Add MapStruct annotation processor path (no compiler args — CDI
component model is specified per-mapper via
`@Mapper(componentModel = MappingConstants.ComponentModel.CDI)`,
see decision 39):

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
    </path>
</annotationProcessorPaths>
```

### quarkus-maven-plugin

Check if `<extensions>true</extensions>` already handles Vaadin's
`generate-code` and `generate-code-tests` goals. If not, add explicit
execution entries per the Vaadin Quarkus docs.

### quarkus-maven-plugin

Added explicit `generate-code` and `generate-code-tests` goals per
[Vaadin Quarkus docs](https://vaadin.com/docs/latest/flow/integrations/quarkus).

### Additional dependencies discovered during implementation

- `org.jboss.slf4j:slf4j-jboss-logmanager` — required by Vaadin/Quarkus
  integration (Quarkus-managed version, no explicit version needed)

### Notes

- Do NOT add `vaadin-jandex` — Pro components only
- `quarkus-langchain4j-ollama` scope is `provided` ([decision 28](decisions.md#28-2026-07-22-1235-edt-ollama-dev-services-via-provided-scope-dependency))
- `quarkus-docling` needs explicit version — not BOM-managed
- `vaadin-quarkus-extension` needs explicit version `${vaadin.version}` —
  not managed by the Vaadin BOM
- All versions confirmed on central.sonatype.com (2026-07-22)

### Verification

`./mvnw compile` — **PASSED** (2026-07-22)

---

## Task 2: Update CI workflow with Ollama service container

### Approach

Based on the pattern from `cescoffier/langchain4j-deep-dive`:

1. Add `services` block with `ollama/ollama` container on port 11434
2. Add a "Pull Ollama models" step before the build that pulls
   `qwen3:4b` (lighter CI model, [decision 26](decisions.md)) and
   `nomic-embed-text` via `docker exec`
3. Pass `-Dquarkus.langchain4j.openai.chat-model.model-name=qwen3:4b`
   to the Maven build to override the default LLM model

### File to modify

`.github/workflows/build.yml`

### Verification

Verify YAML syntax is valid (no runtime verification possible without
pushing to GitHub).

---

## Task 3: Add application.yml configuration

### Approach

Replace empty `application.properties` with `application.yml`. Structure:

- Default profile: `provider=openai`, OpenAI config pointing to Ollama
  `/v1` endpoint for production flexibility
- `%dev,test` profile: `provider=ollama`, Ollama Dev Services auto-start
  and pull models ([CLAUDE.md](../../CLAUDE.md) convention: use `%dev,test`
  combined syntax)
- Three named pgvector stores with `default-store-enabled=false`
- Dimension 768 (matches `nomic-embed-text`)

### Files

- Delete: `src/main/resources/application.properties`
- Create: `src/main/resources/application.yml`

### Verification

`./mvnw compile` must succeed.

---

## Task 4: Create Mode enum and model package value objects

### Package

`dev.ericdeandrea.docling.model`

### Classes

- `Mode` — enum with three values, each mapping to its pgvector store
  name and a display label
- `ChunkMetadata` — record: pageNumber (Integer, nullable for Mode A),
  elementType (String, nullable), elementLabel (String, nullable),
  mode (Mode), relevanceScore (double), timestamp (Instant)
- `RetrievedChunk` — record: text (String), metadata (ChunkMetadata)
- `ChatResponseEvent` — sealed interface with subtypes (added in
  [task 16](tasks.md#task-16) but defining the interface here since
  it's a model-package type):
  - Deferred to task 16 — only create Mode, ChunkMetadata,
    RetrievedChunk now

### Notes

- No tests needed — simple data-carrying types
  ([CLAUDE.md](../../CLAUDE.md) convention)
- No LangChain4j types in this package

---

## Task 5: Create MapStruct mapper skeleton

### Package

`dev.ericdeandrea.docling.mapping`

### Classes

- `ChunkMapper` — `@Mapper(componentModel = MappingConstants.ComponentModel.CDI)`
  interface ([decision 39](decisions.md)). Maps LangChain4j
  `TextSegment` + metadata to `RetrievedChunk`/`ChunkMetadata`.
  Custom `@Mapping` annotations to extract metadata keys
  (`page_number`, `element_type`, `element_label`, `mode`) from
  `TextSegment.metadata()`.

### Test

Unit test in `src/test/java/dev/ericdeandrea/docling/mapping/` that
creates a `TextSegment` with metadata, runs it through the mapper, and
asserts the `ChunkMetadata` fields are correctly populated. Also test
the case where metadata keys are missing (Mode A — no page info).

---

## Task 6: Create retrieval config mapping

### Package

`dev.ericdeandrea.docling.ai`

### Class

`RagConfig` — `@ConfigMapping(prefix = "rag")` interface with:
- `topK()` — int, default 4
- `maxTokens()` — int (for sentence splitter), no default yet (TBD
  during [task 12](tasks.md) chunk size validation)
- `overlap()` — int (for sentence splitter), no default yet
- `fixturePath()` — String, default `fixtures/doclaynet-2206.01062v1.pdf`

### Config in application.yml

Add `rag` section with defaults.

### Test

`@QuarkusTest` that injects `RagConfig` and asserts defaults.

---

## Task 7: Implement ExtractionStrategy and TikaExtractor

### Package

`dev.ericdeandrea.docling.ai.ingestion`

### Classes

- `ExtractionResult` — record holding a LangChain4j `Document` + an
  optional provenance map (`Map<String, ProvenanceEntry>` or similar).
  Internal AI-layer type, not a boundary type
  ([decision 23](decisions.md)).
- `ExtractionStrategy` — interface: `ExtractionResult extract(Path documentPath)`
- `TikaExtractor` — CDI bean wrapping `ApacheTikaDocumentParser`. Parses
  the PDF, returns `ExtractionResult` with the `Document` and empty
  provenance (Mode A has no page tracking).

### Test

`@QuarkusTest` that injects `TikaExtractor`, extracts the fixture PDF,
asserts the result has non-empty text and empty provenance.

---

## Task 8: Implement DoclingExtractor (conversion endpoint, Mode B)

### API calls

Use `DoclingService.convertFile(path, OutputFormat.JSON)` which returns
`InBodyConvertDocumentResponse`. Access `document.jsonContent` to get
the `DoclingDocument` Java object.

### Building the provenance map

Iterate over the `DoclingDocument`'s text items (body items). Each has:
- `List<ProvenanceItem> prov` → `pageNo`, `charspan`
- A label from `DocItemLabel` enum (TABLE, PARAGRAPH, CAPTION, etc.)

Build `List<ProvenanceEntry>` from these, mapping `charspan[0]`/`[1]`
to `startChar`/`endChar`.

### Element labels

Tables and figures may have labels like "Table 2" or "Figure 3" that
need to be extracted from the item's text content or captions. The
exact API for this depends on how `DoclingDocument` structures
table/figure items — check during implementation.

### Provenance map scope

The provenance map is used by `NaiveChunker` (task 10) to post-process
segments. It maps character ranges in the extracted text to page/element
metadata.

### Test

`@QuarkusTest` (needs Docling dev services) that converts the fixture
PDF and asserts provenance entries exist with page numbers.

---

## Task 9: Extend DoclingExtractor for hybrid chunking (Mode C)

### API call

Use `DoclingService.chunkFileHybrid(path, OutputFormat.JSON)` which
returns `ChunkDocumentResponse`. Each `Chunk` has:
- `text` — chunk text with structural context
- `pageNumbers` — `List<Integer>` of pages this chunk spans
- `headings` — section headings
- `captions` — captions for tables/pictures
- `docItems` — doc item references

### Approach

Add a `extractAndChunk(Path)` method to `DoclingExtractor` that returns
`List<TextSegment>` directly (bypasses `ChunkingStrategy` — [decision
19](decisions.md)). Each `TextSegment` gets metadata:
- `page_number` — first page from `chunk.pageNumbers`
- `element_type` — inferred from chunk content/docItems
- `element_label` — from captions if present
- `mode` — `DOCLING_HYBRID_CHUNK`

### Test

`@QuarkusTest` that chunks the fixture PDF via Docling and asserts
chunks are returned with page metadata.

---

## Task 10: Implement ChunkingStrategy and NaiveChunker

### Package

`dev.ericdeandrea.docling.ai.ingestion`

### Classes

- `ChunkingStrategy` — interface: `List<TextSegment> chunk(ExtractionResult result, Mode mode)`
  Used by modes A and B only.
- `NaiveChunker` — CDI bean implementing `ChunkingStrategy`:
  1. Chunk the `Document` from `ExtractionResult` with
     `DocumentBySentenceSplitter(maxTokens, overlap)`
  2. Enrich with `collectTextSegmentAndExtendedContent(segments, 2, 2)`
  3. Attach `mode` metadata to each segment
  4. If `ExtractionResult.hasProvenance()` (Mode B), post-process each
     segment to map its text position back to the provenance entries
     for `page_number`, `element_type`, `element_label`

### Config

Inject `RagConfig` for `maxTokens` and `overlap` values.

### Test

Unit test with synthetic text verifying:
- Segments produced with `mode` metadata
- Provenance post-processing attaches page metadata when available
- Missing provenance (Mode A) skips post-processing

---

## Task 11: Implement IngestionStartup with ingestion guard

### Approach

CDI bean that runs at startup. For each mode:
1. Check if the named pgvector store has data — use
   `EmbeddingStore.search()` with a zero vector and `maxResults(1)`.
   If results exist, skip ([decision 41](decisions.md)).
2. If empty, run the pipeline:
   - Modes A/B: `ExtractionStrategy.extract()` → `ChunkingStrategy.chunk()`
     → `EmbeddingStoreIngestor.ingest()`
   - Mode C: `DoclingExtractor.extractAndChunk()` → embed → store

### Injected beans

- `TikaExtractor` — for Mode A
- `DoclingExtractor` — for Modes B/C
- `NaiveChunker` — for Modes A/B
- Three named `EmbeddingStore<TextSegment>` via `@EmbeddingStoreName`
- `EmbeddingModel` — for computing embeddings
- `RagConfig` — for fixture path

### Test

`@QuarkusTest` that verifies the ingestion guard skips re-ingestion
when the store has data.

---

## Task 12: Chunk size validation (Table 2 fragmentation)

### Investigation method

Wrote diagnostic tests to extract the fixture PDF and inspect how
Table 2 content appears in each mode's chunks:

1. **`ChunkSizeSimulationTest`** — ran the sentence splitter at
   maxTokens 100/150/200/250/300, searched for chunks containing
   Table 2's key values (76.8, 73.4).

2. **`ModeAvsModeBTest`** — compared Mode A (Tika) and Mode B
   (Docling + same chunker) side by side, looking at chunks containing
   Table 2 markers.

### Findings

**Original hypothesis did not hold:** The sentence splitter never
fragments the `All` row (`All | 82-83 | 72.4 | 73.5 | 73.4 | 76.8`,
~45 chars) at any chunk size. Sentence boundaries don't fall within
pipe-separated table rows.

**Better fragmentation found:** Column headers (MRCNN R50, MRCNN R101,
FRCNN R101, YOLOv5x6) are in a different chunk from the data values.
Mode B gets `73.4 | 76.8` but the LLM doesn't know which value is
FRCNN vs YOLO — the column-header-to-value relationship is broken.

### Three-mode progression

| Mode | Table 2 chunk content | Metadata | LLM can answer? |
|------|----------------------|----------|-----------------|
| A (Tika) | Garbled: values run together, unrelated body text spliced in, no pipe separators | None | No |
| B (Docling + naive) | Clean pipe-separated values, but column headers in a different chunk | page 6, TABLE | No — doesn't know which value is FRCNN vs YOLO |
| C (Docling hybrid) | Self-describing triplets: `All, FRCNN.R101 = 73.4. All, YOLO.v5x6 = 76.8` | page 6 | Yes — column names inline with values |

### Decision

Keep `maxTokens=300`, `overlap=30`. The column-header separation
failure mode is more realistic and common than artificial row splitting.
See [decision 48](decisions.md) for the full rationale.

### Diagnostic test files

- `src/test/java/.../ChunkSizeSimulationTest.java` — chunk size sweep
- `src/test/java/.../ChunkSizeValidationTest.java` — initial Table 2 inspection
- `src/test/java/.../ModeAvsModeBTest.java` — A vs B comparison
