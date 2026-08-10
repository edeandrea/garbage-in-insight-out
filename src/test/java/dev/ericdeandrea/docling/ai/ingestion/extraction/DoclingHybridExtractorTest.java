package dev.ericdeandrea.docling.ai.ingestion.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import dev.ericdeandrea.docling.DoclingWiremockTestProfile;
import dev.ericdeandrea.docling.model.Mode;

@QuarkusTest
@TestProfile(DoclingWiremockTestProfile.class)
class DoclingHybridExtractorTest {

    @Inject
    DoclingHybridExtractor doclingExtractor;

    @Test
    void chunksFixturePdfWithPageMetadata() {
        var segments = doclingExtractor.extractAndChunk(Path.of("fixtures/doclaynet-2206.01062v1.pdf"))
            .await()
            .atMost(Duration.ofMinutes(5));

        assertThat(segments)
            .isNotEmpty()
            .allSatisfy(segment -> {
                assertThat(segment.text()).isNotBlank();
                assertThat(segment.metadata().getString("mode"))
                    .isEqualTo(Mode.DOCLING_HYBRID_CHUNK.name());
            });

        assertThat(segments)
            .anySatisfy(segment ->
                assertThat(segment.metadata().getInteger("page_number")).isNotNull()
            );
    }

    @Test
    void chunksHaveElementTypeFromDocItems() {
        var segments = doclingExtractor.extractAndChunk(Path.of("fixtures/doclaynet-2206.01062v1.pdf"))
            .await()
            .atMost(Duration.ofMinutes(5));

        assertThat(segments)
            .anySatisfy(segment ->
                assertThat(segment.metadata().getString("element_type"))
                    .as("At least one chunk should have an element_type")
                    .isNotNull()
            );

        var elementTypes = segments.stream()
            .map(segment -> segment.metadata().getString("element_type"))
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        assertThat(elementTypes)
            .as("Should contain a variety of element types")
            .hasSizeGreaterThan(1);
    }

    @Test
    void syntheticPictureSegmentsContainChartLabels() {
        var segments = doclingExtractor.extractAndChunk(Path.of("fixtures/doclaynet-2206.01062v1.pdf"))
            .await()
            .atMost(Duration.ofMinutes(5));

        var pictureSegments = segments.stream()
            .filter(s -> "PICTURE".equals(s.metadata().getString("element_type")))
            .toList();

        assertThat(pictureSegments)
            .as("Should have synthetic picture segments")
            .isNotEmpty();

        assertThat(pictureSegments)
            .anySatisfy(segment -> {
                assertThat(segment.text())
                    .as("At least one picture segment should contain Figure 2 chart labels")
                    .contains("Patents");
                assertThat(segment.text()).contains("8%");
            });
    }
}
