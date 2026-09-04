# Spec 016 — Adopt langchain4j DoclingDocumentParser (PR #6255) in Mode B (DoclingNaiveExtractor)

Status: Approved

> **Scope note (2026-09-04):** This spec was originally written to cover *both* Docling
> extractors. It has been **de-scoped to Mode B (`DoclingNaiveExtractor`) only**. Mode C
> (`DoclingHybridExtractor`) is deferred to a follow-up spec — its `List<TextSegment>` output
> doesn't fit the parser's single-`Document` contract cleanly and deserves its own design pass.
> Because the scope changed materially, Status is reset to **Draft** pending re-approval. See
> `decisions.md` §10.

## Summary

Adopt the `DoclingDocumentParser` enhancements from
[langchain4j PR #6255](https://github.com/langchain4j/langchain4j/pull/6255)
(released in langchain4j **1.20.0-beta30**) in **`DoclingNaiveExtractor` (Mode B)** to replace the
hand-rolled Docling Serve invocation and response-mapping plumbing with the parser's cast-free
building blocks. The guiding principle: **let `DoclingDocumentParser` do as much work as possible**
(request injection, endpoint routing, async invocation, cast-free response typing, base64/stream
handling) so the extractor keeps **as little Docling-specific code as necessary** — only the
unavoidable `DoclingDocument` → text/provenance mapping — then fork back to the reactive pipeline
via `Uni.createFrom().completionStage(...)`. The extractor's output contract
(`Uni<ExtractionResult>`) and all observable Mode B behavior are unchanged.

## Motivation

`DoclingNaiveExtractor` currently hand-rolls everything against the raw
`ai.docling.serve.api.DoclingServeApi` bean supplied by quarkus-docling:

- builds the `ConvertDocumentRequest` by hand,
- wraps `convertFilesAsync(...)` in a `Uni.createFrom().completionStage(...)`,
- performs an unchecked `InBodyConvertDocumentResponse.class::cast`,
- then digs the JSON content out of the response and maps it to a full-text `Document` plus a
  character-level `List<ProvenanceEntry>`.

PR #6255 introduces the pieces that make this cleaner and type-safe, and lets the parser absorb the
plumbing:

- a `documentRequest(DocumentRequest)` template that selects the convert operation and routes to
  the matching endpoint (the parser injects the document `FileSource` per call),
- **typed, cast-free** response mapping via `documentExtractor`
  (`Function<InBodyConvertDocumentResponse, Document>`) with full control over the returned
  `Document` and its `Metadata`,
- non-blocking `parseAsync(InputStream)` returning `CompletionStage<Document>` — a natural fit for
  `Uni.createFrom().completionStage(...)`,
- automatic `document_size_bytes` metadata and blank/error handling.

The PR was authored by this project's owner specifically to improve the Docling integration, and
this demo — whose whole point is showcasing high-quality Docling document intelligence in a
Quarkus/LangChain4j app — should use it. Mode B is the clean fit: its natural output *is* a single
`Document`, so `documentExtractor` produces it directly; the only value the parser can't return
inline is the `List<ProvenanceEntry>` side-channel that `NaiveChunker` needs (how to carry it is
OQ1, settled in the plan).

Mode C (`DoclingHybridExtractor`) is intentionally excluded: it returns a `List<TextSegment>` with
per-chunk metadata and orphan-picture rescue, which the parser's single-`Document` contract cannot
express without a discarded-`Document` workaround. That warrants its own design decision and is
deferred to a follow-up spec.

## Requirements

R1. **Dependencies (shared groundwork, already applied).** quarkus-docling is bumped to **1.4.3**
    (which brings docling-java **0.6.5**), and `dev.langchain4j:langchain4j-document-parser-docling`
    is added pinned to **1.20.0-beta30**, overriding the version managed transitively by
    quarkus-langchain4j 1.13.0. The project builds and tests with a single, consistent docling-java
    **0.6.5** on the classpath.

R2. **`DoclingNaiveExtractor` invokes Docling via `DoclingDocumentParser`.** It configures a
    `DoclingDocumentParser` (`.doclingClient(doclingServeApi)`, `.documentRequest(...)` carrying a
    `ConvertDocumentRequest`, and a typed `documentExtractor`) and calls `parseAsync(...)`, in place
    of the manual `convertFilesAsync(...)` call. The parser owns request injection, endpoint
    routing, async invocation, response typing, and stream/base64 handling.

R3. **No unchecked response casts.** No `InBodyConvertDocumentResponse.class::cast` (or equivalent)
    remains in `DoclingNaiveExtractor`; response typing is provided by the parser's typed
    `documentExtractor` `Function`.

R4. **Output contract unchanged.** `DoclingNaiveExtractor.extract(Path) : Uni<ExtractionResult>`.

R5. **No functionality loss for Mode B.** Preserved:
    - full-text `Document` (same flattened text: `getTexts()` + pipe-rendered tables) plus
      character-level `List<ProvenanceEntry>` (start/end char, page number, element type, caption
      label);
    - `NaiveChunker`'s per-chunk provenance matching (`page_number` / `element_type` /
      `element_label`) and `extended_content` enrichment for Modes A/B are unaffected.
    - The parser's additive `document_size_bytes` metadata on the returned `Document` is kept
      (`decisions.md` §9).

R6. **Reactive conventions preserved.** No blocking `.await().indefinitely()`; any blocking await
    uses a bounded `.atMost(...)`. The `InputStream` handed to `parseAsync` is managed correctly:
    the parser reads it asynchronously and does not close it, so the extractor closes it on stage
    completion (it must not close it synchronously before the async read runs).

R7. **Tests.** Existing Mode B tests (`DoclingNaiveExtractorTest`, and `DocItemIndexTest` /
    `NaiveChunkerTest` where they exercise Mode B) still pass — unchanged where public behavior is
    unchanged, updated only where the refactor legitimately alters internals. New or updated tests
    cover the parser-driven invocation path (per project hygiene: changed behavior isn't done until
    a test covers it).

R8. **WireMock stubs.** The Docling convert stub (`/v1/convert/source/async` in
    `src/test/resources/mappings/docling-convert.json`, plus its `__files/*`) remains valid — the
    parser's default executor targets the same endpoint the current `convertFilesAsync` default
    delegates to; **no stub changes are needed for Mode B**. Tests run with
    `-Duse.wiremock.docling=true` pass. Failsafe integration tests continue to run under `verify`
    and are never skipped.

R9. **Documentation.** `README.md` / `CONTEXT.md` (`CLAUDE.md` symlink), this spec's
    `decisions.md`, and any affected `DoclingNaiveExtractor` Javadoc are updated.

## Out of scope

- **Mode C (`DoclingHybridExtractor`) — deferred to a follow-up spec.** The chunk-path adoption
  (parser-driven via `chunkExtractor` with a captured `List<TextSegment>` vs. a direct
  pattern-matched `chunkSourceWithHybridChunkerAsync` call) is a separate design decision.
- Adopting the full `DocumentParser.parse()` / single-`Document` contract for the ingestion
  pipeline (explicitly rejected — see Decision 1).
- Mode A (`TikaExtractor`) — untouched.
- The RAG/retrieval, chat/assistant, and Vaadin UI layers.
- The hierarchical chunker endpoint.
- Any change to the A/B/C demo narrative or the canonical Table 2 (`76.8`/`73.4`) probe.
- The Spring AI reference snippet.

## Open questions

OQ1. **Provenance carry mechanism (Mode B).** `parseAsync` returns only a `Document`, but
     `ExtractionResult` also needs `List<ProvenanceEntry>`. How does `DoclingNaiveExtractor` get
     the provenance out of the parser? Options to weigh in `/spec-plan`: (a) a per-call capture
     holder (`AtomicReference`) that the `documentExtractor` writes provenance into, read back in
     the `Uni` `.map`; (b) encode provenance into the returned `Document`'s `Metadata` (the
     `documentExtractor` Javadoc explicitly suggests this use) and have `NaiveChunker` read it from
     there; (c) another approach. To be settled in the plan.

## Resolved (from the earlier API investigation; carried forward)

These were open questions in the original two-mode spec and are now settled for Mode B; recorded
here so the restarted plan doesn't re-investigate:

- **Endpoints / WireMock (was OQ2):** No stub changes for Mode B — `convertFilesAsync` is a default
  method delegating to `convertSourceAsync` (`POST /v1/convert/source/async`), the same endpoint
  the parser's default executor targets and the stub already maps. See `decisions.md` §7.
- **Client bean (was OQ3):** quarkus-docling's `DoclingServeApi` bean plugs directly into
  `DoclingDocumentParser.builder().doclingClient(...)`. See `decisions.md` §8.

## Blocker

**Cleared.** quarkus-docling **1.4.3** (docling-java **0.6.5**) is on Maven Central, `pom.xml` is
bumped and the parser is pinned to 1.20.0-beta30, and all extractor/chunking tests pass on the
resulting single-version classpath. See `decisions.md` §3, §5.
