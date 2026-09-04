# Plan — Spec 016: Adopt DoclingDocumentParser (PR #6255) in Mode B (DoclingNaiveExtractor)

Status: Approved

## Goal

Rework `DoclingNaiveExtractor` (Mode B) so `DoclingDocumentParser` (langchain4j 1.20.0-beta30)
owns all the Docling invocation plumbing, and the extractor keeps only the unavoidable
`DoclingDocument` → text/provenance mapping, forking back to Mutiny via
`Uni.createFrom().completionStage(...)`. Output contract (`Uni<ExtractionResult>`) and all Mode B
behavior are unchanged. Mode C is out of scope (follow-up spec).

## Division of labour (the "let the parser do the work" principle)

**`DoclingDocumentParser` now owns:**
- Reading the `InputStream` bytes (offloaded to the common `ForkJoinPool`).
- Base64-encoding the bytes into a `FileSource` and injecting it into a fresh copy of the request
  template (`toBuilder().clearSources().source(...)`).
- Endpoint routing (`ConvertDocumentRequest` → `convertSourceAsync`, i.e.
  `POST /v1/convert/source/async`).
- Async invocation and **cast-free response typing** (internal pattern-match to
  `InBodyConvertDocumentResponse`, handed typed to our `documentExtractor`).
- Blank-input (`BlankDocumentException`) and error wrapping.
- Adding the `document_size_bytes` metadata (kept — Decision 9).

**`DoclingNaiveExtractor` keeps (the necessary Docling-specific code):**
- The `ConvertDocumentOptions` (`toFormat(JSON)`) it needs.
- `DoclingDocument` → flattened full text (`buildFullText`, `tableToText`) — unchanged.
- `DoclingDocument` → `List<ProvenanceEntry>` (`buildProvenance`, `toProvenanceEntry`,
  `DocItemIndex`) — unchanged.
- The Mutiny bridge (`Uni.createFrom().completionStage`), the provenance carry-out (OQ1), and the
  `InputStream` lifecycle.

**Removed from the extractor:** the hand-built `ConvertDocumentRequest` in the method body (becomes
a shared constant), the `Uni.createFrom().completionStage(() -> convertFilesAsync(request, path))`
wrapper, the unchecked `.map(InBodyConvertDocumentResponse.class::cast)` (R3), and the inline
`.map(response -> response.getDocument().getJsonContent())`.

## OQ1 — how Mode B carries provenance out of the parser (RESOLVED → (c))

`parseAsync` returns only a `Document`, but `ExtractionResult` needs `Document` **+**
`List<ProvenanceEntry>`. **Resolved: (c) per-call capture holder carrying the raw `DoclingDocument`**
(`decisions.md` §11). Exactly one value must cross the async boundary (everything is computed
synchronously inside the one `documentExtractor` call), so a multi-field "accumulator" is rejected as
over-modeling a non-staged computation.

- **(c) Stash the `DoclingDocument` (CHOSEN).** The `documentExtractor` is a thin capture step:
  `response.getDocument().getJsonContent()` → `Document.from(buildFullText(doclingDoc))`, stashing the
  raw `DoclingDocument` in a per-call `AtomicReference<DoclingDocument>`. All Docling→domain mapping
  (`buildProvenance`) moves to the `Uni` `.map`, co-located with `ExtractionResult` assembly, and runs
  against the returned `Document`'s text (which also carries the parser's `document_size_bytes`,
  Decision 9). Downstream untouched.
- **(a) Stash the computed `List<ProvenanceEntry>`.** Same single-value holder, but the
  `documentExtractor` also runs `buildProvenance` (business mapping inside the side-effecting
  Function). Viable; rejected in favor of (c)'s thinner Function / co-located mapping.
- **(b) Encode provenance into `Document` metadata — REJECTED.** Verified against the actual
  `dev.langchain4j.data.document.Metadata` on the classpath: it is **scalar-only in both
  directions** (`SUPPORTED_VALUE_TYPES = {String, UUID, Integer, Long, Float, Double}`; both the
  `Map` constructor and `putAll(Map<String,Object>)` run a `validate()` that throws on any other
  value type; `toMap()` returns only those scalars). A `List<ProvenanceEntry>` would have to be
  JSON-serialized to a String and re-parsed. Worse, `DocumentBySentenceSplitter` copies document
  metadata onto every segment, so the blob would pollute every chunk/embedding unless scrubbed, and
  reading it in an `ExtractionResult(Document)` constructor would bake a JSON convention into a
  domain record. Opposite of the goal.

## Target shape of `DoclingNaiveExtractor` (Option (c))

```java
@ApplicationScoped
public class DoclingNaiveExtractor {

    private static final ConvertDocumentRequest CONVERT_REQUEST = ConvertDocumentRequest.builder()
        .options(ConvertDocumentOptions.builder().toFormat(OutputFormat.JSON).build())
        .build();

    private final DoclingServeApi doclingServeApi;

    DoclingNaiveExtractor(DoclingServeApi doclingServeApi) {
        this.doclingServeApi = doclingServeApi;
    }

    public Uni<ExtractionResult> extract(Path documentPath) {
        // The parser's documentExtractor returns only a Document; we capture the raw DoclingDocument
        // so the Uni chain can build provenance downstream, co-located with ExtractionResult assembly.
        var captured = new AtomicReference<DoclingDocument>();

        var parser = DoclingDocumentParser.builder()
            .doclingClient(this.doclingServeApi)
            .documentRequest(CONVERT_REQUEST)
            .documentExtractor(response -> toDocument(response, captured))
            .build();

        return Uni.createFrom()
            .completionStage(() -> parseAsync(parser, documentPath))
            .map(document -> new ExtractionResult(document, buildProvenance(captured.get(), document.text())));
    }

    // Thin capture step: the parser hands us the typed response cast-free; we flatten it to the
    // full-text Document (which the parser also tags with document_size_bytes) and stash the raw
    // DoclingDocument for the Uni chain to map into provenance.
    private Document toDocument(InBodyConvertDocumentResponse response,
                               AtomicReference<DoclingDocument> captured) {
        var doclingDoc = response.getDocument().getJsonContent();
        captured.set(doclingDoc);
        return Document.from(buildFullText(doclingDoc));
    }

    // parseAsync reads the stream asynchronously (ForkJoinPool) and does NOT close it (caller owns
    // the lifecycle), so we defer the close to stage completion via whenComplete. Deliberately NOT
    // try-with-resources: that closes at the end of the try block — before the async read runs —
    // which would break the read. The stream's lifetime is bound to completion, not lexical scope.
    private static CompletionStage<Document> parseAsync(DoclingDocumentParser parser, Path path) {
        try {
            var in = Files.newInputStream(path);
            return parser.parseAsync(in).whenComplete((document, error) -> closeQuietly(in));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open %s".formatted(path), e);
        }
    }

    // buildFullText / tableToText / buildProvenance / toProvenanceEntry — unchanged from today.
    // closeQuietly(InputStream) — closes, logging any IOException via io.quarkus.logging.Log.
}
```

Notes:
- `buildProvenance` moves to the `Uni` `.map`; it runs against `document.text()` (the same flattened
  text `buildFullText` produced) exactly as it does against `fullText` today, so provenance offsets
  are identical. The `documentExtractor` no longer contains business mapping — it just captures.
- `CONVERT_REQUEST` is a shared constant: the template is stateless and the parser injects a fresh
  `FileSource` per call, so only the parser (which closes over the per-call holder) is built per
  call.
- No `.class::cast` anywhere (R3). `InBodyConvertDocumentResponse` appears only as the
  `documentExtractor` parameter type; the captured value is a typed `DoclingDocument`, not a cast.
- Reactive contract intact (R6): no `.await().indefinitely()`; the `Uni` composes over
  `parseAsync`'s `CompletionStage`. The `CompletionStage` supplies the happens-before edge between
  the capture write (in `toDocument`) and the `.map` read.

## Files to change

- `pom.xml` — **already edited** (quarkus-docling 1.4.3, parser pin 1.20.0-beta30). Uncommitted;
  not committing this turn per the user's instruction.
- `src/main/java/.../ingestion/extraction/DoclingNaiveExtractor.java` — rework `extract(...)` as
  above; keep the four mapping helpers unchanged; add `toDocument`, `parseAsync`, `closeQuietly`
  and the `CONVERT_REQUEST` constant; update class/method Javadoc to describe the parser-driven
  flow.
- `src/test/java/.../ingestion/extraction/DoclingNaiveExtractorTest.java` — the four existing
  black-box tests should pass **unchanged** (same injected bean, same WireMock convert endpoint,
  same asserted behavior). Add one test asserting the returned `Document` carries the
  `document_size_bytes` metadata key (proves the parser path is exercised and locks in Decision 9).
- `DocItemIndexTest`, `NaiveChunkerTest` — untouched; expected green.
- `src/test/resources/mappings/*`, `__files/*` — **unchanged** (parser hits the same
  `/v1/convert/source/async` endpoint the convert stub already maps).
- Docs: `README.md`, `CONTEXT.md` (`CLAUDE.md` symlink) — refresh spec-016 status on
  implementation; `decisions.md` — already carries §1–§10; `DoclingNaiveExtractor` Javadoc.

## Key interfaces / classes

- `dev.langchain4j.data.document.parser.docling.DoclingDocumentParser` (+ `.builder()`,
  `doclingClient`, `documentRequest`, `documentExtractor`, `parseAsync`).
- `ai.docling.serve.api.convert.request.ConvertDocumentRequest` /
  `...options.ConvertDocumentOptions` / `OutputFormat.JSON` (request template).
- `ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse` (`documentExtractor`
  input type — not a cast).
- `ai.docling.serve.api.DoclingServeApi` (injected quarkus-docling bean).
- Unchanged: `ExtractionResult`, `ProvenanceEntry`, `DocItemIndex`, `NaiveChunker`, `Document`.

## Tradeoffs / alternatives

- **Per-call parser build** — required so the capture holder is per-call/thread-safe. The parser
  is a cheap config object; negligible churn. (A field-level parser would need a `ThreadLocal`
  holder — uglier. Rejected.)
- **`InputStream` lifecycle** — moving to `parseAsync(InputStream)` means the extractor owns the
  stream; the file handle stays open for the Docling round-trip (closed on completion via
  `whenComplete`). Opening on the caller thread is cheap; the read is offloaded. Reading bytes
  synchronously to avoid holding the handle would block the caller thread — rejected in favor of
  staying non-blocking.
- **`BlankDocumentException`** — a new failure mode for empty input, propagated through the `Uni`.
  Not present in the demo corpus; acceptable.
- **`documentTextExtractor` (text-only)** — rejected: it can't carry provenance, so it can't build
  `ExtractionResult`.
- **Custom `DoclingRequestExecutor`** — unnecessary: the default executor already targets the
  correct source-async endpoint; no retry/threading customization is needed here.

## Verification

- `./mvnw -Duse.wiremock.docling=true verify` — full unit + failsafe IT suite green
  (`DoclingNaiveExtractorTest`, `DocItemIndexTest`, `NaiveChunkerTest`, plus the ingestion/chunk
  ITs).
- Grep-confirm no `InBodyConvertDocumentResponse.class::cast` and no `.await().indefinitely()` in
  `DoclingNaiveExtractor` (R3, R6).
- Optional diagnostics: `-Drun.simulations=true` (`ModeAvsModeBTest`) to confirm Mode B chunk
  behavior is unchanged.
- Manual: run against Docling Serve dev services; verify the Mode B store populates and answers as
  before.

## Open questions for this plan

None outstanding. OQ1 is resolved → **(a) per-call capture holder** (`decisions.md` §11); the
`pom.xml` groundwork is applied; the WireMock/client-bean findings are settled (`decisions.md`
§7–§8). This plan is ready for review.
