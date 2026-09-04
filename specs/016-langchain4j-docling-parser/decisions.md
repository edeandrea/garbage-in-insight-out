# Decisions — Spec 016 (Adopt langchain4j DoclingDocumentParser enhancements, PR #6255)

## 1. [2026-09-04 10:55 EDT]: Adoption depth — cherry-pick the new APIs, keep the reactive pipeline

**Question:** `DoclingDocumentParser.parse()` returns a single `Document`, but the pipelines
need richer output — Mode C returns a `List<TextSegment>` with per-chunk metadata plus synthetic
orphan-picture segments, and Mode B returns a `Document` plus a character-level
`List<ProvenanceEntry>` side-channel. How deep should adoption of PR #6255 go?

**Options considered:**
- **Cherry-pick new APIs** — keep the reactive `Uni<ExtractionResult>` / `Uni<List<TextSegment>>`
  pipeline and current output shapes; use the new building blocks (typed
  `documentExtractor`/`chunkExtractor` `Function`s, `DocumentRequest` templates,
  `DoclingRequestExecutor`/async invocation) to replace the manual
  `convertFilesAsync`/`chunkFilesWithHybridChunkerAsync` + cast plumbing.
- **Both modes, rework pipeline** — fully adopt `parse`/`parseAsync` for Modes B and C,
  reworking the pipeline to the single-`Document` contract (Mode C would lose per-chunk metadata
  unless re-derived downstream).
- **Mode B only** — adopt the parser for the convert path only, leave Mode C as-is.

**Decision:** Cherry-pick the new APIs. The `DocumentParser` single-`Document` contract cannot
carry Mode C's per-chunk segments or Mode B's provenance side-channel, so a full swap would lose
information or force awkward re-derivation. Cherry-picking gets the type-safety and cleaner
invocation wins without disturbing the demo's ingestion output shapes.

## 2. [2026-09-04 10:55 EDT]: langchain4j version — override the docling parser module to 1.20.0-beta30

**Question:** PR #6255's features shipped in langchain4j 1.20.0-beta30, but the project is on
quarkus-langchain4j 1.13.0, which imports langchain4j-bom **1.19.0**. How to get the newer parser?

**Options considered:**
- **Bump the extensions** — upgrade quarkus-langchain4j to a release tracking langchain4j 1.20.x
  so the BOM manages it (no such release available yet).
- **Pin/override the module** — add `langchain4j-document-parser-docling` at 1.20.0-beta30 with an
  explicit version override on top of the current quarkus-langchain4j 1.13.0.

**Decision:** Override just the `langchain4j-document-parser-docling` module to **1.20.0-beta30**
for now, until quarkus-langchain4j catches up to langchain4j 1.20.x. The rest of the langchain4j
surface stays on the BOM-managed 1.19.0. This is coupled to docling-java 0.6.4 — see Decision 3.

## 3. [2026-09-04 10:55 EDT]: docling-java 0.6.4 — block on quarkus-docling 1.4.3

**Question:** The parser override (1.20.0-beta30) is compiled against docling-serve-client
**0.6.4** (it needs `DocumentRequest.toBuilder()` promoted to the abstract base), but
quarkus-docling 1.4.2 supplies docling-java **0.6.1**. How do we get 0.6.4 onto the classpath?

**Options considered:**
- **Block on quarkus-docling 1.4.3** — the project owner is releasing quarkus-docling 1.4.3
  (docling-java 0.6.4 bump) shortly; wait and bump quarkus-docling → 1.4.3 together with the
  parser override.
- **Proceed now with a docling-java override** — keep quarkus-docling 1.4.2 but explicitly
  override docling-serve-client (and related docling-java artifacts) to 0.6.4.

**Decision:** Block implementation on **quarkus-docling 1.4.3**. Overriding docling-java to 0.6.4
while running the quarkus-docling 1.4.2 extension (built against 0.6.1) risks a fragile
mixed-version classpath (extension bean vs parser expecting 0.6.4) and a throwaway override we'd
remove days later. Spec/plan/tasks proceed now; the implementation phase waits for 1.4.3, then
does both version bumps together.

## 4. [2026-09-04 10:55 EDT]: Behavior — clean API use with no loss of functionality

**Question:** Must the current extraction behavior be preserved exactly, or is simplification
acceptable in exchange for a cleaner parser-based design?

**Options considered:**
- **Preserve exactly** — keep all current behavior; existing tests pass unchanged.
- **Simplification OK** — prefer the cleanest parser API even if some current behavior
  (e.g. orphan-rescue nuances) is simplified or dropped.

**Decision:** Use the new API as cleanly as possible, but with **no loss in overall
functionality**. Mode B provenance/page/element/caption metadata, Mode C per-chunk
`mode`/`page_number`/`element_type`/`element_label`, orphan picture-text rescue, and
`NaiveChunker` `extended_content` enrichment all remain. Existing extractor tests stay green.

## 5. [2026-09-04 11:04 EDT]: Halt after the Draft spec — plan and tasks also wait for 1.4.3

**Question:** Decision 3 blocked *implementation* on quarkus-docling 1.4.3 but said spec, plan, and
tasks could proceed now. Can `/spec-plan` and `/spec-tasks` really be written meaningfully before
1.4.3 is available?

**Options considered:**
- **Proceed with plan/tasks now** — draft the technical design and checklist against the PR
  description and current docling-java 0.6.1, refining once 1.4.3 lands.
- **Halt after the Draft spec** — approve the requirements now, but hold `/spec-plan` (and
  therefore `/spec-tasks`) until quarkus-docling 1.4.3 (docling-java 0.6.4) is on the classpath.

**Decision:** Halt after the Draft spec. All three open questions (OQ1: which new-API surface to
use given the single-`Document` mismatch; OQ2: source vs file async endpoints and WireMock stub
impact; OQ3: whether quarkus-docling's `DoclingServeApi` bean plugs into `doclingClient(...)`) can
only be resolved by inspecting the real `DoclingDocumentParser` / `DoclingRequestExecutor` at
1.20.0-beta30 against docling-java 0.6.4. A plan written against assumptions we cannot verify would
likely need rework. The requirements (this spec) are stable and independent of the exact API, so
they can be reviewed and approved now; design and the task list wait for 1.4.3. This supersedes the
"spec/plan/tasks may proceed" note in Decision 3.

## 6. [2026-09-04 13:10 EDT]: OQ1 resolution — hybrid API surface (Mode B parser, Mode C direct)

**Question:** With the 1.4.3 blocker cleared, the real 1.20.0-beta30 API confirmed that
`DoclingDocumentParser.parse()`/`parseAsync()` emit only a single `Document`, and the PR's
cast-free typing is delivered *only* through the parser's typed `documentExtractor`/`chunkExtractor`
`Function<Response, Document>` (running inside `parseAsync`). Mode B needs `Document` +
`List<ProvenanceEntry>`; Mode C needs `List<TextSegment>`. `DoclingRequestExecutor` used standalone
returns the supertype `ProcessedDocumentResponse`, so it yields no cast elimination on its own.
So R3 (cast-free typing via the parser) and R5 (no functionality loss) collide in the actual API.
How should each extractor use PR #6255?

**Options considered:**
- **A — Parser-driven both modes:** run `parseAsync()` in both; the typed extractor `Function`
  does all mapping cast-free. Mode C's `Function` captures `List<TextSegment>` via a per-call
  holder and returns a throwaway `Document`. Fullest PR #6255 adoption (satisfies R2+R3+R5), but
  Mode C uses the `Function` partly as a side effect — a mild anti-pattern for a showcase demo.
- **B — Direct + pattern matching (no parser):** keep direct client calls, replace the unchecked
  `.class::cast` with a checked `instanceof`/`switch`. Cleanest and zero risk, but adopts none of
  PR #6255's headline feature; fails R2/R3 as written.
- **C — Hybrid:** Mode B through the parser (clean — `Document` is the natural convert output,
  only provenance captured); Mode C stays a direct `chunkSourceWithHybridChunkerAsync` call with
  pattern matching (the parser adds nothing where we don't want a `Document`).

**Decision:** **Option C (hybrid).** Mode B genuinely benefits from the parser — its natural
output *is* a `Document`, so `documentExtractor` reads the typed `InBodyConvertDocumentResponse`
cast-free and returns the flattened full-text `Document`, with the character-level
`List<ProvenanceEntry>` captured as the single side-channel. Mode C wants `List<TextSegment>`, not
a `Document`; forcing it through the parser would mean using the extractor `Function` for a
discarded-`Document` side effect, which is not clean enough for this demo. Mode C therefore calls
`chunkSourceWithHybridChunkerAsync(HybridChunkDocumentRequest)` directly and pattern-matches the
already-typed `ChunkDocumentResponse` (Mode C carries no unchecked cast today anyway). Consequence:
R3's "typing provided by the parser's typed extractor `Function`s" applies to **Mode B only**; the
spec's R3 wording is relaxed accordingly (Mode C has no unchecked cast, satisfying R3's intent by a
different, cleaner means). This keeps each extractor idiomatic and preserves all behavior (R4, R5).

## 7. [2026-09-04 13:10 EDT]: OQ2 resolution — no WireMock stub changes needed

**Question:** Does adopting the parser (Mode B) / source-async calls (Mode C) require migrating the
WireMock stub mappings from the file endpoints to the source endpoints?

**Decision:** No. In docling-java 0.6.5 the current `convertFilesAsync` /
`chunkFilesWithHybridChunkerAsync` calls are **default methods** that read the file into a base64
`FileSource` and delegate to `convertSourceAsync` / `chunkSourceWithHybridChunkerAsync` — the same
HTTP endpoints (`POST /v1/convert/source/async`, `POST /v1/chunk/hybrid/source/async`) the existing
stubs map, and the same ones `DoclingDocumentParser`'s default `DoclingRequestExecutor` targets.
Both the Mode B parser path and the Mode C direct source-async path hit these unchanged endpoints,
so `src/test/resources/mappings/*` and `__files/*` need no changes (R8 satisfied by construction).

## 8. [2026-09-04 13:10 EDT]: OQ3 resolution — quarkus-docling bean plugs into the parser directly

**Question:** Is quarkus-docling's `DoclingServeApi` CDI bean accepted by
`DoclingDocumentParser.builder().doclingClient(...)` under docling-java 0.6.5?

**Decision:** Yes. `doclingClient(...)` takes `ai.docling.serve.api.DoclingServeApi` — the exact
interface the quarkus-docling bean implements. The project compiles and all 22 extractor/chunking
tests pass with the parser module (1.20.0-beta30) on the classpath against docling-java 0.6.5, and
the default executor's source-async methods are present on that interface. The injected bean is
passed straight to the parser builder for Mode B; no adapter is required.

## 9. [2026-09-04 13:10 EDT]: Keep the parser's additive `document_size_bytes` metadata (Mode B)

**Question:** `DoclingDocumentParser` adds a `document_size_bytes` entry to the `Document` it
returns. Now that Mode B is parser-driven, its `ExtractionResult` `Document` inherits that key.
Keep it, or strip it to keep Mode B's `Document` metadata byte-identical to today?

**Options considered:**
- **Keep it** — additive metadata; no functionality loss (R5); free source provenance; less code.
- **Strip it** — explicitly remove the key post-`parseAsync` for byte-identical output.

**Decision:** **Keep it.** The key is additive and harmless — it doesn't remove or alter any
existing behavior, so R5 (no functionality loss) is satisfied, and it's a small bonus. During
implementation, confirm no exact-match assertion in `DoclingNaiveExtractorTest` breaks on the
extra key; adjust that assertion if needed rather than stripping the metadata.

## 10. [2026-09-04 13:55 EDT]: De-scope this spec to Mode B only; defer Mode C to a follow-up

**Question:** While reworking the plan, the owner set a sharper goal — *let
`DoclingDocumentParser` do as much work as possible; keep as little Docling-specific code in the
extractors as necessary; fork back to Mutiny via `Uni.createFrom().completionStage`*. That goal is
a clean, unambiguous fit for Mode B (`DoclingNaiveExtractor`), whose natural output is a single
`Document`. For Mode C (`DoclingHybridExtractor`) it re-opened the Option A-vs-C question, because
its `List<TextSegment>` output can only come out of the parser via a discarded-`Document` +
capture-holder workaround. Should this one spec carry both modes, or narrow?

**Options considered:**
- **Keep both modes in spec 016** — resolve Mode C's parser-vs-direct question now (reversing or
  confirming Decision 6's Option C) and implement both extractors together.
- **De-scope to Mode B; follow-up spec for Mode C** — implement the clean, unambiguous Mode B
  refactor now; move Mode C (and its parser-driven-with-holder vs. direct-pattern-matched decision)
  to a separate spec.

**Decision:** **De-scope spec 016 to Mode B only.** The two extractors are independent classes with
no forced coupling; the shared groundwork (the `pom.xml` bump, `DocItemIndex`) either applies to
both or is untouched here. De-scoping dissolves the OQ1 Option A-vs-C tension entirely for this
spec (it only ever existed because of Mode C's output mismatch) and lets Mode B land as a small,
clean, well-tested change. Mode C's chunk-path adoption — parser-driven via `chunkExtractor` with a
captured `List<TextSegment>` (Option A) vs. a direct pattern-matched `chunkSourceWithHybridChunkerAsync`
call (Option C / Decision 6) — is deferred to a follow-up spec where it can get a proper design
pass. **Consequence:** Decision 6's Option C selection is *parked, not final* — it becomes an input
to the follow-up spec, not a commitment of this one. Because the scope changed materially, `spec.md`
Status is reset to **Draft** for re-approval, and the two-mode `plan.md` (built around the now-moot
OQ1) is removed so `/spec-plan` regenerates a fresh Mode-B-only plan on restart. The durable API
findings from that plan survive in §7–§9 above.

## 11. [2026-09-04 14:10 EDT]: OQ1 (Mode B) — carry provenance via a per-call capture holder, not `Document` metadata

**Question:** `DoclingDocumentParser.parseAsync(...)` returns only a `CompletionStage<Document>`,
but `ExtractionResult` needs `Document` **+** a character-level `List<ProvenanceEntry>`. How does
`DoclingNaiveExtractor` get the provenance out of the parser's typed `documentExtractor`?

**Options considered:**
- **(a) Per-call capture holder.** Build the parser inside `extract(...)` closing over an
  `AtomicReference<List<ProvenanceEntry>>`; the `documentExtractor` computes full text + provenance
  from the typed `InBodyConvertDocumentResponse`, writes the provenance into the holder, and returns
  `Document.from(fullText)`. The `Uni` `.map` reads the holder to assemble `ExtractionResult`.
- **(b) Encode provenance into the returned `Document`'s `Metadata`** (optionally with an
  `ExtractionResult(Document)` overload that reads it back). The owner proposed this as potentially
  cleaner — the `documentExtractor` returns a fully-formed `Document` and the `Uni` pipeline just
  reads from it.
- **(c) Stash the whole `DoclingDocument`/typed response in the holder** and map to text+provenance
  outside the extractor `Function` — same holder mechanics as (a), no benefit.

**Investigation (why (b) is not viable):** Inspected the actual `dev.langchain4j.data.document.Metadata`
source on the classpath. `Metadata` is **scalar-only in both directions**: `SUPPORTED_VALUE_TYPES =
{String, UUID, Integer, Long, Float, Double}`, and **both** the `Map` constructor and
`putAll(Map<String,Object>)` run a private `validate()` that throws `IllegalArgumentException` for
any value whose `getClass()` isn't in that set (the `Object` value type on `putAll` is a
compile-time convenience only). `toMap()` likewise only ever returns those scalar types. So a
`List<ProvenanceEntry>` **cannot** be stored as-is — option (b) would require JSON-serializing the
list to a `String` and re-parsing it. Two further costs: (1) `NaiveChunker` splits with
`DocumentBySentenceSplitter`, which **copies document-level metadata onto every segment**, so a
`provenance` blob would be duplicated into every chunk and persisted in pgvector unless explicitly
`remove()`d before chunking; (2) reading provenance inside an `ExtractionResult(Document)`
constructor would bake a JSON encoding convention into a domain record and change the meaning of the
existing `ExtractionResult(Document)` constructor (today: "no provenance", `this(document, List.of())`).

**Decision:** **Option (c) — per-call capture holder carrying the raw `DoclingDocument`.** Both (a)
and (c) need exactly one value to cross the async boundary (the `documentExtractor` return type is
fixed to `Document`, and everything — `DoclingDocument`, text, provenance — is computed synchronously
in that one call, so there is no genuine multi-stage accumulation to model with a multi-field object).
(c) is chosen over (a) because it gives the cleaner split of responsibilities: the `documentExtractor`
becomes a **thin capture step** — `response.getDocument().getJsonContent()` → `Document.from(buildFullText(doclingDoc))`,
stashing the raw `DoclingDocument` — with no provenance logic and no side-effecting business mapping;
all Docling→domain mapping (`buildProvenance`) then lives in the `Uni` `.map`, co-located with the
`ExtractionResult` assembly. The returned `Document` still carries the text and the parser's
`document_size_bytes` (Decision 9); the `.map` reads the stashed `DoclingDocument` plus that
`Document` to build provenance against `document.text()`. Downstream is **completely untouched**
(`ExtractionResult`, `ProvenanceEntry`, `NaiveChunker` unchanged; no metadata pollution). The single
mutable holder (`AtomicReference<DoclingDocument>`, built per `extract` call) is confined to the
async bridge, and the `CompletionStage` gives the happens-before edge between the capture write and
the `.map` read. Option (b) is rejected as above; the multi-field accumulator variant is rejected as
over-modeling a non-staged, synchronous computation. This resolves OQ1 and unblocks plan approval.
The `Metadata` scalar-only finding and the `DocumentBySentenceSplitter` metadata-propagation wrinkle
are recorded here so the Mode C follow-up spec doesn't re-investigate them.

## 12. [2026-09-04 17:09 EDT]: Would the capture holder be a good candidate for `ScopedValue`?

**Question (asked during implementation review):** The per-call holder that carries the
`DoclingDocument` out of the parser's `documentExtractor` callback is a Java 25 codebase — would
`ScopedValue` (JEP 506, finalized in JDK 25) be a better fit than `AtomicReference`?

**Options considered:**
- **`ScopedValue`** — bind a value at the top of a dynamic scope, readable by callees (incl.
  `StructuredTaskScope` forks) within that scope.
- **Keep `AtomicReference`** (per-call mutable single-slot holder).

**Decision:** Keep `AtomicReference`; `ScopedValue` is the wrong tool here — in fact it is the
anti-pattern `ScopedValue` was designed to replace. Reasons:
- **Data flows the wrong direction.** `ScopedValue` is one-way *inward* (caller → callees): you bind
  a *known* value before entering the scope. Our value is produced *deep inside* the parser callback
  and must surface back *out* to the `.map` — a callee→caller rendezvous, which `ScopedValue`
  forbids.
- **`ScopedValue` is immutable within its scope.** There is no "set later, read back" — that mutable
  upward-communication is exactly the `ThreadLocal` misuse JEP 506 set out to eliminate.
- **We don't own the binding site.** To bind we'd wrap the parser call in `.where(...).run(...)`, but
  at that point we don't have the value yet, and the parser establishes its own async stages
  (`supplyAsync` → `requestExecutor.execute` → `thenApply`); we can't guarantee the read happens
  inside our dynamic scope.
- **No safety gain.** The `CompletionStage` returned by `parseAsync` already provides the
  happens-before edge; `AtomicReference` adds safe publication on top. `ScopedValue` would add
  nothing.

**Note:** `ScopedValue` *would* fit the inverse problem — threading request context (correlation id,
source `Path`, tenant) *into* the parser callbacks without extra parameters. Filed as a mental note,
not a need today.

## 13. [2026-09-04 17:09 EDT]: Is any `java.util.concurrent` utility better than `AtomicReference` for the holder?

**Question (asked during implementation review):** Setting `ScopedValue` aside, is there a better
`java.util.concurrent` type for carrying the `DoclingDocument` out of the callback?

**What the slot needs:** written exactly once (in `documentExtractor`), read exactly once (in
`.map`), no concurrent writers, no CAS/retry, with a happens-before edge already supplied by the
parser's `CompletionStage`. i.e. a *single-slot carrier with safe publication* — nothing more.

**Options considered (all rejected in favor of `AtomicReference`):**
- **`CompletableFuture<DoclingDocument>`** — reading it means `.join()`, a **blocking** call that
  violates the reactive conventions (R6) and can trip Quarkus's event-loop blocking detection; it
  also just duplicates the `CompletionStage` the parser already returns. Strictly worse.
- **`Exchanger` / `SynchronousQueue`** — thread-*rendezvous* primitives where both sides block until
  they meet; we have produce-then-consume across a happens-before edge, not two threads meeting.
  Introduces blocking for no reason.
- **`BlockingQueue` / `CountDownLatch` / `Phaser`** — synchronization aids or multi-value channels; a
  latch/phaser doesn't even carry a value. Wrong shape.
- **`T[] holder = new T[1]` / plain mutable field** — lighter, but relies entirely on the
  `CompletionStage` edge for visibility with nothing self-documenting, and reads worse. Not an
  improvement.

**Decision:** Keep `AtomicReference` — it is the canonical single-slot carrier with safe publication;
we simply don't exercise its CAS ops. The only thing that would genuinely beat it is *not needing a
side channel at all*, which is blocked by the parser API: `documentExtractor` is
`Function<InBodyConvertDocumentResponse, Document>`, so only one value can ride its return and we need
two (`Document` + the structured `DoclingDocument` for provenance). Given that constraint, a per-call
holder is required (Decision 11 / OQ1 option c) and `AtomicReference` is the cleanest holder for it.

## 14. [2026-09-04 17:23 EDT]: Bridge the checked `IOException` with Mutiny's `Unchecked.supplier`, not a manual `try/catch`

**Question (post-implementation refinement):** The `Uni.createFrom().completionStage(...)` bridge
opens `Files.newInputStream(documentPath)`, which throws a checked `IOException`. The first
implementation wrapped the supplier body in a `try/catch` that rethrew as `java.io.UncheckedIOException`
("Failed to open %s"). Is there a cleaner way to surface that failure into the `Uni`?

**Options considered:**
- **Manual `try/catch` → `UncheckedIOException`.** Explicit, but adds five lines of boilerplate, a
  second exception type, and an extra import purely to satisfy the `Supplier` functional-interface
  signature (which forbids checked throws).
- **Mutiny `io.smallrye.mutiny.unchecked.Unchecked.supplier(...)`.** Wraps a supplier that is allowed
  to throw a checked exception; Mutiny catches it and routes it into the `Uni` failure channel, so the
  body can call `Files.newInputStream(...)` and simply let `IOException` propagate. Removes the
  `try/catch`, the `UncheckedIOException` wrap, and its import.

**Decision:** Use `Unchecked.supplier(...)`. It is the idiomatic Mutiny way to lift a checked-throwing
supplier into a `Uni`, keeps the failure on the reactive failure channel (same observable behavior —
the `Uni` fails), and leaves the bridge body as just the open + `parseAsync` + `whenComplete` close.
Net: `java.io.UncheckedIOException` import dropped, `io.smallrye.mutiny.unchecked.Unchecked` added.
This is the shape Mode C (spec 017) mirrors (its R6). No behavior change; existing Mode B tests stay
green.
