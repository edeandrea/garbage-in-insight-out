# Implementation 012 — Reuse mode panels & preserve history on toggle

## Task 1 — ChatView: create-once + hide/show reuse, fixed order, visible-only borders

**Approach (per plan.md):**

- `toggleMode(Mode)`: lazily obtain the panel via
  `panels.computeIfAbsent(mode, this::createPanel)`, then flip
  `setVisible(...)` on both layouts and add/remove the button's `LUMO_PRIMARY`
  variant based on the new visibility, then `updatePanelBorders()`.
- `createPanel(Mode)` (new): build the `ChatPanel`, insert both layouts at
  `insertionIndex(mode)` via `addComponentAtIndex`, and start them hidden so the
  first `toggleMode` flips them visible. Runs as the `computeIfAbsent` mapping
  function, so the mode is not yet in `panels` — `insertionIndex` correctly
  counts already-created lower-ordinal modes.
- `insertionIndex(Mode)` (new): `panels.keySet()` stream filtered to lower
  ordinals, counted — the fixed-order DOM position.
- `applyBordersBetween(HorizontalLayout)`: single declarative pass;
  `visible.indexOf(child) > 0` selects divider panels (`-1` hidden, `0`
  leftmost visible). Intentional local `Stream.forEach` exception for the style
  side effect (per user direction; global rule stands elsewhere).
- Add `import com.vaadin.flow.component.Component;`.

**Existing `ChatViewTest` updates** (old destructive contract -> reuse contract):
`togglesOffExistingPanel` (panel retained, both areas hidden),
`maxOnePanelPerType` (second toggle reuses same instance, ends hidden),
`panelStatePreservesAfterToggle` (re-shown panel `isSameAs` original).

## Tasks 2–6 — test coverage

Added a package-private `ChatView.toggleButtons()` accessor (mirrors `panels()`)
so the button-variant test can assert theme state without deep tree traversal.

- **Task 2 (`reshownPanelKeepsFixedModeOrder`)**: toggle C on then B on; assert
  the shared parent's child index of each panel's `messageArea()` is ordered
  A < B < C, proving fixed `Mode` order regardless of toggle sequence.
- **Task 3 (`toggleButtonReflectsVisibility`)**: via `toggleButtons()`, assert
  the button `hasThemeName("primary")` when visible and not after toggling off.
- **Task 4 (`hiddenPanelCarriesNoBorder`)**: toggle B on (A,B visible), toggle B
  off, toggle C on (A,C visible); assert A has no inline-start border, C has the
  solid divider, and hidden B has neither inline-start nor inline-end border.
- **Task 5 (`historyPreservedAcrossToggle`)**: `fireSubmit` on NAIVE, assert
  MessageList items + "Retrieved Chunks (2)"; toggle off then on; assert same
  instance and history intact.
- **Task 6 (`streamingContinuesWhileHidden`)**: mock `chat(...)` with a
  caller-controlled `Multi.emitter`; submit, emit one token, hide, emit the rest
  + chunks + complete, show; assert full accumulated text and 2 chunks. Relies on
  `ui.access` applying synchronously in the browserless harness (as existing
  tests do); fallback to `isAttached()` if flaky.

## Results

- `./mvnw verify` — BUILD SUCCESS (exit 0). Surefire: 79 tests, 0 failures,
  0 errors, 9 skipped (gated `-Drun.simulations` / `-Drun.planted-questions`
  suites). No `*IT.java` classes exist in the project, so failsafe has nothing
  to run — unchanged by this spec.
- `streamingContinuesWhileHidden` passed on the real `ui.access` path; the
  `isAttached()` fallback was not needed.
- Manual dev-mode visual walkthrough (task 7) not performed in an interactive
  browser session; all eight requirements are covered by the automated tests
  above. Recommended for the demo author before the talk.

## Deviations from plan

None. Only addition beyond the plan: a package-private `ChatView.toggleButtons()`
accessor (mirroring `panels()`) to support the button-variant test.
