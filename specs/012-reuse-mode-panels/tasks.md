# Tasks 012 — Reuse mode panels & preserve history on toggle

Status: Approved

Ordered by dependency. Each task is independently verifiable and includes its
own test coverage (per project hygiene: no behavior change is done without a
passing test).

- [x] 1. **ChatView: create-once + hide/show reuse with fixed order and
  visible-only borders.** In `ChatView` (`src/main/java/dev/ericdeandrea/docling/ui/ChatView.java`):
  rewrite `toggleMode` to lazily create each panel once via
  `panels.computeIfAbsent(mode, this::createPanel)` and flip `setVisible(...)` on
  both layouts plus the button's `LUMO_PRIMARY` variant; add `createPanel(Mode)`
  (inserts both layouts at `insertionIndex(mode)`, starts them hidden) and
  `insertionIndex(Mode)` (count of already-created lower-ordinal modes); rewrite
  `applyBordersBetween` to the single-pass visible-only form from the plan.
  Update the three `ChatViewTest` cases that encode the old destructive contract
  so the suite stays green: `togglesOffExistingPanel` (panel retained but both
  areas `isVisible() == false`), `maxOnePanelPerType` (second toggle reuses the
  same instance and ends hidden), `panelStatePreservesAfterToggle` (re-shown
  panel `isSameAs` the original). Verify: existing `ChatViewTest` and
  `ChatPanelTest` all pass, including `singlePanelHasNoBorder` and
  `multiplePanelsHaveBordersBetweenOnly`. (Req 1, 3, 4, 6, 7)

- [x] 2. **Test: re-shown panel keeps fixed Mode order (Req 4).** Add
  `ChatViewTest.reshownPanelKeepsFixedModeOrder`: with A visible, toggle C on
  then B on, and assert the message container's visible children appear in
  `Mode` order A, B, C (map each child back to its panel, or assert visible
  children indices match `Mode.values()` order). Verify: test passes.

- [x] 3. **Test: toggle button reflects visibility (Req 5).** Add
  `ChatViewTest.toggleButtonReflectsVisibility`: assert a mode's button has the
  `LUMO_PRIMARY` theme variant when its panel is visible and lacks it after
  toggling off. Verify: test passes.

- [x] 4. **Test: hidden panel carries no border (Req 6).** Add
  `ChatViewTest.hiddenPanelCarriesNoBorder`: with A and B visible (B has the
  inline-start divider), toggle B off then C on, and assert only the leftmost
  visible panel lacks a divider, subsequent visible panels have one, and the
  hidden B panel has neither border. Verify: test passes.

- [x] 5. **Test: chat + chunk history preserved across toggle (Req 2).** Add
  `ChatPanelTest.historyPreservedAcrossToggle` using the existing
  `@InjectMock AssistantService` + `fireSubmit` pattern: submit a question on
  NAIVE, assert the `MessageList` items and 2 chunk rows / "Retrieved Chunks (2)";
  toggle NAIVE off then on; assert `panels().get(NAIVE)` is the same instance and
  the `MessageList` items and chunk count are still intact. Verify: test passes.

- [x] 6. **Test: streaming continues while hidden (Req 8).** Add
  `ChatPanelTest.streamingContinuesWhileHidden`: mock `chat(...)` to return a
  caller-controlled `Multi` (emitter/processor); `fireSubmit`, toggle NAIVE off,
  emit the remaining `TokenEvent`s + `CompletedEvent`, toggle NAIVE on, assert the
  full accumulated assistant text is present. If `ui.access` timing proves flaky
  in the browserless harness, fall back to asserting `messageArea().isAttached()`
  remains true after hide (the property Req 8 relies on) and record the fallback
  in `decisions.md`. Verify: test passes.

- [x] 7. **Full verification.** Run `./mvnw verify` (full suite incl. failsafe
  ITs — never skip ITs) and confirm green. Then manual dev-mode check per
  `plan.md`: converse in Modes A and B, toggle B off/on and confirm chat + chunk
  history and follow-up context survive; confirm fixed A-B-C ordering regardless
  of toggle sequence, dividers only between visible panels, no phantom spacing
  from hidden panels, and mid-stream toggle-off/on shows the completed answer.
