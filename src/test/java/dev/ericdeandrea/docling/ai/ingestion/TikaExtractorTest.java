package dev.ericdeandrea.docling.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TikaExtractorTest {

    @Inject
    TikaExtractor tikaExtractor;

    @Test
    void extractsFixturePdfWithNonEmptyText() {
        var result = tikaExtractor.extract(Path.of("fixtures/doclaynet-2206.01062v1.pdf"));

        assertThat(result)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.document()).isNotNull();
                assertThat(r.document().text()).isNotBlank();
                assertThat(r.hasProvenance()).isFalse();
                assertThat(r.provenance()).isEmpty();
            });
    }
}
