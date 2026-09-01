# Spec 015 — Layout-aware Preview truncation (plan)

Status: Approved

## Context

The Retrieved-Chunks grid's **Preview** column currently truncates chunk text
to a hard-coded 80 characters and appends `...`
(`ChatPanel.createChunksGrid()`, `ChatPanel.java:103-108`):

```java
var text = row.chunk().text();
return (text.length() > 80) ? "%s...".formatted(text.substring(0, 80)) : text;
```

The magic number ignores the actual column width: when the column is wide,
useful text is hidden; when narrow, the 80-char string still overflows and is
clipped anyway. The column is resizable (spec 002) and flex-grows to fill space,
so the natural behavior is to show as much text as fits and ellipsize the rest,
reflowing live on resize. This spec replaces the fixed cut with layout-aware CSS
ellipsis. Full text stays available via the existing tooltip and the spec-014
expanded item-details `Pre` (both untouched).

Two design decisions were confirmed with the user:
- **Ellipsis via explicit, scoped CSS** (a class on the Preview column + a rule
  in the theme), not reliance on undocumented Lumo cell defaults.
- **Newline collapsing done in Java** in a small extracted helper, so the
  "no char limit + single line" behavior is unit-testable; the tooltip keeps the
  full raw multi-line text.

## Approach

1. **Remove the 80-char truncation and `setAutoWidth(true)`** on the Preview
   column. `setAutoWidth(true)` sizes the column to its widest content — with
   full text it would grow the column to the full string width and defeat the
   ellipsis. Removing it lets `flexGrow(1)` bound the column to the remaining
   grid width so CSS ellipsis engages. `flexGrow(1)`, `setResizable(true)`, the
   header, and the full-text tooltip are all kept (requirement 6).

   Because truncation is pure CSS on a width-bounded cell (no fixed character
   count, no JS), it is inherently resize-reactive: dragging the column wider or
   narrower recomputes the ellipsis live in the browser — more text shows when
   widened, less when narrowed — with no server round-trip or page reload
   (requirement 3). This is the whole reason CSS is used instead of computing a
   character cut in Java.

2. **Extract a pure helper** `previewText(String)` that collapses line breaks to
   a single space (single-line preview, requirement 4) and returns the full
   text (no character limit, requirement 2). The Preview value provider calls
   it. The tooltip generator stays `row -> row.chunk().text()` (raw, full,
   multi-line — requirement 5).

3. **Scope the ellipsis with a CSS class.** Apply
   `setClassNameGenerator(row -> "chunk-preview")` to the Preview column only
   (leaving Score/Page/Type/Label untouched, per Out-of-scope) and add one rule
   to the theme stylesheet. Vaadin applies the class to the column's
   `vaadin-grid-cell-content` element (light DOM), which the global theme CSS can
   target directly.

## Files to change

- **`src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java`**
  - Preview column (currently lines 103-108): value provider becomes
    `row -> previewText(row.chunk().text())`; drop `.setAutoWidth(true)`; keep
    `.setFlexGrow(1).setResizable(true)`, header, and tooltip; add
    `.setClassNameGenerator(row -> "chunk-preview")` (called on the returned
    `Column`, since it is not part of the fluent header/width chain).
    > Implementation note: `Grid.Column.setClassNameGenerator` does not exist in
    > Vaadin 25, so the Preview column was instead rendered with a `LitRenderer`
    > wrapping the text in `<span class="chunk-preview">`. Same outcome, different
    > mechanism — see
    > [decision 4](decisions.md#4-2026-09-01-1105-edt-how-to-attach-the-ellipsis-class-in-vaadin-25-plan-deviation).
  - New package-private static helper near `headerWithTooltip` (line 183):
    ```java
    static String previewText(String text) {
        return text.replaceAll("\\R+", " ");
    }
    ```
    (`\R` matches any Unicode line break; runs collapse to a single space.)

- **`src/main/frontend/themes/garbage-in-insight-out/styles.css`**
  - Append, alongside the existing `vaadin-grid::part(round-color-N)` rules:
    ```css
    vaadin-grid-cell-content.chunk-preview {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    ```

- **`src/test/java/dev/ericdeandrea/docling/ui/ChatPanelTest.java`**
  - Add focused tests for `ChatPanel.previewText(...)` (same package, package-
    private access): a >80-char string is returned in full (no truncation, no
    `...`); a multi-line string collapses to one line with no line breaks; a
    string with no newlines is returned unchanged. Use chained AssertJ
    (`doesNotContain("\n", "...")`, `isEqualTo(...)`).
  - The existing `allColumnsAreResizable` test already guards requirement 6's
    resizable clause; no regression expected.

## Alternatives considered (not chosen)

- **Rely on Vaadin's default cell ellipsis** (no CSS): rejected — depends on
  undocumented Lumo defaults that can shift on a version bump; explicit CSS is
  self-documenting and scoped.
- **`white-space: nowrap` alone to collapse newlines** (no Java transform):
  rejected — leaves no unit-testable seam for the truncation-behavior change,
  which the project's testing rule requires.

## Verification

- **Unit/browserless tests:** `./mvnw test` — new `previewText` assertions pass
  and the existing `ChatPanelTest` suite stays green.
- **Manual, in the running app** (`quarkus dev`): ask a question in any mode,
  then drag the Preview column wider — more text appears; drag it narrower —
  less text, ellipsized at the edge, all on a single line, no page reload
  (requirements 1 & 3). Hover a truncated cell → tooltip shows full text; expand
  the row → `Pre` shows full multi-line text (requirement 5). Confirm
  Score/Page/Type/Label are visually unchanged.
