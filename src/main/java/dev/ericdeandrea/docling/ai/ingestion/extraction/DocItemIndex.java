package dev.ericdeandrea.docling.ai.ingestion.extraction;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.docling.core.DoclingDocument;
import ai.docling.core.DoclingDocument.BaseTextItem;
import ai.docling.core.DoclingDocument.RefItem;
import ai.docling.core.DoclingDocument.TableItem;

record DocItemIndex(
    Map<String, BaseTextItem> textsByRef,
    Map<String, TableItem> tablesByRef
) {

    static DocItemIndex of(DoclingDocument doc) {
        var textsByRef = doc.getTexts().stream()
            .collect(Collectors.toMap(BaseTextItem::getSelfRef, Function.identity()));

        var tablesByRef = doc.getTables().stream()
            .collect(Collectors.toMap(TableItem::getSelfRef, Function.identity()));

        return new DocItemIndex(textsByRef, tablesByRef);
    }

    Optional<String> labelFor(String ref) {
        return Optional.ofNullable(this.textsByRef.get(ref))
            .map(item -> item.getLabel().toString())
            .or(() -> Optional.ofNullable(this.tablesByRef.get(ref))
                .map(table -> Objects.requireNonNullElse(table.getLabel(), "TABLE")));
    }

    Optional<String> captionTextFor(List<RefItem> captions) {
        return Optional.ofNullable(captions)
            .filter(caps -> !caps.isEmpty())
            .map(caps -> caps.getFirst())
            .map(RefItem::getRef)
            .map(this.textsByRef::get)
            .map(BaseTextItem::getText);
    }

    List<BaseTextItem> orphanedChildrenOf(String pictureRef, Set<String> referencedRefs) {
        return this.textsByRef.values().stream()
            .filter(item -> {
                var parent = item.getParent();
                return (parent != null) && pictureRef.equals(parent.getRef());
            })
            .filter(item -> !referencedRefs.contains(item.getSelfRef()))
            .toList();
    }

    Optional<String> resolvedCaptionFor(String ref) {
        return Optional.ofNullable(this.tablesByRef.get(ref))
            .flatMap(table -> captionTextFor(table.getCaptions()));
    }
}
