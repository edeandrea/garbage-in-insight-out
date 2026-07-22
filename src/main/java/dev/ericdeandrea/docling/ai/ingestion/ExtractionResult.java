package dev.ericdeandrea.docling.ai.ingestion;

import java.util.List;

import dev.langchain4j.data.document.Document;

record ExtractionResult(
    Document document,
    List<ProvenanceEntry> provenance
) {

    ExtractionResult(Document document) {
        this(document, List.of());
    }

    boolean hasProvenance() {
        return !provenance.isEmpty();
    }
}
