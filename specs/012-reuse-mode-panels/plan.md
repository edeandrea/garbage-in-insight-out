# Plan 012 — Reuse mode panels & preserve history on toggle

Status: Approved

## Approach

Switch `ChatView` from a destructive add/remove model to a **create-once,
hide/show** model. Each mode's `ChatPanel` is created lazily on its first
toggle-on, its two layouts are inserted into the containers at the position
dictated by `Mode` declaration order, and it is retained for the lifetime of the
view. Toggling thereafter flips `setVisible(...)` on the panel's two layouts and
the button's `LUMO_PRIMARY` variant — the `ChatPanel` object (and therefore all
its history: `items`, `allChunkRows`, `roundToAssistantItem`, `currentRound`,
`conversationId`) is never discarded.

Because the layouts stay attached (only hidden), an in-flight streamed response
keeps landing on the panel via `ui.access` and is already present when the mode
is shown again (Req 8).

`ChatPanel` itself needs **no changes** — all state already lives in instance
fields, so reuse alone preserves history.

### Reading "is this mode visible"

The shown/hidden state is inherently mirrored across two components — the toggle
button's `LUMO_PRIMARY` variant and the two layouts — since both are set by
`toggleMode`. We read it back from `panel.messageArea().isVisible()` rather than
from the button, because layout visibility is the exact property the border logic
already filters on (`Component::isVisible`), is semantic rather than stylistic, and
adds no extra state (`setVisible(...)` is called anyway). Reading the button
variant instead would infer logical state from a Lumo styling token. See
`decisions.md` #4. The `panels` `EnumMap` becomes the cache of *created* panels;
`panels()` keeps its signature but shifts meaning from "active" to "created".

## Files to change

### `src/main/java/dev/ericdeandrea/docling/ui/ChatView.java`

- **`toggleMode(Mode)`** — rewrite. Lazily create the panel (once), flip
  visibility on both layouts and the button variant, then recompute borders:

  ```java
  void toggleMode(Mode mode) {
      var panel = this.panels.computeIfAbsent(mode, this::createPanel);
      var nowVisible = !panel.messageArea().isVisible();

      panel.messageArea().setVisible(nowVisible);
      panel.chunksArea().setVisible(nowVisible);

      var button = this.toggleButtons.get(mode);
      if (nowVisible) {
          button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      }
      else {
          button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
      }

      updatePanelBorders();
  }
  ```

- **`createPanel(Mode)`** — new private method. Builds the panel, inserts its two
  layouts at the fixed-order index, and starts them hidden so `toggleMode` flips
  them to visible:

  ```java
  private ChatPanel createPanel(Mode mode) {
      var panel = new ChatPanel(mode, this.assistantService);
      var index = insertionIndex(mode);

      this.messageContainer.addComponentAtIndex(index, panel.messageArea());
      this.chunksContainer.addComponentAtIndex(index, panel.chunksArea());

      panel.messageArea().setVisible(false);
      panel.chunksArea().setVisible(false);

      return panel;
  }

  private int insertionIndex(Mode mode) {
      return (int) this.panels.keySet().stream()
          .filter(m -> m.ordinal() < mode.ordinal())
          .count();
  }
  ```

  Note: `computeIfAbsent` inserts into `panels` *after* `createPanel` returns, so
  during `insertionIndex` the new mode is not yet in the map — the count of
  already-created lower-ordinal modes is the correct DOM index. Containers thus
  always hold panels in full `Mode` order (Req 4); the visible subset preserves
  that order automatically.

- **`applyBordersBetween(HorizontalLayout)`** — compute dividers over
  *visible-only* children, and clear any stale border on hidden ones (Req 6):

  ```java
  private void applyBordersBetween(HorizontalLayout container) {
      var visible = container.getChildren()
          .filter(Component::isVisible)
          .toList();

      container.getChildren().forEach(child -> {
          var style = child.getStyle();
          style.remove("border-inline-end");

          if (visible.indexOf(child) > 0) {
              style.set("border-inline-start", "1px solid var(--lumo-contrast-30pct)");
          }
          else {
              style.remove("border-inline-start");
          }
      });
  }
  ```

  A single declarative pass: `visible.indexOf(child)` is `-1` for hidden children
  and `0` for the leftmost visible one, so `> 0` selects exactly the panels that
  get a divider. This method intentionally uses `Stream.forEach` for the style
  side effect as a local readability exception to the global "avoid forEach for
  side effects" guideline (per user direction; the global rule stands elsewhere).

- `panels()` accessor stays but its meaning shifts from "active modes" to
  "created panels". No signature change.
- Constructor's `toggleMode(Mode.NAIVE)` call is unchanged — with the new logic
  it creates the NAIVE panel and shows it, leaving B/C uncreated (Req 3, Req 7).

No other production files change. `ChatPanel`, `Mode`, `AssistantService`,
`ChatService` are untouched.

## Tests

### `src/test/java/dev/ericdeandrea/docling/ui/ChatViewTest.java`

Rewrite the three that encode the old destructive contract:

- **`togglesOffExistingPanel`** — after toggling NAIVE off, the panel is retained
  in `panels()` but hidden. Assert `panels().get(NAIVE)` non-null and
  `messageArea().isVisible()` / `chunksArea().isVisible()` are `false`.
- **`maxOnePanelPerType`** — rename intent to "toggling twice reuses one
  instance and ends hidden". Capture the instance after the first toggle, toggle
  again, assert same instance (`isSameAs`) and not visible.
- **`panelStatePreservesAfterToggle`** — invert: assert the re-shown panel
  `isSameAs` the original instance.

Add:

- **`reshownPanelKeepsFixedModeOrder`** — with A visible, toggle C on, toggle B
  on; assert the message container's children order is A, B, C by inspecting each
  child's owning panel (or assert visible children indices match `Mode` order).
- **`toggleButtonReflectsVisibility`** (Req 5) — assert the button has/doesn't
  have `LUMO_PRIMARY` after show/hide.
- **`hiddenPanelCarriesNoBorder`** (Req 6) — with A and B visible (B has left
  border), hide B, then show C; assert only the leftmost visible panel lacks a
  border and hidden B has none.

### `src/test/java/dev/ericdeandrea/docling/ui/ChatPanelTest.java`

Add, reusing the existing `@InjectMock AssistantService` + `fireSubmit` pattern:

- **`historyPreservedAcrossToggle`** (Req 2) — `fireSubmit` a question on NAIVE,
  assert MessageList has the items and the chunks grid has 2 rows; toggle NAIVE
  off then on; assert the same panel instance, MessageList items intact, and
  "Retrieved Chunks (2)" still shown.
- **`streamingContinuesWhileHidden`** (Req 8) — mock `chat(...)` to return a
  caller-controlled `Multi` (emitter/processor). `fireSubmit`, toggle NAIVE off,
  emit remaining `TokenEvent`s + `CompletedEvent`, toggle NAIVE on, assert the
  full accumulated text is present. If `ui.access` timing makes this flaky in the
  browserless harness, fall back to asserting the panel's `messageArea()` remains
  attached (`isAttached()`) after hide, which is the property Req 8 depends on.

## Tradeoffs / alternatives considered

- **`setVisible` (chosen) vs. detach-and-cache.** Detaching (remove from
  container, keep object in a cache map, re-add on show) also preserves field
  state, but a detached component drops out of the UI tree, so live `ui.access`
  updates and `executeJs` scroll calls during streaming would not render —
  breaking Req 8. `setVisible(false)` keeps the panel attached, so streaming
  keeps flowing. It also makes fixed order trivial (children never move).
- **Derive visibility from the component (chosen) vs. a separate
  `EnumSet<Mode> visibleModes`.** A separate set is a second source of truth that
  can drift from `setVisible`. Reading `isVisible()` keeps one source.
- **`panels()` semantics.** Left as-is (now "created panels") rather than
  introducing a parallel `visibleModes()` accessor, to keep the surface small;
  tests read visibility off the layouts.

## Verification

1. `./mvnw verify` — full suite incl. failsafe ITs (never skip ITs).
2. Manual (`quarkus_start` / dev mode): with a real conversation in Mode A and
   Mode B, ask questions in each; toggle B off then on — its chat + chunk table
   return intact and the LLM answers a follow-up with prior context. Confirm B
   returns to its A-B-C slot regardless of toggle order, dividers sit only
   between visible panels, and no phantom spacing from hidden panels.
3. Start a long answer in a mode, toggle it off mid-stream, toggle back on —
   the completed answer and chunks are present (Req 8).
