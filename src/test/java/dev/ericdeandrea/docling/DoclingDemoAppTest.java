package dev.ericdeandrea.docling;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DoclingDemoAppTest {
    @Inject
    DoclingDemoApp app;

    @Test
    void appStarts() {
        assertThat(app).isNotNull();
    }
}
