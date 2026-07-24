package dev.ericdeandrea.docling.ai.ingestion.pipeline;

import java.nio.file.Path;
import java.util.List;

import io.smallrye.mutiny.Uni;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

/**
 * Shared embed-and-store logic for all pipeline modes.
 *
 * <p>Subclasses only implement {@link #mode()} and {@link #buildSegments(Path)} — the
 * extraction and chunking step that differs between modes. Everything else (embedding,
 * storing into Qdrant) is identical and handled here. This makes it clear to demo viewers
 * that the only variable across modes is how segments are built.</p>
 */
abstract class AbstractIngestionPipeline implements IngestionPipeline {

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> store;

    // CDI proxy constructor
    AbstractIngestionPipeline() {}

    AbstractIngestionPipeline(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> store) {
        this.embeddingModel = embeddingModel;
        this.store = store;
    }

    /**
     * Extract and chunk the document — the step that differs between modes.
     * Mode A uses Tika + sentence splitter, Mode B uses Docling + sentence splitter,
     * Mode C uses Docling's server-side hybrid chunker.
     */
    abstract Uni<List<TextSegment>> buildSegments(Path documentPath);

    @Override
    public final Uni<List<TextSegment>> processAndStore(Path documentPath) {
        return buildSegments(documentPath)
            .invoke(segments ->
                EmbeddingStoreIngestor.builder()
                    .embeddingStore(this.store)
                    .embeddingModel(this.embeddingModel)
                    .build()
                    .ingest(segments.stream()
                        .map(s -> Document.from(s.text(), s.metadata()))
                        .toList()));
    }
}
