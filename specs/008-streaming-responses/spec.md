# Spec 008: Streaming Chat Responses

**Status:** Approved

## Summary

Enable Vaadin Push so LLM responses stream token-by-token to the
browser instead of appearing all at once after the full response
completes.

## Motivation

Investigation verified the full streaming chain is correct:
- `ChatService.chat()` returns `Multi<ChatEvent>` — genuinely
  streaming from the LLM via `StreamingChatLanguageModel`
- `AssistantService.chat()` applies non-buffering reactive operators
  (`.filter().map()`) — no buffering
- `ChatPanel.onSubmit()` subscribes and appends each `TokenEvent`
  via `ui.access()` — correct incremental updates

The only broken link is the last mile: `ui.access()` queues UI
changes but without `@Push`, Vaadin never pushes them to the client.
The tokens accumulate silently on the server and only flush when the
next browser-initiated request arrives — which is typically after the
stream completes, making the full response appear all at once.

Enabling `@Push` on the `AppShellConfigurator` activates
WebSocket-based server push, so each `ui.access()` call flushes
immediately to the client.

## Requirements

1. **Enable Vaadin Push:** Add `@Push` annotation to `AppConfig.java`
   (the existing `AppShellConfigurator` implementation). This enables
   WebSocket-based server push for all UIs.

2. **Token-by-token streaming:** After enabling push, LLM responses
   must visibly stream in the browser — tokens appear incrementally
   as they arrive from the LLM, not all at once.

3. **No regression in existing behavior:** Chat functionality, chunk
   display, highlight-on-click, and multi-panel comparison must
   continue to work correctly.

## Out of scope

- Changing the token streaming logic in `ChatPanel.onSubmit()` or
  `AssistantService`
- Adding typing indicators or loading spinners
- Optimizing token batching or debouncing

## Open questions

None.
