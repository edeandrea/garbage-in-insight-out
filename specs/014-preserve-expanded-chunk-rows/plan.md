# Spec 014 — Plan: Preserve expanded Retrieved-chunks rows

Status: Approved

## Approach

Take manual control of the Grid's item-details visibility instead of relying on
Vaadin's default single-open click behavior.

Today `ChatPanel.createChunksGrid()` sets an item-details renderer but leaves
`detailsVisibleOnClick` at its Vaadin default of `true`. With that default, a
row click is handled entirely on the client via
`DetailsManager.setDetailsVisibleFromClient(...)`, which **clears** the
`detailsVisible` map and re-adds only the clicked row — that is the "only one row
open at a time" behavior being reported
(`Grid.java` `DetailsManager.setDetailsVisibleFromClient`, lines ~1323-1330).

The fix:

1. Call `grid.setDetailsVisibleOnClick(false)` so the client no longer
   auto-toggles details on click.
2. In the existing `addItemClickListener`, toggle the clicked row's details
   ourselves and keep the existing highlight call:

   ```java
   grid.addItemClickListener(event -> {
       var row = event.getItem();
       grid.setDetailsVisible(row, !grid.isDetailsVisible(row));
       highlightMessageForRound(row.round());
   });
   ```

`setDetailsVisible(item, ...)` mutates only the clicked item's entry in the
`DetailsManager.detailsVisible` map, so sibling rows are untouched
(requirement 3). Re-clicking flips `isDetailsVisible`, giving toggle-closed
(requirements 1, 2). The highlight/scroll behavior is preserved unchanged
(requirement 4).

## Behavior across a new response (verified against Vaadin 25.2 source)

`addChunks(...)` (`ChatPanel.java:173-181`) prepends new rows and calls
`chunksGrid.setItems(List.copyOf(this.allChunkRows))`, which installs a fresh
`ListDataProvider` on every response. This does **not** collapse
already-expanded rows:

- `DetailsManager.detailsVisible` is keyed by `getDataProvider().getId(item)`,
  which for `ListDataProvider` defaults to the item itself (value equality).
  `ChunkRow` / `RetrievedChunk` / `ChunkMetadata` are all records, so the same
  logical row across two data providers is `equals` and hashes identically.
- On data-provider change, `DetailsManager.destroyAllData()` deliberately keeps
  the `detailsVisible` map ("Remove the displayed details but keep the items from
  list of details"); `onDataProviderChange()` only resets the editor.

Net effect: rows expanded in an earlier round remain expanded after the user
asks another question. This is a desirable property for the demo (comparing
chunks across rounds) and is consistent with requirement 3. Spec 014's
out-of-scope note ("no persistence of expansion state across panel toggle /
re-render") remains accurate for the panel show/hide case (spec 012 rebuilds
happen at a different layer); this plan does not add any extra machinery to force
or prevent cross-response persistence — it simply documents the built-in
behavior. See Open questions.

## Files to change

- `src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java`
  - `createChunksGrid()`: add `grid.setDetailsVisibleOnClick(false)`; replace the
    current `addItemClickListener(event -> highlightMessageForRound(...))` with
    the toggle-plus-highlight listener above. No other lines change; the
    item-details renderer, columns, part-name generator, and `addChunks` stay
    as-is.

- `src/test/java/dev/ericdeandrea/docling/ui/ChatPanelTest.java`
  - Add Karibu (`QuarkusBrowserlessTest`) tests. To read a row's item, use
    `grid.getListDataView().getItem(index)`; to check state, use
    `grid.isDetailsVisible(item)`; to interact, use `test(grid).clickRow(index)`
    (already used by existing tests). Proposed cases:
    - `clickingRowExpandsDetails` — after `clickRow(0)`, item 0's details are
      visible.
    - `clickingSecondRowKeepsFirstExpanded` — expand row 0, then row 1; both
      items' details are visible (the core regression this spec fixes).
    - `reclickingRowCollapsesOnlyThatRow` — expand rows 0 and 1, re-click row 0;
      row 0 collapses, row 1 stays visible.
    - `rowClickStillHighlightsAndExpands` — a single click both sets details
      visible and moves the `highlighted` class (guards requirement 4 against
      regression). May be folded into existing highlight tests.
    - `expandedRowsSurviveNewResponse` — expand a row, trigger a second response
      (new chunks prepended via `addChunks`/`setItems`), assert the earlier row's
      details are still visible. Characterizes the confirmed keep-expanded
      behavior (Open question 1).
  - Existing tests (`clickingChunkRowHighlightsAssistantMessage`,
    `clickingDifferentRoundMovesHighlight`, etc.) must still pass unchanged.

No CSS changes: the expanded preview styling in the item-details renderer is
untouched.

## Tradeoffs / alternatives considered

- **Alternative: keep `detailsVisibleOnClick(true)` and re-open siblings.** Not
  viable — the client clears all-but-clicked before the server sees the event,
  so there is no clean hook to restore siblings. Manual control is the
  idiomatic Vaadin approach for multi-open details.
- **Alternative: a custom expand column / toggle button instead of row click.**
  Rejected — heavier UI change, and the row-click affordance already exists and
  is what users expect.

## Open questions

None. (Resolved: rows expanded in an earlier round stay expanded when a new
question is asked — the built-in behavior is kept, no extra code, and it is
covered by the `expandedRowsSurviveNewResponse` characterization test.)
