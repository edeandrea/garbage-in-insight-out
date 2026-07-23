package dev.ericdeandrea.docling.ai;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import dev.ericdeandrea.docling.config.DemoConfig;

@QuarkusTest
class DemoConfigTest {

    @Inject
    DemoConfig demoConfig;

    @Test
    void defaultsAreApplied() {
        assertThat(demoConfig)
            .isNotNull()
            .satisfies(config -> {
                assertThat(config.rag().topK()).isEqualTo(4);
                assertThat(config.rag().maxTokens()).isEqualTo(300);
                assertThat(config.rag().overlap()).isEqualTo(30);
                assertThat(config.rag().fixturePath()).isEqualTo("fixtures/doclaynet-2206.01062v1.pdf");
            });
    }
}
