# Spec 009: Q&A Round Separator

**Status:** Approved

## Summary

Add a visual separator between each question/answer round in the chat
panels so consecutive rounds are visually distinct.

## Motivation

When multiple questions are asked in a chat panel, the rounds run
together with no visual break. The per-round color coding on assistant
message avatars helps, but it's subtle. A clear separator between
rounds makes the conversation easier to scan during a demo —
especially when the audience is comparing across multiple panels.

## Requirements

1. **Visual separator between rounds:** Each Q&A round (user message +
   assistant response) must be visually separated from the
   previous/next round. Since every round starts with a "You" message
   (which already has the `current-user` class from spec 007), a
   CSS-only solution targeting `vaadin-message.current-user` is
   preferred — e.g., a top border or extra margin on user messages
   that are not the first message in the list.

2. **First message has no separator:** The first user message in the
   list should NOT have a separator above it.

3. **Works in both light and dark themes.**

## Out of scope

- Changing the message content or layout
- Adding collapsible/foldable round sections
- Numbered round indicators

## Open questions

None.
