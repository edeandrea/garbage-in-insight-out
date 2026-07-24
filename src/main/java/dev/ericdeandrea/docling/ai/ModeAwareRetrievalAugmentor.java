package dev.ericdeandrea.docling.ai;

import java.util.List;
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
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;

// Routes each chat request to the correct Qdrant collection based on the active mode.
//
// At ingestion time, each mode's pipeline stored its segments into a separate named
// collection (naive, docling-naive, docling-hybrid). At query time, this augmentor
// reads the current mode from the @RequestScoped CurrentMode bean and looks up a
// pre-built retriever pointed at that mode's collection. This is the mechanism that
// lets the demo run three RAG pipelines side-by-side with a single @RegisterAiService.
@ApplicationScoped
class ModeAwareRetrievalAugmentor implements RetrievalAugmentor {

    private final CurrentMode currentMode;

    // Pre-built augmentors — one per mode, each wired to its own Qdrant collection.
    // Built once at construction time; the only thing that changes per request is which one we pick.
    private final Map<Mode, RetrievalAugmentor> augmentors;

    ModeAwareRetrievalAugmentor(
            CurrentMode currentMode,
            @EmbeddingStoreName("naive") EmbeddingStore<TextSegment> naiveStore,
            @EmbeddingStoreName("docling-naive") EmbeddingStore<TextSegment> doclingNaiveStore,
            @EmbeddingStoreName("docling-hybrid") EmbeddingStore<TextSegment> doclingHybridStore,
            EmbeddingModel embeddingModel,
            DemoConfig demoConfig) {
        this.currentMode = currentMode;

        var topK = demoConfig.rag().topK();
        this.augmentors = Map.of(
            Mode.NAIVE, buildAugmentor(naiveStore, embeddingModel, topK),
            Mode.DOCLING_NAIVE_CHUNK, buildAugmentor(doclingNaiveStore, embeddingModel, topK),
            Mode.DOCLING_HYBRID_CHUNK, buildAugmentor(doclingHybridStore, embeddingModel, topK)
        );
    }

    @Override
    public AugmentationResult augment(AugmentationRequest request) {
        // Look up the active mode (set by AssistantService before each chat call)
        // and delegate to the pre-built augmentor for that mode's Qdrant collection.
        return this.augmentors.get(this.currentMode.mode())
            .augment(request);
    }

    private static RetrievalAugmentor buildAugmentor(EmbeddingStore<TextSegment> store,
                                                      EmbeddingModel embeddingModel, int topK) {
        var retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .maxResults(topK)
            .build();

        var contentInjector = DefaultContentInjector.builder()
            .metadataKeysToInclude(List.of("extended_content"))
            .build();

        return DefaultRetrievalAugmentor.builder()
            .contentRetriever(retriever)
            .contentInjector(contentInjector)
            .build();
    }
}
