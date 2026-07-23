package dev.ericdeandrea.docling.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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

        assertThat(view.panels()).isEmpty();
    }

    @Test
    void maxOnePanelPerType() {
        var view = navigate(ChatView.class);

        view.toggleMode(Mode.DOCLING_HYBRID_CHUNK);
        view.toggleMode(Mode.DOCLING_HYBRID_CHUNK);

        assertThat(view.panels())
            .hasSize(1)
            .containsKey(Mode.NAIVE)
            .doesNotContainKey(Mode.DOCLING_HYBRID_CHUNK);
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
            .as("Re-toggling creates a new panel instance")
            .isNotSameAs(panelBefore);
    }
}
