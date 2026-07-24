# Spec 002: UI Polish — Tasks

**Status:** Approved

## Tasks

- [x] 1. **Decompose ChatPanel into a plain coordinator class ([req 6](spec.md#requirements))**
  ChatPanel stops extending `VerticalLayout`. Constructor builds two
  internal `VerticalLayout` containers: `messageArea` (MessageList +
  MessageInput) and `chunksArea` (header Span + Grid). Expose via
  `messageArea()` and `chunksArea()` getters. Remove `Details` wrapper —
  replace `chunksDetails` field with a `Span chunksHeader`. Update
  `addChunks()` to set header text instead of `Details.setSummaryText()`.
  Remove `setAllRowsVisible(true)` from the grid. Remove all layout
  calls (`setSizeFull`, `setPadding`, `setSpacing`, `add`, `expand`) from
  ChatPanel itself — those now belong on the internal containers.

- [x] 2. **Restructure ChatView with SplitLayout ([req 6](spec.md#requirements))**
  Replace `panelContainer` (`HorizontalLayout`) with `splitLayout`
  (`SplitLayout`, `Orientation.VERTICAL`), `messageContainer`
  (`HorizontalLayout`), and `chunksContainer` (`HorizontalLayout`).
  Set splitter position to 70. Add `messageContainer` as primary,
  `chunksContainer` as secondary. Update `toggleMode()` to add/remove
  `panel.messageArea()` and `panel.chunksArea()` to/from the respective
  containers instead of adding `panel` to `panelContainer`.

- [x] 3. **Update existing tests for decomposition ([req 6](spec.md#requirements))**
  Update `ChatPanelTest`: change `find()` calls to use
  `panel.messageArea()` / `panel.chunksArea()` as search roots. Update
  `fireSubmit()` helper to find `MessageInput` inside
  `panel.messageArea()`. Update `ChatViewTest`: existing toggle tests
  should still pass via `view.panels()` / `view.toggleMode()`. Fix any
  compilation errors from the ChatPanel type change. Run the test suite
  to confirm all existing tests pass.

- [x] 4. **Remove `#` column ([req 3](spec.md#requirements))**
  Delete the `grid.addColumn(ChunkRow::round).setHeader("#")...` call
  from `createChunksGrid()`. Add test: verify grid has 6 columns and
  none has header `"#"`.

- [x] 5. **Make grid columns resizable ([req 2](spec.md#requirements))**
  Chain `.setResizable(true)` on every `addColumn()` call in
  `createChunksGrid()`. Add test: verify every column in
  `grid.getColumns()` returns `isResizable() == true`.

- [x] 6. **Add row color coding ([req 1](spec.md#requirements))**
  Change `MAX_COLOR_INDEX` from `9` to `7`. Add
  `grid.setPartNameGenerator()` in `createChunksGrid()` using
  `row.round() % MAX_COLOR_INDEX` to produce part names `round-color-0`
  through `round-color-6`. Add 7 `vaadin-grid::part(round-color-N)` CSS
  rules in `styles.css` using `color-mix(in srgb,
  var(--vaadin-user-color-N) 15%, transparent)`. Add test: verify part
  name generator returns expected values for known rounds.

- [x] 7. **Non-wrapping title ([req 4](spec.md#requirements))**
  Add `LumoUtility.Whitespace.NOWRAP` class to the `H2` in
  `createToolbar()`. Add test: verify `H2` has the `whitespace-nowrap`
  class.

- [x] 8. **Panel borders ([req 5](spec.md#requirements))**
  Add private `updatePanelBorders()` in `ChatView`, called from
  `toggleMode()` after every add/remove. When `panels.size() >= 2`, add
  `Border.START`, `Border.END`, and `BorderColor.CONTRAST_10` to every
  panel's `messageArea()` and `chunksArea()`. When `< 2`, remove those
  classes. Add tests: single panel has no border classes; two panels
  have border classes; toggling back to one removes them.

- [x] 9. **Sticky toolbar ([req 7](spec.md#requirements))**
  Add `LumoUtility` classes to the toolbar: `Position.STICKY`,
  `Position.Top.NONE`, `ZIndex.SMALL`, `Border.BOTTOM`,
  `BorderColor.CONTRAST_10`, and custom class `"sticky-toolbar"`. Add
  `.sticky-toolbar { background-color: var(--lumo-base-color); }` rule
  to `styles.css`. Add test: verify toolbar has the expected class names.

- [x] 10. **Add new SplitLayout and layout tests ([req 6](spec.md#requirements))**
  Add `ChatViewTest` tests: verify `SplitLayout` has vertical
  orientation, splitter position is 70, primary component is the message
  container, secondary is the chunks container. Verify `MessageInput` is
  in the top half and `Grid` is in the bottom half. Verify no `Details`
  component exists. Add `ChatPanelTest` test: verify "Retrieved
  Chunks (N)" header label appears in `chunksArea()` after a submit.

- [x] 11. **Full test suite pass**
  Run `./mvnw verify` and confirm all tests pass. Fix any failures.
