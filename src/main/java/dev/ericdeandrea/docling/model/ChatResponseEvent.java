package dev.ericdeandrea.docling.model;

import java.util.List;

public sealed interface ChatResponseEvent permits
    ChatResponseEvent.TokenEvent,
    ChatResponseEvent.ChunksRetrievedEvent,
    ChatResponseEvent.CompletedEvent {

    record TokenEvent(String text) implements ChatResponseEvent {}

    record ChunksRetrievedEvent(List<RetrievedChunk> chunks) implements ChatResponseEvent {}

    record CompletedEvent() implements ChatResponseEvent {}
}
