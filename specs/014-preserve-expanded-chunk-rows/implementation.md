# Spec 014 — Implementation notes

## Tasks 1 & 2 — Manual detail-visibility toggle in `createChunksGrid()`

**Approach:** In `ChatPanel.createChunksGrid()`
(`src/main/java/dev/ericdeandrea/docling/ui/ChatPanel.java`):

1. Add `grid.setDetailsVisibleOnClick(false)` so Vaadin's client no longer
   auto-toggles item details on row click (that default clears all-but-clicked,
   which is the single-open behavior being fixed).
2. Replace the existing single-expression click listener
   (`grid.addItemClickListener(event -> highlightMessageForRound(event.getItem().round()))`)
   with a block listener that:
   - reads `event.getItem()` once into `row`,
   - toggles that row only: `grid.setDetailsVisible(row, !grid.isDetailsVisible(row))`,
   - keeps the highlight call: `highlightMessageForRound(row.round())`.

Everything else in the method (item-details renderer, columns, part-name
generator, `setSizeFull`) is unchanged. No new imports needed.

Because `setDetailsVisible` mutates only the clicked item's entry in Vaadin's
`DetailsManager.detailsVisible` map, sibling rows are unaffected, and the map
survives the `setItems(...)` rebuild in `addChunks(...)` (record value equality),
so earlier-round expansions persist across new responses.

## Tasks 3–8 — Tests (`ChatPanelTest`)

Added five browserless (Karibu) tests, using `grid.getGenericDataView().getItem(n)`
to read a row and `grid.isDetailsVisible(item)` to assert state, driven by the
existing `test(grid).clickRow(n)` helper:

- `clickingRowExpandsDetails` — collapsed before click, visible after.
- `clickingSecondRowKeepsFirstExpanded` — the core regression: both rows visible.
- `reclickingRowCollapsesOnlyThatRow` — re-click collapses just that row.
- `rowClickBothExpandsAndHighlights` — one click both expands and highlights.
- `expandedRowsSurviveNewResponse` — expand a round-1 row, ask a second question,
  earlier row stays expanded (4 rows total).

Test-harness notes (not production behavior):
- Karibu locators (`find(...)`) trigger a `clientRoundtrip()` that flushes queued
  `UI.access(...)` commands. `fireSubmit` uses raw `ComponentUtil.fireEvent`, so
  the response's `addChunks` runs only on the next locator call. In
  `expandedRowsSurviveNewResponse` the grid is re-located via `find(...)` after
  the second submit so the round-2 chunks are flushed before asserting.
- `Multi.createFrom().items(...)` reused across subscriptions does not reliably
  re-emit, so the multi-round test stubs a fresh `Multi` per question (matching
  the existing `clickingDifferentRoundMovesHighlight` pattern).

All 19 `ChatPanelTest` tests pass, including the 9 pre-existing chunk-grid tests
(task 8).
