# Spec 012 — Reuse mode panels & preserve history on toggle

Status: Approved

## Summary

Make the mode toggle buttons in `ChatView` hide and show a single, reused
`ChatPanel` per mode instead of destroying and recreating it. Toggling a mode
off then on again preserves that panel's full state: chat conversation, the
retrieved-chunks table and its count, message-highlight mapping, and the
LangChain4j conversation id.

## Motivation

During the live demo the presenter toggles modes on and off to compare RAG
strategies side by side. Today each toggle-off discards the panel and each
toggle-on creates a new one with a fresh conversation, so all chat history and
retrieved chunks are lost and the LLM starts a new memory thread. The presenter
cannot temporarily collapse a mode to reduce screen clutter and bring it back
with its context intact. Reusing the panel lets a mode be hidden and restored
without losing anything.

## Requirements

1. Toggling a mode **off** hides its panel (both the message area and the chunks
   area) without destroying the backing `ChatPanel` instance or any of its
   state.
2. Toggling a previously-shown mode **on** reuses the same `ChatPanel` instance,
   preserving: chat message history, retrieved-chunk rows and the
   "Retrieved Chunks (N)" count, the round-to-assistant-message highlight
   mapping, the current round counter, and the `conversationId` (so the backend
   LangChain4j chat memory continues in the same conversation).
3. A mode's `ChatPanel` is created lazily on its first toggle-on and retained
   for the lifetime of the `ChatView` instance; no mode's panel is ever
   constructed more than once per view.
4. Currently-visible panels are displayed in fixed `Mode` declaration order
   (NAIVE, DOCLING_NAIVE_CHUNK, DOCLING_HYBRID_CHUNK) left-to-right in both the
   message container and the chunks container, regardless of the order in which
   modes were toggled on.
5. Each toggle button's active visual state (the `LUMO_PRIMARY` theme variant)
   reflects whether that mode's panel is currently visible.
6. Panel dividers are computed over only the currently-visible panels: the
   leftmost visible panel has no inline-start border, every subsequent visible
   panel has an inline-start divider, and hidden panels carry no divider.
7. Initial load state is unchanged: only Mode A (NAIVE) is visible.
8. If a mode is hidden while its response is still streaming, the in-flight
   response continues to update that panel; when the mode is shown again, the
   streamed answer and its retrieved chunks are present.

## Out of scope

- Persisting history across page reloads or new browser sessions (state remains
  in-memory for the life of the `ChatView` instance).
- Changing the chat-memory storage backend or introducing a custom
  `ChatMemoryStore`/`ChatMemoryProvider`.
- Cancelling in-flight requests when a mode is hidden.
- Any change to mode semantics, RAG behavior, or the set of modes.

## Open questions

None. Two prior questions were resolved:

- Re-shown panel placement -> fixed Mode order (Req 4).
- Hide during streaming -> keep streaming, show on return (Req 8).
