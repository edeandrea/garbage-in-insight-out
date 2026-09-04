package dev.ericdeandrea.docling.ai.ingestion.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;

import ai.docling.core.DoclingDocument;
import ai.docling.core.DoclingDocument.BaseTextItem;
import ai.docling.core.DoclingDocument.DocItemLabel;
import ai.docling.core.DoclingDocument.ProvenanceItem;
import ai.docling.core.DoclingDocument.TableItem;
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.docling.DoclingDocumentParser;

/**
 * Mode B extractor: routes the document through langchain4j's {@link DoclingDocumentParser}, which
 * owns all Docling Serve plumbing — base64/stream handling, request injection, endpoint routing to
 * {@code POST /v1/convert/source/async}, async invocation, and cast-free response typing — and hands
 * back a {@link Document}. The extractor keeps only the Docling-specific mapping: flattening the
 * {@link DoclingDocument} into a single continuous text and building character-level provenance so
 * the downstream {@link dev.ericdeandrea.docling.ai.ingestion.chunking.NaiveChunker} can attach
 * page/element metadata to each sentence-split chunk.
 */
@ApplicationScoped
public class DoclingNaiveExtractor {
    private final DoclingServeApi doclingServeApi;

    DoclingNaiveExtractor(DoclingServeApi doclingServeApi) {
        this.doclingServeApi = doclingServeApi;
    }

    /**
     * Parses the document via {@link DoclingDocumentParser} and assembles an {@link ExtractionResult}.
     *
     * <p>The parser's {@code documentExtractor} returns only a {@link Document}, but
     * {@code ExtractionResult} also needs a {@code List<ProvenanceEntry>} side-channel. Since the raw
     * {@link DoclingDocument} is available only inside that callback, a per-call
     * {@link AtomicReference} captures it so the {@code Uni} chain can build provenance downstream,
     * co-located with {@code ExtractionResult} assembly. The {@link CompletionStage} returned by
     * {@code parseAsync} supplies the happens-before edge between the capture write and the
     * {@code .map} read.
     */
    public Uni<ExtractionResult> extract(Path documentPath) {
        var doclingDocumentHolder = new AtomicReference<DoclingDocument>();
        var request = ConvertDocumentRequest.builder()
                    .options(ConvertDocumentOptions.builder()
                      .toFormat(OutputFormat.JSON)
                      .build())
            .build();

        var parser = DoclingDocumentParser.builder()
            .doclingClient(this.doclingServeApi)
            .documentRequest(request)
            .documentExtractor(response -> {
              // Flattens the structured DoclingDocument into the full-text Document the naive chunker sentence-splits (the
              // parser additionally tags it with document_size_bytes) and stashes the raw DoclingDocument
              var doclingDoc = response.getDocument().getJsonContent();
              doclingDocumentHolder.set(doclingDoc);

              return Document.from(buildFullText(doclingDoc));
            })
            .build();

        return Uni.createFrom()
            .completionStage(Unchecked.supplier(() -> {
                var in = Files.newInputStream(documentPath);
                return parser.parseAsync(in)
                             .whenComplete((_, _) -> closeQuietly(in));
            }))
            .map(document -> new ExtractionResult(document, buildProvenance(doclingDocumentHolder.get(), document.text())));
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        }
        catch (IOException e) {
            Log.warn("Failed to close document input stream", e);
        }
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
