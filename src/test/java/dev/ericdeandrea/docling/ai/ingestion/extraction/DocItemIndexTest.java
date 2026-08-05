package dev.ericdeandrea.docling.ai.ingestion.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ai.docling.core.DoclingDocument.BaseTextItem;
import ai.docling.core.DoclingDocument.DocItemLabel;
import ai.docling.core.DoclingDocument.RefItem;
import ai.docling.core.DoclingDocument.TableItem;
import ai.docling.core.DoclingDocument.TextItem;

class DocItemIndexTest {

    @Test
    void labelForReturnsTextItemLabel() {
        var textItem = TextItem.builder()
            .selfRef("#/texts/0")
            .label(DocItemLabel.PARAGRAPH)
            .text("some text")
            .build();

        var index = new DocItemIndex(
            Map.of("#/texts/0", textItem),
            Map.of()
        );

        assertThat(index.labelFor("#/texts/0"))
            .isPresent()
            .hasValue("PARAGRAPH");
    }

    @Test
    void labelForReturnsTableItemLabel() {
        var table = TableItem.builder()
            .selfRef("#/tables/0")
            .label("table")
            .build();

        var index = new DocItemIndex(
            Map.of(),
            Map.of("#/tables/0", table)
        );

        assertThat(index.labelFor("#/tables/0"))
            .isPresent()
            .hasValue("table");
    }

    @Test
    void labelForReturnsEmptyForUnknownRef() {
        var index = new DocItemIndex(Map.of(), Map.of());

        assertThat(index.labelFor("#/texts/999"))
            .isEmpty();
    }

    @Test
    void captionTextForResolvesTableCaption() {
        var captionItem = TextItem.builder()
            .selfRef("#/texts/5")
            .label(DocItemLabel.CAPTION)
            .text("Table 1: DocLayNet dataset overview")
            .build();

        var captionRef = RefItem.builder()
            .ref("#/texts/5")
            .build();

        var table = TableItem.builder()
            .selfRef("#/tables/0")
            .label("table")
            .caption(captionRef)
            .build();

        var index = new DocItemIndex(
            Map.of("#/texts/5", captionItem),
            Map.of("#/tables/0", table)
        );

        assertThat(index.captionTextFor(table.getCaptions()))
            .isPresent()
            .hasValue("Table 1: DocLayNet dataset overview");
    }

    @Test
    void captionTextForReturnsEmptyWhenNoCaptions() {
        var table = TableItem.builder()
            .selfRef("#/tables/0")
            .label("table")
            .build();

        var index = new DocItemIndex(Map.of(), Map.of("#/tables/0", table));

        assertThat(index.captionTextFor(table.getCaptions()))
            .isEmpty();
    }

    @Test
    void resolvedCaptionForResolvesTableRefCaption() {
        var captionItem = TextItem.builder()
            .selfRef("#/texts/5")
            .label(DocItemLabel.CAPTION)
            .text("Table 1: DocLayNet dataset overview")
            .build();

        var captionRef = RefItem.builder()
            .ref("#/texts/5")
            .build();

        var table = TableItem.builder()
            .selfRef("#/tables/0")
            .label("table")
            .caption(captionRef)
            .build();

        var index = new DocItemIndex(
            Map.of("#/texts/5", captionItem),
            Map.of("#/tables/0", table)
        );

        assertThat(index.resolvedCaptionFor("#/tables/0"))
            .isPresent()
            .hasValue("Table 1: DocLayNet dataset overview");
    }

    @Test
    void resolvedCaptionForReturnsEmptyForTextRef() {
        var textItem = TextItem.builder()
            .selfRef("#/texts/0")
            .label(DocItemLabel.TEXT)
            .text("some text")
            .build();

        var index = new DocItemIndex(
            Map.of("#/texts/0", textItem),
            Map.of()
        );

        assertThat(index.resolvedCaptionFor("#/texts/0"))
            .isEmpty();
    }

    @Test
    void captionTextForReturnsEmptyWhenCaptionRefUnresolvable() {
        var captionRef = RefItem.builder()
            .ref("#/texts/999")
            .build();

        var table = TableItem.builder()
            .selfRef("#/tables/0")
            .label("table")
            .caption(captionRef)
            .build();

        var index = new DocItemIndex(Map.of(), Map.of("#/tables/0", table));

        assertThat(index.captionTextFor(table.getCaptions()))
            .isEmpty();
    }

    @Test
    void orphanedChildrenOfReturnsUnreferencedChildren() {
        var parentRef = RefItem.builder().ref("#/pictures/1").build();

        var child1 = TextItem.builder()
            .selfRef("#/texts/10")
            .label(DocItemLabel.TEXT)
            .text("Patents")
            .parent(parentRef)
            .build();

        var child2 = TextItem.builder()
            .selfRef("#/texts/11")
            .label(DocItemLabel.TEXT)
            .text("8%")
            .parent(parentRef)
            .build();

        var index = new DocItemIndex(
            Map.of("#/texts/10", child1, "#/texts/11", child2),
            Map.of()
        );

        var orphans = index.orphanedChildrenOf("#/pictures/1", Set.of());

        assertThat(orphans)
            .hasSize(2)
            .extracting(BaseTextItem::getText)
            .containsExactlyInAnyOrder("Patents", "8%");
    }

    @Test
    void orphanedChildrenOfExcludesReferencedItems() {
        var parentRef = RefItem.builder().ref("#/pictures/1").build();

        var child = TextItem.builder()
            .selfRef("#/texts/10")
            .label(DocItemLabel.TEXT)
            .text("Patents")
            .parent(parentRef)
            .build();

        var index = new DocItemIndex(
            Map.of("#/texts/10", child),
            Map.of()
        );

        var orphans = index.orphanedChildrenOf("#/pictures/1", Set.of("#/texts/10"));

        assertThat(orphans).isEmpty();
    }

    @Test
    void orphanedChildrenOfExcludesItemsWithDifferentParent() {
        var pictureParent = RefItem.builder().ref("#/pictures/1").build();
        var bodyParent = RefItem.builder().ref("#/body").build();

        var pictureChild = TextItem.builder()
            .selfRef("#/texts/10")
            .label(DocItemLabel.TEXT)
            .text("chart label")
            .parent(pictureParent)
            .build();

        var bodyChild = TextItem.builder()
            .selfRef("#/texts/20")
            .label(DocItemLabel.TEXT)
            .text("body text")
            .parent(bodyParent)
            .build();

        var index = new DocItemIndex(
            Map.of("#/texts/10", pictureChild, "#/texts/20", bodyChild),
            Map.of()
        );

        var orphans = index.orphanedChildrenOf("#/pictures/1", Set.of());

        assertThat(orphans)
            .hasSize(1)
            .extracting(BaseTextItem::getText)
            .containsExactly("chart label");
    }
}
