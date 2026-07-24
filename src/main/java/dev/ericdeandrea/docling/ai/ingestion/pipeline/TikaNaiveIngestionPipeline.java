package dev.ericdeandrea.docling.ai.ingestion.pipeline;

import java.nio.file.Path;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.EmbeddingStoreName;
import io.smallrye.mutiny.Uni;

import dev.ericdeandrea.docling.ai.ingestion.chunking.NaiveChunker;
import dev.ericdeandrea.docling.ai.ingestion.extraction.TikaExtractor;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Mode A pipeline: Apache Tika plain-text extraction + {@link NaiveChunker} sentence splitting.
 * Demonstrates how low-fidelity extraction produces garbled chunks that mislead the LLM.
 */
@ApplicationScoped
class TikaNaiveIngestionPipeline extends AbstractIngestionPipeline {

    private final TikaExtractor tikaExtractor;
    private final NaiveChunker naiveChunker;

    TikaNaiveIngestionPipeline(
            TikaExtractor tikaExtractor,
            NaiveChunker naiveChunker,
            EmbeddingModel embeddingModel,
            @EmbeddingStoreName("naive") EmbeddingStore<TextSegment> store) {
        super(embeddingModel, store);
        this.tikaExtractor = tikaExtractor;
        this.naiveChunker = naiveChunker;
    }

    @Override
    public Mode mode() {
        return Mode.NAIVE;
    }

    @Override
    Uni<List<TextSegment>> buildSegments(Path documentPath) {
        return Uni.createFrom().item(() -> {
            // Tika extracts raw text — no structural awareness of tables, headings, or layout.
            // This is the "garbage in" step: table rows get concatenated, columns merge, and
            // unrelated text from adjacent page regions gets spliced together.
            var result = this.tikaExtractor.extract(documentPath);

            // Same sentence-splitter chunker as Mode B — the only variable is extraction quality.
            return this.naiveChunker.chunk(result, Mode.NAIVE);
        });
    }
}
