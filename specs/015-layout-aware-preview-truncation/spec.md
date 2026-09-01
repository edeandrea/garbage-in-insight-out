# Spec 015 — Layout-aware Preview truncation

Status: Approved

## Summary

Replace the fixed 80-character truncation of the Retrieved Chunks "Preview"
column with layout-aware truncation: the preview text fills the available column
width and is trimmed with an ellipsis only when it runs out of horizontal space.
Because the column is resizable, the amount of visible text adapts live as the
column is widened or narrowed. The existing full-text tooltip is retained.

## Motivation

The Preview column currently truncates chunk text to a hard-coded 80 characters
(`ChatPanel.createChunksGrid()`,
`src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java:103-108`):

```java
var text = row.chunk().text();
return (text.length() > 80) ? "%s...".formatted(text.substring(0, 80)) : text;
```

The 80-char cut is arbitrary and ignores the actual column width. When the
column is wide, useful text is hidden behind "..."; when narrow, the truncated
string still overflows and gets clipped by the cell anyway. The column is
already resizable (spec 002), so the natural behavior is to show as much text as
fits and ellipsize the remainder — which is also what Vaadin grid cells do by
default for plain-text columns. Letting CSS handle truncation removes the magic
number, always uses the full available width, and reflows automatically on
resize.

## Requirements

1. The Preview cell shows chunk text that fills the available column width and
   is truncated with a trailing ellipsis only when the text exceeds that width.
2. Truncation is driven by available layout width, not a fixed character count;
   no hard-coded character limit remains in the Preview column code.
3. When the Preview column is resized wider, more text becomes visible; when
   resized narrower, less text is visible — without a page reload.
4. The Preview cell renders on a single line (chunk text containing newlines is
   collapsed to a single line for the preview).
5. The full, untruncated chunk text remains available via the existing cell
   tooltip and via the row's expanded details preview (unchanged).
6. The change does not regress other Preview-column behavior: it keeps
   `flexGrow(1)`, remains resizable, and keeps its header/tooltip.

## Out of scope

- No changes to the Score, Page, Type, or Label columns.
- No change to the expanded item-details preview (spec 014 territory).
- No change to row-expansion state behavior.
- No word-wrapping / multi-line preview cells — the preview stays single-line.

## Open questions

None.
