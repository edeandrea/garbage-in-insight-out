package dev.ericdeandrea.docling.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import dev.ericdeandrea.docling.model.Mode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DoclingHybridChunkingTest {

    @Inject
    DoclingExtractor doclingExtractor;

    @Test
    void chunksFixturePdfWithPageMetadata() {
        var segments = doclingExtractor.extractAndChunk(Path.of("fixtures/doclaynet-2206.01062v1.pdf"));

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
}
