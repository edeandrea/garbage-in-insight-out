package dev.ericdeandrea.docling.ai.ingestion.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import dev.ericdeandrea.docling.DoclingWiremockTestProfile;
import dev.ericdeandrea.docling.ai.ingestion.chunking.NaiveChunker;
import dev.ericdeandrea.docling.ai.ingestion.extraction.DoclingExtractor;
import dev.ericdeandrea.docling.ai.ingestion.extraction.TikaExtractor;
import dev.ericdeandrea.docling.model.Mode;

@QuarkusTest
@TestProfile(DoclingWiremockTestProfile.class)
class ChunkSizeValidationTest {

    @Inject
    TikaExtractor tikaExtractor;

    @Inject
    DoclingExtractor doclingExtractor;

    @Inject
    NaiveChunker naiveChunker;

    private static final Path FIXTURE = Path.of("fixtures/doclaynet-2206.01062v1.pdf");

    @Test
    void modeATable2ChunksAreGarbled() {
        var result = tikaExtractor.extract(FIXTURE);
        var segments = naiveChunker.chunk(result, Mode.NAIVE);

        var table2Chunks = segments.stream()
            .filter(s -> s.text().contains("76.8") || s.text().contains("73.4"))
            .toList();

        assertThat(table2Chunks).isNotEmpty();

        assertThat(table2Chunks)
            .allSatisfy(chunk -> {
                assertThat(chunk.metadata().getInteger("page_number")).isNull();
                assertThat(chunk.metadata().getString("element_type")).isNull();
            });
    }

    @Test
    void modeBTable2ValuesLackColumnHeaders() {
        var result = doclingExtractor.extract(FIXTURE)
            .await()
            .atMost(Duration.ofMinutes(5));
        var segments = naiveChunker.chunk(result, Mode.DOCLING_NAIVE_CHUNK);

        var chunkWithValues = segments.stream()
            .filter(s -> s.text().contains("76.8") && s.text().contains("73.4"))
            .findFirst();

        assertThat(chunkWithValues)
            .isPresent()
            .hasValueSatisfying(chunk -> {
                assertThat(chunk.text())
                    .doesNotContain("YOLOv5x6")
                    .doesNotContain("FRCNN")
                    .doesNotContain("Faster R-CNN");
            });
    }

    @Test
    void modeCTable2HasSelfDescribingValues() {
        var segments = doclingExtractor.extractAndChunk(FIXTURE)
            .await()
            .atMost(Duration.ofMinutes(5));

        var chunkWithValues = segments.stream()
            .filter(s -> s.text().contains("76.8") && s.text().contains("73.4"))
            .findFirst();

        assertThat(chunkWithValues)
            .isPresent()
            .hasValueSatisfying(chunk ->
                assertThat(chunk.text())
                    .contains("YOLO")
                    .contains("FRCNN")
            );
    }
}
