package dev.ericdeandrea.docling.ai.ingestion.pipeline;

import java.nio.file.Path;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusTest;

import dev.ericdeandrea.docling.ai.ingestion.chunking.NaiveChunker;
import dev.ericdeandrea.docling.ai.ingestion.extraction.DoclingExtractor;
import dev.ericdeandrea.docling.ai.ingestion.extraction.TikaExtractor;
import dev.ericdeandrea.docling.model.Mode;

// Diagnostic test that side-by-side compares how Mode A (Tika) and Mode B (Docling)
// chunk the same Table 2 data from the DocLayNet paper.
//
// Skipped by default. Run with -Drun.simulations=true to see console output.
// Requires a real Docling Serve instance (dev services will start one).
//
// The output shows the raw chunk text for each mode, making it visually obvious that:
//   - Mode A chunks are garbled — table values run together, columns merge,
//     unrelated text from adjacent page regions gets spliced in.
//   - Mode B chunks are clean — but column headers ("FRCNN", "YOLOv5") end up
//     in a different chunk from the data values (76.8, 73.4).
// This is the core demo narrative: same chunker, different extraction = different quality.
@QuarkusTest
@EnabledIfSystemProperty(named = "run.simulations", matches = "true")
class ModeAvsModeBTest {

    @Inject
    TikaExtractor tikaExtractor;

    @Inject
    DoclingExtractor doclingExtractor;

    @Inject
    NaiveChunker naiveChunker;

    private static final Path FIXTURE = Path.of("fixtures/doclaynet-2206.01062v1.pdf");

    @Test
    void compareTable2InModeAvsB() {
        System.out.println("\n=== MODE A (Tika) — chunks containing Table 2 markers ===\n");
        var tikaResult = tikaExtractor.extract(FIXTURE);
        var modeASegments = naiveChunker.chunk(tikaResult, Mode.NAIVE);

        System.out.println("Total Mode A segments: " + modeASegments.size());
        var modeATable2 = modeASegments.stream()
            .filter(s -> s.text().contains("76.8") || s.text().contains("73.4")
                || s.text().contains("YOLOv5") || s.text().contains("Table 2"))
            .toList();

        System.out.println("Chunks matching Table 2 markers: " + modeATable2.size());
        for (int i = 0; i < modeATable2.size(); i++) {
            var seg = modeATable2.get(i);
            System.out.printf("\n--- Mode A chunk %d/%d (%d chars) ---%n", i + 1, modeATable2.size(), seg.text().length());
            System.out.println(seg.text());
        }

        System.out.println("\n\n=== MODE B (Docling + same chunker) — chunks containing Table 2 markers ===\n");
        var doclingResult = doclingExtractor.extract(FIXTURE)
            .await()
            .atMost(Duration.ofMinutes(5));
        var modeBSegments = naiveChunker.chunk(doclingResult, Mode.DOCLING_NAIVE_CHUNK);

        System.out.println("Total Mode B segments: " + modeBSegments.size());
        var modeBTable2 = modeBSegments.stream()
            .filter(s -> s.text().contains("76.8") || s.text().contains("73.4")
                || s.text().contains("YOLOv5") || s.text().contains("Table 2"))
            .toList();

        System.out.println("Chunks matching Table 2 markers: " + modeBTable2.size());
        for (int i = 0; i < modeBTable2.size(); i++) {
            var seg = modeBTable2.get(i);
            System.out.printf("\n--- Mode B chunk %d/%d (%d chars) ---%n", i + 1, modeBTable2.size(), seg.text().length());
            System.out.println(seg.text());
        }
    }
}
