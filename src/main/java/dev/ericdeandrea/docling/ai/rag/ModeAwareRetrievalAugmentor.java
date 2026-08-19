package dev.ericdeandrea.docling.ai.rag;

import static dev.ericdeandrea.docling.ai.rag.ExtendedContentRetrievalAugmentorBuilder.build;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.EmbeddingStoreName;

import dev.ericdeandrea.docling.ai.CurrentMode;
import dev.ericdeandrea.docling.config.DemoConfig;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;

// Routes each chat request to the correct embedding store based on the active mode.
//
// At ingestion time, each mode's pipeline stored its segments into a separate named
// store (naive, docling-naive, docling-hybrid). At query time, this augmentor
// reads the current mode from the @RequestScoped CurrentMode bean and looks up a
// pre-built retriever pointed at that mode's store. This is the mechanism that
// lets the demo run three RAG pipelines side-by-side with a single @RegisterAiService.
@ApplicationScoped
class ModeAwareRetrievalAugmentor implements RetrievalAugmentor {
  private final CurrentMode currentMode;

  // Pre-built augmentors — one per mode, each wired to its own embedding store.
  // Built once at construction time; the only thing that changes per request is which one we pick.
  private final Map<Mode, RetrievalAugmentor> augmentors;

  ModeAwareRetrievalAugmentor(CurrentMode currentMode,
                              @EmbeddingStoreName("naive") EmbeddingStore<TextSegment> naiveStore,
                              @EmbeddingStoreName("docling-naive") EmbeddingStore<TextSegment> doclingNaiveStore,
                              @EmbeddingStoreName("docling-hybrid") EmbeddingStore<TextSegment> doclingHybridStore,
                              EmbeddingModel embeddingModel,
                              DemoConfig demoConfig) {

    this.currentMode = currentMode;
    this.augmentors = Map.of(
      Mode.NAIVE, build(naiveStore, embeddingModel, demoConfig),
      Mode.DOCLING_NAIVE_CHUNK, build(doclingNaiveStore, embeddingModel, demoConfig),
      Mode.DOCLING_HYBRID_CHUNK, build(doclingHybridStore, embeddingModel, demoConfig));
  }

  @Override
  public AugmentationResult augment(AugmentationRequest request) {
    // Look up the active mode (set by AssistantService before each chat call)
    // and delegate to the pre-built augmentor for that mode's embedding store.
    return this.augmentors
      .get(this.currentMode.mode())
      .augment(request);
  }
}
