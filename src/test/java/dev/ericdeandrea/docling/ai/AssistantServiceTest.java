package dev.ericdeandrea.docling.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

import dev.ericdeandrea.docling.model.ChatResponseEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.ChunksRetrievedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.CompletedEvent;
import dev.ericdeandrea.docling.model.ChatResponseEvent.TokenEvent;
import dev.ericdeandrea.docling.model.Mode;

@QuarkusTest
class AssistantServiceTest {

    @Inject
    AssistantService assistantService;

    @BeforeEach
    void activateSessionContext() {
        Arc.container().sessionContext().activate();
    }

    @AfterEach
    void deactivateSessionContext() {
        Arc.container().sessionContext().deactivate();
    }

    @Test
    void producesTokenAndCompletedEvents() {
        var events = assistantService.chat(Mode.NAIVE, UUID.randomUUID(), "What is DocLayNet?")
            .collect().asList()
            .await().indefinitely();

        assertThat(events)
            .isNotEmpty()
            .anySatisfy(event -> assertThat(event).isInstanceOf(TokenEvent.class))
            .last().isInstanceOf(CompletedEvent.class);
    }

    @Test
    void producesChunksRetrievedEvents() {
        var events = assistantService.chat(Mode.DOCLING_HYBRID_CHUNK, UUID.randomUUID(), "What does Table 2 show?")
            .collect().asList()
            .await().indefinitely();

        assertThat(events)
            .anySatisfy(event -> assertThat(event).isInstanceOf(ChunksRetrievedEvent.class));

        var chunksEvent = events.stream()
            .filter(ChunksRetrievedEvent.class::isInstance)
            .map(ChunksRetrievedEvent.class::cast)
            .findFirst()
            .orElseThrow();

        assertThat(chunksEvent.chunks()).isNotEmpty();
    }

    @Test
    void switchesModesBetweenRequests() {
        var modeAEvents = assistantService.chat(Mode.NAIVE, UUID.randomUUID(), "What is DocLayNet?")
            .collect().asList()
            .await().indefinitely();

        assertThat(modeAEvents).isNotEmpty();

        var modeCEvents = assistantService.chat(Mode.DOCLING_HYBRID_CHUNK, UUID.randomUUID(), "What is DocLayNet?")
            .collect().asList()
            .await().indefinitely();

        assertThat(modeCEvents).isNotEmpty();
    }

    @Test
    void noLangChain4jTypesInEvents() {
        var events = assistantService.chat(Mode.NAIVE, UUID.randomUUID(), "What is DocLayNet?")
            .collect().asList()
            .await().indefinitely();

        assertThat(events)
            .isNotEmpty()
            .allSatisfy(event -> assertThat(event).isInstanceOf(ChatResponseEvent.class));
    }
}
