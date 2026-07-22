package dev.ericdeandrea.docling.model;

import java.time.Instant;

public record ChunkMetadata(
    Integer pageNumber,
    String elementType,
    String elementLabel,
    Mode mode,
    double relevanceScore,
    Instant timestamp
) {
}
