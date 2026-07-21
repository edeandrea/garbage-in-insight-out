# Docling Java Demo App

Demo app for a talk ("Garbage In, Insight Out: Document Intelligence for AI-Infused Java Applications"), 
built and maintained by the docling-java / quarkus-docling project lead.

## Tech stack & conventions

- Build: Maven, multi-module. New capabilities should extend the module
  map defined in `specs/000-project-skeleton/`, not invent ad hoc
  structure. If a new feature doesn't fit any existing module, that's a
  signal to propose a new module in that feature's plan phase, not to
  bolt it onto an unrelated one.
- Language: Java 25
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
- Dependencies: kept current via Dependabot.
- CI: every PR builds and runs the full test suite via GitHub Actions
  before merge.

## Bootstrapping

Spec 000 (project skeleton: Maven multi-module layout, Dependabot
configuration, and a baseline GitHub Actions CI workflow) must exist and
be Approved before any other spec moves past the planning phase. It's
foundational, everything else is built inside the structure it defines,
so it goes through the same spec -> plan -> tasks -> implement discipline
as any feature, it isn't exempt just because it's plumbing.

## Git workflow

- Never auto-commit. Always ask the user for approval before running
  `git commit`, even during `/spec-implement` or other multi-step
  workflows. After the user approves, push immediately.

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
   off `tasks.md` as it goes.

Each phase file starts with a `Status:` field (`Draft` / `Approved`). Do
not advance to the next phase, and do not write code, until the current
phase's Status has been set to `Approved` by the user. Stop and show the
file instead of proceeding on your own judgment.

Note: this is different from Claude Code's built-in Plan Mode (Shift+Tab
twice). Plan Mode produces an ephemeral plan inside one turn with nothing
persisted to disk. This workflow persists a reviewable file at every
phase, on purpose, so specs survive across sessions and context resets.

## Active specs

- `specs/000-project-skeleton/` — Maven multi-module layout. Not yet
  created, run `/spec-new project-skeleton` first, before anything else.
- `specs/001-three-mode-rag-demo/` — the core cold-open/verdict/advanced
  RAG demo. Status: Draft, needs review before planning starts. Depends
  on spec 000 being Approved.