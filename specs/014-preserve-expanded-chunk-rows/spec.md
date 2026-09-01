# Spec 014 — Preserve expanded Retrieved-chunks rows

Status: Approved

## Summary

Change the Retrieved Chunks grid so that expanding one row's full-text preview
no longer collapses any other expanded row. Each row's expanded/collapsed state
is independent, allowing multiple previews to be open at once for side-by-side
comparison. Re-clicking an expanded row collapses just that row.

## Motivation

Each row in the Retrieved Chunks grid can be clicked to expand an inline
full-text preview of the chunk. Today the grid relies on Vaadin's default
item-details behavior (`detailsVisibleOnClick = true`), which opens the clicked
row's details while collapsing whichever row was previously open. Only one
preview can be visible at a time.

During the live demo, comparing the text of several retrieved chunks (e.g.
across scores, pages, or element types) means expanding one, reading it,
collapsing it, then expanding the next. Being able to keep several previews open
simultaneously makes it far easier to show why particular chunks were retrieved
and how they differ — which is the whole point of the Retrieved Chunks panel.

## Requirements

1. Clicking a collapsed chunk row expands it to show the full-text preview.
2. Clicking an expanded chunk row collapses only that row.
3. Expanding or collapsing one row does not change the expanded/collapsed state
   of any other row; multiple rows may be expanded simultaneously.
4. Every row click continues to highlight and scroll to the corresponding
   assistant chat message (the existing `highlightMessageForRound` behavior),
   on both expand and collapse.
5. The new behavior coexists with existing grid behaviors — round color coding,
   resizable columns, the 5-column layout, and panel toggle/history
   preservation — without regression.

## Out of scope

- No "expand all" / "collapse all" control.
- No change to the columns, the Preview column's 80-char truncation, its
  tooltip, or the details preview styling.
- No persistence of expansion state across panel toggle or panel re-render; this
  spec is only about not collapsing sibling rows on click. (A later spec may add
  cross-render persistence if desired.)

## Open questions

None.
