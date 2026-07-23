package dev.ericdeandrea.docling.ai;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.EmbeddingStoreName;

import dev.ericdeandrea.docling.config.DemoConfig;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;

@ApplicationScoped
class ModeAwareRetrievalAugmentor implements RetrievalAugmentor {

    private final CurrentMode currentMode;
    private final Map<Mode, EmbeddingStore<TextSegment>> stores;
    private final EmbeddingModel embeddingModel;
    private final DemoConfig demoConfig;

    ModeAwareRetrievalAugmentor(
            CurrentMode currentMode,
            @EmbeddingStoreName("naive") EmbeddingStore<TextSegment> naiveStore,
            @EmbeddingStoreName("docling-naive") EmbeddingStore<TextSegment> doclingNaiveStore,
            @EmbeddingStoreName("docling-hybrid") EmbeddingStore<TextSegment> doclingHybridStore,
            EmbeddingModel embeddingModel,
            DemoConfig demoConfig) {
        this.currentMode = currentMode;
        this.stores = Map.of(
            Mode.NAIVE, naiveStore,
            Mode.DOCLING_NAIVE_CHUNK, doclingNaiveStore,
            Mode.DOCLING_HYBRID_CHUNK, doclingHybridStore
        );
        this.embeddingModel = embeddingModel;
        this.demoConfig = demoConfig;
    }

    @Override
    public AugmentationResult augment(AugmentationRequest request) {
        var mode = currentMode.mode();
        var store = stores.get(mode);

        var retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .maxResults(demoConfig.rag().topK())
            .build();

        var augmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(retriever)
            .build();

        return augmentor.augment(request);
    }
}
