package dev.ericdeandrea.docling.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

import dev.ericdeandrea.docling.model.Mode;

@QuarkusTest
class ChatServiceTest {

    @Inject
    ChatService chatService;

    @Inject
    CurrentMode currentMode;

    @BeforeEach
    void activateSessionContext() {
        Arc.container().sessionContext().activate();
    }

    @AfterEach
    void deactivateSessionContext() {
        Arc.container().sessionContext().deactivate();
    }

    @Test
    void isInjectable() {
        assertThat(chatService).isNotNull();
    }

    @Test
    void chatStreamsEventsForEachMode() {
        for (var mode : Mode.values()) {
            currentMode.mode(mode);

            var events = chatService.chat(UUID.randomUUID(), "What is DocLayNet?")
                .collect().asList()
                .await().indefinitely();

            assertThat(events)
                .as("Mode %s should produce chat events", mode)
                .isNotEmpty();
        }
    }

    @Test
    void requestScopedModeChangesPerCall() {
        currentMode.mode(Mode.NAIVE);
        var memoryId1 = UUID.randomUUID();

        var events1 = chatService.chat(memoryId1, "What is DocLayNet?")
            .collect().asList()
            .await().indefinitely();

        assertThat(events1).isNotEmpty();

        currentMode.mode(Mode.DOCLING_HYBRID_CHUNK);
        var memoryId2 = UUID.randomUUID();

        var events2 = chatService.chat(memoryId2, "What is DocLayNet?")
            .collect().asList()
            .await().indefinitely();

        assertThat(events2).isNotEmpty();
    }
}
