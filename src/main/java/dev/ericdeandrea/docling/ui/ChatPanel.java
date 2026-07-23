package dev.ericdeandrea.docling.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;

import dev.ericdeandrea.docling.ai.AssistantService;
import dev.ericdeandrea.docling.model.ChatResponseEvent.ChunksRetrievedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.CompletedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.TokenEvent;
import dev.ericdeandrea.docling.model.Mode;

class ChatPanel extends VerticalLayout {

    private static final int MAX_COLOR_INDEX = 9;

    private final Mode mode;
    private final MessageList messageList;
    private final MessageInput messageInput;
    private final AssistantService assistantService;
    private final Details chunksDetails;
    private final Grid<ChunkRow> chunksGrid;
    private final ArrayList<MessageListItem> items = new ArrayList<>();
    private final ArrayList<ChunkRow> allChunkRows = new ArrayList<>();
    private final Map<Integer, MessageListItem> roundToAssistantItem = new HashMap<>();
    private final UUID conversationId = UUID.randomUUID();
    private int currentRound;
    private MessageListItem currentAssistantItem;

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

    private Grid<ChunkRow> createChunksGrid() {
        var grid = new Grid<>(ChunkRow.class, false);

        grid.addColumn(ChunkRow::round)
            .setHeader("#").setFlexGrow(0).setWidth("40px");
        grid.addColumn(row -> "%.2f".formatted(row.chunk().metadata().relevanceScore()))
            .setHeader("Score").setFlexGrow(0).setWidth("70px");
        grid.addColumn(row -> (row.chunk().metadata().pageNumber() != null)
                ? row.chunk().metadata().pageNumber().toString() : "—")
            .setHeader("Page").setFlexGrow(0).setWidth("60px");
        grid.addColumn(row -> (row.chunk().metadata().elementType() != null)
                ? row.chunk().metadata().elementType() : "—")
            .setHeader("Type").setFlexGrow(0).setWidth("100px");
        grid.addColumn(row -> (row.chunk().metadata().elementLabel() != null)
                ? row.chunk().metadata().elementLabel() : "")
            .setHeader("Label").setFlexGrow(0).setWidth("90px");
        grid.addColumn(row -> (row.chunk().metadata().timestamp() != null)
                ? row.chunk().metadata().timestamp().toString() : "")
            .setHeader("Time").setFlexGrow(0).setWidth("110px");
        grid.addColumn(row -> {
                var text = row.chunk().text();
                return (text.length() > 80) ? "%s...".formatted(text.substring(0, 80)) : text;
            })
            .setHeader("Preview").setFlexGrow(1);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(row -> {
            var pre = new Pre(row.chunk().text());
            pre.addClassNames(LumoUtility.Whitespace.PRE_WRAP);
            return pre;
        }));

        grid.addItemClickListener(event -> highlightMessageForRound(event.getItem().round()));

        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        return grid;
    }

    private void onSubmit(SubmitEvent event) {
        var userMessage = event.getValue();
        var ui = UI.getCurrent();
        currentRound++;
        var colorIndex = currentRound % MAX_COLOR_INDEX;

        var userItem = new MessageListItem(userMessage);
        userItem.setUserName("You");
        items.add(userItem);

        currentAssistantItem = new MessageListItem("");
        currentAssistantItem.setUserName(mode.displayLabel());
        currentAssistantItem.setUserColorIndex(colorIndex);
        roundToAssistantItem.put(currentRound, currentAssistantItem);
        items.add(currentAssistantItem);

        messageList.setItems(new ArrayList<>(items));

        var round = currentRound;
        assistantService.chat(mode, conversationId, userMessage)
            .subscribe()
            .with(
                chatEvent -> ui.access(() -> {
                    switch (chatEvent) {
                        case TokenEvent token -> {
                            currentAssistantItem.setText("%s%s".formatted(currentAssistantItem.getText(), token.text()));
                            messageList.setItems(new ArrayList<>(items));
                        }
                        case ChunksRetrievedEvent chunksEvent -> addChunks(chunksEvent, round);
                        case CompletedEvent ignored -> {}
                    }
                }),
                failure -> ui.access(() -> {
                    currentAssistantItem.setText("Error: %s".formatted(failure.getMessage()));
                    messageList.setItems(new ArrayList<>(items));
                })
            );
    }

    private void addChunks(ChunksRetrievedEvent event, int round) {
        var newRows = event.chunks().stream()
            .map(chunk -> new ChunkRow(round, chunk))
            .toList();

        allChunkRows.addAll(0, newRows);
        chunksGrid.setItems(new ArrayList<>(allChunkRows));
        chunksDetails.setSummaryText("Retrieved Chunks (%d)".formatted(allChunkRows.size()));
        chunksDetails.setOpened(true);
    }

    private void highlightMessageForRound(int round) {
        for (var item : roundToAssistantItem.values()) {
            item.removeClassNames("highlighted");
        }

        var targetItem = roundToAssistantItem.get(round);
        if (targetItem != null) {
            targetItem.addClassNames("highlighted");
        }

        messageList.setItems(new ArrayList<>(items));
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
