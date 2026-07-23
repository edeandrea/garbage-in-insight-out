package dev.ericdeandrea.docling.ai.ingestion.extraction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

@ApplicationScoped
public class TikaExtractor {

    private final ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

    public ExtractionResult extract(Path documentPath) {
        try (var inputStream = Files.newInputStream(documentPath)) {
            var document = parser.parse(inputStream);
            return new ExtractionResult(document);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to extract PDF with Tika: %s".formatted(documentPath), e);
        }
    }
}
