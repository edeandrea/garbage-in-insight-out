# Plan 001: Three-Mode RAG Demo

Status: Approved

## Architecture overview

Single Quarkus application, one pipeline shape, three modes:

```
              ┌────────────────────────────────┐
              │     startup ingestion          │
              │  (runs once on app start)      │
              │                                │
              └──────────────┬─────────────────┘
                             │
             ┌───────────────┼───────────────┐
             ▼               ▼               ▼
      ┌──────────┐    ┌──────────┐    ┌──────────┐
      │  naive   │    │ docling- │    │ docling- │
      │          │    │ naive-   │    │ hybrid-  │
      │          │    │ chunk    │    │ chunk    │
      └────┬─────┘    └────┬─────┘    └────┬─────┘
           │               │               │
           ▼               ▼               ▼
      pgvector        pgvector        pgvector
      store "naive"   store "docling- store "docling-
                      naive"          hybrid"
                             │
              ┌──────────────┴─────────────────┐
              │       chat (Vaadin UI)          │
              │  mode selector + streamed RAG   │
              └────────────────────────────────┘
```

Ingestion runs once at application startup using the
`EmbeddingStoreIngestor` pattern. The chat layer retrieves from
whichever pgvector table the UI's mode selector specifies, augments the
prompt, and streams the LLM response alongside the retrieved chunks.

All code lives in one module, separated by package:
- `dev.ericdeandrea.docling.ai` — AI service, RAG augmentor, mode
  selection (LangChain4j / Quarkus LangChain4j code)
- `dev.ericdeandrea.docling.ai.ingestion` — extraction and chunking
  strategies (presentable on screen, spec requirement 7)
- `dev.ericdeandrea.docling.mapping` — MapStruct mappers (CDI component
  model) bridging between LangChain4j types and application value objects
- `dev.ericdeandrea.docling.model` — purpose-built value objects
  (records) shared between the AI and UI layers. No LangChain4j or
  Quarkus-specific types cross the package boundary.
- `dev.ericdeandrea.docling.ui` — Vaadin chat views, mode selector UI

The AI and UI layers are decoupled via the `model` package.
LangChain4j types (`TextSegment`, `ChatEvent`, `EmbeddingStore`, etc.)
never cross into the UI layer. MapStruct mappers in the `mapping`
package handle conversion — they sit at the boundary, not owned by
either side.

## Quarkus extensions and dependencies

- `quarkus-langchain4j-openai` — OpenAI-compatible API for both LLM and
  embedding model. Works with OpenAI, Ollama (`/v1`), or any compatible
  endpoint via configuration.
- `quarkus-langchain4j-pgvector` — vector store backed by PostgreSQL
  with pgvector. One named embedding store per mode. Postgres dev
  services handle local Docker provisioning.
- `quarkus-docling` — Docling Serve client + dev services (starts a
  docling-serve container in dev/test mode). Modes B and C both use
  `DoclingServeApi` directly (different endpoints) to preserve page-level
  provenance metadata.
- `langchain4j-document-parser-apache-tika` — `ApacheTikaDocumentParser`
  for mode A. Wraps Apache Tika and produces a LangChain4j `Document`
  directly. Managed by the Quarkus LangChain4j BOM.
- Vaadin (`vaadin-quarkus-extension`) — pure-Java chat UI with built-in
  push for streaming.
- MapStruct — type-safe mapping between LangChain4j types and
  application value objects. Uses CDI component model for Quarkus.

## LLM model

Default: `qwen3:30b-a3b` (Qwen3-30B-A3B MoE — 30B total params, ~3.3B
active per query, ~16 GB at Q4). Fast due to MoE architecture, strong
instruction following, 262K context window.

Fallback options if quality or speed is insufficient:
- `qwen3:8b` (~5.5 GB) — faster, lighter, best instruction-following
  in the 8B class.
- `mistral-small3.2` (~12 GB) — fast conversational RAG, excellent
  grounding.

## Embedding model

Default: `nomic-embed-text` (274 MB, 768 dimensions, 8K context).
Fast, widely used with Ollama, proven with LangChain4j.

Fallback options if retrieval quality is insufficient:
- `mxbai-embed-large` (~670 MB, 1024 dims) — higher accuracy, still
  lightweight.
- `Qwen3-Embedding-0.6B` (~1.5 GB, flexible dims) — best
  quality-per-VRAM in 2026.

## Prompt template

Identical across all three modes (spec requirement 3). Strictly grounded
— the LLM must not fill in gaps from its own training data:

> Use the following context to answer the user's question. If the answer
> is not in the context, say you don't have enough information to answer.

This is critical for the demo: in Mode A, garbled context should produce
a wrong or "I don't know" answer, not a correct one compensated by the
LLM's general knowledge.

## Ingestion design

Ingestion runs once at application startup, using the
`EmbeddingStoreIngestor` pattern from the Quarkus LangChain4j docs. It processes the fixture PDF through all
three pipelines and populates the corresponding pgvector tables.

### Pipeline per mode

Each mode implements the same pipeline shape:

1. **Extract** — produce text from the PDF.
2. **Chunk** — split text into segments.
3. **Embed** — compute embeddings via the OpenAI-compatible API.
4. **Store** — write embeddings to the mode's pgvector table.

| Mode | Extractor | Chunker | Page metadata |
|------|-----------|---------|---------------|
| A (`naive`) | `ApacheTikaDocumentParser` → LangChain4j `Document` | `DocumentBySentenceSplitter(maxTokens, overlap)` + `collectTextSegmentAndExtendedContent(segments, 2, 2)` (Java-side) | Not available (Tika plain text extraction has no page tracking) |
| B (`docling-naive-chunk`) | `DoclingServeApi` conversion endpoint (`/v1/convert/...`) → raw `DoclingDocument` JSON | Same sentence splitter + context enrichment as A (Java-side) | Yes — extract page numbers from `DoclingDocument` provenance, attach as `TextSegment` metadata |
| C (`docling-hybrid-chunk`) | Docling Serve chunking endpoint (`/v1alpha/convert/chunked/file`, `type=hybrid`) — conversion + chunking in one server-side call | N/A — chunking already happened server-side; map results to `TextSegment`s | Yes — resolve chunk provenance (`doc_items`) for page numbers, attach as `TextSegment` metadata |

Both Mode B and Mode C use `DoclingServeApi` directly (bypassing
`DoclingDocumentParser`) to preserve page-level provenance. Mode B calls
the conversion endpoint and processes the raw `DoclingDocument` JSON to
extract text with page numbers before chunking Java-side. Mode C calls
the chunking endpoint, which performs extraction and structure-aware
chunking in a single server-side call, returning pre-chunked segments
with provenance references.

`DoclingDocumentParser` (from `langchain4j-document-parser-docling`) is
not used because it flattens the `DoclingDocument` to Markdown, which
is lossy — page metadata is dropped. A future enhancement to
`DoclingDocumentParser` in LangChain4j may make this unnecessary, but
for now the raw JSON approach preserves the metadata we need.

### Key classes (in `dev.ericdeandrea.docling.ai.ingestion`)

- `IngestionStartup` — bean that orchestrates all three pipelines on
  application start. Checks each pgvector table for
  existing rows before running that mode's pipeline; skips ingestion
  if the table already has data (drop/truncate tables to force
  re-ingest).
- `ExtractionStrategy` — interface with two implementations:
  `TikaExtractor` (mode A, wraps `ApacheTikaDocumentParser`) and
  `DoclingExtractor` (modes B/C). `DoclingExtractor` uses
  `DoclingServeApi` directly — the conversion endpoint for mode B (raw
  `DoclingDocument` JSON with page provenance) and the chunking endpoint
  for mode C (pre-chunked with provenance).
- `ChunkingStrategy` — interface with two implementations:
  - `NaiveChunker` (modes A/B) — wraps
    `DocumentBySentenceSplitter(maxTokens, overlap)` followed by
    `collectTextSegmentAndExtendedContent(segments, 2, 2)` to enrich
    each segment with 2 surrounding segments of context.
  - `DoclingHybridChunker` (mode C) — calls Docling Serve's chunking
    endpoint (`/v1alpha/convert/chunked/file`, `type=hybrid`) and maps
    the pre-chunked results to `TextSegment`s with provenance metadata.

### Segment metadata

Each `TextSegment` stored in pgvector carries metadata where available:

- `page_number` — source page in the PDF. Available in modes B and C
  (from `DoclingDocument` provenance). Not available in mode A (Tika
  plain text has no page tracking).
- `element_type` — structural type of the source element (`table`,
  `figure`, `caption`, `text`, etc.). Available in modes B and C (from
  `DoclingDocument` structure). Not available in mode A.
- `element_label` — the document label of the source element (e.g.,
  `Table 2`, `Figure 3`). Resolved from `DoclingDocument` when
  available. Not available in mode A.
- `mode` — which pipeline produced this segment (`naive`,
  `docling-naive-chunk`, or `docling-hybrid-chunk`).

The Vaadin UI's "retrieved chunks" panel shows this metadata alongside
each chunk — e.g., "Table 2, page 5" for modes B and C. This
reinforces the demo thesis: structured extraction preserves not just
text but document context — what kind of element it is, where it came
from, and how it relates to the document structure.

### Chunk size

The sentence splitter's `maxTokens` and `overlap` must be chosen such
that Table 2 from the DocLayNet paper is split across chunk boundaries
in modes A and B but kept intact in mode C. This is the open question
from the spec — the plan's approach is:

1. Extract with Docling and inspect Table 2's token length.
2. Choose a `maxTokens` value for `DocumentBySentenceSplitter` that is
   smaller than Table 2 so the sentence splitter fragments it.
3. Verify that the context enrichment from
   `collectTextSegmentAndExtendedContent` does not fully compensate for
   the fragmentation (i.e., Mode B + enrichment should still answer
   planted question 5 worse than Mode C).
4. Verify that Mode C's hybrid chunker keeps Table 2 as one chunk.
5. If context enrichment compensates too well (Mode B answers question 5
   correctly), drop it and fall back to `DocumentBySentenceSplitter`
   alone. If the sentence splitter itself doesn't fragment Table 2,
   adjust `maxTokens` downward or choose a different planted question.
   Resolve during implementation tasks, not on demo day.

### Ingestion guard

On startup, each pipeline checks its pgvector table for existing rows.
If the table already contains embeddings, that mode's pipeline is
skipped entirely. This avoids re-running extraction, chunking, and
embedding on every dev-mode restart. To force re-ingestion, drop or
truncate the target table(s).

## Chat design

### Declarative AI service with dynamic RAG

The chat layer uses the declarative `@RegisterAiService` pattern with
a custom `RetrievalAugmentor` that selects the right pgvector embedding
store based on the current mode. This follows the idiomatic Quarkus
LangChain4j approach rather than manually wiring the RAG pipeline.

**Streaming + retrieved chunks:** The AI service returns
`Multi<ChatEvent>`. Retrieved chunks arrive as `ContentFetchedEvent`
events in the stream — the Vaadin UI renders them in the chunks panel
as they arrive, before the LLM tokens start streaming.

**Session and memory management:** The AI service is `@SessionScoped`,
tied to the Vaadin/HTTP session. Each conversation is identified by a
`@MemoryId` parameter (a UUID generated per conversation). When the
user switches modes, a new UUID is generated, giving a fresh
conversation with no carry-over from the previous mode. The default
`InMemoryChatMemoryStore` handles storage. When the HTTP session ends,
CDI destroys the session scope, which triggers `ChatMemory.clear()` →
`ChatMemoryStore.deleteMessages()` — automatic cleanup.

**Mode propagation:** A `@RequestScoped` CDI bean (`CurrentMode`) holds
the active mode for the current request. The Vaadin view sets it before
each AI service call. The `ModeAwareRetrievalAugmentor` injects
`CurrentMode` to select the correct pgvector embedding store.

### Key classes (in `dev.ericdeandrea.docling.ai`)

- `ChatService` — `@SessionScoped` `@RegisterAiService`-annotated AI
  service returning `Multi<ChatEvent>` for streaming with chunk events.
  Uses `@MemoryId` for per-conversation memory isolation.
- `ModeAwareRetrievalAugmentor` — implements `RetrievalAugmentor`
  directly (not `Supplier`). Injects `CurrentMode` to select the right
  pgvector embedding store per request.
- `CurrentMode` — `@RequestScoped` CDI bean holding the active mode.
  Set by the Vaadin view before each AI service call.
- `Mode` — enum (`NAIVE`, `DOCLING_NAIVE_CHUNK`, `DOCLING_HYBRID_CHUNK`)
  mapping to pgvector table names.

## Chat UI (Vaadin)

The UI is built with Vaadin — pure Java, no JavaScript/CSS to maintain.
Vaadin's push mechanism handles streaming natively. Each panel uses
Vaadin's built-in `MessageList` and `MessageInput` components for chat,
which support markdown rendering and token-by-token streaming out of
the box.

Vaadin also provides `AIOrchestrator` with a built-in
`LangChain4JLLMProvider` that wires streaming, conversation history,
and UI components together with minimal boilerplate. Evaluate during
implementation whether it fits the custom RAG / multi-panel
requirements; fall back to raw `MessageList`/`MessageInput` if the
orchestrator is too opinionated for mode-switching and retrieved chunk
display.

### Multi-panel layout

The UI uses a dynamic multi-panel layout designed for the progressive
demo flow: start with one panel (Mode A for the cold open), add panels
as the talk builds (A vs B for the verdict, B vs C for the chunking
payoff).

- Panels can be dynamically added and removed during the demo.
- Each panel is bound to a single mode (A, B, or C) and has its own
  independent conversation history, input field, and chunks display.
- Each panel has its own chat input — questions are typed per-panel,
  not broadcast. This gives full control over what's asked in each
  mode and when.
- Panels preserve their conversation history when other panels are
  added or removed.

### Per-panel requirements

Each panel must include:

- A mode label (A/B/C), always visible.
- A chat input area.
- A response area showing the streamed LLM answer.
- A "retrieved chunks" display showing, for each chunk:
  - The chunk text.
  - Relevance score.
  - Segment metadata: page number, element type, element label (modes
    B/C only — see Segment metadata section).
  - Timestamp of when the chunk was retrieved.
- Clicking a response in the chat area highlights the chunks that
  were used to generate it.

## Configuration

All configurable via `application.properties` with sensible defaults for
local development (Ollama + Postgres dev services).

```properties
# LLM (OpenAI-compatible — works with OpenAI, Ollama /v1, etc.)
quarkus.langchain4j.openai.base-url=http://localhost:11434/v1
quarkus.langchain4j.openai.api-key=none
quarkus.langchain4j.openai.chat-model.model-name=qwen3:30b-a3b
quarkus.langchain4j.openai.embedding-model.model-name=nomic-embed-text
quarkus.langchain4j.openai.chat-model.temperature=0

# pgvector (Postgres dev services auto-provision in dev mode)
# One named embedding store per mode
quarkus.langchain4j.pgvector.naive.table=naive
quarkus.langchain4j.pgvector.docling-naive.table=docling_naive_chunk
quarkus.langchain4j.pgvector.docling-hybrid.table=docling_hybrid_chunk

# Retrieval (app-specific config via @ConfigMapping, exact property
# names determined during implementation)
# top-k = 4
```

## Tradeoffs considered

1. **`@SessionScoped` + `@MemoryId` vs. Chat Scopes.** The Chat Scopes
   framework (`@ChatRoute`) provides its own conversation management,
   but Vaadin already manages sessions and push/WebSocket. Using both
   creates overlapping session abstractions. The simpler approach:
   `@SessionScoped` AI service + `@MemoryId` (UUID per conversation) +
   the default `InMemoryChatMemoryStore`. Memory cleanup on session end
   is automatic via CDI scope destruction. Mode propagation uses a
   `@RequestScoped` `CurrentMode` bean — standard CDI, no extra
   framework needed.

2. **Raw `DoclingDocument` JSON over `DoclingDocumentParser`.**
   `DoclingDocumentParser` flattens to Markdown, which drops page-level
   provenance. Both Mode B and C use `DoclingServeApi` directly to
   preserve page metadata as `TextSegment` metadata. Mode B calls the
   conversion endpoint and processes the raw JSON; Mode C calls the
   chunking endpoint. A future `DoclingDocumentParser` enhancement in
   LangChain4j may eliminate the need for raw JSON processing in Mode B.

3. **pgvector vs. Qdrant.** pgvector was chosen — it uses PostgreSQL
   (familiar, widely available) and the `quarkus-langchain4j-pgvector`
   extension supports multiple named embedding stores. Qdrant is more
   purpose-built but adds another service to manage.

4. **`quarkus-langchain4j-openai` vs. `quarkus-langchain4j-ollama`.**
   The OpenAI extension pointed at Ollama's `/v1` endpoint satisfies
   the spec's requirement for a configurable OpenAI-compatible API.
   Switching from Ollama to OpenAI (or any compatible provider) is a
   config change — no code change needed.

5. **Vaadin vs. plain HTML/JS vs. Quinoa+React.** Vaadin gives a
   polished UI in pure Java — no frontend tooling, no CSS to write.
   Vaadin's push mechanism handles streaming natively. Plain HTML/JS is
   lighter but requires manual SSE/WebSocket wiring and styling.
   React/Quinoa is the most polished but heaviest lift.

6. **Single module.** Ingestion and chat code separated by package, not
   by Maven module. Simplifies the build, eliminates cross-module
   dependency wiring, and avoids the shared-database problem since
   everything runs in one Quarkus app.

7. **Easy RAG not used.** The `quarkus-langchain4j-easy-rag` extension
   provides automatic ingestion with embedding reuse, but it runs a
   single pipeline with one extractor and one chunker. It cannot handle
   three pipelines with different extraction/chunking strategies. The
   manual `EmbeddingStoreIngestor` approach with per-table ingestion
   guards provides the needed flexibility.
