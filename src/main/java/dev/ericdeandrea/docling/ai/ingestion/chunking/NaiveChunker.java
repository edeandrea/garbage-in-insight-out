package dev.ericdeandrea.docling.ai.ingestion.chunking;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.enterprise.context.ApplicationScoped;

import dev.ericdeandrea.docling.config.DemoConfig;
import dev.ericdeandrea.docling.ai.ingestion.extraction.ExtractionResult;
import dev.ericdeandrea.docling.ai.ingestion.extraction.ProvenanceEntry;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;

@ApplicationScoped
public class NaiveChunker {

    private final DemoConfig demoConfig;

    NaiveChunker(DemoConfig demoConfig) {
        this.demoConfig = demoConfig;
    }

    public List<TextSegment> chunk(ExtractionResult result, Mode mode) {
        var splitter = new DocumentBySentenceSplitter(demoConfig.rag().maxTokens(), demoConfig.rag().overlap());
        var segments = splitter.split(result.document());
        enrichWithSurroundingContext(segments, 2, 2);

        return segments.stream()
            .map(segment -> attachMetadata(segment, result, mode))
            .toList();
    }

    private TextSegment attachMetadata(TextSegment segment, ExtractionResult result, Mode mode) {
        var metadata = segment.metadata().copy();
        metadata.put("mode", mode.name());

        if (result.hasProvenance()) {
            var fullText = result.document().text();
            var segmentStart = fullText.indexOf(segment.text());

            if (segmentStart >= 0) {
                var segmentEnd = segmentStart + segment.text().length();

                result.provenance().stream()
                    .filter(entry -> overlaps(entry, segmentStart, segmentEnd))
                    .findFirst()
                    .ifPresent(entry -> {
                        if (entry.pageNumber() != null) {
                            metadata.put("page_number", entry.pageNumber());
                        }
                        if (entry.elementType() != null) {
                            metadata.put("element_type", entry.elementType());
                        }
                        if (entry.elementLabel() != null) {
                            metadata.put("element_label", entry.elementLabel());
                        }
                    });
            }
        }

        return TextSegment.from(segment.text(), metadata);
    }

    private boolean overlaps(ProvenanceEntry entry, int segmentStart, int segmentEnd) {
        return (entry.startChar() < segmentEnd) && (entry.endChar() > segmentStart);
    }

    // https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-ingestion.html
    private void enrichWithSurroundingContext(List<TextSegment> segments, int before, int after) {
        for (int i = 0; i < segments.size(); i++) {
            var extendedContent = IntStream.rangeClosed(i - before, i + after)
                .filter(j -> (j >= 0) && (j < segments.size()))
                .mapToObj(j -> segments.get(j).text())
                .collect(Collectors.joining(" "));
            segments.get(i).metadata().put("extended_content", extendedContent);
        }
    }
}
