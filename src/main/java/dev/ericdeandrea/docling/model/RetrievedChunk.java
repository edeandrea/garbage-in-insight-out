package dev.ericdeandrea.docling.model;

public record RetrievedChunk(
    String text,
    ChunkMetadata metadata
) {
}
