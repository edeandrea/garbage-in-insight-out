# Spec 000: Project Skeleton

Status: Approved

## Summary

Establish the Maven project layout, CI pipeline, dependency management,
and baseline documentation that every subsequent spec builds inside.
Nothing in this spec produces demo-visible behavior — it is pure
plumbing so that spec 001 (and beyond) can land cleanly.

## Motivation

CLAUDE.md declares this spec as the gating prerequisite: no other spec
may move past planning until 000 is Approved and implemented. The repo
today contains only the Maven wrapper, fixture files, and a draft spec.
There is no build file, no source tree, no CI, and no Dependabot config.

## Requirements

### R1 — POM

1. A root `pom.xml` for a single Quarkus application.
2. Java 25 source/target (via `maven.compiler.release`).
3. Quarkus BOM imported in `<dependencyManagement>` (latest stable
   release at implementation time).
4. Common plugin configuration (compiler, surefire, failsafe) defined
   in `<pluginManagement>`.

### R1a — Maven coordinates

5. `groupId`: `dev.ericdeandrea`.
6. `artifactId`: `garbage-in-insight-out`.
7. Base Java package: `dev.ericdeandrea.docling`, with sub-packages for
   different concerns (e.g., `dev.ericdeandrea.docling.ingestion`,
   `dev.ericdeandrea.docling.chat`).

### R2 — Project structure

8. A single Quarkus application with `src/main/java`,
   `src/main/resources`, `src/test/java`, and `src/test/resources`.
9. Ingestion and chat code live in separate packages within the same
   application for on-screen readability.

### R3 — Smoke-test class

10. One minimal class (e.g., an empty CDI bean) and a corresponding
    `@QuarkusTest` that verifies the application context starts. This
    proves the wiring is correct and gives CI something to run.

### R4 — GitHub Actions CI

11. A single workflow file (`.github/workflows/build.yml`) triggered on
    `push` and `pull_request` against `main`.
12. Runs `./mvnw verify` on the latest Ubuntu runner with Java 25
    (Temurin).
13. Caches `~/.m2/repository` for speed.

### R5 — Dependabot

14. `.github/dependabot.yml` watching the `maven` ecosystem, weekly
    schedule.
15. Also watching `github-actions` ecosystem for workflow action version
    bumps.

### R6 — README

16. A root `README.md` with: project title, one-paragraph description
    tied to the talk, a "Prerequisites" section (Java 25, Maven, Docker
    for dev services), a "Build" section (`./mvnw verify`), and a
    "Fixtures" section describing the demo documents.

### R7 — Fixtures directory

17. The existing `fixtures/` directory is kept as-is. It is not a Maven
    module — just a top-level directory holding demo documents. The root
    `README.md` should mention its purpose.

### R8 — Version currency

18. All dependency versions (Quarkus BOM, plugins, GitHub Action
    versions, etc.) must be resolved at implementation time by checking
    the actual upstream sources (Maven Central, GitHub Marketplace) — not
    from training-data assumptions. Pin to the latest stable release of
    each.

## Out of scope

- Any demo-visible functionality (extraction, chunking, chat UI, RAG
  pipeline) — that is spec 001.
- Spring AI sample code — will be addressed in a later spec if needed.
- Docker Compose or dev-services configuration — will be added by the
  spec that first needs them (likely 001).
- Quarkus extension dependencies beyond what the BOM provides — each
  feature spec adds its own.
- Code formatting / linter / style enforcement tooling.

## Open questions

None — all resolved.

- **Single vs. multi-module:** collapsed to a single Quarkus app.
  Ingestion and chat code separated by package, not by module.
- **Spring AI sample location:** deferred. Will decide after the core
  implementation is complete.
