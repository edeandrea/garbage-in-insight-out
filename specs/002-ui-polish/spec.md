# Spec 002: UI Polish

**Status:** Approved

## Summary

Address seven visual and usability issues in the chat comparison UI to
improve readability, layout stability, and panel management during live
conference demos.

## Motivation

The current UI relies on Vaadin Lumo defaults with minimal customization.
During demos, several rough edges are visible on stage: the title wraps
awkwardly, panels lack visual separation, the chunks table has no color
coding linking it to chat messages, columns can't be resized, and the
toolbar scrolls away. These issues distract from the demo content and
make the comparison view harder to read.

[Decision #54](../001-three-mode-rag-demo/decisions.md#54-2026-07-23-0954-edt-response-to-chunks-via-color-coding-not-click-handlers) in [spec 001](../001-three-mode-rag-demo/spec.md) already called for "matching color on grid rows"
per round, but only the assistant message avatar coloring was implemented
(via `setUserColorIndex()`). The grid row coloring was deferred.

## Requirements

1. **Chunk table row color coding:** Rows in the "Retrieved Chunks" grid
   must be color-coded per conversation round, using the same color as
   the corresponding assistant message avatar. The existing Vaadin user
   color palette (`--vaadin-user-color-0` through `--vaadin-user-color-6`)
   is the source of truth. Each round's chunk rows must visually match
   the avatar ring color of that round's assistant message.
   This is a passive always-visible indicator. The existing
   click-to-highlight interaction (clicking a chunk row highlights the
   corresponding assistant message) remains unchanged and coexists with
   the per-round coloring. This completes the intent of
   [spec 001 decision #54](../001-three-mode-rag-demo/decisions.md#54-2026-07-23-0954-edt-response-to-chunks-via-color-coding-not-click-handlers),
   which specified "matching color on grid rows" but was not
   implemented for the grid side.

2. **Resizable grid columns:** All columns in the "Retrieved Chunks"
   grid must be user-resizable by dragging column borders.

3. **Remove `#` column:** The round-number (`#`) column in the chunks
   grid must be removed. The round is already conveyed by the row color
   coding (requirement 1).

4. **Non-wrapping title:** The "Garbage In, Insight Out" heading in the
   toolbar must remain on a single line. It must not wrap regardless of
   how many mode buttons are visible or how narrow the viewport becomes.

5. **Panel borders:** When two or more chat panels are displayed
   simultaneously, each panel must have a visible border to distinguish
   it from adjacent panels. When only one panel is shown, no border is
   needed.

6. **Shared resizable split between messages and chunks:** The layout
   must use a single shared horizontal divider (vertical `SplitLayout`)
   separating the message-list area from the chunks-table area. All
   active panels' message lists sit side by side above the divider; all
   active panels' chunk tables sit side by side below it. Dragging the
   divider resizes both sections for all panels simultaneously, keeping
   them aligned. The default split ratio is **70/30** (messages get 70%
   of the vertical space, chunks get 30%). When three modes are active,
   each panel gets ~33% width. When two are active, each gets ~50%.

   Layout detail:
   - **Top half (above splitter):** Each panel's `MessageList` and
     `MessageInput` — the input stays with its message list.
   - **Bottom half (below splitter):** Each panel's chunks grid, shown
     directly (no `Details` collapsible wrapper). A label/header reading
     "Retrieved Chunks (N)" must appear above each panel's grid to
     preserve the count display currently provided by the `Details`
     summary text.

7. **Sticky toolbar:** The page header (title, mode buttons, theme
   toggle) must be fixed at the top of the viewport. It must not scroll
   away when the message list or chunks table content overflows.

## Out of scope

- Custom color palette or full theme overhaul (Lumo defaults remain)
- Changes to chat functionality, message streaming, or LLM behavior
- Mobile or responsive layout considerations
- New demo modes or features
- Accessibility improvements beyond what these changes naturally provide

## Verification

Each requirement must have a corresponding test (unit or integration)
covering it. Tests should follow the project's existing Vaadin UI test
patterns.

1. **Chunk table row color coding:** Verify that after submitting a
   question and receiving chunks, each chunk row's CSS includes a
   background color derived from the same `--vaadin-user-color-{n}`
   variable as the corresponding assistant message's `userColorIndex`.
   Verify across at least two rounds to confirm distinct colors.

2. **Resizable grid columns:** Verify that all grid columns have
   `setResizable(true)`.

3. **Remove `#` column:** Verify the grid has no column keyed to the
   round number. Verify total column count matches expected count
   (current minus one).

4. **Non-wrapping title:** Verify the `H2` element has `white-space:
   nowrap` (or equivalent Vaadin utility class) applied.

5. **Panel borders:** Verify that when two or more panels are visible,
   each panel element has a border style applied. Verify that when only
   one panel is visible, no border is present.

6. **Shared resizable split:** Verify that the layout uses a
   `SplitLayout` with vertical orientation, that the default splitter
   position is 70%, and that message lists and chunk tables are in
   separate halves of the split. Verify panels are arranged side by
   side within each half. Verify each panel's `MessageInput` is in the
   top half (with its `MessageList`). Verify chunks grids are shown
   directly (no `Details` wrapper). Verify a "Retrieved Chunks (N)"
   label appears above each panel's grid.

7. **Sticky toolbar:** Verify that the toolbar has a fixed/sticky
   position style applied so it does not scroll with content.

## Open questions

None — all resolved during spec drafting.
