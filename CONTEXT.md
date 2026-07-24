# Docling Java Demo App

Demo app for a talk ("Garbage In, Insight Out: Document Intelligence for AI-Infused Java Applications"), 
built and maintained by the docling-java / quarkus-docling project lead.

## Tech stack & conventions

- Build: Maven, single-module Quarkus application. Different concerns
  (ingestion, chat, etc.) are separated by package, not by module.
- Language: Java 25
- Import order: follow `import-order.txt` at the project root.
  Group order: static, `java`, `javax`, `jakarta`, `org`, `com`, `ai`,
  `org.apache.commons`, `org.springframework`, `io.quarkus`, `io`,
  then everything else (`dev`, etc.). Blank line between groups.
- Primary: Quarkus + LangChain4j (this is the main live demo)
- Secondary: a short Spring AI code sample only, for a slide/screen flash,
  not a full runnable app
- Write idiomatic, production-quality Quarkus/LangChain4j code. The author
  is a 26+ year Java veteran and Java Champion, not a beginner audience.

## Project hygiene

- Testing: new or changed behavior isn't done until it has a passing test
  covering it. Running the pre-existing suite isn't sufficient on its own,
  `/spec-implement` must add or update tests for whatever a task built
  before that task gets checked off.
- Integration tests: every module must run failsafe integration tests
  during `verify`. Never set `<skipITs>true</skipITs>` or otherwise
  skip integration tests.
- Gated tests (skipped by default, enabled via system property):
  - `-Drun.simulations=true` — `ChunkSizeSimulationTest`,
    `ModeAvsModeBTest`. Diagnostic tests for chunk tuning.
  - `-Drun.planted-questions=true` — `PlantedQuestionsValidationTest`.
    Needs a real LLM (not WireMock). CI enables this.
  - `-Duse.wiremock.docling=true` — activates WireMock stubs for
    Docling Serve in tests with `@TestProfile(DoclingWiremockTestProfile.class)`.
    CI enables this.
- Dependencies: kept current via Dependabot.
- CI: every PR builds and runs the full test suite via GitHub Actions
  before merge.

## Bootstrapping

Spec 000 (project skeleton: Maven project layout, Dependabot
configuration, and a baseline GitHub Actions CI workflow) must exist and
be Approved before any other spec moves past the planning phase. It's
foundational, everything else is built inside the structure it defines,
so it goes through the same spec -> plan -> tasks -> implement discipline
as any feature, it isn't exempt just because it's plumbing.

## Git workflow

- Never auto-commit. Always ask the user for approval before running
  `git commit`, even during `/spec-implement` or other multi-step
  workflows. After the user approves, push immediately.

## Design decisions

- Never assume. If anything is ambiguous — a technology choice, a
  configuration value, an API approach — ask for clarity rather than
  picking a default and moving forward. This applies to specs, plans,
  and implementation alike.
- **Don't test simple POJOs:** Don't write tests for records, enums,
  value objects, or other simple data-carrying types. The compiler
  enforces their shape. Reserve tests for behavior and logic.
- **Quarkus profile config:** When dev and test profiles share the same
  value, use `%dev,test` combined syntax rather than duplicating the
  config across separate `%dev` and `%test` entries.
- Capture design questions, their decisions, and reasoning about the
  decisions in a `decisions.md` in the spec directory. This is a
  lightweight architectural decision record (ADR) — modeled after
  Michael Nygard's ADR format but kept in one chronological file per
  spec rather than one file per decision. Entries are timestamped and
  formatted as:
  `## <number>. [YYYY-MM-DD HH:MM <timezone>]: <description>`.
  Use the machine's actual timezone (e.g., `date "+%Z"`). Include the
  question that prompted the decision, options considered, and the
  chosen approach.

## Non-goals (apply to every feature, not just the first)

- No need to explain or expose Docling's internal model architecture
- No full Spring AI or Arconia app, reference snippet only
- No agentic/MCP functionality unless a spec explicitly calls for it

## Spec-driven workflow (required for every new feature)

Never write or edit application source code without an approved spec.
Small fixes (typos, formatting, config/dependency bumps) don't need this,
use judgment, but any new feature, architecture change, or demo behavior
does. Work through these phases in order, using the matching slash
command:

1. `/spec-new <feature>` — requirements gathering only. Writes
   `specs/<NNN-slug>/spec.md`.
2. `/spec-plan <slug>` — technical design. Writes `specs/<slug>/plan.md`.
3. `/spec-tasks <slug>` — ordered checklist. Writes `specs/<slug>/tasks.md`.
4. `/spec-implement <slug>` — implementation, one task at a time, checking
   off `tasks.md` as it goes. Writes implementation details per task to
   `specs/<slug>/implementation.md`.

Each phase file starts with a `Status:` field (`Draft` / `Approved`). Do
not advance to the next phase, and do not write code, until the current
phase's Status has been set to `Approved` by the user. Stop and show the
file instead of proceeding on your own judgment.

Note: this is different from Claude Code's built-in Plan Mode (Shift+Tab
twice). Plan Mode produces an ephemeral plan inside one turn with nothing
persisted to disk. This workflow persists a reviewable file at every
phase, on purpose, so specs survive across sessions and context resets.

## Active specs

- `specs/000-project-skeleton/` — Maven project layout, CI, Dependabot.
  Status: Approved, implemented (single-module structure).
- `specs/001-three-mode-rag-demo/` — the core cold-open/verdict/advanced
  RAG demo. Status: Approved, implemented. 61 decisions recorded in
  `decisions.md`. Pending: true black-box IT via Playwright/WebSocket,
  PR [#2691](https://github.com/quarkiverse/quarkus-langchain4j/pull/2691)
  for Qdrant Dev Services create-collections simplification.
- `specs/002-ui-polish/` — Seven UI fixes: chunk table color coding,
  resizable columns, remove `#` column, non-wrapping title, panel
  borders, shared resizable split layout, sticky toolbar.
  Status: Approved.