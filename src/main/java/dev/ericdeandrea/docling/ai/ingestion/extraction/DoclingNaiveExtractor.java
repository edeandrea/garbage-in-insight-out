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
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;

import io.smallrye.mutiny.Uni;

import dev.langchain4j.data.document.Document;

/**
 * Mode B extractor: calls Docling Serve's {@code /convert} endpoint to get a structured
 * {@link DoclingDocument}, then assembles a single continuous text and character-level
 * provenance so the downstream {@link dev.ericdeandrea.docling.ai.ingestion.chunking.NaiveChunker}
 * can attach page/element metadata to each sentence-split chunk.
 */
@ApplicationScoped
public class DoclingNaiveExtractor {

    private final DoclingServeApi doclingServeApi;

    DoclingNaiveExtractor(DoclingServeApi doclingServeApi) {
        this.doclingServeApi = doclingServeApi;
    }

    /**
     * Sends the document to Docling Serve's /convert endpoint, which returns a structured
     * {@link ai.docling.core.DoclingDocument} with classified elements (headings, paragraphs,
     * tables, captions). We flatten that into a single continuous text for the naive chunker
     * and build character-level provenance so each sentence-split chunk can inherit its
     * source element's page number and type.
     */
    public Uni<ExtractionResult> extract(Path documentPath) {
        var options = ConvertDocumentOptions.builder()
            .toFormat(OutputFormat.JSON)
            .build();

        var request = ConvertDocumentRequest.builder()
            .options(options)
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

    // Docling returns structured items (paragraphs, headings, tables), but the naive chunker
    // needs a single continuous string to sentence-split. This flattens everything into one text
    // block so character offsets in provenance entries remain meaningful.
    private String buildFullText(DoclingDocument doc) {
        var textParts = doc.getTexts().stream()
            .map(BaseTextItem::getText);

        var tableParts = doc.getTables().stream()
            .map(this::tableToText)
            .filter(text -> !text.isEmpty());

        return Stream.concat(textParts, tableParts)
            .collect(Collectors.joining("\n\n"));
    }

    // Tables are grid-structured data — render as pipe-delimited rows so they embed as readable
    // text rather than being silently dropped from the full-text representation.
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

    // Provenance maps each document element back to its character range in the full text.
    // The naive chunker uses these offsets to inherit page number and element type metadata
    // onto the sentence-split chunks it produces — without provenance, chunks lose all
    // structural context from the original document.
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
                index.captionTextFor(table.getCaptions()).orElse(null),
                table.getProv(),
                fullText));

        return Stream.concat(textEntries, tableEntries)
            .flatMap(Optional::stream)
            .toList();
    }

    // Locates an item's text within the full document string to compute its character offset.
    // Returns empty if the text is blank or not found — some items (e.g., empty table cells)
    // legitimately don't appear in the concatenated output.
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
}
