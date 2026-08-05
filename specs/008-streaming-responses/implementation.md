# Spec 008: Streaming Chat Responses — Implementation Notes

Added `@Push` annotation to `AppConfig.java`. This enables
WebSocket-based server push so `ui.access()` calls in
`ChatPanel.onSubmit()` flush immediately to the browser, making
LLM tokens appear incrementally as they arrive.
