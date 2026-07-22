package dev.ericdeandrea.docling.model;

public enum Mode {
    NAIVE("naive", "Mode A: Naive"),
    DOCLING_NAIVE_CHUNK("docling-naive", "Mode B: Docling + Naive Chunk"),
    DOCLING_HYBRID_CHUNK("docling-hybrid", "Mode C: Docling + Hybrid Chunk");

    private final String storeName;
    private final String displayLabel;

    Mode(String storeName, String displayLabel) {
        this.storeName = storeName;
        this.displayLabel = displayLabel;
    }

    public String storeName() {
        return storeName;
    }

    public String displayLabel() {
        return displayLabel;
    }
}
