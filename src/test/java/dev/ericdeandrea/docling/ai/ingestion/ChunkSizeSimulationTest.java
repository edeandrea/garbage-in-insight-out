package dev.ericdeandrea.docling.ai.ingestion;

import java.nio.file.Path;
import java.util.List;

import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ChunkSizeSimulationTest {

    @Inject
    DoclingExtractor doclingExtractor;

    private static final Path FIXTURE = Path.of("fixtures/doclaynet-2206.01062v1.pdf");

    @Test
    void findTable2Content() {
        var result = doclingExtractor.extract(FIXTURE);
        var fullText = result.document().text();

        System.out.println("\n=== Searching for Table 2 markers in full text ===\n");
        System.out.println("Contains '76.8': " + fullText.contains("76.8"));
        System.out.println("Contains '73.4': " + fullText.contains("73.4"));
        System.out.println("Contains 'YOLOv5x6': " + fullText.contains("YOLOv5x6"));
        System.out.println("Contains 'YOLOv5': " + fullText.contains("YOLOv5"));
        System.out.println("Contains 'Faster R-CNN': " + fullText.contains("Faster R-CNN"));
        System.out.println("Contains 'FRCNN': " + fullText.contains("FRCNN"));

        var idx768 = fullText.indexOf("76.8");
        if (idx768 >= 0) {
            var start = Math.max(0, idx768 - 200);
            var end = Math.min(fullText.length(), idx768 + 200);
            System.out.println("\nContext around '76.8' (pos " + idx768 + "):");
            System.out.println(fullText.substring(start, end));
        }

        var idx734 = fullText.indexOf("73.4");
        if (idx734 >= 0) {
            var start = Math.max(0, idx734 - 200);
            var end = Math.min(fullText.length(), idx734 + 200);
            System.out.println("\nContext around '73.4' (pos " + idx734 + "):");
            System.out.println(fullText.substring(start, end));
        }
    }

    @Test
    void simulateChunkSizes() {
        var result = doclingExtractor.extract(FIXTURE);

        for (var maxTokens : List.of(100, 150, 200, 250, 300)) {
            simulateAtSize(result, maxTokens);
        }
    }

    private void simulateAtSize(ExtractionResult result, int maxTokens) {
        var overlap = maxTokens / 10;
        var splitter = new DocumentBySentenceSplitter(maxTokens, overlap);
        var segments = splitter.split(result.document());

        var table2Segments = segments.stream()
            .filter(s -> s.text().contains("76.8") || s.text().contains("73.4"))
            .toList();

        var allSizes = segments.stream()
            .mapToInt(s -> s.text().length())
            .summaryStatistics();

        System.out.println("\n========================================");
        System.out.printf("maxTokens=%d, overlap=%d%n", maxTokens, overlap);
        System.out.printf("Total segments: %d%n", segments.size());
        System.out.printf("Segment sizes: min=%d, avg=%.0f, max=%d chars%n",
            allSizes.getMin(), allSizes.getAverage(), allSizes.getMax());
        System.out.printf("Chunks containing 76.8 or 73.4: %d%n", table2Segments.size());

        for (int i = 0; i < table2Segments.size(); i++) {
            var seg = table2Segments.get(i);
            var has768 = seg.text().contains("76.8");
            var has734 = seg.text().contains("73.4");

            System.out.printf("  Chunk %d/%d: %d chars | has 76.8=%s | has 73.4=%s%n",
                i + 1, table2Segments.size(), seg.text().length(), has768, has734);
            System.out.println("    Full text:");
            System.out.println("    " + seg.text().replace("\n", "\n    "));
            System.out.println();
        }

        var bothInOneChunk = table2Segments.stream()
            .anyMatch(s -> s.text().contains("76.8") && s.text().contains("73.4"));

        System.out.printf("  VERDICT: 76.8 and 73.4 are %s at maxTokens=%d%n",
            bothInOneChunk ? "IN SAME CHUNK (not fragmented)" : "SPLIT ACROSS CHUNKS (fragmented)",
            maxTokens);
        System.out.println("========================================");
    }
}
