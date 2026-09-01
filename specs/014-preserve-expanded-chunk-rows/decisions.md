# Spec 014 — Decisions

## 1. [2026-09-01 10:30 EDT]: Re-click behavior for an expanded row

**Question:** When a user clicks a chunk row that is already expanded, should it
toggle closed or stay open?

**Options considered:**
- Toggle it closed (clicking an expanded row collapses just that row).
- Keep it open (no way to collapse an individual row via click).

**Decision:** Toggle it closed. Only the clicked row collapses; other expanded
rows are unaffected. Standard toggle affordance.

## 2. [2026-09-01 10:30 EDT]: Highlight on every row click vs. only on expand

**Question:** The row click currently highlights and scrolls to the matching
assistant chat message. Should that keep firing on every click, or only when a
row is expanded (not when collapsed)?

**Options considered:**
- Keep highlighting on every click (expand and collapse).
- Highlight only when expanding.

**Decision:** Keep highlighting on every click, unchanged from today's
`highlightMessageForRound(...)` behavior. Simpler, and the highlight is useful
regardless of expand/collapse direction.

## 3. [2026-09-01 10:30 EDT]: Expanded-row persistence across a new response

**Question:** With the manual-toggle fix (`setDetailsVisibleOnClick(false)` +
per-item `setDetailsVisible`), Vaadin's `DetailsManager` keeps its
`detailsVisible` map across the `setItems(...)` rebuild in `addChunks(...)`
(keyed by record value equality). So earlier-round rows naturally stay expanded
when the user asks a new question. Keep that, or explicitly collapse all rows on
each new response?

**Options considered:**
- Keep them expanded (free from the implementation; aids cross-round chunk
  comparison).
- Force-collapse on each new response (extra code in `addChunks`; loses
  cross-round comparison).

**Decision:** Keep them expanded. It requires no extra code, is verified against
the Vaadin 25.2 `Grid` source (`DetailsManager.destroyAllData` deliberately
preserves the visibility map), and supports comparing chunks across rounds in
the live demo. Covered by an `expandedRowsSurviveNewResponse` characterization
test. Note: this refines spec 014's out-of-scope line about cross-render
persistence — that line still applies to panel show/hide (spec 012 layer); it
does not apply to the new-response case, which now persists.
