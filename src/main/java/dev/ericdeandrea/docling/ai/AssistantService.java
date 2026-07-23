package dev.ericdeandrea.docling.ai;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent.ChatCompletedEvent;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent.ContentFetchedEvent;
import io.quarkiverse.langchain4j.runtime.aiservice.ChatEvent.PartialResponseEvent;
import io.smallrye.mutiny.Multi;

import dev.ericdeandrea.docling.mapping.ChunkMapper;
import dev.ericdeandrea.docling.model.ChatResponseEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.ChunksRetrievedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.CompletedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.TokenEvent;
import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.rag.content.ContentMetadata;

@ApplicationScoped
public class AssistantService {

    private final ChatService chatService;
    private final CurrentMode currentMode;
    private final ChunkMapper chunkMapper;

    AssistantService(ChatService chatService, CurrentMode currentMode, ChunkMapper chunkMapper) {
        this.chatService = chatService;
        this.currentMode = currentMode;
        this.chunkMapper = chunkMapper;
    }

    public Multi<ChatResponseEvent> chat(Mode mode, UUID memoryId, String message) {
        currentMode.mode(mode);

        return chatService.chat(memoryId, message)
            .filter(event -> event instanceof PartialResponseEvent
                || event instanceof ContentFetchedEvent
                || event instanceof ChatCompletedEvent)
            .map(this::toModelEvent);
    }

    private ChatResponseEvent toModelEvent(ChatEvent event) {
        return switch (event) {
            case PartialResponseEvent partial -> new TokenEvent(partial.getChunk());
            case ContentFetchedEvent fetched -> {
                var chunks = fetched.getContent().stream()
                    .map(content -> {
                        var score = (content.metadata() != null)
                            ? (Double) content.metadata().getOrDefault(ContentMetadata.SCORE, 0.0)
                            : 0.0;
                        return chunkMapper.toRetrievedChunk(content.textSegment(), score, Instant.now());
                    })
                    .toList();
                yield new ChunksRetrievedEvent(chunks);
            }
            case ChatCompletedEvent ignored -> new CompletedEvent();
            default -> throw new IllegalStateException(
                "Unexpected event type: %s".formatted(event.getEventType()));
        };
    }
}
