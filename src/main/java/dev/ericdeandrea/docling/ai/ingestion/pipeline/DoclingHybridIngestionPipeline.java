package dev.ericdeandrea.docling.ai.ingestion.pipeline;

import java.nio.file.Path;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.EmbeddingStoreName;
import io.smallrye.mutiny.Uni;

import dev.ericdeandrea.docling.ai.ingestion.extraction.DoclingExtractor;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Mode C pipeline: Docling structured extraction + server-side hybrid chunking.
 * Produces self-describing chunks that preserve table structure, so the LLM can answer
 * questions that require correlating column headers with data values.
 */
@ApplicationScoped
class DoclingHybridIngestionPipeline extends AbstractIngestionPipeline {

    private final DoclingExtractor doclingExtractor;

    DoclingHybridIngestionPipeline(
            DoclingExtractor doclingExtractor,
            EmbeddingModel embeddingModel,
            @EmbeddingStoreName("docling-hybrid") EmbeddingStore<TextSegment> store) {
        super(embeddingModel, store);
        this.doclingExtractor = doclingExtractor;
    }

    @Override
    public Mode mode() {
        return Mode.DOCLING_HYBRID_CHUNK;
    }

    @Override
    Uni<List<TextSegment>> buildSegments(Path documentPath) {
        // Docling's hybrid chunker runs server-side and is structure-aware: it keeps table
        // rows together as self-describing triplets (e.g. "All, FRCNN.R101 = 73.4") instead
        // of splitting column headers from data values. This is the "insight out" step —
        // the LLM can answer from the chunk alone without needing to correlate across chunks.
        return this.doclingExtractor.extractAndChunk(documentPath);
    }
}
