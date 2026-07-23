package dev.ericdeandrea.docling.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.quarkiverse.langchain4j.qdrant.runtime.QdrantEmbeddingStoreConfig;

@QuarkusTest
class IngestionStartupTest {

    @Inject
    QdrantEmbeddingStoreConfig qdrantConfig;

    @Test
    void skipsIngestionWhenCollectionExists() throws ExecutionException, InterruptedException {
        var host = qdrantConfig.defaultConfig().host().orElse("localhost");
        var port = qdrantConfig.defaultConfig().port();

        try (var client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build())) {
            var collections = client.listCollectionsAsync().get();

            assertThat(collections)
                .containsAll(List.of("naive", "docling_naive_chunk", "docling_hybrid_chunk"));
        }
    }

    @Test
    void collectionsCreatedWithCorrectConfig() throws ExecutionException, InterruptedException {
        var host = qdrantConfig.defaultConfig().host().orElse("localhost");
        var port = qdrantConfig.defaultConfig().port();

        try (var client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build())) {
            var info = client.getCollectionInfoAsync("naive").get();

            assertThat(info.getConfig().getParams().getVectorsConfig()
                .getParams().getSize()).isEqualTo(768);
            assertThat(info.getConfig().getParams().getVectorsConfig()
                .getParams().getDistance()).isEqualTo(Distance.Cosine);
        }
    }
}
