package dev.ericdeandrea.docling.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DoclingExtractorTest {

    @Inject
    DoclingExtractor doclingExtractor;

    @Test
    void extractsFixturePdfWithProvenance() {
        var result = doclingExtractor.extract(Path.of("fixtures/doclaynet-2206.01062v1.pdf"));

        assertThat(result)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.document()).isNotNull();
                assertThat(r.document().text()).isNotBlank();
                assertThat(r.hasProvenance()).isTrue();
                assertThat(r.provenance())
                    .isNotEmpty()
                    .allSatisfy(entry -> assertThat(entry.pageNumber()).isNotNull());
            });
    }
}
