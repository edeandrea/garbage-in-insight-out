package dev.ericdeandrea.docling.ai;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RagConfigTest {

    @Inject
    RagConfig ragConfig;

    @Test
    void defaultsAreApplied() {
        assertThat(ragConfig)
            .isNotNull()
            .satisfies(config -> {
                assertThat(config.topK()).isEqualTo(4);
                assertThat(config.maxTokens()).isEqualTo(300);
                assertThat(config.overlap()).isEqualTo(30);
                assertThat(config.fixturePath()).isEqualTo("fixtures/doclaynet-2206.01062v1.pdf");
            });
    }
}
