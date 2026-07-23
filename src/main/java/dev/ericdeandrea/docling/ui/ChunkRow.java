package dev.ericdeandrea.docling.ui;

import dev.ericdeandrea.docling.model.RetrievedChunk;

record ChunkRow(
    int round,
    RetrievedChunk chunk
) {
}
