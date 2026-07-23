package dev.ericdeandrea.docling.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dev.ericdeandrea.docling.ai.AssistantService;
import dev.ericdeandrea.docling.model.ChatResponseEvent.ChunksRetrievedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.CompletedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.TokenEvent;
import dev.ericdeandrea.docling.model.Mode;
import dev.ericdeandrea.docling.model.RetrievedChunk;

class ChatPanel extends VerticalLayout {

    private final Mode mode;
    private final MessageList messageList;
    private final MessageInput messageInput;
    private final AssistantService assistantService;
    private final Details chunksDetails;
    private final Grid<RetrievedChunk> chunksGrid;
    private final ArrayList<MessageListItem> items = new ArrayList<>();
    private UUID conversationId = UUID.randomUUID();

    ChatPanel(Mode mode, AssistantService assistantService) {
        this.mode = mode;
        this.assistantService = assistantService;
        this.messageList = new MessageList();
        this.messageInput = new MessageInput();
        this.chunksGrid = createChunksGrid();
        this.chunksDetails = new Details("Retrieved Chunks", chunksGrid);

        messageList.setMarkdown(true);
        messageList.setSizeFull();

        messageInput.addSubmitListener(this::onSubmit);

        chunksDetails.setOpened(false);
        chunksDetails.setWidthFull();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(messageList, messageInput, chunksDetails);
        expand(messageList);
        messageInput.setWidthFull();
    }

    private Grid<RetrievedChunk> createChunksGrid() {
        var grid = new Grid<>(RetrievedChunk.class, false);

        grid.addColumn(chunk -> "%.2f".formatted(chunk.metadata().relevanceScore()))
            .setHeader("Score").setFlexGrow(0).setWidth("70px");
        grid.addColumn(chunk -> chunk.metadata().pageNumber() != null
                ? chunk.metadata().pageNumber().toString() : "—")
            .setHeader("Page").setFlexGrow(0).setWidth("60px");
        grid.addColumn(chunk -> chunk.metadata().elementType() != null
                ? chunk.metadata().elementType() : "—")
            .setHeader("Type").setFlexGrow(0).setWidth("100px");
        grid.addColumn(chunk -> chunk.metadata().elementLabel() != null
                ? chunk.metadata().elementLabel() : "")
            .setHeader("Label").setFlexGrow(0).setWidth("90px");
        grid.addColumn(chunk -> chunk.metadata().timestamp() != null
                ? chunk.metadata().timestamp().toString() : "")
            .setHeader("Time").setFlexGrow(0).setWidth("110px");
        grid.addColumn(chunk -> chunk.text().length() > 80
                ? chunk.text().substring(0, 80) + "..." : chunk.text())
            .setHeader("Preview").setFlexGrow(1);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(chunk -> {
            var pre = new Pre(chunk.text());
            pre.getStyle().set("white-space", "pre-wrap");
            pre.getStyle().set("word-break", "break-word");
            return pre;
        }));

        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        return grid;
    }

    private void onSubmit(SubmitEvent event) {
        var userMessage = event.getValue();
        var ui = UI.getCurrent();

        var userItem = new MessageListItem(userMessage);
        userItem.setUserName("You");
        items.add(userItem);

        var assistantItem = new MessageListItem("");
        assistantItem.setUserName(mode.displayLabel());
        items.add(assistantItem);

        messageList.setItems(new ArrayList<>(items));

        assistantService.chat(mode, conversationId, userMessage)
            .subscribe().with(
                event1 -> ui.access(() -> {
                    switch (event1) {
                        case TokenEvent token -> {
                            assistantItem.setText(assistantItem.getText() + token.text());
                            messageList.setItems(new ArrayList<>(items));
                        }
                        case ChunksRetrievedEvent chunksEvent -> displayChunks(chunksEvent);
                        case CompletedEvent ignored -> {}
                    }
                }),
                failure -> ui.access(() -> {
                    assistantItem.setText("Error: " + failure.getMessage());
                    messageList.setItems(new ArrayList<>(items));
                })
            );
    }

    private void displayChunks(ChunksRetrievedEvent event) {
        chunksGrid.setItems(event.chunks());
        chunksDetails.setSummaryText("Retrieved Chunks (%d)".formatted(event.chunks().size()));
        chunksDetails.setOpened(true);
    }

    Mode mode() {
        return mode;
    }

    MessageList messageList() {
        return messageList;
    }

    MessageInput messageInput() {
        return messageInput;
    }
}
