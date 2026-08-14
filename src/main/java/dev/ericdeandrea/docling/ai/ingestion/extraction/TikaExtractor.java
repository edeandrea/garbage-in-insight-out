package dev.ericdeandrea.docling.ai.ingestion.extraction;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

@ApplicationScoped
public class TikaExtractor {
  private final ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

  public ExtractionResult extract(Path documentPath) {
    return new ExtractionResult(FileSystemDocumentLoader.loadDocument(documentPath, parser));
  }
}
