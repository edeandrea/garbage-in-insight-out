package dev.ericdeandrea.docling.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import io.quarkiverse.langchain4j.EmbeddingStoreName;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

@QuarkusTest
class IngestionStartupTest {

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    @EmbeddingStoreName("naive")
    EmbeddingStore<TextSegment> naiveStore;

    @Inject
    @EmbeddingStoreName("docling-naive")
    EmbeddingStore<TextSegment> doclingNaiveStore;

    @Inject
    @EmbeddingStoreName("docling-hybrid")
    EmbeddingStore<TextSegment> doclingHybridStore;

    @Test
    void allStoresPopulatedAfterStartup() {
        var probe = new float[embeddingModel.dimension()];
        Arrays.fill(probe, 1.0f);

        var searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(Embedding.from(probe))
            .maxResults(1)
            .minScore(0.0)
            .build();

        assertThat(naiveStore.search(searchRequest).matches()).isNotEmpty();
        assertThat(doclingNaiveStore.search(searchRequest).matches()).isNotEmpty();
        assertThat(doclingHybridStore.search(searchRequest).matches()).isNotEmpty();
    }
}
