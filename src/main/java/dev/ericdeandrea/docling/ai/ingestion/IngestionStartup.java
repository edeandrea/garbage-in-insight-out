package dev.ericdeandrea.docling.ai.ingestion;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.quarkiverse.langchain4j.qdrant.runtime.QdrantEmbeddingStoreConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import dev.ericdeandrea.docling.config.DemoConfig;
import dev.ericdeandrea.docling.ai.ingestion.pipeline.IngestionPipeline;

@ApplicationScoped
class IngestionStartup {

    private static final int VECTOR_SIZE = 768;

    private final Instance<IngestionPipeline> pipelines;
    private final DemoConfig demoConfig;
    private final QdrantEmbeddingStoreConfig qdrantConfig;

    IngestionStartup(
            Instance<IngestionPipeline> pipelines,
            DemoConfig demoConfig,
            QdrantEmbeddingStoreConfig qdrantConfig) {
        this.pipelines = pipelines;
        this.demoConfig = demoConfig;
        this.qdrantConfig = qdrantConfig;
    }

    void onStart(@Observes StartupEvent event) {
        var documentPath = Path.of(demoConfig.rag().fixturePath());
        var defaultConfig = qdrantConfig.defaultConfig();
        var host = defaultConfig.host().orElse("localhost");
        var port = defaultConfig.port();

        try (var client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build())) {
            var existingCollections = client.listCollectionsAsync().get();

            Log.infof("Starting ingestion for document: %s", documentPath);

            var unis = pipelines.stream()
                .map(pipeline -> Uni.createFrom().voidItem()
                    .invoke(() -> runPipeline(pipeline, documentPath, client, existingCollections))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()))
                .toList();

            Uni.join()
                .all(unis)
                .andCollectFailures()
                .onFailure()
                .invoke(t -> Log.error("Ingestion failed for one or more modes", t))
                .await()
                .atMost(Duration.ofMinutes(10));

            Log.info("Ingestion complete");
        }
        catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Ingestion failed", e);
        }
    }

    private void runPipeline(IngestionPipeline pipeline, Path documentPath,
                             QdrantClient client, List<String> existingCollections) {
        var collectionName = resolveCollectionName(pipeline);

        if (existingCollections.contains(collectionName)) {
            Log.infof("%s collection '%s' already exists, skipping ingestion",
                pipeline.mode().displayLabel(), collectionName);
            return;
        }

        Log.infof("Ingesting %s...", pipeline.mode().displayLabel());
        createCollection(client, collectionName);
        var segments = pipeline.processAndStore(documentPath);
        Log.infof("%s ingested %d segments", pipeline.mode().displayLabel(), segments.size());
    }

    private String resolveCollectionName(IngestionPipeline pipeline) {
        return Optional.ofNullable(qdrantConfig.namedConfig().get(pipeline.collectionName()))
            .map(namedConfig -> namedConfig.collection().name())
            .orElseGet(() -> qdrantConfig.defaultConfig().collection().name());
    }

    private void createCollection(QdrantClient client, String collectionName) {
        try {
            client.createCollectionAsync(collectionName,
                VectorParams.newBuilder()
                    .setSize(VECTOR_SIZE)
                    .setDistance(Distance.Cosine)
                    .build())
                .get();
        }
        catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to create Qdrant collection: %s".formatted(collectionName), e);
        }
    }
}
