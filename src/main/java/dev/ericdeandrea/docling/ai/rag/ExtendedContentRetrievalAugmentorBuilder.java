package dev.ericdeandrea.docling.ai.rag;

import java.util.List;

import dev.ericdeandrea.docling.config.DemoConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;

final class ExtendedContentRetrievalAugmentorBuilder {
  private ExtendedContentRetrievalAugmentorBuilder() {
  }

  static RetrievalAugmentor build(EmbeddingStore<TextSegment> embeddingStore,
                                  EmbeddingModel embeddingModel,
                                  DemoConfig config) {

    var retriever = EmbeddingStoreContentRetriever.builder()
                                                  .embeddingStore(embeddingStore)
                                                  .embeddingModel(embeddingModel)
                                                  .maxResults(config.rag().topK())
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
