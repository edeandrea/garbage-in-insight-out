# Tasks 001: Three-Mode RAG Demo

Status: Approved

## Checklist

- [ ] 1. **Add POM dependencies** — Add all extension dependencies to
      `pom.xml`: `quarkus-langchain4j-openai`, `quarkus-langchain4j-pgvector`,
      `quarkus-docling`, `langchain4j-document-parser-apache-tika` (from the
      Quarkus LangChain4j BOM), `quarkus-langchain4j-ollama` (scope
      `provided` — Dev Services only, not on production classpath),
      `vaadin-quarkus-extension` (from the Vaadin BOM),
      `quarkus-config-yaml` (YAML config support), `browserless-test-quarkus`
      (test scope, open-source Vaadin browserless UI testing), and MapStruct
      (`mapstruct` + `mapstruct-processor` with CDI component model
      configured in the compiler plugin). Verify `./mvnw compile` succeeds.

- [ ] 2. **Update CI workflow with Ollama service container** — Add an
      Ollama service container to `.github/workflows/build.yml` (see
      `github.com/cescoffier/langchain4j-deep-dive` for the pattern). Pull
      `qwen3:4b` (lighter CI model) and `nomic-embed-text` in a pre-build
      step via `docker exec`. Pass
      `-Dquarkus.langchain4j.openai.chat-model.model-name=qwen3:4b` to
      the Maven build to override the default LLM model. Tests assert on
      retrieval, not generation quality, so a lighter model is sufficient
      for CI. Verify the workflow syntax is valid.

- [ ] 3. **Add application.yml configuration** — Replace
      `src/main/resources/application.properties` with `application.yml`.
      Configure: OpenAI-compatible LLM config (base-url pointing to Ollama
      `/v1`, api-key `none`, model-name `qwen3:30b-a3b`, embedding model
      `nomic-embed-text`, temperature 0), three named pgvector embedding
      stores (`naive`, `docling-naive`, `docling-hybrid`) with distinct
      table names, and Postgres dev services enabled. Use the `provider`
      attribute on `chat-model` and `embedding-model`: set to `ollama` in
      `%dev` and `%test` profiles (uses Ollama Dev Services), set to
      `openai` in the default profile (uses the OpenAI-compatible
      endpoint). Verify `./mvnw compile` still succeeds.

- [ ] 4. **Create Mode enum and model package value objects** — Create the
      `dev.ericdeandrea.docling.model` package. Implement `Mode` enum with
      three values (`NAIVE`, `DOCLING_NAIVE_CHUNK`, `DOCLING_HYBRID_CHUNK`),
      each mapping to its pgvector store name and a display label. Create
      value-object records: `ChunkMetadata` (pageNumber, elementType,
      elementLabel, mode, relevanceScore, timestamp) and `RetrievedChunk`
      (text, metadata). These are the boundary types — no LangChain4j types.
      No tests needed — these are simple data-carrying types.

- [ ] 5. **Create MapStruct mapper skeleton** — Create a MapStruct mapper
      interface in `dev.ericdeandrea.docling.mapping` (CDI component model) that
      maps from LangChain4j `TextSegment` + metadata to the model package's
      `RetrievedChunk`/`ChunkMetadata` records. Write unit tests verifying
      the generated mapper correctly extracts metadata keys (`page_number`,
      `element_type`, `element_label`, `mode`) from a `TextSegment`'s
      `Metadata` into a `ChunkMetadata` record.

- [ ] 6. **Create retrieval config mapping** — Create a `@ConfigMapping`
      interface in `dev.ericdeandrea.docling.ai` for app-specific retrieval
      configuration (top-k with default 4, maxTokens and overlap for the
      sentence splitter, fixture PDF path defaulting to
      `fixtures/doclaynet-2206.01062v1.pdf`). Write a `@QuarkusTest` that
      injects the config and verifies defaults are applied.

- [ ] 7. **Implement ExtractionStrategy interface and TikaExtractor** —
      Create the `dev.ericdeandrea.docling.ai.ingestion` package. Define
      `ExtractionStrategy` interface returning an `ExtractionResult` record
      (lives in `ai.ingestion` — internal type, not a boundary type, since
      it holds LangChain4j's `Document` + optional provenance map). `TikaExtractor` wraps
      `ApacheTikaDocumentParser`, returns `ExtractionResult` with the
      `Document` and empty provenance. Write a `@QuarkusTest` that extracts
      the fixture PDF with Tika and asserts the result has non-empty text
      and no provenance.

- [ ] 8. **Implement DoclingExtractor (conversion endpoint, Mode B)** —
      Implement `DoclingExtractor` using `DoclingServeApi` directly. For
      Mode B, call the conversion endpoint to get raw `DoclingDocument`
      JSON. Convert to a LangChain4j `Document` (clean text) and build a
      provenance map linking text positions to page numbers, element types,
      and element labels. Return `ExtractionResult` with both. Write an
      `@QuarkusTest` (needs Docling dev services) that converts the
      fixture PDF and asserts provenance metadata is present.

- [ ] 9. **Extend DoclingExtractor for hybrid chunking (Mode C)** — Add
      Mode C support as a single extract+chunk step: call the Docling Serve
      chunking endpoint (`/v1alpha/convert/chunked/file`, `type=hybrid`).
      Map pre-chunked results directly to `TextSegment`s with provenance
      metadata resolved from `doc_items` (page numbers, element types,
      element labels). Mode C bypasses `ChunkingStrategy` — extraction and
      chunking are inseparable in one server-side call. Write a
      `@QuarkusTest` that chunks the fixture PDF via Docling and asserts
      chunks are returned with page metadata.

- [ ] 10. **Implement ChunkingStrategy interface and NaiveChunker** — Define
      `ChunkingStrategy` interface (takes an `ExtractionResult`, returns a
      list of `TextSegment`s). Used by modes A and B only — Mode C bypasses
      this. Implement `NaiveChunker`: chunk the `Document` with
      `DocumentBySentenceSplitter(maxTokens, overlap)`, enrich with
      `collectTextSegmentAndExtendedContent(segments, 2, 2)`, attach `mode`
      metadata. If the `ExtractionResult` has a provenance map (Mode B),
      post-process each segment to map its text position back to page
      numbers, element types, and element labels. Mode A's empty provenance
      skips this step. Inject maxTokens and overlap from `@ConfigMapping`.
      Write unit tests verifying: (a) segments are produced with `mode`
      metadata, (b) provenance post-processing attaches page metadata when
      available.

- [ ] 11. **Implement IngestionStartup with ingestion guard** — Create
       `IngestionStartup` as a CDI bean that runs at application startup.
       For each mode, check the corresponding pgvector table for existing
       rows — skip if data exists. If empty, run the pipeline: modes A/B
       go through extract → chunk → embed → store (via
       `EmbeddingStoreIngestor`); Mode C uses `DoclingExtractor`'s single
       extract+chunk step → embed → store. Wire each mode to its correct
       strategy. Write a `@QuarkusTest` verifying the ingestion guard
       skips re-ingestion when the table has data.

- [ ] 12. **Chunk size validation (Table 2 fragmentation)** — Extract the
       fixture PDF with Docling, locate Table 2, measure its token length.
       Set `maxTokens` so the sentence splitter fragments Table 2. Write
       `@QuarkusTest`s that exercise the full retrieval path (embed planted
       question 5 → retrieve from vector store → assert on retrieved
       chunks): (a) Mode B's retrieved chunks fragment Table 2 across
       boundaries, (b) context enrichment does not fully reassemble it,
       (c) Mode C's retrieved chunks contain Table 2 as a single coherent
       unit. LLM and embedding model are available via dev services. If
       context enrichment compensates too well, drop it — fall back to
       `DocumentBySentenceSplitter` alone. Document the chosen `maxTokens`
       value.

- [ ] 13. **Create CurrentMode request-scoped bean** — Create `CurrentMode`
       as a `@RequestScoped` CDI bean in `dev.ericdeandrea.docling.ai`
       holding a `Mode` value. Provide getter/setter. Write a `@QuarkusTest`
       verifying the bean is injectable and holds the mode within a request
       scope.

- [ ] 14. **Implement ModeAwareRetrievalAugmentor** — Create in
       `dev.ericdeandrea.docling.ai`, implementing `RetrievalAugmentor`
       directly. Inject `CurrentMode` and the three named pgvector
       `EmbeddingStore` instances. On each request, select the correct
       store based on `CurrentMode`, retrieve top-k segments (from
       `@ConfigMapping`), and augment the prompt with the grounding
       template. Write a test verifying the correct store is selected for
       each mode value.

- [ ] 15. **Implement ChatService (package-private)** — Create as a
       package-private `@SessionScoped` `@RegisterAiService` interface in
       `dev.ericdeandrea.docling.ai`. Configure with
       `ModeAwareRetrievalAugmentor`. Chat method accepts `@MemoryId` UUID
       and user message, returns `Multi<ChatEvent>` for streaming with
       `ContentFetchedEvent` carrying retrieved chunks. Set the system
       prompt to the grounding template. Write a test verifying the AI
       service bean is injectable and session-scoped.

- [ ] 16. **Implement AssistantService (public chat API)** — Create
       `AssistantService` as the public-facing bean in
       `dev.ericdeandrea.docling.ai`. Its chat method accepts a `Mode`,
       a memory ID (UUID), and a user message. Internally it sets the
       `@RequestScoped` `CurrentMode` bean (so `ModeAwareRetrievalAugmentor`
       picks up the right store), calls package-private `ChatService`,
       subscribes to the resulting `Multi<ChatEvent>`, filters to relevant
       events (`ContentFetchedEvent`, `PartialResponseEvent`,
       `ChatCompletedEvent`), maps them to model-package types via
       MapStruct, and returns `Multi<ChatResponseEvent>` (sealed interface
       in model package). Add `ChatResponseEvent` subtypes to the model
       package: `TokenEvent(text)`,
       `ChunksRetrievedEvent(List<RetrievedChunk>)`, `CompletedEvent`.
       The UI calls `AssistantService` — never `ChatService` or
       `CurrentMode` directly. Write a `@QuarkusTest` verifying event
       mapping.

- [ ] 17. **Evaluate Vaadin AIOrchestrator vs raw components** — Create a
       minimal Vaadin route testing whether `AIOrchestrator` with
       `LangChain4JLLMProvider` supports: (a) multiple independent chat
       panels, (b) per-panel mode switching via `AssistantService`,
       (c) displaying retrieved chunks from `ChunksRetrievedEvent`. If it
       fits, use it. If too opinionated, document why and proceed with raw
       `MessageList`/`MessageInput`. Output: documented decision and
       chosen approach skeleton with a smoke test.

- [ ] 18. **Build multi-panel layout** — Before writing code, present
       layout options to the user (panel arrangement, where chunks display
       goes relative to chat, toolbar placement for add/remove buttons,
       overall page structure). Create the main Vaadin view with dynamic
       multi-panel layout based on the chosen design. Support adding and
       removing panels. Each panel bound to a single mode with a mode
       label. Panels preserve state when others are added/removed. Layout
       adjusts column widths (1 = full, 2 = halves, 3 = thirds). Write a
       `QuarkusBrowserlessTest` verifying panels can be added/removed
       independently.

- [ ] 19. **Build per-panel chat view with streaming** — Implement chat
       within each panel using the approach from task 17. Each panel has
       its own `MessageInput`, `MessageList`, and conversation history.
       On submit: generate UUID, call `AssistantService` (which sets
       `CurrentMode` and handles event mapping internally), subscribe to
       `Multi<ChatResponseEvent>`, render `TokenEvent`s to `MessageList`
       via Vaadin push. Write a `QuarkusBrowserlessTest` verifying the
       streaming lifecycle.

- [ ] 20. **Build retrieved chunks display with metadata** — Add a
       "retrieved chunks" section to each panel. On
       `ChunksRetrievedEvent` (from `AssistantService`), render each
       chunk with: text, relevance score, segment metadata (page number,
       element type, element label for B/C; unavailable for A), and
       timestamp. All data arrives as model-package types — no LangChain4j
       types in the UI. Write a `QuarkusBrowserlessTest` verifying chunks
       are displayed with metadata.

- [ ] 21. **Implement response-to-chunks highlighting** — Clicking a
       response in the chat area highlights the chunks used to generate
       that response. Each response tracks its associated chunks (captured
       from `ChunksRetrievedEvent`s in the `AssistantService` stream).
       Clicking a different response updates the highlighting. Write a
       `QuarkusBrowserlessTest` verifying the response-to-chunks
       association.

- [ ] 22. **End-to-end planted questions validation** — Run the full app
       and test planted questions across all three modes via
       `AssistantService`. Assert on retrieval, not generation — LLM
       output is non-deterministic, so tests verify the right chunks were
       retrieved (via `ChunksRetrievedEvent`), not the LLM's phrasing.
       Verify: (a) Mode A retrieves garbled/irrelevant chunks for Set 1
       questions, (b) Mode B retrieves clean, relevant chunks for the
       same questions, (c) for planted question 5, Mode B's retrieved
       chunks fragment Table 2 while Mode C retrieves it intact. Write
       `@QuarkusTest`s asserting chunk content and metadata per mode.
       Adjust chunk size or select alternative questions if contrasts
       don't hold.

- [ ] 23. **Update README and CLAUDE.md** — Update `README.md` with: how
       to run (prerequisites: Ollama with models, Docker for dev services),
       three modes explained, planted questions and expected answers, demo
       flow (cold-open / verdict / advanced payoff). Update `CLAUDE.md`'s
       "Active specs" section. Verify README accuracy by following its own
       instructions.
