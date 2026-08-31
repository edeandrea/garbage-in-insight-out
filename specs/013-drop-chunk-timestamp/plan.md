# Plan 013 — Drop the retrieved-chunks "Time" column and `ChunkMetadata.timestamp` plumbing

Status: Approved

## Approach

Pure deletion, top-down through the layers. The `timestamp` field is fed from
`AssistantService` → `ChunkMapper` → `ChunkMetadata` → displayed in
`ChatPanel`'s grid. Removing it is a straight-line change: drop the display
column, drop the record component, and drop the two links that produce and carry
the value. No behavior other than the vanished column changes, so the work is
mechanical and the compiler/tests will flag anything missed.

Order chosen so each step compiles against the next (record change ripples out
to its two producers and one consumer, all edited in the same pass):

1. **`ChatPanel`** (consumer) — remove the "Time" column and its formatter.
2. **`ChunkMetadata`** (record) — remove the `timestamp` component.
3. **`ChunkMapper`** (producer) — drop the `timestamp` param + `@Mapping` ref.
4. **`AssistantService`** (producer) — drop `Instant.now()` + import.
5. **Tests** — update constructors, the column-count assertion, and mapper test.

## Files to change

### `src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java`

- Delete the "Time" column `addColumn(...)` block (current lines 108–112).
- Delete the `TIMESTAMP_FORMAT` constant (lines 32–34).
- Remove the now-unused imports `java.time.ZoneId` and
  `java.time.format.DateTimeFormatter` (lines 3–4). No other reference to
  either exists in the file.
- Remaining grid columns unchanged: Score, Page, Type, Label, Preview.

### `src/main/java/dev/ericdeandrea/docling/model/ChunkMetadata.java`

- Remove the `Instant timestamp` component (last component) and the
  `import java.time.Instant;`. Result:
  ```java
  public record ChunkMetadata(
      Integer pageNumber,
      String elementType,
      String elementLabel,
      Mode mode,
      double relevanceScore
  ) {
  }
  ```

### `src/main/java/dev/ericdeandrea/docling/mapping/ChunkMapper.java`

- `toRetrievedChunk`: drop the `Instant timestamp` parameter; change the
  `metadata` `@Mapping` expression to
  `java(toMetadata(segment, relevanceScore))`.
- `toMetadata`: drop the `Instant timestamp` parameter and the `timestamp`
  argument to the `new ChunkMetadata(...)` call.
- Remove `import java.time.Instant;`.

### `src/main/java/dev/ericdeandrea/docling/ai/AssistantService.java`

- Change the map body to
  `this.chunkMapper.toRetrievedChunk(content.textSegment(), score)`.
- Remove `import java.time.Instant;`.

### Tests

- `src/test/java/dev/ericdeandrea/docling/ui/ChatPanelTest.java`:
  - `MOCK_CHUNKS` (lines 47–48) and the inline `new ChunkMetadata(...)` at line
    189 drop the trailing `Instant.now()` argument.
  - Remove the now-unused `import java.time.Instant;`.
  - `chunksGridHasNoRoundColumn` (lines 232–247): assert `hasSize(5)` and update
    the assertion description from "6 columns" to "5 columns". (The
    `doesNotContain("#")` assertion is unaffected.)
- `src/test/java/dev/ericdeandrea/docling/mapping/ChunkMapperTest.java`:
  - Remove the `var timestamp = Instant.now();` locals (lines 25, 47).
  - Call `mapper.toRetrievedChunk(segment, 0.87)` / `(segment, 0.42)`.
  - Remove the `assertThat(chunk.metadata().timestamp()).isEqualTo(timestamp)`
    assertions (lines 38, 60) — fold remaining metadata assertions into the
    existing chained AssertJ block.
  - Remove the `import java.time.Instant;` if no longer referenced.

## Key interfaces / signatures after change

```java
// ChunkMapper
RetrievedChunk toRetrievedChunk(TextSegment segment, double relevanceScore);
default ChunkMetadata toMetadata(TextSegment segment, double relevanceScore) { ... }

// ChunkMetadata
record ChunkMetadata(Integer pageNumber, String elementType,
                     String elementLabel, Mode mode, double relevanceScore) {}
```

## Tradeoffs / alternatives considered

- **Keep the column but source a meaningful value (retrieval latency, rank,
  document date):** rejected — out of scope per the spec; the point is to remove
  dead signal, not invent a replacement. A future spec can add real retrieval
  metadata if wanted.
- **Keep `timestamp` on the record but stop displaying it:** rejected — it would
  leave `Instant.now()` plumbing that produces an unused field, exactly the dead
  code this spec removes.
- **MapStruct regeneration:** no annotation-processor config change needed;
  removing the parameter and the `@Mapping` expression reference is sufficient
  and the generated `ChunkMapperImpl` is rebuilt on compile.

## Verification

- `./mvnw verify` — full suite incl. failsafe (never skip ITs) passes.
- No remaining references to `timestamp` / `TIMESTAMP_FORMAT` in
  `src/main`/`src/test` Java sources (the frontend `generated/` matches are
  Vaadin/Copilot artifacts, unrelated).

## Open questions

None.
