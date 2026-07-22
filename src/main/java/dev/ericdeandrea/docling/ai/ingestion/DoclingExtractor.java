package dev.ericdeandrea.docling.ai.ingestion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ai.docling.core.DoclingDocument;
import ai.docling.core.DoclingDocument.ProvenanceItem;
import ai.docling.core.DoclingDocument.TableItem;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import io.quarkiverse.docling.runtime.client.DoclingService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class DoclingExtractor implements ExtractionStrategy {

    private final DoclingService doclingService;

    DoclingExtractor(DoclingService doclingService) {
        this.doclingService = doclingService;
    }

    @Override
    public ExtractionResult extract(Path documentPath) {
        try {
            var response = (InBodyConvertDocumentResponse) doclingService.convertFile(documentPath, OutputFormat.JSON);
            var doclingDoc = response.getDocument().getJsonContent();
            var fullText = buildFullText(doclingDoc);
            var provenance = buildProvenance(doclingDoc, fullText);
            var document = Document.from(fullText);

            return new ExtractionResult(document, provenance);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to extract document with Docling: %s".formatted(documentPath), e);
        }
    }

    private String buildFullText(DoclingDocument doc) {
        var sb = new StringBuilder();

        for (var item : doc.getTexts()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(item.getText());
        }

        for (var table : doc.getTables()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(table.getData() != null ? tableToText(table) : "");
        }

        return sb.toString();
    }

    private String tableToText(TableItem table) {
        var data = table.getData();
        if (data == null || data.getGrid() == null) {
            return "";
        }

        var sb = new StringBuilder();
        for (var row : data.getGrid()) {
            var cells = row.stream()
                .map(cell -> cell.getText() != null ? cell.getText() : "")
                .toList();
            sb.append(String.join(" | ", cells));
            sb.append("\n");
        }

        return sb.toString();
    }

    private List<ProvenanceEntry> buildProvenance(DoclingDocument doc, String fullText) {
        var entries = new ArrayList<ProvenanceEntry>();

        for (var item : doc.getTexts()) {
            addProvenanceForItem(entries, item.getText(), item.getLabel().toString(), item.getProv(), fullText);
        }

        for (var table : doc.getTables()) {
            var tableText = table.getData() != null ? tableToText(table) : "";
            var label = table.getLabel() != null ? table.getLabel() : "TABLE";
            addProvenanceForItem(entries, tableText, label, table.getProv(), fullText);
        }

        return List.copyOf(entries);
    }

    private void addProvenanceForItem(List<ProvenanceEntry> entries, String itemText,
                                      String elementType, List<ProvenanceItem> provItems,
                                      String fullText) {
        if (itemText == null || itemText.isEmpty()) {
            return;
        }

        var startChar = fullText.indexOf(itemText);
        if (startChar < 0) {
            return;
        }
        var endChar = startChar + itemText.length();

        var pageNumber = provItems.stream()
            .map(ProvenanceItem::getPageNo)
            .findFirst()
            .orElse(null);

        entries.add(new ProvenanceEntry(startChar, endChar, pageNumber, elementType, null));
    }

    List<TextSegment> extractAndChunk(Path documentPath) {
        try {
            var response = doclingService.chunkFileHybrid(documentPath, OutputFormat.JSON);

            return response.getChunks().stream()
                .map(chunk -> {
                    var metadata = new Metadata()
                        .put("mode", Mode.DOCLING_HYBRID_CHUNK.name());

                    if (chunk.getPageNumbers() != null && !chunk.getPageNumbers().isEmpty()) {
                        metadata.put("page_number", chunk.getPageNumbers().getFirst());
                    }

                    if (chunk.getCaptions() != null && !chunk.getCaptions().isEmpty()) {
                        metadata.put("element_label", chunk.getCaptions().getFirst());
                    }

                    return TextSegment.from(chunk.getText(), metadata);
                })
                .toList();
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to chunk document with Docling: %s".formatted(documentPath), e);
        }
    }
}
