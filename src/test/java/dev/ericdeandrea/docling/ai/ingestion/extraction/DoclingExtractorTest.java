package dev.ericdeandrea.docling.ai.ingestion.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import dev.ericdeandrea.docling.DoclingWiremockTestProfile;

@QuarkusTest
@TestProfile(DoclingWiremockTestProfile.class)
class DoclingExtractorTest {

    @Inject
    DoclingExtractor doclingExtractor;

    @Test
    void extractsFixturePdfWithProvenance() {
        var result = doclingExtractor.extract(Path.of("fixtures/doclaynet-2206.01062v1.pdf"))
            .await()
            .atMost(Duration.ofMinutes(5));

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
