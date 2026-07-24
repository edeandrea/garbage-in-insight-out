package dev.ericdeandrea.docling.ai.ingestion.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

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

        assertThat(index.captionTextFor(table))
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

        assertThat(index.captionTextFor(table))
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

        assertThat(index.captionTextFor(table))
            .isEmpty();
    }
}
