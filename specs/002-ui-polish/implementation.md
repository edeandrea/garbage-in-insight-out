# Spec 002: UI Polish — Implementation Notes

## Task 1: Decompose ChatPanel into a plain coordinator class

ChatPanel stops extending `VerticalLayout`. The constructor builds two
internal `VerticalLayout` containers (`messageArea` and `chunksArea`)
and wires the existing components into them. All event logic stays in
ChatPanel. The `Details` field is replaced with a `Span` header.
`setAllRowsVisible(true)` is removed from the grid.

## Task 2: Restructure ChatView with SplitLayout

Replace `panelContainer` with `SplitLayout` (vertical, 70/30).
`messageContainer` and `chunksContainer` become the primary and
secondary. `toggleMode()` adds/removes from both containers.

## Task 3: Update existing tests for decomposition

`ChatPanelTest`: change `find()` search roots from `panel` (no longer a
Component) to `panel.messageArea()` or `panel.chunksArea()`. The
`fireSubmit` helper searches `panel.messageArea()` for the MessageInput.
`ChatViewTest`: unchanged — it uses `view.panels()` / `view.toggleMode()`
which still work.

## Task 4: Remove `#` column

Delete the round-number column from `createChunksGrid()`. The `round`
field on `ChunkRow` is retained for the part name generator and
highlight logic.

## Task 5: Make grid columns resizable

Chain `.setResizable(true)` on every `addColumn()` call.

## Task 6: Add row color coding

Change `MAX_COLOR_INDEX` from 9 to 7. Add `setPartNameGenerator()` to
the grid. Add 7 CSS `::part()` rules in `styles.css`. Also added the
`.sticky-toolbar` CSS rule ahead of task 9 since it was in the same file.

## Task 7: Non-wrapping title

Add `Whitespace.NOWRAP` to the `H2`.

## Task 8: Panel borders

Add `updatePanelBorders()` to `ChatView`. Uses `Border.START`,
`Border.END`, and `BorderColor.CONTRAST_10` on both `messageArea()`
and `chunksArea()` when 2+ panels are active.

## Task 9: Sticky toolbar

Add `Position.STICKY`, `Position.Top.NONE`, `ZIndex.SMALL`,
`Border.BOTTOM`, `BorderColor.CONTRAST_10`, and `"sticky-toolbar"` to
the toolbar.

## Task 10: New tests

ChatPanelTest: `chunksGridHasNoRoundColumn`, `allColumnsAreResizable`,
`chunkRowsGetPartNameByRound`, `chunksHeaderShowsCount`,
`noDetailsComponentInChunksArea`.

ChatViewTest: `layoutUsesSplitLayoutWithVerticalOrientation`,
`splitLayoutDefaultPositionIs70`, `titleDoesNotWrap`,
`singlePanelHasNoBorder`, `multiplePanelsHaveBorders`,
`toolbarIsStickyAtTop`.

## Additional refinements during implementation

- **Two-row toolbar** (decision #14): title centered on top row, mode
  buttons centered on bottom row, theme toggle right-justified on top
  row. Title font reduced to `--lumo-font-size-xl`. H2 margin removed.
  Toolbar and ChatView padding removed.
- **Chunks header always visible**: initialized to "Retrieved Chunks (0)"
  instead of empty.
- **Borders between panels only**: left border on non-first panels via
  inline styles. No borders on screen edges.
- **Column header tooltips**: each column header is a `Span` with a
  `title` attribute.
- **Page title**: `@PageTitle("Garbage In, Insight Out")` on ChatView.
- **Timestamp formatting**: `DateTimeFormatter.ofPattern("MMM d, h:mm a")`
  with system default timezone.
- **Grid scrollability**: `setSizeFull()` instead of `setWidthFull()`.
- **Metadata label/type**: deferred to separate spec (decision #15).
