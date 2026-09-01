# Spec 014 — Tasks: Preserve expanded Retrieved-chunks rows

Status: Approved

Ordered by dependency. Each task is independently verifiable.

## Implementation

- [x] 1. In `ChatPanel.createChunksGrid()`
  (`src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java`), disable
  client-driven single-open behavior by calling
  `grid.setDetailsVisibleOnClick(false)`.

- [x] 2. In the same method, replace the existing
  `grid.addItemClickListener(event -> highlightMessageForRound(event.getItem().round()))`
  with a listener that first toggles the clicked row's details
  (`grid.setDetailsVisible(row, !grid.isDetailsVisible(row))`) and then calls
  `highlightMessageForRound(row.round())`. Leave the item-details renderer,
  columns, part-name generator, and `addChunks(...)` untouched.

## Tests (`src/test/java/dev/ericdeandrea/docling/ui/ChatPanelTest.java`)

- [x] 3. `clickingRowExpandsDetails` — after `test(grid).clickRow(0)`, assert
  `grid.isDetailsVisible(grid.getListDataView().getItem(0))` is `true`.

- [x] 4. `clickingSecondRowKeepsFirstExpanded` — expand row 0, then row 1;
  assert both items' details are visible. (Core regression this spec fixes.)

- [x] 5. `reclickingRowCollapsesOnlyThatRow` — expand rows 0 and 1, re-click
  row 0; assert row 0's details are hidden and row 1's are still visible
  (covers requirements 1, 2, 3).

- [x] 6. `rowClickStillHighlightsAndExpands` — a single `clickRow` both makes the
  row's details visible and moves the `highlighted` class to that round's
  assistant message (guards requirement 4). May extend an existing highlight
  test rather than adding a new method if that reads cleaner.

- [x] 7. `expandedRowsSurviveNewResponse` — expand a row, trigger a second
  response (new chunks via the same path `addChunks`/`setItems` uses), then
  assert the earlier row's details are still visible (characterizes decision 3).

- [x] 8. Confirm the existing chunk-grid tests still pass unchanged
  (`clickingChunkRowHighlightsAssistantMessage`,
  `clickingDifferentRoundMovesHighlight`, `chunksGridHasNoRoundColumn`,
  `allColumnsAreResizable`, `chunkRowsGetPartNameByRound`, `chunksHeaderShowsCount`,
  `noDetailsComponentInChunksArea`, `historyPreservedAcrossToggle`,
  `streamingContinuesWhileHidden`).

## Verification

- [x] 9. Run `./mvnw verify` (full build + failsafe ITs; never skip ITs) and
  confirm green. Record implementation notes per task in
  `specs/014-preserve-expanded-chunk-rows/implementation.md`.
