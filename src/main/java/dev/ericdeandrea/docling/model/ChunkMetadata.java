package dev.ericdeandrea.docling.model;

public record ChunkMetadata(
    Integer pageNumber,
    String elementType,
    String elementLabel,
    Mode mode,
    double relevanceScore
) {
}
