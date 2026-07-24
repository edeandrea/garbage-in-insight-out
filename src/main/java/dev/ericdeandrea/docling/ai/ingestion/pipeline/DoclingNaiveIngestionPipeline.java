package dev.ericdeandrea.docling.ai.ingestion.pipeline;

import java.nio.file.Path;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.EmbeddingStoreName;
import io.smallrye.mutiny.Uni;

import dev.ericdeandrea.docling.ai.ingestion.chunking.NaiveChunker;
import dev.ericdeandrea.docling.ai.ingestion.extraction.DoclingExtractor;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Mode B pipeline: Docling structured extraction + {@link NaiveChunker} sentence splitting.
 * Better extraction than Mode A, but the sentence splitter still separates table headers from values.
 */
@ApplicationScoped
class DoclingNaiveIngestionPipeline extends AbstractIngestionPipeline {

    private final DoclingExtractor doclingExtractor;
    private final NaiveChunker naiveChunker;

    DoclingNaiveIngestionPipeline(
            DoclingExtractor doclingExtractor,
            NaiveChunker naiveChunker,
            EmbeddingModel embeddingModel,
            @EmbeddingStoreName("docling-naive") EmbeddingStore<TextSegment> store) {
        super(embeddingModel, store);
        this.doclingExtractor = doclingExtractor;
        this.naiveChunker = naiveChunker;
    }

    @Override
    public Mode mode() {
        return Mode.DOCLING_NAIVE_CHUNK;
    }

    @Override
    Uni<List<TextSegment>> buildSegments(Path documentPath) {
        // Docling produces clean, structured text — tables are properly formatted, headings
        // are separated from body text, and provenance (page number, element type) is preserved.
        return this.doclingExtractor.extract(documentPath)
            // Same sentence-splitter chunker as Mode A. Better extraction = better answers, but
            // the naive chunker still splits table headers from data values into separate chunks.
            // This limitation is what Mode C fixes.
            .map(result -> this.naiveChunker.chunk(result, Mode.DOCLING_NAIVE_CHUNK));
    }
}
