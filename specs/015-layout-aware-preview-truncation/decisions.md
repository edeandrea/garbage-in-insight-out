# Spec 015 — Decisions

## 1. [2026-09-01 10:34 EDT]: How to apply the layout-aware ellipsis

**Question:** Should the Preview cell's ellipsis truncation rely on Vaadin's
built-in grid cell defaults, or be applied via explicit CSS?

**Options considered:**
- Rely on Vaadin/Lumo default cell styling (add no CSS; just remove the 80-char
  cap and `setAutoWidth(true)`).
- Add an explicit, scoped CSS rule targeting only the Preview column.

**Decision:** Explicit, scoped CSS. Apply a class to the Preview column via
`setClassNameGenerator(row -> "chunk-preview")` and add a single rule
`vaadin-grid-cell-content.chunk-preview { white-space: nowrap; overflow: hidden;
text-overflow: ellipsis; }` to the theme stylesheet.

**Reasoning:** Relying on undocumented Lumo cell defaults is fragile — they can
shift on a Vaadin version bump. Explicit CSS is self-documenting, guarantees the
behavior, and scoping it to the Preview column keeps Score/Page/Type/Label
unchanged (Out-of-scope in the spec).

## 2. [2026-09-01 10:34 EDT]: How to collapse newlines to a single line

**Question:** Requirement 4 needs the preview on a single line. Should newlines
be collapsed in Java, or left to CSS `white-space: nowrap`?

**Options considered:**
- Collapse in Java via an extracted `previewText(String)` helper.
- Do no transform; let `white-space: nowrap` collapse newlines visually.

**Decision:** Collapse in Java. Extract a pure package-private helper
`previewText(String)` that replaces line-break runs (`\R+`) with a single space
and returns the full text (no character limit). The tooltip keeps the raw,
full, multi-line text unchanged.

**Reasoning:** The project's testing rule requires changed behavior to have a
passing test. A pure helper gives a fast, unit-testable seam for "no char limit
+ single line"; a CSS-only approach leaves nothing to unit-test. The `white-
space: nowrap` rule (decision 1) still guards against any stray whitespace.

## 3. [2026-09-01 10:34 EDT]: Removing `setAutoWidth(true)` from the Preview column

**Question:** The Preview column currently sets `setAutoWidth(true)`. Can it stay
once the 80-char cap is removed?

**Decision:** Remove `setAutoWidth(true)`; keep `setFlexGrow(1)` and
`setResizable(true)`.

**Reasoning:** `setAutoWidth(true)` sizes the column to its widest content. With
the full (untruncated) text, it would grow the column to the entire string width
and defeat the CSS ellipsis. Removing it lets `flexGrow(1)` bound the column to
the remaining grid width so the ellipsis engages and reflows live on resize
(requirement 3). Requirement 6 only mandates keeping `flexGrow(1)`, resizable,
header, and tooltip — not `autoWidth`.

## 4. [2026-09-01 11:05 EDT]: How to attach the ellipsis class in Vaadin 25 (plan deviation)

**Question:** The plan ([decision 1](#1-2026-09-01-1034-edt-how-to-apply-the-layout-aware-ellipsis))
attaches the `chunk-preview` CSS class via
`Column.setClassNameGenerator(...)` on a value-provider column. That method does
not exist on `Grid.Column` in Vaadin 25 (only `setPartNameGenerator` does), and
compilation failed. How should the class be attached without abandoning the
approved "explicit scoped CSS" decision?

**Options considered:**
- `Column.setPartNameGenerator(row -> "chunk-preview")` + `::part()` CSS —
  rejected: column part names style the shadow-DOM cell `<td>`, not the slotted
  light-DOM `vaadin-grid-cell-content` where the text sits, so `text-overflow`
  would not ellipsize the text.
- Drop the explicit CSS and rely on Vaadin's built-in cell ellipsis — rejected:
  reverses [decision 1](#1-2026-09-01-1034-edt-how-to-apply-the-layout-aware-ellipsis),
  which deliberately chose explicit CSS over defaults.
- Render the preview via a `LitRenderer` that wraps the text in
  `<span class="chunk-preview">${item.preview}</span>` — chosen.

**Decision:** Use a `LitRenderer` for the Preview column, binding the
`previewText(...)` result to a `preview` property inside a
`<span class="chunk-preview">`. Style `.chunk-preview` (light-DOM span) with
`display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis`.

**Reasoning:** This is the correct Vaadin 25 way to put an explicit, scoped class
on the element that actually holds the cell text, so it faithfully realizes
[decision 1](#1-2026-09-01-1034-edt-how-to-apply-the-layout-aware-ellipsis) with
an identical visual outcome; only the Java mechanism changed (renderer instead of
value provider + class generator), forced by the framework API.
`${item.preview}` is Lit-escaped, so chunk text is rendered safely. The column
keeps `flexGrow(1)`, resizable, header, and the full-text tooltip generator, and
`previewText(...)`
([decision 2](#2-2026-09-01-1034-edt-how-to-collapse-newlines-to-a-single-line))
is unchanged.
