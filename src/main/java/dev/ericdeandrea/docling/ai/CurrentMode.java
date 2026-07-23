package dev.ericdeandrea.docling.ai;

import jakarta.enterprise.context.RequestScoped;

import dev.ericdeandrea.docling.model.Mode;

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
