# Spec 007: Right-Justify User Messages

**Status:** Approved

## Summary

Right-justify the "You" messages in the chat panels so user messages
appear on the right side and assistant messages remain on the left,
following the standard chat UI convention.

## Motivation

Standard chat UIs place the current user's messages on the right and
the other party's messages on the left. This visual distinction makes
conversations easier to follow at a glance — especially during a live
demo where the audience needs to quickly distinguish questions from
answers.

## Requirements

1. **Right-justify user messages:** Messages with userName "You" must
   be right-aligned in the MessageList — avatar on the right, text
   right-aligned. Assistant messages remain left-aligned (unchanged).

2. **CSS-only layout change:** Use `addClassNames("current-user")` on
   user `MessageListItem`s and CSS targeting `vaadin-message.current-user`
   with `flex-direction: row-reverse` and `::part()` selectors. This
   follows the existing `.highlighted` pattern in the project.

3. **Works in both light and dark themes.**

## Out of scope

- Changing message content, formatting, or markdown rendering
- Chat bubble styling or background colors for user messages
- Changes to assistant message layout

## Open questions

None.
