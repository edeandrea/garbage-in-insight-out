package dev.ericdeandrea.docling.ai.ingestion.extraction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import ai.docling.core.DoclingDocument;
import ai.docling.core.DoclingDocument.BaseTextItem;
import ai.docling.core.DoclingDocument.ProvenanceItem;
import ai.docling.core.DoclingDocument.TableItem;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;

import io.quarkiverse.docling.runtime.client.DoclingService;

import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

@ApplicationScoped
public class DoclingExtractor {

    private final DoclingService doclingService;

    DoclingExtractor(DoclingService doclingService) {
        this.doclingService = doclingService;
    }

    public ExtractionResult extract(Path documentPath) {
        try {
            var response = (InBodyConvertDocumentResponse) doclingService.convertFile(documentPath, OutputFormat.JSON);
            var doclingDoc = response.getDocument().getJsonContent();
            var fullText = buildFullText(doclingDoc);
            var provenance = buildProvenance(doclingDoc, fullText);

            return new ExtractionResult(Document.from(fullText), provenance);
        }
        catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to extract document with Docling: %s".formatted(documentPath), e);
        }
    }

    private String buildFullText(DoclingDocument doc) {
        var textParts = doc.getTexts().stream()
            .map(BaseTextItem::getText);

        var tableParts = doc.getTables().stream()
            .map(this::tableToText)
            .filter(text -> !text.isEmpty());

        return Stream.concat(textParts, tableParts)
            .collect(Collectors.joining("\n\n"));
    }

    private String tableToText(TableItem table) {
        var data = table.getData();
        if ((data == null) || (data.getGrid() == null)) {
            return "";
        }

        return data.getGrid().stream()
            .map(row -> row.stream()
                .map(cell -> Objects.requireNonNullElse(cell.getText(), ""))
                .collect(Collectors.joining(" | ")))
            .collect(Collectors.joining("\n"));
    }

    private List<ProvenanceEntry> buildProvenance(DoclingDocument doc, String fullText) {
        var textEntries = doc.getTexts().stream()
            .map(item -> toProvenanceEntry(item.getText(), item.getLabel().toString(), item.getProv(), fullText));

        var tableEntries = doc.getTables().stream()
            .map(table -> toProvenanceEntry(
                tableToText(table),
                Objects.requireNonNullElse(table.getLabel(), "TABLE"),
                table.getProv(),
                fullText));

        return Stream.concat(textEntries, tableEntries)
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<ProvenanceEntry> toProvenanceEntry(String itemText, String elementType,
                                                        List<ProvenanceItem> provItems, String fullText) {
        if ((itemText == null) || itemText.isEmpty()) {
            return Optional.empty();
        }

        var startChar = fullText.indexOf(itemText);
        if (startChar < 0) {
            return Optional.empty();
        }

        var pageNumber = provItems.stream()
            .map(ProvenanceItem::getPageNo)
            .findFirst()
            .orElse(null);

        return Optional.of(
            new ProvenanceEntry(startChar, startChar + itemText.length(), pageNumber, elementType, null));
    }

    public List<TextSegment> extractAndChunk(Path documentPath) {
        try {
            var response = doclingService.chunkFileHybrid(documentPath, OutputFormat.JSON);

            return response.getChunks().stream()
                .map(chunk -> {
                    var metadata = new Metadata()
                        .put("mode", Mode.DOCLING_HYBRID_CHUNK.name());

                    if ((chunk.getPageNumbers() != null) && !chunk.getPageNumbers().isEmpty()) {
                        metadata.put("page_number", chunk.getPageNumbers().getFirst());
                    }

                    if ((chunk.getCaptions() != null) && !chunk.getCaptions().isEmpty()) {
                        metadata.put("element_label", chunk.getCaptions().getFirst());
                    }

                    return TextSegment.from(chunk.getText(), metadata);
                })
                .toList();
        }
        catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to chunk document with Docling: %s".formatted(documentPath), e);
        }
    }
}
