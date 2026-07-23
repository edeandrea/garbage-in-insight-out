package dev.ericdeandrea.docling.ai;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

import dev.ericdeandrea.docling.model.Mode;

@QuarkusTest
class CurrentModeTest {

    @Inject
    CurrentMode currentMode;

    @Test
    void holdsModWithinRequestScope() {
        currentMode.mode(Mode.DOCLING_NAIVE_CHUNK);

        assertThat(currentMode.mode()).isEqualTo(Mode.DOCLING_NAIVE_CHUNK);
    }

    @Test
    void defaultsToNull() {
        assertThat(currentMode.mode()).isNull();
    }

    @Test
    void isolatesBetweenRequestScopes() {
        var requestContext = Arc.container().requestContext();

        requestContext.activate();
        currentMode.mode(Mode.NAIVE);
        assertThat(currentMode.mode()).isEqualTo(Mode.NAIVE);
        requestContext.terminate();

        requestContext.activate();
        assertThat(currentMode.mode())
            .as("New request scope should not see previous request's mode")
            .isNull();
        requestContext.terminate();
    }
}
