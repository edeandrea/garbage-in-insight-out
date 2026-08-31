# Implementation 013 — Drop the "Time" column and `ChunkMetadata.timestamp` plumbing

## Tasks 1–6 — top-down deletion (one coherent compile unit)

The record change (task 2) is a compile-breaking signature change that ripples
to `ChunkMapper` (task 3), `AssistantService` (task 4), and the two test classes
(tasks 5–6). These cannot compile independently, so they are applied as a single
top-down pass; each task is still individually checked off once the full pass
compiles and its tests pass.

- **Task 1 — `ChatPanel`:** delete the "Time" `addColumn(...)` block, the
  `TIMESTAMP_FORMAT` constant, and the unused `java.time.ZoneId` /
  `java.time.format.DateTimeFormatter` imports. Grid now has 5 columns: Score,
  Page, Type, Label, Preview.
- **Task 2 — `ChunkMetadata`:** remove the `Instant timestamp` component and the
  `java.time.Instant` import (5 components remain).
- **Task 3 — `ChunkMapper`:** drop the `Instant timestamp` param from
  `toRetrievedChunk`/`toMetadata`, change the `metadata` `@Mapping` expression to
  `java(toMetadata(segment, relevanceScore))`, drop the `timestamp` arg to
  `new ChunkMetadata(...)`, remove the `java.time.Instant` import.
- **Task 4 — `AssistantService`:** call
  `toRetrievedChunk(content.textSegment(), score)`, remove the
  `java.time.Instant` import.
- **Task 5 — `ChunkMapperTest`:** remove the `timestamp` locals, call
  `toRetrievedChunk(segment, <score>)`, remove the
  `chunk.metadata().timestamp()` assertions, drop the `Instant` import.
- **Task 6 — `ChatPanelTest`:** drop the trailing `Instant.now()` arg from
  `MOCK_CHUNKS` and the inline `new ChunkMetadata(...)`, remove the `Instant`
  import, change `chunksGridHasNoRoundColumn` to `hasSize(5)` with a "5 columns"
  description.

## Task 7 — verification

Run `./mvnw verify` (full suite incl. failsafe ITs) and confirm no remaining
`timestamp`/`TIMESTAMP_FORMAT` references in Java sources.

## Results

- `./mvnw verify -Duse.wiremock.docling=true` — BUILD SUCCESS. Surefire: 79
  tests, 0 failures, 0 errors, 9 skipped (gated `-Drun.simulations` /
  `-Drun.planted-questions` suites). No `*IT.java` classes exist, so failsafe
  had nothing to run.
- `grep` confirms no remaining `timestamp` / `TIMESTAMP_FORMAT` /
  `java.time.Instant` references in `src/main` or `src/test` Java sources (the
  only surviving `new ChunkMetadata(...)` calls are the 5-arg form).
- All 7 tasks checked off in `tasks.md`.

## Deviations from plan

None. No design decisions were required, so no `decisions.md` was created (the
spec had no open questions).
