package dev.ericdeandrea.docling.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderColor;
import com.vaadin.flow.theme.lumo.LumoUtility.Position;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import com.vaadin.flow.theme.lumo.LumoUtility.ZIndex;

import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;

import io.quarkus.test.junit.QuarkusTest;

import dev.ericdeandrea.docling.model.Mode;

@QuarkusTest
class ChatViewTest extends QuarkusBrowserlessTest {

    @Test
    void defaultsToModeAOnly() {
        var view = navigate(ChatView.class);

        assertThat(view.panels())
            .hasSize(1)
            .containsKey(Mode.NAIVE);
    }

    @Test
    void togglesModeBPanel() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);

        assertThat(view.panels())
            .hasSize(2)
            .containsKeys(Mode.NAIVE, Mode.DOCLING_NAIVE_CHUNK);
    }

    @Test
    void togglesOffExistingPanel() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.NAIVE);

        var panel = view.panels().get(Mode.NAIVE);

        assertThat(panel)
            .as("Panel is retained (not destroyed) after toggling off")
            .isNotNull();
        assertThat(panel.messageArea().isVisible())
            .as("Message area is hidden after toggling off")
            .isFalse();
        assertThat(panel.chunksArea().isVisible())
            .as("Chunks area is hidden after toggling off")
            .isFalse();
    }

    @Test
    void maxOnePanelPerType() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.DOCLING_HYBRID_CHUNK);
        var firstInstance = view.panels().get(Mode.DOCLING_HYBRID_CHUNK);
        view.toggleMode(Mode.DOCLING_HYBRID_CHUNK);

        assertThat(view.panels().get(Mode.DOCLING_HYBRID_CHUNK))
            .as("Re-toggling reuses the same panel instance, never a second one")
            .isSameAs(firstInstance);
        assertThat(firstInstance.messageArea().isVisible())
            .as("Panel is hidden after toggling back off")
            .isFalse();
    }

    @Test
    void allThreePanelsCanBeActive() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);
        view.toggleMode(Mode.DOCLING_HYBRID_CHUNK);

        assertThat(view.panels())
            .hasSize(3)
            .containsKeys(Mode.NAIVE, Mode.DOCLING_NAIVE_CHUNK, Mode.DOCLING_HYBRID_CHUNK);
    }

    @Test
    void panelStatePreservesAfterToggle() {
        var view = navigate(ChatView.class);

        var panelBefore = view.panels().get(Mode.NAIVE);

        view.toggleMode(Mode.NAIVE);
        view.toggleMode(Mode.NAIVE);

        assertThat(view.panels().get(Mode.NAIVE))
            .as("Re-toggling reuses the same panel instance, preserving its state")
            .isSameAs(panelBefore);
    }

    @Test
    void reshownPanelKeepsFixedModeOrder() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.DOCLING_HYBRID_CHUNK);
        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);

        var panelA = view.panels().get(Mode.NAIVE);
        var panelB = view.panels().get(Mode.DOCLING_NAIVE_CHUNK);
        var panelC = view.panels().get(Mode.DOCLING_HYBRID_CHUNK);

        var children = panelA.messageArea().getParent().orElseThrow().getChildren().toList();

        assertThat(children.indexOf(panelA.messageArea()))
            .as("Mode A precedes Mode B regardless of toggle order")
            .isLessThan(children.indexOf(panelB.messageArea()));
        assertThat(children.indexOf(panelB.messageArea()))
            .as("Mode B precedes Mode C regardless of toggle order")
            .isLessThan(children.indexOf(panelC.messageArea()));
    }

    @Test
    void toggleButtonReflectsVisibility() {
        var view = navigate(ChatView.class);
        var button = view.toggleButtons().get(Mode.DOCLING_NAIVE_CHUNK);
        var primary = ButtonVariant.LUMO_PRIMARY.getVariantName();

        assertThat(button.hasThemeName(primary))
            .as("Button is not primary while its mode is hidden")
            .isFalse();

        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);

        assertThat(button.hasThemeName(primary))
            .as("Button is primary while its mode is visible")
            .isTrue();

        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);

        assertThat(button.hasThemeName(primary))
            .as("Button reverts to non-primary after hiding")
            .isFalse();
    }

    @Test
    void hiddenPanelCarriesNoBorder() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);
        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);
        view.toggleMode(Mode.DOCLING_HYBRID_CHUNK);

        var panelA = view.panels().get(Mode.NAIVE);
        var panelB = view.panels().get(Mode.DOCLING_NAIVE_CHUNK);
        var panelC = view.panels().get(Mode.DOCLING_HYBRID_CHUNK);

        assertThat(panelA.messageArea().getStyle().has("border-inline-start"))
            .as("Leftmost visible panel (A) has no divider")
            .isFalse();
        assertThat(panelC.messageArea().getStyle().get("border-inline-start"))
            .as("Second visible panel (C) has a divider")
            .contains("solid");
        assertThat(panelB.messageArea().getStyle().has("border-inline-start"))
            .as("Hidden panel (B) has no inline-start border")
            .isFalse();
        assertThat(panelB.messageArea().getStyle().has("border-inline-end"))
            .as("Hidden panel (B) has no inline-end border")
            .isFalse();
    }

    @Test
    void layoutUsesSplitLayoutWithVerticalOrientation() {
        var view = navigate(ChatView.class);

        var splitLayout = find(SplitLayout.class, view).single();

        assertThat(splitLayout.getOrientation())
            .isEqualTo(SplitLayout.Orientation.VERTICAL);
    }

    @Test
    void splitLayoutDefaultPositionIs70() {
        var view = navigate(ChatView.class);

        var splitLayout = find(SplitLayout.class, view).single();

        assertThat(splitLayout.getSplitterPosition())
            .isEqualTo(70.0);
    }

    @Test
    void titleDoesNotWrap() {
        var view = navigate(ChatView.class);

        var title = find(H2.class, view).single();

        assertThat(title.hasClassName(Whitespace.NOWRAP))
            .as("Title should have nowrap class")
            .isTrue();
    }

    @Test
    void singlePanelHasNoBorder() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        assertThat(panel.messageArea().getStyle().has("border-inline-start"))
            .as("Single panel messageArea should have no border")
            .isFalse();

        assertThat(panel.chunksArea().getStyle().has("border-inline-start"))
            .as("Single panel chunksArea should have no border")
            .isFalse();
    }

    @Test
    void multiplePanelsHaveBordersBetweenOnly() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);

        var firstPanel = view.panels().get(Mode.NAIVE);
        var secondPanel = view.panels().get(Mode.DOCLING_NAIVE_CHUNK);

        assertThat(firstPanel.messageArea().getStyle().has("border-inline-start"))
            .as("First panel should have no left border (screen edge)")
            .isFalse();

        assertThat(secondPanel.messageArea().getStyle().get("border-inline-start"))
            .as("Second panel should have a left border (divider)")
            .contains("solid");

        assertThat(secondPanel.messageArea().getStyle().has("border-inline-end"))
            .as("Second panel should have no right border (screen edge)")
            .isFalse();

        view.toggleMode(Mode.DOCLING_NAIVE_CHUNK);

        assertThat(firstPanel.messageArea().getStyle().has("border-inline-start"))
            .as("After toggling back to single, no border")
            .isFalse();
    }

    @Test
    void toolbarIsStickyAtTop() {
        var view = navigate(ChatView.class);

        var toolbar = view.getChildren()
            .filter(c -> c.hasClassName(Position.STICKY))
            .findFirst()
            .orElseThrow();

        assertThat(toolbar.hasClassName(Position.STICKY)).isTrue();
        assertThat(toolbar.hasClassName(Position.Top.NONE)).isTrue();
        assertThat(toolbar.hasClassName(ZIndex.SMALL)).isTrue();
        assertThat(toolbar.hasClassName(Border.BOTTOM)).isTrue();
        assertThat(toolbar.hasClassName(BorderColor.CONTRAST_30)).isTrue();
        assertThat(toolbar.hasClassName("sticky-toolbar")).isTrue();
        assertThat(toolbar).isInstanceOf(com.vaadin.flow.component.orderedlayout.VerticalLayout.class);
    }
}
