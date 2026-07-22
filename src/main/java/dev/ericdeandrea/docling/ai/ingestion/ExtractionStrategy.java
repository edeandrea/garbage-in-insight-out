package dev.ericdeandrea.docling.ai.ingestion;

import java.nio.file.Path;

interface ExtractionStrategy {
    ExtractionResult extract(Path documentPath);
}
