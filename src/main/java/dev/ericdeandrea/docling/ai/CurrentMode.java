package dev.ericdeandrea.docling.ai;

import jakarta.enterprise.context.RequestScoped;

import dev.ericdeandrea.docling.model.Mode;

@RequestScoped
public class CurrentMode {
    private Mode mode;

    public Mode mode() {
        return this.mode;
    }

    public void mode(Mode mode) {
        this.mode = mode;
    }
}
