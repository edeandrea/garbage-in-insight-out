package dev.ericdeandrea.docling.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;

import dev.ericdeandrea.docling.ai.AssistantService;
import dev.ericdeandrea.docling.model.ChatResponseEvent.ChunksRetrievedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.CompletedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.TokenEvent;
import dev.ericdeandrea.docling.model.Mode;

class ChatPanel {

    private static final int MAX_COLOR_INDEX = 7;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
        .ofPattern("MMM d, h:mm a")
        .withZone(ZoneId.systemDefault());

    private final Mode mode;
    private final MessageList messageList;
    private final AssistantService assistantService;
    private final Span chunksHeader;
    private final Grid<ChunkRow> chunksGrid;
    private final VerticalLayout messageArea;
    private final VerticalLayout chunksArea;
    private final List<MessageListItem> items = new ArrayList<>();
    private final List<ChunkRow> allChunkRows = new ArrayList<>();
    private final Map<Integer, MessageListItem> roundToAssistantItem = new HashMap<>();
    private final UUID conversationId = UUID.randomUUID();
    private int currentRound;
    private MessageListItem currentAssistantItem;

    ChatPanel(Mode mode, AssistantService assistantService) {
        this.mode = mode;
        this.assistantService = assistantService;
        this.messageList = new MessageList();
        var messageInput = new MessageInput();
        this.chunksGrid = createChunksGrid();
        this.chunksHeader = new Span("Retrieved Chunks (0)");

        messageList.setMarkdown(true);
        messageList.setSizeFull();

        messageInput.addSubmitListener(this::onSubmit);
        messageInput.setWidthFull();

        chunksHeader.setWidthFull();

        this.messageArea = new VerticalLayout(this.messageList, messageInput);
        this.messageArea.setSizeFull();
        this.messageArea.setPadding(false);
        this.messageArea.setSpacing(false);
        this.messageArea.expand(this.messageList);

        this.chunksArea = new VerticalLayout(this.chunksHeader, this.chunksGrid);
        this.chunksArea.setSizeFull();
        this.chunksArea.setPadding(false);
        this.chunksArea.setSpacing(false);
        this.chunksArea.expand(this.chunksGrid);
    }

    VerticalLayout messageArea() {
        return this.messageArea;
    }

    VerticalLayout chunksArea() {
        return this.chunksArea;
    }

    private Grid<ChunkRow> createChunksGrid() {
        var grid = new Grid<>(ChunkRow.class, false);

        grid.addColumn(row -> "%.2f".formatted(row.chunk().metadata().relevanceScore()))
            .setHeader(headerWithTooltip("Score")).setFlexGrow(0).setWidth("90px").setResizable(true);
        grid.addColumn(row -> (row.chunk().metadata().pageNumber() != null)
                ? row.chunk().metadata().pageNumber().toString() : "—")
            .setHeader(headerWithTooltip("Page")).setFlexGrow(0).setWidth("80px").setResizable(true);
        grid.addColumn(row -> (row.chunk().metadata().elementType() != null)
                ? row.chunk().metadata().elementType() : "—")
            .setHeader(headerWithTooltip("Type")).setFlexGrow(0).setWidth("100px").setResizable(true);
        grid.addColumn(row -> (row.chunk().metadata().elementLabel() != null)
                ? row.chunk().metadata().elementLabel() : "")
            .setHeader(headerWithTooltip("Label")).setFlexGrow(0).setWidth("90px").setResizable(true);
        grid.addColumn(row -> (row.chunk().metadata().timestamp() != null)
                ? TIMESTAMP_FORMAT.format(row.chunk().metadata().timestamp()) : "")
            .setHeader(headerWithTooltip("Time")).setFlexGrow(0).setWidth("110px").setResizable(true);
        grid.addColumn(row -> {
                var text = row.chunk().text();
                return (text.length() > 80) ? "%s...".formatted(text.substring(0, 80)) : text;
            })
            .setHeader(headerWithTooltip("Preview")).setFlexGrow(1).setAutoWidth(true).setResizable(true);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(row -> {
            var pre = new Pre(row.chunk().text());
            pre.addClassNames(Whitespace.PRE_WRAP);
            pre.getStyle()
                .set("overflow-x", "auto")
                .set("word-break", "break-word")
                .set("max-width", "100%")
                .set("margin", "0");
            return pre;
        }));

        grid.setPartNameGenerator(row -> "round-color-%d".formatted(row.round() % MAX_COLOR_INDEX));

        grid.addItemClickListener(event -> highlightMessageForRound(event.getItem().round()));

        grid.setSizeFull();

        return grid;
    }

    private void onSubmit(SubmitEvent event) {
        var userMessage = event.getValue();
        var ui = UI.getCurrent();
        this.currentRound++;
        var colorIndex = this.currentRound % MAX_COLOR_INDEX;

        var userItem = new MessageListItem(userMessage);
        userItem.setUserName("You");
        this.items.add(userItem);

        this.currentAssistantItem = new MessageListItem("");
        this.currentAssistantItem.setUserName(this.mode.displayLabel());
        this.currentAssistantItem.setUserColorIndex(colorIndex);
        this.roundToAssistantItem.put(this.currentRound, this.currentAssistantItem);
        this.items.add(this.currentAssistantItem);

        this.messageList.setItems(List.copyOf(this.items));

        var round = this.currentRound;
        this.assistantService.chat(this.mode, this.conversationId, userMessage)
            .subscribe()
            .with(
                chatEvent -> ui.access(() -> {
                    switch (chatEvent) {
                        case TokenEvent token -> {
                            this.currentAssistantItem.setText("%s%s".formatted(this.currentAssistantItem.getText(), token.text()));
                            this.messageList.setItems(List.copyOf(this.items));
                        }
                        case ChunksRetrievedEvent chunksEvent -> addChunks(chunksEvent, round);
                        case CompletedEvent _ -> {}
                    }
                }),
                failure -> ui.access(() -> {
                    this.currentAssistantItem.setText("Error: %s".formatted(failure.getMessage()));
                    this.messageList.setItems(List.copyOf(this.items));
                })
            );
    }

    private void addChunks(ChunksRetrievedEvent event, int round) {
        var newRows = event.chunks().stream()
            .map(chunk -> new ChunkRow(round, chunk))
            .toList();

        this.allChunkRows.addAll(0, newRows);
        this.chunksGrid.setItems(List.copyOf(this.allChunkRows));
        this.chunksHeader.setText("Retrieved Chunks (%d)".formatted(this.allChunkRows.size()));
    }

    private static Span headerWithTooltip(String text) {
        var span = new Span(text);
        span.getElement().setAttribute("title", text);
        return span;
    }

    private void highlightMessageForRound(int round) {
        for (var item : this.roundToAssistantItem.values()) {
            item.removeClassNames("highlighted");
        }

        var targetItem = this.roundToAssistantItem.get(round);
        if (targetItem != null) {
            targetItem.addClassNames("highlighted");
        }

        this.messageList.setItems(List.copyOf(this.items));
    }
}
