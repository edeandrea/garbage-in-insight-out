package dev.ericdeandrea.docling.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

class ChunkMapperTest {

    final ChunkMapper mapper = new ChunkMapperImpl();

    @Test
    void mapsSegmentWithFullMetadata() {
        var metadata = new Metadata()
            .put("page_number", 5)
            .put("element_type", "table")
            .put("element_label", "Table 2")
            .put("mode", "DOCLING_NAIVE_CHUNK");
        var segment = TextSegment.from("some chunk text", metadata);
        var timestamp = Instant.now();

        var result = mapper.toRetrievedChunk(segment, 0.87, timestamp);

        assertThat(result)
            .isNotNull()
            .satisfies(chunk -> {
                assertThat(chunk.text()).isEqualTo("some chunk text");
                assertThat(chunk.metadata().pageNumber()).isEqualTo(5);
                assertThat(chunk.metadata().elementType()).isEqualTo("table");
                assertThat(chunk.metadata().elementLabel()).isEqualTo("Table 2");
                assertThat(chunk.metadata().mode()).isEqualTo(Mode.DOCLING_NAIVE_CHUNK);
                assertThat(chunk.metadata().relevanceScore()).isEqualTo(0.87);
                assertThat(chunk.metadata().timestamp()).isEqualTo(timestamp);
            });
    }

    @Test
    void mapsSegmentWithMissingMetadata() {
        var metadata = new Metadata()
            .put("mode", "NAIVE");
        var segment = TextSegment.from("garbled text from tika", metadata);
        var timestamp = Instant.now();

        var result = mapper.toRetrievedChunk(segment, 0.42, timestamp);

        assertThat(result)
            .isNotNull()
            .satisfies(chunk -> {
                assertThat(chunk.text()).isEqualTo("garbled text from tika");
                assertThat(chunk.metadata().pageNumber()).isNull();
                assertThat(chunk.metadata().elementType()).isNull();
                assertThat(chunk.metadata().elementLabel()).isNull();
                assertThat(chunk.metadata().mode()).isEqualTo(Mode.NAIVE);
                assertThat(chunk.metadata().relevanceScore()).isEqualTo(0.42);
                assertThat(chunk.metadata().timestamp()).isEqualTo(timestamp);
            });
    }
}
