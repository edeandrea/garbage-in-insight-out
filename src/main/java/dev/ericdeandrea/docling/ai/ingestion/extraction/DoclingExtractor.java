package dev.ericdeandrea.docling.ai.ingestion.extraction;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import ai.docling.core.DoclingDocument;
import ai.docling.core.DoclingDocument.BaseTextItem;
import ai.docling.core.DoclingDocument.DocItemLabel;
import ai.docling.core.DoclingDocument.ProvenanceItem;
import ai.docling.core.DoclingDocument.TableItem;
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;

import io.smallrye.mutiny.Uni;

import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

@ApplicationScoped
public class DoclingExtractor {

    private static final ConvertDocumentOptions JSON_OPTIONS = ConvertDocumentOptions.builder()
        .toFormat(OutputFormat.JSON)
        .build();

    private final DoclingServeApi doclingServeApi;

    DoclingExtractor(DoclingServeApi doclingServeApi) {
        this.doclingServeApi = doclingServeApi;
    }

    public Uni<ExtractionResult> extract(Path documentPath) {
        var request = ConvertDocumentRequest.builder()
            .options(JSON_OPTIONS)
            .build();

        return Uni.createFrom()
            .completionStage(() -> this.doclingServeApi.convertFilesAsync(request, documentPath))
            .map(InBodyConvertDocumentResponse.class::cast)
            .map(response -> response.getDocument().getJsonContent())
            .map(doclingDoc -> {
                var fullText = buildFullText(doclingDoc);
                var provenance = buildProvenance(doclingDoc, fullText);
                return new ExtractionResult(Document.from(fullText), provenance);
            });
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
        return Optional.of(table.getData())
            .map(DoclingDocument.TableData::getGrid)
            .map(grid -> grid.stream()
                .map(row -> row.stream()
                    .map(cell -> Objects.requireNonNullElse(cell.getText(), ""))
                    .collect(Collectors.joining(" | ")))
                .collect(Collectors.joining("\n")))
            .orElse("");
    }

    private List<ProvenanceEntry> buildProvenance(DoclingDocument doc, String fullText) {
        var index = DocItemIndex.of(doc);

        var textEntries = doc.getTexts().stream()
            .map(item -> {
                var elementLabel = (item.getLabel() == DocItemLabel.CAPTION) ? item.getText() : null;
                return toProvenanceEntry(item.getText(), item.getLabel().toString(), elementLabel, item.getProv(), fullText);
            });

        var tableEntries = doc.getTables().stream()
            .map(table -> toProvenanceEntry(
                tableToText(table),
                Objects.requireNonNullElse(table.getLabel(), "TABLE"),
                index.captionTextFor(table).orElse(null),
                table.getProv(),
                fullText));

        return Stream.concat(textEntries, tableEntries)
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<ProvenanceEntry> toProvenanceEntry(String itemText, String elementType,
                                                        String elementLabel,
                                                        List<ProvenanceItem> provItems, String fullText) {
        return Optional.ofNullable(itemText)
            .filter(text -> !text.isEmpty())
            .map(fullText::indexOf)
            .filter(startChar -> (startChar >= 0))
            .map(startChar -> {
                var pageNumber = provItems.stream()
                    .map(ProvenanceItem::getPageNo)
                    .findFirst()
                    .orElse(null);

                return new ProvenanceEntry(startChar, startChar + itemText.length(), pageNumber, elementType, elementLabel);
            });
    }

    public Uni<List<TextSegment>> extractAndChunk(Path documentPath) {
        var request = HybridChunkDocumentRequest.builder()
            .options(JSON_OPTIONS)
            .includeConvertedDoc(true)
            .build();

        return Uni.createFrom()
            .completionStage(() -> this.doclingServeApi.chunkFilesWithHybridChunkerAsync(request, documentPath))
            .map(response -> {
                var index = DocItemIndex.of(
                    response.getDocuments().getFirst().getContent().getJsonContent()
                );

                return response.getChunks()
                    .stream()
                    .map(chunk -> {
                        var metadata = new Metadata()
                            .put("mode", Mode.DOCLING_HYBRID_CHUNK.name());

                        if ((chunk.getPageNumbers() != null) && !chunk.getPageNumbers().isEmpty()) {
                            metadata.put("page_number", chunk.getPageNumbers().getFirst());
                        }

                        if ((chunk.getDocItems() != null) && !chunk.getDocItems().isEmpty()) {
                            var firstRef = chunk.getDocItems().getFirst();

                            index.labelFor(firstRef)
                                .ifPresent(label -> metadata.put("element_type", label));

                            var caption = ((chunk.getCaptions() != null) && !chunk.getCaptions().isEmpty())
                                ? Optional.of(chunk.getCaptions().getFirst())
                                : index.resolvedCaptionFor(firstRef);

                            caption.ifPresent(text -> metadata.put("element_label", text));
                        }
                        else if ((chunk.getCaptions() != null) && !chunk.getCaptions().isEmpty()) {
                            metadata.put("element_label", chunk.getCaptions().getFirst());
                        }

                        return TextSegment.from(chunk.getText(), metadata);
                    })
                    .toList();
            });
    }
}
