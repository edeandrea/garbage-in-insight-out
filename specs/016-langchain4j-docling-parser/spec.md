# Spec 016 — Adopt langchain4j DoclingDocumentParser enhancements (PR #6255)

Status: Approved

## Summary

Adopt the `DoclingDocumentParser` enhancements from
[langchain4j PR #6255](https://github.com/langchain4j/langchain4j/pull/6255)
(released in langchain4j **1.20.0-beta30**) to replace the hand-rolled Docling Serve
invocation and response-mapping plumbing in the two Docling extractors —
`DoclingNaiveExtractor` (Mode B) and `DoclingHybridExtractor` (Mode C) — with the
PR's new, cast-free building blocks. This is a **cherry-pick** adoption: the extractors
keep their current reactive output contracts (`Uni<ExtractionResult>` and
`Uni<List<TextSegment>>`) and all observable behavior; only the invocation/mapping
mechanics change.

## Motivation

The extractors currently hand-roll everything against the raw
`ai.docling.serve.api.DoclingServeApi` bean supplied by quarkus-docling:

- build request options and the request object by hand,
- wrap `convertFilesAsync(...)` / `chunkFilesWithHybridChunkerAsync(...)` in a
  `Uni.createFrom().completionStage(...)`,
- perform an unchecked `InBodyConvertDocumentResponse.class::cast`,
- then dig the JSON content / chunks out of the response.

PR #6255 introduces the pieces that make this cleaner and type-safe:

- a `documentRequest(DocumentRequest)` template that selects the operation
  (convert / hierarchical-chunk / hybrid-chunk) and routes to the matching endpoint,
- **typed, cast-free** response mapping via `documentTextExtractor` /
  `documentExtractor` (over `InBodyConvertDocumentResponse`) and `chunkTextExtractor` /
  `chunkExtractor` (over `ChunkDocumentResponse`),
- non-blocking `parseAsync(InputStream)` returning `CompletionStage<Document>`,
- a pluggable `DoclingRequestExecutor` `(client, request)` seam for custom async
  invocation, retry, or threading.

The PR was authored by this project's owner specifically to improve the Docling
integration, and this demo — whose whole point is showcasing high-quality Docling
document intelligence in a Quarkus/LangChain4j app — should use it. Because
`DoclingDocumentParser` is a `DocumentParser` (its `parse()` returns a single
`Document`), it cannot directly express Mode C's per-chunk `List<TextSegment>` nor
Mode B's `Document` + character-level provenance side-channel; hence the cherry-pick
scope rather than a wholesale `DocumentParser` swap.

## Requirements

R1. quarkus-docling is bumped to **1.4.3** (which brings docling-java **0.6.4**), and
    `dev.langchain4j:langchain4j-document-parser-docling` is added pinned to
    **1.20.0-beta30**, overriding the version managed transitively by
    quarkus-langchain4j 1.13.0 (langchain4j-bom 1.19.0). The project builds and tests
    with a single, consistent docling-java **0.6.4** on the classpath.

R2. `DoclingNaiveExtractor` and `DoclingHybridExtractor` invoke Docling Serve and map
    responses using PR #6255 APIs (`DoclingDocumentParser` and/or `DoclingRequestExecutor`,
    `DocumentRequest` templates, and the typed `documentExtractor` / `chunkExtractor`
    `Function`s), in place of the manual `convertFilesAsync` /
    `chunkFilesWithHybridChunkerAsync` calls.

R3. No unchecked response casts (`InBodyConvertDocumentResponse.class::cast` or
    equivalent) remain in the extractors; response typing is provided by the parser's
    typed extractor `Function`s.

R4. Extractor output contracts are unchanged:
    `DoclingNaiveExtractor.extract(Path) : Uni<ExtractionResult>` and
    `DoclingHybridExtractor.extractAndChunk(Path) : Uni<List<TextSegment>>`.

R5. No functionality is lost. All current behavior is preserved:
    - Mode B: full-text `Document` plus character-level `List<ProvenanceEntry>`
      (page number, element type, caption label);
    - Mode C: per-chunk metadata `mode`, `page_number`, `element_type`,
      `element_label`, and the orphaned picture-child text rescue into synthetic
      `PICTURE` segments;
    - `NaiveChunker`'s `extended_content` enrichment (Modes A/B).

R6. Reactive conventions are preserved: no blocking `.await().indefinitely()`; any
    blocking await uses a bounded `.atMost(...)`.

R7. Existing extractor-layer tests (`DoclingNaiveExtractorTest`,
    `DoclingHybridExtractorTest`, `DocItemIndexTest`, `NaiveChunkerTest`) still pass —
    unchanged where the public behavior is unchanged, updated only where the refactor
    legitimately alters internals. New or updated tests cover the new invocation path
    (per project hygiene: changed behavior isn't done until a test covers it).

R8. WireMock Docling stubs (`src/test/resources/mappings/*`,
    `src/test/resources/__files/*`) remain valid, or are updated, for whatever async
    endpoints the new invocation targets; tests run with `-Duse.wiremock.docling=true`
    pass. Failsafe integration tests continue to run under `verify` and are never
    skipped.

R9. Documentation is updated: `README.md` / `CONTEXT.md` (`CLAUDE.md` symlink),
    this spec's `decisions.md`, and any affected extractor Javadoc.

## Out of scope

- Adopting the full `DocumentParser.parse()` / single-`Document` contract for the
  ingestion pipelines (explicitly rejected — see Decision 1).
- Mode A (`TikaExtractor`) — untouched.
- The RAG/retrieval, chat/assistant, and Vaadin UI layers.
- The hierarchical chunker endpoint (available in the API but unused by this app).
- Any change to the A/B/C demo narrative or the canonical Table 2 (`76.8`/`73.4`) probe.
- The Spring AI reference snippet.

## Open questions

OQ1. Given the single-`Document` mismatch, what is the exact new-API surface the
     extractors should use? Options to weigh in `/spec-plan`: (a) `DoclingRequestExecutor`
     + typed extractor `Function`s while our code continues to assemble
     `ExtractionResult` / `List<TextSegment>`; (b) instantiate `DoclingDocumentParser`
     and read the typed response inside a custom extractor `Function`; (c) some hybrid.

OQ2. Endpoint shape. The parser's default executor targets the **source** async
     endpoints (`convertSourceAsync` / `chunkSourceWith*Async`), whereas the app
     currently uses the **file** endpoints (`convertFilesAsync` /
     `chunkFilesWithHybridChunkerAsync`). Determine which endpoints the refactored
     extractors hit and whether the WireMock stub mappings need updating accordingly.

OQ3. Confirm quarkus-docling's `DoclingServeApi` CDI bean is accepted directly by
     `DoclingDocumentParser.builder().doclingClient(...)` under docling-java 0.6.4
     (expected: yes — same `ai.docling.serve.api.DoclingServeApi` type).

## Blocker

Everything past this Draft spec is **blocked on the quarkus-docling 1.4.3 release**
(docling-java 0.6.4). The langchain4j parser override (1.20.0-beta30) requires
docling-java 0.6.4 (`DocumentRequest.toBuilder()` on the abstract base), and we
deliberately avoid a mixed-version classpath override.

This blocker gates `/spec-plan` and `/spec-tasks`, not just implementation: all three
open questions (OQ1–OQ3) can only be answered by inspecting the real
`DoclingDocumentParser` / `DoclingRequestExecutor` API at 1.20.0-beta30 running against
docling-java 0.6.4. Planning before 1.4.3 lands would be speculation against an
unverified API and risks rework. The **requirements in this spec are stable and can be
reviewed/approved now**; design (plan) and the task list wait for 1.4.3. See
`decisions.md` §5.
