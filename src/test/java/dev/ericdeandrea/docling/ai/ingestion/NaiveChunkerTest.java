package dev.ericdeandrea.docling.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.document.Document;

class NaiveChunkerTest {

    final NaiveChunker chunker = new NaiveChunker(new TestRagConfig());

    @Test
    void chunksDocumentWithModeMetadata() {
        var document = Document.from("First sentence about docling. Second sentence about extraction. Third sentence about chunking. Fourth sentence about embeddings. Fifth sentence about retrieval.");
        var result = new ExtractionResult(document);

        var segments = chunker.chunk(result, Mode.NAIVE);

        assertThat(segments)
            .isNotEmpty()
            .allSatisfy(segment -> {
                assertThat(segment.text()).isNotBlank();
                assertThat(segment.metadata().getString("mode")).isEqualTo("NAIVE");
            });
    }

    @Test
    void attachesProvenanceMetadataWhenPresent() {
        var text = "Page one content here. Page two content follows.";
        var document = Document.from(text);
        var provenance = List.of(
            new ProvenanceEntry(0, 22, 1, "PARAGRAPH", null),
            new ProvenanceEntry(23, 48, 2, "PARAGRAPH", null)
        );
        var result = new ExtractionResult(document, provenance);

        var segments = chunker.chunk(result, Mode.DOCLING_NAIVE_CHUNK);

        assertThat(segments)
            .isNotEmpty()
            .allSatisfy(segment ->
                assertThat(segment.metadata().getString("mode")).isEqualTo("DOCLING_NAIVE_CHUNK")
            );

        assertThat(segments)
            .anySatisfy(segment ->
                assertThat(segment.metadata().getInteger("page_number")).isNotNull()
            );
    }

    @Test
    void skipsProvenanceWhenAbsent() {
        var document = Document.from("Some text extracted by tika with no page tracking available.");
        var result = new ExtractionResult(document);

        var segments = chunker.chunk(result, Mode.NAIVE);

        assertThat(segments)
            .isNotEmpty()
            .allSatisfy(segment -> {
                assertThat(segment.metadata().getString("mode")).isEqualTo("NAIVE");
                assertThat(segment.metadata().getInteger("page_number")).isNull();
            });
    }

    @Test
    void enrichesWithExtendedContent() {
        var document = Document.from("First sentence about docling. Second sentence about extraction. Third sentence about chunking. Fourth sentence about embeddings. Fifth sentence about retrieval.");
        var result = new ExtractionResult(document);

        var segments = chunker.chunk(result, Mode.NAIVE);

        assertThat(segments)
            .isNotEmpty()
            .allSatisfy(segment ->
                assertThat(segment.metadata().getString("extended_content")).isNotBlank()
            );
    }

    private static class TestRagConfig implements dev.ericdeandrea.docling.ai.RagConfig {
        @Override public int topK() { return 4; }
        @Override public int maxTokens() { return 50; }
        @Override public int overlap() { return 10; }
        @Override public String fixturePath() { return "fixtures/doclaynet-2206.01062v1.pdf"; }
    }
}
