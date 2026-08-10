package dev.ericdeandrea.docling.ai.ingestion.extraction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import ai.docling.core.DoclingDocument.BaseTextItem;
import ai.docling.core.DoclingDocument.PictureItem;
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;

import io.smallrye.mutiny.Uni;

import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

/**
 * Mode C extractor: calls Docling Serve's {@code /chunk} endpoint with the hybrid chunker,
 * which performs both extraction and structure-aware chunking server-side in a single round trip.
 * Unlike Mode B's sentence splitter, the hybrid chunker keeps table rows as self-describing
 * triplets (e.g., "All, FRCNN.R101 = 73.4") so the LLM can answer from a single chunk without
 * needing to correlate headers across chunks.
 */
@ApplicationScoped
public class DoclingHybridExtractor {

    private final DoclingServeApi doclingServeApi;

    DoclingHybridExtractor(DoclingServeApi doclingServeApi) {
        this.doclingServeApi = doclingServeApi;
    }

    /**
     * Sends the document to Docling Serve's /chunk endpoint with the hybrid chunker, which
     * performs extraction and structure-aware chunking in one server-side round trip. We request
     * {@code includeConvertedDoc(true)} so the response embeds the full {@code DoclingDocument}
     * — needed to cross-reference docItem refs for element types, resolve captions, and rescue
     * orphaned picture text that the chunker drops.
     *
     * <p>Each server-side chunk is mapped to a {@link TextSegment} with mode, page, element type,
     * and caption metadata. Orphaned picture children are then collected into synthetic PICTURE
     * segments so chart/figure labels aren't lost.
     */
    public Uni<List<TextSegment>> extractAndChunk(Path documentPath) {
        var options = ConvertDocumentOptions.builder()
            .toFormat(OutputFormat.JSON)
            .build();

        var request = HybridChunkDocumentRequest.builder()
            .options(options)
            .includeConvertedDoc(true)
            .build();

        return Uni.createFrom()
            .completionStage(() -> this.doclingServeApi.chunkFilesWithHybridChunkerAsync(request, documentPath))
            .map(response -> {
                var doclingDoc = response.getDocuments()
                                         .getFirst()
                                         .getContent()
                                         .getJsonContent();

                var index = DocItemIndex.of(doclingDoc);

                // Track which docItem refs the chunker already included in a chunk,
                // so buildPictureSegment can identify the orphaned ones.
                var referencedRefs = response.getChunks().stream()
                    .filter(chunk -> (chunk.getDocItems() != null))
                    .flatMap(chunk -> chunk.getDocItems().stream())
                    .collect(Collectors.toSet());

                var segments = new ArrayList<>(response.getChunks()
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
                    .toList());

                doclingDoc.getPictures().stream()
                    .map(picture -> buildPictureSegment(picture, index, referencedRefs))
                    .flatMap(Optional::stream)
                    .forEach(segments::add);

                return segments;
            });
    }

    // The hybrid chunker drops text labels that are children of picture items (e.g., pie chart
    // percentages like "Patents 8%"). This rescues those orphaned children by collecting any
    // picture text refs not already claimed by a chunk and synthesizing a PICTURE segment,
    // so Mode C can answer questions about figure data that would otherwise be lost.
    private Optional<TextSegment> buildPictureSegment(PictureItem picture,
                                                       DocItemIndex index,
                                                       Set<String> referencedRefs) {
        var orphans = index.orphanedChildrenOf(picture.getSelfRef(), referencedRefs);

        if (orphans.isEmpty()) {
            return Optional.empty();
        }

        var captionText = index.captionTextFor(picture.getCaptions()).orElse("");
        var orphanText = orphans.stream()
            .map(BaseTextItem::getText)
            .collect(Collectors.joining(" "));

        var text = captionText.isEmpty() ? orphanText : "%s\n%s".formatted(captionText, orphanText);

        var metadata = new Metadata()
            .put("mode", Mode.DOCLING_HYBRID_CHUNK.name())
            .put("element_type", "PICTURE");

        if ((picture.getProv() != null) && !picture.getProv().isEmpty()) {
            metadata.put("page_number", picture.getProv().getFirst().getPageNo());
        }

        index.captionTextFor(picture.getCaptions())
            .ifPresent(cap -> metadata.put("element_label", cap));

        return Optional.of(TextSegment.from(text, metadata));
    }
}
