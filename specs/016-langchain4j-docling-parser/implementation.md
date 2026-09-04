# Implementation — Spec 016: Adopt DoclingDocumentParser in Mode B

Implementation notes per task, keyed by task number from `tasks.md`.

## Task 1 — Confirm dependency groundwork compiles

**Approach:** `pom.xml` already carries the groundwork (quarkus-docling 1.4.3
bringing docling-java 0.6.5, and `dev.langchain4j:langchain4j-document-parser-docling`
pinned to 1.20.0-beta30 to override the BOM-managed version). Verify the artifact
resolves from Maven Central and the project compiles on a single consistent
docling-java 0.6.5 classpath by running `./mvnw -q clean compile`. Confirm
`DoclingDocumentParser` and `InBodyConvertDocumentResponse` are on the classpath
by inspecting the resolved parser jar's public API before writing Task 2 code.

**Outcome:** `./mvnw -q clean compile` succeeds. Verified against the resolved
1.20.0-beta30 parser jar and docling-java 0.6.5:
- Builder exposes `doclingClient(DoclingServeApi)`, `documentRequest(DocumentRequest)`,
  `documentExtractor(Function<InBodyConvertDocumentResponse, Document>)`,
  `parseAsync(InputStream) : CompletionStage<Document>`.
- `ConvertDocumentRequest extends DocumentRequest` (in 0.6.5), so the shared
  `CONVERT_REQUEST` constant passes straight to `.documentRequest(...)`.
- Parser adds `document_size_bytes` (String) via `document.metadata().put(...)`
  on top of whatever the `documentExtractor` returns (source line 220), confirming
  the Task 4 assertion is valid.
- `parseAsync` offloads the blocking read to the common pool (`supplyAsync`) and
  **does not close** the stream (caller owns lifecycle), confirming the
  `whenComplete`-based close in Task 2.

## Tasks 2 & 3 — Rework `DoclingNaiveExtractor` + Javadoc (same file)

**Approach:** Replaced the hand-rolled convert flow with a `DoclingDocumentParser`
built per call (so the capture holder is per-call/thread-safe). Implemented exactly
the plan's Option (c) target shape:
- `CONVERT_REQUEST` shared constant (`ConvertDocumentRequest` +
  `ConvertDocumentOptions.toFormat(JSON)`).
- `extract(Path)` builds the parser (`doclingClient`, `documentRequest`,
  `documentExtractor`), composes `Uni.createFrom().completionStage(parseAsync(...))`,
  and moves `buildProvenance` into `.map`, running against `document.text()`.
- `toDocument(InBodyConvertDocumentResponse, AtomicReference<DoclingDocument>)` — thin
  capture step; returns `Document.from(buildFullText(doclingDoc))`.
- `parseAsync(parser, path)` — opens `Files.newInputStream`, closes via `whenComplete`
  (not try-with-resources); wraps `IOException` as `UncheckedIOException`.
- `closeQuietly(InputStream)` — closes, logging via `io.quarkus.logging.Log`.
- Removed: hand-built request in the method body, the
  `completionStage(convertFilesAsync(...))` wrapper, `.map(InBodyConvertDocumentResponse.class::cast)`,
  and the inline `.map(response -> response.getDocument().getJsonContent())`.
- The four mapping helpers (`buildFullText`, `tableToText`, `buildProvenance`,
  `toProvenanceEntry`) are byte-for-byte unchanged.
- Class + `extract` Javadoc rewritten to describe the parser-driven flow and the
  capture-holder provenance carry. Imports follow `import-order.txt`
  (`io.quarkus.logging.Log`, then `io.smallrye...`, then `dev.langchain4j...`).

## Task 4 — `document_size_bytes` metadata test

**Approach:** Added `documentCarriesSizeMetadataFromParser()` to
`DoclingNaiveExtractorTest`, asserting the returned `Document`'s metadata carries a
positive `document_size_bytes`. This is only present when the parser path runs (the
parser adds it after the `documentExtractor`), so it locks in Decision 9 and proves
the refactor exercises `DoclingDocumentParser`. `Metadata.getInteger` parses the
String-stored value via `Integer.parseInt` (verified in langchain4j-core source),
so `getInteger(...)` is the correct accessor.

## Task 5 — Run scoped Mode B suite under WireMock

**Outcome:** `./mvnw -Duse.wiremock.docling=true test -Dtest='DoclingNaiveExtractorTest,DocItemIndexTest,NaiveChunkerTest'`
— all green: `DoclingNaiveExtractorTest` 5 (4 original unchanged + 1 new metadata
test), `DocItemIndexTest` 11, `NaiveChunkerTest` 4. The four original black-box
tests passed unchanged, confirming Mode B behavior is preserved (R7, R8).

## Task 6 — Grep verification

**Outcome:** `DoclingNaiveExtractor.java` contains no `class::cast` and no
`await().indefinitely()` (R3, R6).

## Task 7 — Full verify

**Outcome:** `./mvnw -Duse.wiremock.docling=true verify` → BUILD SUCCESS, 88 tests,
0 failures/errors, 9 skipped (the default-gated diagnostics: `ChunkSizeSimulationTest`,
`ModeAvsModeBTest`, `PlantedQuestionsValidationTest`, `CaptureDoclingResponsesTest`).
Failsafe ran under `verify`; the project currently has no `*IT.java` classes
(spec 001's black-box Playwright/WebSocket IT is still pending), so failsafe
executed with nothing to run — a pre-existing state, not a regression, and ITs are
not skipped via config.

## Task 8 — Optional diagnostic (ModeAvsModeBTest)

**Outcome:** `./mvnw -Drun.simulations=true test -Dtest=ModeAvsModeBTest` passed
against a **real** Docling Serve dev-services container (v1.29.0) — 1 test, 0
failures, 36.3s. Mode B produced 245 clean segments and the canonical Table 2
markers (`76.8`/`73.4`) remain intact, confirming the parser-driven refactor
preserves Mode B chunk behavior end-to-end (not just under WireMock).

## Task 9 — Documentation

**Approach/outcome:** Updated the spec-016 entry in `CONTEXT.md` (`CLAUDE.md`
symlink) to "Status: Approved, implemented", describing the parser-driven flow,
the capture-holder provenance carry (Decision 11), the retained `document_size_bytes`
metadata (Decision 9), and the 11 decisions recorded. `README.md` needed no change:
its only `DoclingNaiveExtractor` references are structural links that remain accurate
(no stale internal descriptions of `convertFilesAsync`/`DoclingServeApi`). No new
decision arose during implementation — the plan matched the resolved API exactly.
Two review Q&As about the capture holder were later recorded as `decisions.md` §12
(ScopedValue not a fit — wrong data-flow direction) and §13 (`AtomicReference` beats
the other `java.util.concurrent` carriers), bringing the total to 13 entries.

## Task 10 — Manual verification (pending)

Requires driving the running app in a browser to confirm the Mode B store populates
and answers. The browser MCP servers (chrome-devtools/playwright) failed to connect
this session, so this is left as a pending manual step. The real-Docling extraction
path is already exercised by Task 8 (`ModeAvsModeBTest`), which confirms Mode B
chunking against a live Docling Serve container.
