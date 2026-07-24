package dev.ericdeandrea.docling.ai.ingestion.pipeline;

import java.nio.file.Path;
import java.util.List;

import io.smallrye.mutiny.Uni;

import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.segment.TextSegment;

/**
 * A composable extract-chunk-embed-store pipeline for a single RAG {@link Mode}.
 *
 * <p>Each implementation wires together an extractor, an optional chunker, an embedding model,
 * and a named Qdrant collection. {@link dev.ericdeandrea.docling.ai.ingestion.IngestionStartup}
 * discovers all implementations via {@code @All List<IngestionPipeline>} and runs them in
 * parallel at startup.</p>
 *
 * <p>Implementations:
 * <ul>
 *   <li><strong>Mode A</strong> — {@code TikaNaiveIngestionPipeline}: Tika extraction + sentence-splitter chunking</li>
 *   <li><strong>Mode B</strong> — {@code DoclingNaiveIngestionPipeline}: Docling extraction + same sentence-splitter</li>
 *   <li><strong>Mode C</strong> — {@code DoclingHybridIngestionPipeline}: Docling extraction + server-side hybrid chunking</li>
 * </ul>
 */
public interface IngestionPipeline {

    /** The RAG mode this pipeline implements. */
    Mode mode();

    /** The Qdrant collection name, derived from {@link Mode#storeName()} by default. */
    default String collectionName() {
        return mode().storeName();
    }

    /**
     * Extracts, chunks, embeds, and stores the document at the given path.
     *
     * @param documentPath path to the source document (e.g. a PDF fixture)
     * @return a {@link Uni} emitting the text segments that were embedded and stored
     */
    Uni<List<TextSegment>> processAndStore(Path documentPath);
}
