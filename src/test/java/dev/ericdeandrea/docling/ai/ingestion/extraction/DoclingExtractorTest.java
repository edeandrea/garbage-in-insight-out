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

    @Test
    void tableProvenanceEntriesHaveCaptionLabel() {
        var result = doclingExtractor.extract(Path.of("fixtures/doclaynet-2206.01062v1.pdf"))
            .await()
            .atMost(Duration.ofMinutes(5));

        var tableEntries = result.provenance().stream()
            .filter(entry -> "TABLE".equalsIgnoreCase(entry.elementType()))
            .toList();

        assertThat(tableEntries)
            .isNotEmpty()
            .anySatisfy(entry ->
                assertThat(entry.elementLabel())
                    .as("At least one table entry should have a caption label")
                    .isNotNull()
                    .containsIgnoringCase("Table")
            );
    }

    @Test
    void captionTextItemsHaveOwnTextAsLabel() {
        var result = doclingExtractor.extract(Path.of("fixtures/doclaynet-2206.01062v1.pdf"))
            .await()
            .atMost(Duration.ofMinutes(5));

        var captionEntries = result.provenance().stream()
            .filter(entry -> "CAPTION".equalsIgnoreCase(entry.elementType()))
            .toList();

        assertThat(captionEntries)
            .isNotEmpty()
            .allSatisfy(entry ->
                assertThat(entry.elementLabel())
                    .as("Caption entries should have their own text as the label")
                    .isNotNull()
                    .isNotBlank()
            );
    }

    @Test
    void ordinaryTextItemsHaveNullLabel() {
        var result = doclingExtractor.extract(Path.of("fixtures/doclaynet-2206.01062v1.pdf"))
            .await()
            .atMost(Duration.ofMinutes(5));

        var textEntries = result.provenance().stream()
            .filter(entry -> "TEXT".equalsIgnoreCase(entry.elementType()))
            .toList();

        assertThat(textEntries)
            .isNotEmpty()
            .allSatisfy(entry ->
                assertThat(entry.elementLabel())
                    .as("Ordinary text entries should have null elementLabel")
                    .isNull()
            );
    }
}
