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
