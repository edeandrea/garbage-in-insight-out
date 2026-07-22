# Decisions 001: Three-Mode RAG Demo

Architectural decisions resolved during spec 001 plan review and task
breakdown, in chronological order. Each entry records the date/time,
the question that prompted the decision, the options considered, and
the chosen approach.

---

## 1. [2026-07-21 EDT]: Drop Chat Scopes in favor of Vaadin session management

**Question:** The plan uses Chat Scopes (`@ChatRoute`,
`quarkus-langchain4j-chat-scopes`) for conversation management. But
Vaadin already manages its own sessions and push/WebSocket — does Chat
Scopes add value or just overlap?

**Options considered:**
- Keep Chat Scopes alongside Vaadin
- Drop Chat Scopes, use `@SessionScoped` + `@MemoryId` + default store

**Decision:** Drop Chat Scopes. Use `@SessionScoped` AI service +
`@MemoryId` (UUID per conversation, regenerated on mode switch) +
default `InMemoryChatMemoryStore`. CDI scope destruction handles memory
cleanup automatically when the HTTP session ends.

---

## 2. [2026-07-21 EDT]: Mode propagation via @RequestScoped bean

**Question:** The `ModeAwareRetrievalAugmentor` needs to know which
mode (A/B/C) the user selected. Since the mode lives in the Vaadin UI,
how should it propagate to the CDI layer?

**Options considered:**
- `@RequestScoped` CDI bean — Vaadin view sets it before each call
- ThreadLocal / Vert.x context — simpler but less idiomatic

**Decision:** `@RequestScoped` `CurrentMode` CDI bean. Standard CDI
pattern, clean separation.

---

## 3. [2026-07-21 EDT]: Use DoclingServeApi directly, not DoclingDocumentParser

**Question:** The `quarkus-docling` extension doesn't natively produce
LangChain4j `TextSegment` objects — conversion code is needed. Should
we use `DoclingDocumentParser` from `langchain4j-document-parser-docling`
or call the API directly?

**Options considered:**
- Use `DoclingDocumentParser` (wraps conversion endpoint)
- Use `DoclingServeApi` directly (raw `DoclingDocument` JSON)

**Decision:** Use `DoclingServeApi` directly for both Mode B and C.
`DoclingDocumentParser` flattens to Markdown, which drops page-level
provenance metadata. The raw JSON approach preserves page numbers,
element types, and element labels. A future `DoclingDocumentParser`
enhancement in LangChain4j (maintained by Eric) may simplify Mode B
later.

---

## 4. [2026-07-21 EDT]: Mode A uses langchain4j-document-parser-apache-tika

**Question:** The plan uses raw `tika-parsers-standard-package`. There's
a LangChain4j Apache Tika document parser — should we use that instead?

**Decision:** Use `ApacheTikaDocumentParser` from
`langchain4j-document-parser-apache-tika` (BOM-managed). Wraps Tika and
produces a LangChain4j `Document` directly — less glue code.

---

## 5. [2026-07-21 EDT]: Sentence splitter with context enrichment for naive chunker

**Question:** For the naive chunker (modes A/B), should we use
`DocumentSplitters.recursive()`, `DocumentBySentenceSplitter`, or
`DocumentBySentenceSplitter` + context enrichment?

**Options considered:**
- `DocumentSplitters.recursive()` — simplest, sharpest B-vs-C contrast
- `DocumentBySentenceSplitter` only — sentence-aware, middle ground
- `DocumentBySentenceSplitter` + `collectTextSegmentAndExtendedContent`
  — smartest naive baseline

**Decision:** `DocumentBySentenceSplitter(maxTokens, overlap)` +
`collectTextSegmentAndExtendedContent(segments, 2, 2)`. Sentence-aware
splitting with context enrichment pre-empts "you used a dumb splitter"
objections. If context enrichment compensates too well for the B-vs-C
comparison, drop it and fall back to sentence splitting alone. Context
enrichment N=2 (2 segments before, 2 after).

---

## 6. [2026-07-21 EDT]: Segment metadata: page_number, element_type, element_label

**Question:** Should we capture page numbers and structural element info
(type, label) as metadata on each segment? Is it feasible per mode?

**Decision:** Each `TextSegment` carries `page_number`, `element_type`,
`element_label` (modes B/C only, from `DoclingDocument` provenance/
structure), and `mode`. Mode A (Tika) has no page tracking. Shown as
source references (e.g., "Table 2, page 5") in the retrieval panel.

---

## 7. [2026-07-21 EDT]: Ingestion guard: skip if pgvector table has rows

**Question:** The plan uses `@Startup` to run ingestion. Every dev-mode
restart would re-run all three pipelines. Should we add a guard?

**Options considered:**
- Skip if table has rows (check on startup)
- Always re-ingest

**Decision:** Skip if table has rows. Truncate tables to force
re-ingest. Easy RAG's `reuse-embeddings` was considered but only works
with the in-memory store, not pgvector.

---

## 8. [2026-07-21 EDT]: Vaadin for UI

**Question:** Vaadin vs. plain HTML/JS + SSE vs. Quinoa + React?

**Decision:** Vaadin (`vaadin-quarkus-extension`). Pure Java, built-in
push for streaming. Uses Vaadin's built-in `MessageList` and
`MessageInput` components.

---

## 9. [2026-07-21 EDT]: Models: qwen3:30b-a3b + nomic-embed-text

**Question:** Are the plan's default models still the right choice?

**Decision:** Keep `qwen3:30b-a3b` (LLM, ~16 GB MoE) + `nomic-embed-text`
(embeddings, 768 dims, 274 MB) via Ollama `/v1`. Fallbacks: `qwen3:8b`
or `mistral-small3.2` for LLM; `mxbai-embed-large` or
`Qwen3-Embedding-0.6B` for embeddings.

---

## 10. [2026-07-21 EDT]: Retrieval top-k = 4

**Question:** The config section had `rag.top-k=4` carried over from
the original draft without discussion. Is 4 a good default?

**Decision:** 4 is fine. Configured via `@ConfigMapping`.

---

## 11. [2026-07-21 EDT]: Easy RAG rejected

**Question:** Could `quarkus-langchain4j-easy-rag` simplify the
ingestion pipeline? It has a built-in `reuse-embeddings` feature.

**Decision:** Not used. Runs a single pipeline with one extractor and
one chunker. Cannot handle three pipelines with different extraction/
chunking strategies.

---

## 12. [2026-07-21 EDT]: Multi-panel UI with per-panel input

**Question:** Should switching modes clear the conversation, or should
the UI support comparing conversations side-by-side? Should input be
broadcast to all panels or per-panel?

**Options considered:**
- Mode selector that clears conversation on switch
- Side-by-side panels with broadcast input
- Side-by-side panels with per-panel input
- Tabbed conversations

**Decision:** Dynamic multi-panel layout with per-panel input. Panels
can be added/removed during the demo. Each panel has its own input,
conversation history, and chunks display. Supports the progressive demo
flow: Mode A alone for cold open, A vs B for verdict, B vs C for
chunking payoff.

---

## 13. [2026-07-21 EDT]: Package structure: separate AI from UI

**Question:** Should Vaadin UI code live alongside AI service/LangChain4j
code in the same package?

**Decision:** Separate packages:
- `dev.ericdeandrea.docling.ai` — AI service, RAG augmentor, mode
- `dev.ericdeandrea.docling.ai.ingestion` — extraction, chunking
- `dev.ericdeandrea.docling.model` — shared value objects (records)
- `dev.ericdeandrea.docling.ui` — Vaadin views

---

## 14. [2026-07-21 EDT]: AI/UI decoupling via model package with MapStruct

**Question:** How do we prevent LangChain4j types from leaking into the
Vaadin UI layer?

**Decision:** LangChain4j types (`TextSegment`, `ChatEvent`,
`EmbeddingStore`, etc.) never cross into the UI layer. A
`dev.ericdeandrea.docling.model` package holds purpose-built records.
MapStruct mappers (CDI component model) handle conversion at the AI
layer boundary.

---

## 15. [2026-07-21 EDT]: Retrieval config via @ConfigMapping, not bare properties

**Question:** The plan had `rag.top-k=4` as an unqualified property. Is
that the right approach?

**Decision:** App-specific retrieval config uses a `@ConfigMapping`
interface. Exact property names determined during implementation.

---

## 16. [2026-07-21 EDT]: Startup ingestion mechanism: implementation detail

**Question:** Should startup ingestion use `@Startup` + `@PostConstruct`
or `@Observes StartupEvent`?

**Decision:** Don't prescribe in the plan or tasks. Both are valid
Quarkus patterns. The exact mechanism is an implementation detail for
the tasks phase.

---

## 17. [2026-07-21 EDT]: Vaadin AIOrchestrator: evaluate during implementation

**Question:** Vaadin has `AIOrchestrator` with a built-in
`LangChain4JLLMProvider`. Could it simplify the chat UI?

**Decision:** Evaluate during implementation whether it supports
multi-panel mode-switching and retrieved chunk display from
`ContentFetchedEvent`. Fall back to raw `MessageList`/`MessageInput`
if too opinionated.

---

## 18. [2026-07-22 11:30 EDT]: Hybrid over hierarchical chunking for Mode C

**Question:** Docling supports both `HybridChunker` and
`HierarchicalChunker`. Which should Mode C use?

**Options considered:**
- `HybridChunker` — token-aware, splits oversized, merges undersized,
  repeats table headers
- `HierarchicalChunker` — one chunk per structural element, simpler

**Decision:** `HybridChunker`. Token-aware refinement is a better fit
for RAG with embedding models. Hierarchical can produce chunks too
large or too small for effective embedding.

---

## 19. [2026-07-22 11:35 EDT]: Mode C pipeline: no thin wrapper

**Question:** Mode C's Docling Serve hybrid chunking endpoint does
extraction + chunking in one call. Should Mode C still go through a
separate `ChunkingStrategy` interface (which would be a no-op wrapper)?

**Options considered:**
- Keep two-step interface (consistent but thin wrapper adds ceremony)
- Bypass `ChunkingStrategy` for Mode C

**Decision:** Bypass. Mode C uses a single extract+chunk step via
`DoclingExtractor`. No `DoclingHybridChunker` class. Demo code shown on
screen (spec requirement 7) should have no ceremony — a pass-through
adapter adds a class with no real logic.

---

## 20. [2026-07-22 11:50 EDT]: ExtractionResult return type

**Question:** Mode A's extractor returns a LangChain4j `Document`, but
Mode B needs to carry page-level provenance alongside the text. What
should `ExtractionStrategy` return?

**Options considered:**
- Return LangChain4j `Document` for all (simple but loses metadata)
- Return custom `ExtractionResult` record (more expressive)

**Decision:** `ExtractionResult` record holding a `Document` + optional
provenance map. `TikaExtractor` returns empty provenance;
`DoclingExtractor` populates it. The chunker applies identical splitting,
then post-processes to attach metadata from the provenance map.

---

## 21. [2026-07-22 12:00 EDT]: Mode B page metadata: extractor includes both data and metadata

**Question:** Should Mode B have per-segment page metadata, or should
only Mode C get rich metadata? Adding page info to Mode B is an extra
step that Mode A doesn't have — does that violate the one-variable rule?

**Key insight from Eric:** The extractor is the variable between A and B.
A better extractor naturally produces both better text AND richer
metadata — that's one variable (extraction quality), not two.

**Decision:** Mode B gets page metadata via post-processing after
identical chunking. The metadata enrichment is a consequence of the
extraction quality, not an independent variable. Spec requirement 3
is satisfied — the chunking behavior is identical between A and B.

---

## 22. [2026-07-22 12:10 EDT]: ExtractionResult lives in ai.ingestion, not model

**Question:** `ExtractionResult` holds a LangChain4j `Document` +
provenance map. Should it live in the `model` package (with other value
objects) or in `ai.ingestion`?

**Key principle from Eric:** If a type contains objects from other
libraries, it's an implementation detail and shouldn't be shared across
the server/UI boundary. Only framework-free types belong in `model`.

**Decision:** `ExtractionResult` lives in `ai.ingestion`. It's an
internal AI-layer type, not a boundary type.

---

## 23. [2026-07-22 12:15 EDT]: Assert retrieval, not generation, in LLM tests

**Question:** LLM responses are non-deterministic — even at temperature 0
there can be variation. How should end-to-end tests assert correctness?

**Options considered:**
- Assert key facts in LLM response (e.g., contains "76.8")
- Assert only that the right chunks were retrieved
- Implementation detail

**Decision:** Assert on retrieval, not generation. Tests verify that the
right chunks were retrieved (via `ContentFetchedEvent`), not the LLM's
phrasing. For Mode A, assert garbled/irrelevant chunks. For Mode B,
assert clean chunks. For the B-vs-C comparison, assert Table 2 is
fragmented in B's chunks and intact in C's.

---

## 24. [2026-07-22 12:20 EDT]: CI testing with Ollama service container

**Question:** LLM-dependent tests need Ollama. Should CI use a lighter
model for speed, or the same model as local development?

**Options considered:**
- Lighter model in CI (e.g., `qwen3:4b`) — faster, less resources
- Same model everywhere — tests match production exactly

**Original decision:** Same model everywhere (`qwen3:30b-a3b`).

**Revised decision:** Use `qwen3:4b` in CI, `qwen3:30b-a3b` locally.
Tests assert on retrieval (not generation quality — see decision 25),
so a lighter model is sufficient for CI. The model name is overridden
via `-Dquarkus.langchain4j.openai.chat-model.model-name=qwen3:4b` in
the CI Maven command. Embedding model stays `nomic-embed-text`
everywhere. Tests run as `@QuarkusTest`s. Ollama runs as a GitHub
Actions service container with models pre-pulled via `docker exec`
(pattern from `cescoffier/langchain4j-deep-dive`).

---

## 25. [2026-07-22 12:30 EDT]: YAML configuration over properties

**Question:** Should we use `application.yml` instead of
`application.properties` for Quarkus configuration?

**Decision:** Use `application.yml`. Add the `quarkus-config-yaml`
extension. Replace `application.properties` with `application.yml`.
YAML is more readable for nested configuration like named pgvector
stores.

---

## 27. [2026-07-22 12:45 EDT]: Mappers in dev.ericdeandrea.docling.mapping

**Question:** Where should MapStruct mappers live? They were in `ai`
but they cross the AI/model boundary.

**Decision:** `dev.ericdeandrea.docling.mapping`. Mappers are the
translation layer between the AI and model packages — not owned by
either side. A top-level `mapping` package makes their role explicit.

---

## 28. [2026-07-22 12:35 EDT]: Ollama Dev Services via provided-scope dependency

**Question:** For dev/test mode, should we pull in the Ollama extension
so Dev Services auto-starts an Ollama container and pulls models?

**Approach (from Eric):** Add `quarkus-langchain4j-ollama` with Maven
scope `provided` — Dev Services activate in dev/test but the extension
is not on the production classpath. Use the `provider` attribute on
`chat-model` and `embedding-model` config: set to `ollama` in `%dev`
and `%test` profiles, set to `openai` in the default profile (still
pointed at Ollama's `/v1` endpoint for production flexibility).

**Decision:** Adopted as described. Dev Services auto-start the Ollama
container and pull configured models. No manual Ollama install needed
for development or testing.

---

## 29. [2026-07-22 12:50 EDT]: Default to @QuarkusTest for all tests

**Question:** For tests that need dev services (Docling, Postgres,
Ollama), should they be Surefire `@QuarkusTest`s or Failsafe
`@QuarkusIntegrationTest`s?

**Decision:** Default to `@QuarkusTest` (Surefire, runs during `test`
phase). Dev services start automatically. Only use
`@QuarkusIntegrationTest` if there's a specific reason — ask first.

---

## 30. [2026-07-22 13:03 EDT]: Vaadin UI tests via QuarkusBrowserlessTest

**Question:** How should Vaadin UI behavior be tested? TestBench
(commercial), Playwright (open source, browser-based), or skip?

**Key finding:** Vaadin provides an open-source browserless testing
framework (`browserless-test-quarkus`) with `QuarkusBrowserlessTest`
as the base class. No commercial license needed. Fast (~5-60ms per
test), browser-less, CDI injection works via `@QuarkusTest`.

**Decision:** Use `QuarkusBrowserlessTest` for all Vaadin UI tests.
Add `browserless-test-quarkus` (test scope) to POM. Tests navigate
to views and interact with server-side components directly.

---

## 31. [2026-07-22 13:05 EDT]: Chunk validation uses full retrieval path

**Question:** Task 12 (chunk size validation) — since we assert on
retrieval not generation (decision 25), does this test need the LLM?
Or just chunking assertions?

**Key insight from Eric:** The LLM and embedding model are available
via dev services anyway — no extra cost to use them. Testing the full
retrieval path is more valuable than checking chunk boundaries in
isolation.

**Decision:** Task 12 exercises the full retrieval path: embed planted
question 5 → retrieve from vector store → assert on retrieved chunks.
Verifies that Mode B's retrieved chunks fragment Table 2 and Mode C's
keep it intact. Uses dev services for embedding model and LLM. Asserts
on retrieved chunks, not LLM generation (consistent with decision 25).

---

## 32. [2026-07-22 13:09 EDT]: Watch langchain4j PR #5818 for DoclingDocumentParser builder

**Context:** PR langchain4j/langchain4j#5818 adds a fluent builder to
`DoclingDocumentParser` with a custom `documentTextExtractor` function.
This would allow plugging in extraction logic that preserves page-level
provenance instead of the default Markdown export.

**Impact:** If merged/released, we may be able to use
`DoclingDocumentParser` for Mode B instead of calling `DoclingServeApi`
directly (decision 3). This would require adding
`langchain4j-document-parser-docling` back to the POM. The current
design keeps this pivot easy — Mode B's extraction logic is isolated
in `DoclingExtractor`, so swapping the implementation to use
`DoclingDocumentParser` with a custom extractor would be a localized
change (plus one POM dependency).

**Action:** Monitor PR status. If released before implementation
reaches task 8, evaluate whether it simplifies Mode B's pipeline.
Do not block on it — the raw `DoclingServeApi` approach works regardless.

**URL:** https://github.com/langchain4j/langchain4j/pull/5818

---

## 33. [2026-07-22 13:16 EDT]: AssistantService as public chat API, ChatService package-private

**Question:** `ChatEvent` is a LangChain4j type that shouldn't cross
the AI/UI boundary. Where does the mapping from `ChatEvent` to
model-package types happen?

**Options considered:**
- `default` method on `ChatService` — works but couples mapping to
  the AI service interface
- Separate facade/coordinator bean — cleaner separation of concerns

**Decision:** Create `AssistantService` in `dev.ericdeandrea.docling.ai`
as the public API the UI calls. It wraps `ChatService` (which becomes
package-private), subscribes to `Multi<ChatEvent>`, filters to relevant
events (`ContentFetchedEvent`, `PartialResponseEvent`,
`ChatCompletedEvent`), maps them to model-package types via MapStruct,
and returns `Multi<ChatResponseEvent>` (sealed interface in model
package) to the UI. The UI never sees LangChain4j types.

Model event types: `TokenEvent(text)`,
`ChunksRetrievedEvent(List<RetrievedChunk>)`, `CompletedEvent`.

---

## 34. [2026-07-22 13:21 EDT]: UI never touches CurrentMode directly

**Question:** Who sets the `@RequestScoped` `CurrentMode` bean — the
UI or `AssistantService`?

**Decision:** `AssistantService` handles it internally. Its chat method
accepts `Mode`, memory ID, and user message. It sets `CurrentMode`
before calling `ChatService`, so `ModeAwareRetrievalAugmentor` picks
up the right store. The UI passes `Mode` as a parameter to
`AssistantService` — it never injects or touches `CurrentMode` directly.
This keeps CDI plumbing out of the UI layer.

---

## 35. [2026-07-22 13:24 EDT]: Ask user about layout preferences before building UI

**Question:** At what point should the implementer ask about preferred
Vaadin layouts?

**Decision:** Task 19 (Build multi-panel layout) must present layout
options to the user before writing code — panel arrangement, chunks
display placement, toolbar for add/remove buttons, overall page
structure. Don't assume a layout; ask first.

---

## 36. [2026-07-22 13:48 EDT]: RagConfig defaults

**Question:** What default values for `maxTokens` and `overlap` in the
`@ConfigMapping` for the sentence splitter?

**Decision:** Default `maxTokens=300`, `overlap=30` as reasonable
starting points. These will be tuned during task 12 (chunk size
validation). `topK=4` and `fixturePath=fixtures/doclaynet-2206.01062v1.pdf`
as specified in earlier decisions.

---

## 37. [2026-07-22 13:48 EDT]: Vaadin 25.2.3

**Question:** Which Vaadin version? Project uses Java 25, and
`browserless-test-quarkus` requires Vaadin 25.1+.

**Decision:** Vaadin 25.2.3 (latest stable 25.x). Java 21+ required
(project has 25). Enables browserless testing framework.

---

## 37. [2026-07-22 13:50 EDT]: quarkus-docling 1.4.0

**Question:** Which version of `quarkus-docling`? Research agent
reported 1.3.0 but earlier discussion said 1.4.0.

**Decision:** 1.4.0 — confirmed on
[Maven Central](https://central.sonatype.com/search?q=a:quarkus-docling).
Always use central.sonatype.com or GitHub releases as the source of
record for dependency versions, not web search results.

---

## 38. [2026-07-22 13:54 EDT]: Implementation details persisted in implementation.md

**Question:** Should implementation plans for each task be captured in
version control rather than the ephemeral plan file?

**Decision:** Create `specs/<slug>/implementation.md` to capture
implementation details per task (coordinates, config, approach). Entries
keyed by task number, persisted alongside tasks.md and decisions.md.
Update CLAUDE.md and the `/spec-implement` command to include this.

---

## 39. [2026-07-22 13:59 EDT]: MapStruct CDI component model on @Mapper, not compilerArgs

**Question:** Should the MapStruct CDI component model be configured
globally via `-Amapstruct.defaultComponentModel=cdi` in
`maven-compiler-plugin`, or per-mapper via
`@Mapper(componentModel = MappingConstants.ComponentModel.CDI)`?

**Decision:** Per-mapper on the `@Mapper` annotation. The compiler arg
approach is too hidden. Explicit annotation on each mapper makes the
intent visible at the point of use. Remove `-Amapstruct.defaultComponentModel=cdi`
from the compiler plugin config.

---

## 40. [2026-07-22 15:24 EDT]: Use generic naming for document parameters

**Question:** Should method parameters referencing the input document
be named `pdfPath`/`pdfFile` or something more generic?

**Decision:** Use generic names like `documentToProcess` or
`documentPath`. The pipeline may support formats beyond PDF in the
future, and the naming should reflect the actual abstraction level.
