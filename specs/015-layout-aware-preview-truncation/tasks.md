# Spec 015 — Tasks: Layout-aware Preview truncation

Status: Approved

Ordered by dependency. Each task is independently verifiable.

## Implementation

- [x] 1. In `ChatPanel`
  (`src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java`), add a
  package-private static helper `previewText(String text)` (near
  `headerWithTooltip`) that returns `text.replaceAll("\\R+", " ")` — collapses
  any line-break run to a single space, no character limit (requirements 2, 4).

- [x] 2. In `createChunksGrid()`, change the Preview column to render
  `previewText(row.chunk().text())`. Leave the tooltip generator as
  `row -> row.chunk().text()` (full, raw, multi-line — requirement 5).

- [x] 3. On the same Preview column, drop `.setAutoWidth(true)` while keeping
  `.setFlexGrow(1)`, `.setResizable(true)`, and the header (requirement 6;
  autoWidth would size the column to the full string and defeat the ellipsis).
  Attach the `chunk-preview` CSS class to the cell text. Leave the Score, Page,
  Type, and Label columns untouched. (In Vaadin 25 the class is attached via a
  `LitRenderer` wrapping the text in `<span class="chunk-preview">`, since
  `Column.setClassNameGenerator` does not exist — see
  [decision 4](decisions.md#4-2026-09-01-1105-edt-how-to-attach-the-ellipsis-class-in-vaadin-25-plan-deviation).)

- [x] 4. In the theme stylesheet
  (`src/main/frontend/themes/garbage-in-insight-out/styles.css`), append a rule
  scoped to the Preview column's cell text:
  `.chunk-preview { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }`
  (requirement 1).

## Tests (`src/test/java/dev/ericdeandrea/docling/ui/ChatPanelTest.java`)

- [x] 5. `previewTextKeepsFullTextForLongInput` — a string longer than 80 chars
  is returned in full: `isEqualTo(input)` and `doesNotContain("...")`
  (requirement 2, guards against the removed truncation).

- [x] 6. `previewTextCollapsesNewlinesToSingleLine` — a multi-line string returns
  a single line: `doesNotContain("\n", "\r")` and equals the newline-collapsed
  form (requirement 4).

- [x] 7. `previewTextLeavesSingleLineUnchanged` — a string with no line breaks is
  returned unchanged (`isEqualTo(input)`).

- [x] 8. Confirm the existing chunk-grid tests still pass unchanged — in
  particular `allColumnsAreResizable` (guards requirement 6's resizable clause),
  plus `chunksGridHasNoRoundColumn`, `chunksGridPopulatedAfterResponse`,
  `chunkRowsGetPartNameByRound`, `chunksHeaderShowsCount`.

## Verification

- [x] 9. Run `./mvnw verify` (full build + failsafe ITs; never skip ITs) and
  confirm green. Record implementation notes per task in
  `specs/015-layout-aware-preview-truncation/implementation.md`.
  (Green: 87 tests, 9 gated-skips, failsafe ITs passed.)

- [ ] 10. Manual check in the running app (`quarkus dev`): ask a question, drag
  the Preview column wider (more text shows) and narrower (ellipsis at the edge,
  single line, no reload — requirements 1, 3); hover a truncated cell for the
  full-text tooltip; expand the row for the full multi-line `Pre` (requirement 5);
  confirm Score/Page/Type/Label look unchanged.
