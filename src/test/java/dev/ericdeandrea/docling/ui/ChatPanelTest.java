package dev.ericdeandrea.docling.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent;
import com.vaadin.flow.component.messages.MessageList;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;

import dev.ericdeandrea.docling.ai.AssistantService;
import dev.ericdeandrea.docling.model.ChatResponseEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.ChunksRetrievedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.CompletedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.TokenEvent;
import dev.ericdeandrea.docling.model.ChunkMetadata;
import dev.ericdeandrea.docling.model.Mode;
import dev.ericdeandrea.docling.model.RetrievedChunk;

@QuarkusTest
class ChatPanelTest extends QuarkusBrowserlessTest {

    @InjectMock
    AssistantService assistantService;

    private static final List<RetrievedChunk> MOCK_CHUNKS = List.of(
        new RetrievedChunk("chunk one text", new ChunkMetadata(1, "PARAGRAPH", null, Mode.NAIVE, 0.95, Instant.now())),
        new RetrievedChunk("chunk two text", new ChunkMetadata(2, "TABLE", "Table 2", Mode.NAIVE, 0.87, Instant.now()))
    );

    @BeforeEach
    void setupMock() {
        when(this.assistantService.chat(eq(Mode.NAIVE), any(), any()))
            .thenReturn(Multi.createFrom().items(
                new TokenEvent("Hello "),
                new TokenEvent("world!"),
                new ChunksRetrievedEvent(MOCK_CHUNKS),
                new CompletedEvent()
            ));
    }

    @Test
    void assistantMessageGetsColorIndex() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        var items = find(MessageList.class, panel.messageArea()).single().getItems();

        var assistantItem = items.stream()
            .filter(item -> Mode.NAIVE.displayLabel().equals(item.getUserName()))
            .findFirst()
            .orElseThrow();

        assertThat(assistantItem.getUserColorIndex())
            .as("First round color index should be 1 %% 7 = 1")
            .isEqualTo(1);
    }

    @Test
    void userMessageHasCurrentUserClass() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        var items = find(MessageList.class, panel.messageArea()).single().getItems();

        var userItem = items.stream()
            .filter(item -> "You".equals(item.getUserName()))
            .findFirst()
            .orElseThrow();

        assertThat(userItem.hasClassName("current-user"))
            .as("User message should have current-user class")
            .isTrue();
    }

    @Test
    void chunksGridPopulatedAfterResponse() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        @SuppressWarnings("unchecked")
        var grid = (Grid<ChunkRow>) find(Grid.class, panel.chunksArea()).single();

        assertThat(grid.getGenericDataView().getItems().toList())
            .hasSize(2);
    }

    @Test
    void assistantMessageAccumulatesTokens() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        var items = find(MessageList.class, panel.messageArea()).single().getItems();

        var assistantItem = items.stream()
            .filter(item -> Mode.NAIVE.displayLabel().equals(item.getUserName()))
            .findFirst()
            .orElseThrow();

        assertThat(assistantItem.getText()).isEqualTo("Hello world!");
    }

    @Test
    void secondRoundGetsNextColorIndex() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "first question");
        fireSubmit(panel, "second question");

        var items = find(MessageList.class, panel.messageArea()).single().getItems();

        var assistantItems = items.stream()
            .filter(item -> Mode.NAIVE.displayLabel().equals(item.getUserName()))
            .toList();

        assertThat(assistantItems).hasSize(2);
        assertThat(assistantItems.get(0).getUserColorIndex()).isEqualTo(1);
        assertThat(assistantItems.get(1).getUserColorIndex()).isEqualTo(2);
    }

    @Test
    void clickingChunkRowHighlightsAssistantMessage() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        var messageList = find(MessageList.class, panel.messageArea()).single();
        var assistantItem = messageList.getItems().stream()
            .filter(item -> Mode.NAIVE.displayLabel().equals(item.getUserName()))
            .findFirst()
            .orElseThrow();

        assertThat(assistantItem.hasClassName("highlighted"))
            .as("Before click, no highlight")
            .isFalse();

        @SuppressWarnings("unchecked")
        var grid = (Grid<ChunkRow>) find(Grid.class, panel.chunksArea()).single();
        test(grid).clickRow(0);

        assertThat(assistantItem.hasClassName("highlighted"))
            .as("After clicking chunk row, assistant message should be highlighted")
            .isTrue();
    }

    @Test
    void clickingDifferentRoundMovesHighlight() {
        when(this.assistantService.chat(eq(Mode.NAIVE), any(), eq("first")))
            .thenReturn(Multi.createFrom().items(
                new TokenEvent("Answer one"),
                new ChunksRetrievedEvent(MOCK_CHUNKS),
                new CompletedEvent()
            ));

        when(this.assistantService.chat(eq(Mode.NAIVE), any(), eq("second")))
            .thenReturn(Multi.createFrom().items(
                new TokenEvent("Answer two"),
                new ChunksRetrievedEvent(List.of(
                    new RetrievedChunk("different chunk", new ChunkMetadata(3, "PARAGRAPH", null, Mode.NAIVE, 0.9, Instant.now()))
                )),
                new CompletedEvent()
            ));

        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "first");
        fireSubmit(panel, "second");

        var messageList = find(MessageList.class, panel.messageArea()).single();
        var assistantItems = messageList.getItems().stream()
            .filter(item -> Mode.NAIVE.displayLabel().equals(item.getUserName()))
            .toList();

        assertThat(assistantItems).hasSize(2);

        @SuppressWarnings("unchecked")
        var grid = (Grid<ChunkRow>) find(Grid.class, panel.chunksArea()).single();

        test(grid).clickRow(1);

        assertThat(assistantItems.get(0).hasClassName("highlighted"))
            .as("Round 1 message should be highlighted")
            .isTrue();

        assertThat(assistantItems.get(1).hasClassName("highlighted"))
            .as("Round 2 message should NOT be highlighted")
            .isFalse();

        test(grid).clickRow(0);

        assertThat(assistantItems.get(1).hasClassName("highlighted"))
            .as("Round 2 message should now be highlighted")
            .isTrue();

        assertThat(assistantItems.get(0).hasClassName("highlighted"))
            .as("Round 1 message should no longer be highlighted")
            .isFalse();
    }

    @Test
    void chunksGridHasNoRoundColumn() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        @SuppressWarnings("unchecked")
        var grid = (Grid<ChunkRow>) find(Grid.class, panel.chunksArea()).single();

        assertThat(grid.getColumns())
            .as("Grid should have 6 columns (no round column)")
            .hasSize(6);

        assertThat(grid.getColumns())
            .extracting(col -> col.getHeaderText())
            .as("No column should have header '#'")
            .doesNotContain("#");
    }

    @Test
    void allColumnsAreResizable() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        @SuppressWarnings("unchecked")
        var grid = (Grid<ChunkRow>) find(Grid.class, panel.chunksArea()).single();

        assertThat(grid.getColumns())
            .as("Every column should be resizable")
            .allMatch(col -> col.isResizable());
    }

    @Test
    void chunkRowsGetPartNameByRound() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "first question");
        fireSubmit(panel, "second question");

        @SuppressWarnings("unchecked")
        var grid = (Grid<ChunkRow>) find(Grid.class, panel.chunksArea()).single();
        var partNameGenerator = grid.getPartNameGenerator();

        assertThat(partNameGenerator.apply(new ChunkRow(1, MOCK_CHUNKS.getFirst())))
            .as("Round 1 should produce part name round-color-1")
            .isEqualTo("round-color-1");

        assertThat(partNameGenerator.apply(new ChunkRow(2, MOCK_CHUNKS.getFirst())))
            .as("Round 2 should produce part name round-color-2")
            .isEqualTo("round-color-2");
    }

    @Test
    void chunksHeaderShowsCount() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        var header = find(Span.class, panel.chunksArea()).single();

        assertThat(header.getText())
            .isEqualTo("Retrieved Chunks (2)");
    }

    @Test
    void noDetailsComponentInChunksArea() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        var hasDetails = panel.chunksArea().getChildren()
            .anyMatch(child -> child instanceof Details);

        assertThat(hasDetails)
            .as("No Details component should exist in chunks area")
            .isFalse();
    }

    @Test
    void historyPreservedAcrossToggle() {
        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        assertThat(find(MessageList.class, panel.messageArea()).single().getItems())
            .as("Chat has user + assistant messages before toggling")
            .hasSize(2);
        assertThat(find(Span.class, panel.chunksArea()).single().getText())
            .isEqualTo("Retrieved Chunks (2)");

        view.toggleMode(Mode.NAIVE);
        view.toggleMode(Mode.NAIVE);

        assertThat(view.panels().get(Mode.NAIVE))
            .as("Same panel instance is reused across the toggle")
            .isSameAs(panel);
        assertThat(find(MessageList.class, panel.messageArea()).single().getItems())
            .as("Chat history survives the hide/show cycle")
            .hasSize(2);
        assertThat(find(Span.class, panel.chunksArea()).single().getText())
            .as("Chunk history survives the hide/show cycle")
            .isEqualTo("Retrieved Chunks (2)");
    }

    @Test
    void streamingContinuesWhileHidden() {
        var emitterRef = new AtomicReference<MultiEmitter<? super ChatResponseEvent>>();
        when(this.assistantService.chat(eq(Mode.NAIVE), any(), any()))
            .thenReturn(Multi.createFrom().<ChatResponseEvent>emitter(emitterRef::set));

        var view = navigate(ChatView.class);
        var panel = view.panels().get(Mode.NAIVE);

        fireSubmit(panel, "test question");

        var emitter = emitterRef.get();
        assertThat(emitter)
            .as("Subscription established and emitter captured")
            .isNotNull();

        emitter.emit(new TokenEvent("Hello "));

        view.toggleMode(Mode.NAIVE);

        assertThat(panel.messageArea().isVisible())
            .as("Panel is hidden mid-stream")
            .isFalse();

        emitter.emit(new TokenEvent("world!"));
        emitter.emit(new ChunksRetrievedEvent(MOCK_CHUNKS));
        emitter.complete();

        view.toggleMode(Mode.NAIVE);

        var assistantItem = find(MessageList.class, panel.messageArea()).single().getItems().stream()
            .filter(item -> Mode.NAIVE.displayLabel().equals(item.getUserName()))
            .findFirst()
            .orElseThrow();

        assertThat(assistantItem.getText())
            .as("Tokens emitted while hidden are accumulated and shown on return")
            .isEqualTo("Hello world!");

        @SuppressWarnings("unchecked")
        var grid = (Grid<ChunkRow>) find(Grid.class, panel.chunksArea()).single();

        assertThat(grid.getGenericDataView().getItems().toList())
            .as("Chunks emitted while hidden are shown on return")
            .hasSize(2);
    }

    private void fireSubmit(ChatPanel panel, String message) {
        var messageInput = find(MessageInput.class, panel.messageArea()).single();
        ComponentUtil.fireEvent(messageInput, new SubmitEvent(messageInput, false, message));
    }
}
