package dev.ericdeandrea.docling.ai;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.test.junit.QuarkusTest;

import dev.ericdeandrea.docling.model.Mode;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.query.Metadata;

@QuarkusTest
class ModeAwareRetrievalAugmentorTest {

    @Inject
    CurrentMode currentMode;

    @Inject
    ModeAwareRetrievalAugmentor augmentor;

    @ParameterizedTest
    @EnumSource(Mode.class)
    void augmentsForEachMode(Mode mode) {
        currentMode.mode(mode);

        var userMessage = UserMessage.from("What does Table 2 show?");
        var request = new AugmentationRequest(userMessage, Metadata.from(userMessage, null, null));

        var result = augmentor.augment(request);

        assertThat(result).isNotNull();
        assertThat(result.chatMessage()).isNotNull();
    }
}
