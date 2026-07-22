package dev.ericdeandrea.docling.ai.ingestion;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import dev.ericdeandrea.docling.ai.RagConfig;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.quarkiverse.langchain4j.EmbeddingStoreName;
import io.quarkiverse.langchain4j.qdrant.runtime.QdrantEmbeddingStoreConfig;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
class IngestionStartup {

    private static final int VECTOR_SIZE = 768;

    private final TikaExtractor tikaExtractor;
    private final DoclingExtractor doclingExtractor;
    private final NaiveChunker naiveChunker;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> naiveStore;
    private final EmbeddingStore<TextSegment> doclingNaiveStore;
    private final EmbeddingStore<TextSegment> doclingHybridStore;
    private final RagConfig ragConfig;
    private final QdrantEmbeddingStoreConfig qdrantConfig;

    IngestionStartup(
            TikaExtractor tikaExtractor,
            DoclingExtractor doclingExtractor,
            NaiveChunker naiveChunker,
            EmbeddingModel embeddingModel,
            @EmbeddingStoreName("naive") EmbeddingStore<TextSegment> naiveStore,
            @EmbeddingStoreName("docling-naive") EmbeddingStore<TextSegment> doclingNaiveStore,
            @EmbeddingStoreName("docling-hybrid") EmbeddingStore<TextSegment> doclingHybridStore,
            RagConfig ragConfig,
            QdrantEmbeddingStoreConfig qdrantConfig) {
        this.tikaExtractor = tikaExtractor;
        this.doclingExtractor = doclingExtractor;
        this.naiveChunker = naiveChunker;
        this.embeddingModel = embeddingModel;
        this.naiveStore = naiveStore;
        this.doclingNaiveStore = doclingNaiveStore;
        this.doclingHybridStore = doclingHybridStore;
        this.ragConfig = ragConfig;
        this.qdrantConfig = qdrantConfig;
    }

    void onStart(@Observes StartupEvent event) {
        var documentPath = Path.of(ragConfig.fixturePath());
        var defaultConfig = qdrantConfig.defaultConfig();
        var host = defaultConfig.host().orElse("localhost");
        var port = defaultConfig.port();

        try (var client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build())) {
            var existingCollections = client.listCollectionsAsync().get();

            Log.infof("Starting ingestion for document: %s", documentPath);

            ingestModeA(documentPath, client, existingCollections);
            ingestModeB(documentPath, client, existingCollections);
            ingestModeC(documentPath, client, existingCollections);

            Log.info("Ingestion complete");
        }
        catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Ingestion failed", e);
        }
    }

    private void ingestModeA(Path documentPath, QdrantClient client, List<String> existingCollections)
            throws ExecutionException, InterruptedException {
        var collectionName = collectionName("naive");

        if (existingCollections.contains(collectionName)) {
            Log.infof("Mode A collection '%s' already exists, skipping ingestion", collectionName);
            return;
        }

        Log.info("Ingesting Mode A (naive)...");
        createCollection(client, collectionName);
        var result = tikaExtractor.extract(documentPath);
        var segments = naiveChunker.chunk(result, Mode.NAIVE);
        ingest(segments, naiveStore);
        Log.infof("Mode A ingested %d segments", segments.size());
    }

    private void ingestModeB(Path documentPath, QdrantClient client, List<String> existingCollections)
            throws ExecutionException, InterruptedException {
        var collectionName = collectionName("docling-naive");

        if (existingCollections.contains(collectionName)) {
            Log.infof("Mode B collection '%s' already exists, skipping ingestion", collectionName);
            return;
        }

        Log.info("Ingesting Mode B (docling-naive)...");
        createCollection(client, collectionName);
        var result = doclingExtractor.extract(documentPath);
        var segments = naiveChunker.chunk(result, Mode.DOCLING_NAIVE_CHUNK);
        ingest(segments, doclingNaiveStore);
        Log.infof("Mode B ingested %d segments", segments.size());
    }

    private void ingestModeC(Path documentPath, QdrantClient client, List<String> existingCollections)
            throws ExecutionException, InterruptedException {
        var collectionName = collectionName("docling-hybrid");

        if (existingCollections.contains(collectionName)) {
            Log.infof("Mode C collection '%s' already exists, skipping ingestion", collectionName);
            return;
        }

        Log.info("Ingesting Mode C (docling-hybrid)...");
        createCollection(client, collectionName);
        var segments = doclingExtractor.extractAndChunk(documentPath);
        ingest(segments, doclingHybridStore);
        Log.infof("Mode C ingested %d segments", segments.size());
    }

    private String collectionName(String storeName) {
        var namedConfig = qdrantConfig.namedConfig().get(storeName);
        if (namedConfig != null) {
            return namedConfig.collection().name();
        }
        return qdrantConfig.defaultConfig().collection().name();
    }

    private void createCollection(QdrantClient client, String collectionName)
            throws ExecutionException, InterruptedException {
        client.createCollectionAsync(collectionName,
            VectorParams.newBuilder()
                .setSize(VECTOR_SIZE)
                .setDistance(Distance.Cosine)
                .build())
            .get();
    }

    private void ingest(List<TextSegment> segments, EmbeddingStore<TextSegment> store) {
        var ingestor = EmbeddingStoreIngestor.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .build();

        ingestor.ingest(segments.stream()
            .map(segment -> dev.langchain4j.data.document.Document.from(segment.text(), segment.metadata()))
            .toList());
    }
}
