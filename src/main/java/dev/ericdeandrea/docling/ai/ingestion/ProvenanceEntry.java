package dev.ericdeandrea.docling.ai.ingestion;

record ProvenanceEntry(
    int startChar,
    int endChar,
    Integer pageNumber,
    String elementType,
    String elementLabel
) {
}
