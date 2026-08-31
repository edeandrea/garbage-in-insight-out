# Tasks 013 — Drop the "Time" column and `ChunkMetadata.timestamp` plumbing

Status: Approved

Ordered by dependency. The record change (task 2) ripples to its producers and
consumer, which are all edited in the same top-down pass so the tree compiles at
the end. Tests are updated alongside so the suite stays green (per project
hygiene: no changed behavior is done without a passing test).

- [x] 1. **`ChatPanel`: remove the "Time" column and formatter.** Delete the
  "Time" `addColumn(...)` block, the `TIMESTAMP_FORMAT` constant, and the unused
  `java.time.ZoneId` and `java.time.format.DateTimeFormatter` imports. Remaining
  columns: Score, Page, Type, Label, Preview. (Req 1, 2)

- [x] 2. **`ChunkMetadata`: remove the `timestamp` component.** Delete the
  `Instant timestamp` component and the `java.time.Instant` import; 5 components
  remain. (Req 3)

- [x] 3. **`ChunkMapper`: drop the `timestamp` param.** Remove the `Instant
  timestamp` parameter from `toRetrievedChunk` and `toMetadata`, change the
  `metadata` `@Mapping` expression to `java(toMetadata(segment, relevanceScore))`,
  drop the `timestamp` argument to `new ChunkMetadata(...)`, and remove the
  `java.time.Instant` import. (Req 5)

- [x] 4. **`AssistantService`: drop `Instant.now()`.** Change the map body to
  `this.chunkMapper.toRetrievedChunk(content.textSegment(), score)` and remove
  the `java.time.Instant` import. (Req 4)

- [x] 5. **Update `ChunkMapperTest`.** Remove the `timestamp` locals, call
  `toRetrievedChunk(segment, <score>)`, remove the
  `chunk.metadata().timestamp()` assertions (folding remaining metadata
  assertions into the chained AssertJ block), and drop the `java.time.Instant`
  import if unreferenced. Verify: `ChunkMapperTest` passes. (Req 6)

- [x] 6. **Update `ChatPanelTest`.** Drop the trailing `Instant.now()` argument
  from `MOCK_CHUNKS` and the inline `new ChunkMetadata(...)` in
  `clickingDifferentRoundMovesHighlight`, remove the `java.time.Instant` import,
  and change `chunksGridHasNoRoundColumn` to assert `hasSize(5)` with an updated
  "5 columns" description. Verify: `ChatPanelTest` passes. (Req 6)

- [x] 7. **Full verification.** Run `./mvnw verify` (full suite incl. failsafe
  ITs — never skip ITs) and confirm green. Confirm no remaining
  `timestamp`/`TIMESTAMP_FORMAT` references in `src/main`/`src/test` Java
  sources. (Req 7)
