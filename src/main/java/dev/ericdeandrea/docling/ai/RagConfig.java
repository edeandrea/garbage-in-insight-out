package dev.ericdeandrea.docling.ai;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "rag")
public interface RagConfig {

    @WithDefault("4")
    int topK();

    @WithDefault("300")
    int maxTokens();

    @WithDefault("30")
    int overlap();

    @WithDefault("fixtures/doclaynet-2206.01062v1.pdf")
    String fixturePath();
}
