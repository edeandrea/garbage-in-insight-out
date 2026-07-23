package dev.ericdeandrea.docling.ai;

import dev.ericdeandrea.docling.model.Mode;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
class CurrentMode {

    private Mode mode;

    Mode mode() {
        return mode;
    }

    void mode(Mode mode) {
        this.mode = mode;
    }
}
