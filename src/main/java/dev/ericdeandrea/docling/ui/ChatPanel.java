package dev.ericdeandrea.docling.ui;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dev.ericdeandrea.docling.model.Mode;

class ChatPanel extends VerticalLayout {

    private final Mode mode;
    private final MessageList messageList;
    private final MessageInput messageInput;

    ChatPanel(Mode mode) {
        this.mode = mode;
        this.messageList = new MessageList();
        this.messageInput = new MessageInput();

        messageList.setMarkdown(true);
        messageList.setSizeFull();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(messageList, messageInput);
        expand(messageList);
        messageInput.setWidthFull();
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
