# Spec 013 — Drop the retrieved-chunks "Time" column and the `ChunkMetadata.timestamp` plumbing

Status: Approved

## Summary

Remove the "Time" column from the retrieved-chunks grid in `ChatPanel`, and
remove the `ChunkMetadata.timestamp` field it displays along with all of its
plumbing: the `Instant.now()` captured in `AssistantService`, and the
`timestamp` parameter threaded through `ChunkMapper.toRetrievedChunk` /
`toMetadata`.

## Motivation

The "Time" column shows a timestamp that is just `Instant.now()` captured at the
moment a chunk is retrieved (`AssistantService.java:53`). It conveys nothing
about the document, the chunk, or the retrieval quality — every chunk in a
response gets essentially the same wall-clock instant, which is meaningless to
the audience during the demo. It adds a grid column and a metadata field that
carry no signal. Removing it declutters the chunk table for the talk and
deletes dead plumbing from the model, mapper, and service layers.

## Requirements

1. The retrieved-chunks grid in `ChatPanel` no longer has a "Time" column; the
   remaining columns (Score, Page, Type, Label, Preview) are unchanged in order,
   content, resizability, and part-name/color behavior.
2. The `TIMESTAMP_FORMAT` `DateTimeFormatter` constant in `ChatPanel` is removed
   (nothing references it after the column is gone).
3. The `timestamp` component is removed from the `ChunkMetadata` record; the
   remaining components (`pageNumber`, `elementType`, `elementLabel`, `mode`,
   `relevanceScore`) are unchanged.
4. `AssistantService` no longer captures or passes `Instant.now()` when mapping
   retrieved content to a `RetrievedChunk`; the unused `java.time.Instant`
   import is removed.
5. `ChunkMapper.toRetrievedChunk` and `toMetadata` no longer take a `timestamp`
   parameter, and the MapStruct `@Mapping` expression no longer references it;
   the unused `java.time.Instant` import is removed.
6. All tests that construct `ChunkMetadata` or call `ChunkMapper` are updated to
   the new shapes: `ChatPanelTest.MOCK_CHUNKS` and its other inline
   `new ChunkMetadata(...)` calls drop the timestamp argument;
   `ChatPanelTest.chunksGridHasNoRoundColumn` asserts 5 columns instead of 6;
   `ChunkMapperTest` drops the `timestamp` local, the timestamp argument, and the
   `chunk.metadata().timestamp()` assertions.
7. The full build (`./mvnw verify`) passes with the changes.

## Out of scope

- Replacing the timestamp with any other retrieval metadata (e.g., retrieval
  latency, rank, or a document-derived date). This spec only removes; it does
  not add a substitute.
- Any change to the other grid columns, chunk part-name/color logic, or the
  "Retrieved Chunks (N)" header.
- Any change to mode semantics, RAG behavior, or the embedding/retrieval
  pipeline.

## Open questions

None.
