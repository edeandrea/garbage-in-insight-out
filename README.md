# Garbage In, Insight Out

Demo application for the talk *"Garbage In, Insight Out: Document Intelligence for AI-Infused Java Applications"*. It demonstrates how document extraction quality and chunking strategy directly affect RAG (Retrieval-Augmented Generation) answer accuracy, using Quarkus, LangChain4j, and Docling Java.

## Three Modes

The app runs the same RAG pipeline three times, changing exactly one variable each time:

| Mode | Extractor | Chunker | What it proves |
|------|-----------|---------|----------------|
| **A** (Naive) | Apache Tika (plain text) | Sentence splitter + context enrichment | Bad extraction ruins answers even with a good chunker |
| **B** (Docling + Naive Chunk) | Docling (structured extraction) | Same sentence splitter as A | Better extraction = better answers, same chunker |
| **C** (Docling + Hybrid Chunk) | Docling (structured extraction) | Docling hybrid chunker (server-side) | Structure-aware chunking = even better answers |

## Demo Flow

1. **Cold open (Mode A only):** Ask a question about the DocLayNet paper. Get a wrong answer. Blame the model — then reveal the real cause: garbled extraction.
2. **Closing verdict (Mode A vs B side-by-side):** Same question, right answer this time. Extraction is the only thing that changed.
3. **Advanced payoff (Mode B vs C):** A question that only Mode C answers correctly, because the hybrid chunker preserves table structure that the naive chunker loses.

## Prerequisites

- Java 25 (Temurin recommended)
- Maven 3.9+ (wrapper included: `./mvnw`)
- Docker or Podman (for Quarkus dev services: Qdrant, PostgreSQL, Docling Serve, Ollama)
- Ollama with models pulled (for dev mode — dev services handle this automatically):
  - `qwen3:30b-a3b` (LLM, ~16 GB)
  - `nomic-embed-text` (embeddings, 274 MB)

## Running

### Dev mode

```shell
./mvnw quarkus:dev
```

Opens at [localhost:8080](http://localhost:8080). Dev services automatically start Qdrant, Docling Serve, and Ollama containers. First startup is slow (model pulling + document ingestion).

### Tests

```shell
./mvnw test
```

Tests use WireMock to stub the LLM chat endpoint (no real LLM needed for most tests). Embeddings use real Ollama via dev services. Diagnostic/simulation tests are skipped by default.

### Diagnostic tests

```shell
./mvnw test -Drun.simulations=true
```

Runs simulation tests that output chunk analysis to the console for manual inspection. Requires a real Docling Serve instance (dev services will start one).

- [`ChunkSizeSimulationTest`](src/test/java/dev/ericdeandrea/docling/ai/ingestion/pipeline/ChunkSizeSimulationTest.java) — splits the document at multiple `maxTokens` values and reports whether Table 2 values get fragmented. This is how `maxTokens=300` was determined.
- [`ModeAvsModeBTest`](src/test/java/dev/ericdeandrea/docling/ai/ingestion/pipeline/ModeAvsModeBTest.java) — side-by-side comparison of Mode A (Tika) vs Mode B (Docling) chunks for Table 2. Shows the quality difference from extraction alone.

### Planted questions validation

```shell
./mvnw test -Drun.planted-questions=true
```

Runs [`PlantedQuestionsValidationTest`](src/test/java/dev/ericdeandrea/docling/ai/PlantedQuestionsValidationTest.java), which needs a real LLM (not WireMock). Skipped by default.

### Capturing Docling responses

```shell
./mvnw test -Dcapture.docling.responses=true
```

Runs [`CaptureDoclingResponsesTest`](src/test/java/dev/ericdeandrea/docling/ai/ingestion/extraction/CaptureDoclingResponsesTest.java), which calls a real Docling Serve instance and writes the JSON responses to `src/test/resources/__files/`. These captured files are what the WireMock stubs serve back during tests. Run this when upgrading Docling Serve or changing the fixture PDF.

### WireMock testing

Tests use [WireMock](https://docs.quarkiverse.io/quarkus-wiremock/dev/index.html) for fast, deterministic test execution:

- **LLM chat** — always stubbed in test profile via [`openai-chat-completions.json`](src/test/resources/mappings/openai-chat-completions.json). Real Ollama not needed for chat in tests.
- **Docling Serve** — conditionally stubbed via [`DoclingWiremockTestProfile`](src/test/java/dev/ericdeandrea/docling/DoclingWiremockTestProfile.java). Pass `-Duse.wiremock.docling=true` to use stubs (CI does this); omit for real Docling (local default).
- **Embeddings** — always real Ollama (via dev services). Needed for meaningful vector search.

## Architecture

```
dev.ericdeandrea.docling
├── ai/                           # AI service, RAG augmentor, mode selection
│   └── ingestion/                # Ingestion orchestrator
│       ├── extraction/           # How documents are extracted
│       │   ├── TikaExtractor         (Mode A)
│       │   └── DoclingExtractor      (Modes B/C)
│       ├── chunking/             # How text is split
│       │   └── NaiveChunker          (Modes A/B)
│       └── pipeline/             # Mode compositions (self-documenting)
│           ├── TikaNaiveIngestionPipeline        (Mode A)
│           ├── DoclingNaiveIngestionPipeline     (Mode B)
│           └── DoclingHybridIngestionPipeline    (Mode C)
├── mapping/                      # MapStruct mappers (AI ↔ model boundary)
├── model/                        # Value objects (records) — no framework types
└── ui/                           # Vaadin chat views
```

The AI and UI layers are decoupled via the [`model`](src/main/java/dev/ericdeandrea/docling/model/) package. LangChain4j types never cross into the UI layer. [`MapStruct mappers`](src/main/java/dev/ericdeandrea/docling/mapping/ChunkMapper.java) handle conversion at the boundary.

### Key classes

- [`AssistantService`](src/main/java/dev/ericdeandrea/docling/ai/AssistantService.java) — public chat API; maps LangChain4j events to model types
- [`ChatService`](src/main/java/dev/ericdeandrea/docling/ai/ChatService.java) — package-private `@RegisterAiService` with RAG streaming
- [`ModeAwareRetrievalAugmentor`](src/main/java/dev/ericdeandrea/docling/ai/ModeAwareRetrievalAugmentor.java) — selects Qdrant collection per mode
- [`IngestionStartup`](src/main/java/dev/ericdeandrea/docling/ai/ingestion/IngestionStartup.java) — startup orchestrator, iterates over pipelines
- [`TikaNaiveIngestionPipeline`](src/main/java/dev/ericdeandrea/docling/ai/ingestion/pipeline/TikaNaiveIngestionPipeline.java) / [`DoclingNaiveIngestionPipeline`](src/main/java/dev/ericdeandrea/docling/ai/ingestion/pipeline/DoclingNaiveIngestionPipeline.java) / [`DoclingHybridIngestionPipeline`](src/main/java/dev/ericdeandrea/docling/ai/ingestion/pipeline/DoclingHybridIngestionPipeline.java) — mode pipelines
- [`TikaExtractor`](src/main/java/dev/ericdeandrea/docling/ai/ingestion/extraction/TikaExtractor.java) / [`DoclingExtractor`](src/main/java/dev/ericdeandrea/docling/ai/ingestion/extraction/DoclingExtractor.java) — extraction strategies
- [`NaiveChunker`](src/main/java/dev/ericdeandrea/docling/ai/ingestion/chunking/NaiveChunker.java) — sentence splitter + context enrichment
- [`ChatView`](src/main/java/dev/ericdeandrea/docling/ui/ChatView.java) — Vaadin multi-panel layout with toggle buttons
- [`ChatPanel`](src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java) — per-mode chat + chunk display panel

## Planted Questions

### Table data questions (tests structured extraction)

> "What does Table 2 show, and what network architecture won overall?"

Requires the table caption + surrounding discussion text. Extended
content helps Modes A/B reach a qualitative answer; Mode C gives
precise scores.

> "By how many mAP points does YOLOv5x outperform Faster R-CNN overall?"

Requires reading actual table grid data (specific mAP values per
model). Only Mode C keeps table data intact via hybrid chunking.

> "What is the most frequent class label in DocLayNet, and how many instances does it have?"

Answer: Text, 510,377 (Table 1 data). Requires scanning the Count
column.

> "Which class label has the highest inter-annotator agreement across all document categories?"

Answer: Page-footer at 93-94% (Table 1, "All" column). Requires
comparing values across rows.

### Figure data questions (tests chart/image text extraction)

> "What percentage of DocLayNet pages are Patents?"

Answer: 21% (Figure 2 pie chart). The percentage appears only as a
text label on the chart — not in the body prose. Tests whether
extraction captures chart text.

> "According to Figure 2, which document category is the largest in DocLayNet?"

Answer: Financial Reports at 32%. Same chart, different angle.

### Prose questions (control — all modes should answer similarly)

> "How many annotators were used in the production annotation phase, and how long did it take?"

Answer: 32 annotators, about three months (Section 4, page 5). This
is plain prose text — verifies the baseline works across all modes.

## Fixtures

- `fixtures/doclaynet-2206.01062v1.pdf` — the DocLayNet paper (arXiv:2206.01062v1). The same document Docling's own team uses to demo naive-extraction failures.
- `fixtures/docling.pptx` — supplementary presentation material (not used by the RAG pipeline).

## Tech Stack

- **Quarkus** — runtime
- **LangChain4j** — RAG pipeline, AI services
- **Docling** (`quarkus-docling`) — document extraction and hybrid chunking
- **Qdrant** — vector store (three named collections, one per mode)
- **Vaadin** — pure-Java chat UI with streaming
- **MapStruct** — type-safe mapping between AI and UI layers
- **WireMock** — LLM chat stubbing in tests
