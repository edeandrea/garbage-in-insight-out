# Spec 002: UI Polish — Technical Plan

**Status:** Approved

## Architecture

### ChatPanel decomposition

ChatPanel stops extending `VerticalLayout` and becomes a plain
coordinator class (not a `Component`). It still owns all state and event
logic (`onSubmit`, `addChunks`, `highlightMessageForRound`), but exposes
two composite containers that `ChatView` places into the two halves of a
shared `SplitLayout`:

- `messageArea()` — `VerticalLayout` containing `MessageList` +
  `MessageInput`.
- `chunksArea()` — `VerticalLayout` containing a header `Span`
  ("Retrieved Chunks (N)") + `Grid<ChunkRow>`.

**Why keep ChatPanel as a single coordinator** rather than splitting into
two classes: `onSubmit` naturally coordinates both halves (appending to
the MessageList AND adding rows to the Grid). Splitting would require a
coupling mechanism (event bus, callbacks) between the two halves.
Keeping one coordinator avoids that complexity and keeps tests
straightforward — a single `ChatPanel` instance is the test subject.

### ChatView layout structure

```
ChatView (VerticalLayout, setSizeFull)
  ├── toolbar (HorizontalLayout) [sticky at top]
  └── splitLayout (SplitLayout, VERTICAL, 70/30)
        ├── primary: messageContainer (HorizontalLayout, setSizeFull)
        │     ├── panelA.messageArea()
        │     ├── panelB.messageArea()
        │     └── panelC.messageArea()
        └── secondary: chunksContainer (HorizontalLayout, setSizeFull)
              ├── panelA.chunksArea()
              ├── panelB.chunksArea()
              └── panelC.chunksArea()
```

## Files to modify

| File | Summary |
|------|---------|
| `ChatPanel.java` | Stop extending `VerticalLayout`. Expose `messageArea()`/`chunksArea()` getters. Remove `Details` wrapper → header `Span`. Remove `#` column. Add `setResizable(true)` to all columns. Add `setPartNameGenerator()` for row coloring. Remove `setAllRowsVisible(true)` so the grid scrolls within the allocated split area. |
| `ChatView.java` | Replace `panelContainer` with `SplitLayout` (vertical, 70/30). Build `messageContainer` and `chunksContainer` `HorizontalLayout`s as primary/secondary. Add sticky toolbar classes. Add `Whitespace.NOWRAP` to title. Add `updatePanelBorders()` helper. Update `toggleMode()` to add/remove from both containers. |
| `styles.css` | Add 7 `vaadin-grid::part()` rules for per-round row coloring. Add sticky toolbar background rule. Keep existing `.highlighted` rule. |
| `ChatViewTest.java` | Update for new layout structure. Add tests: SplitLayout orientation/position, sticky toolbar, non-wrapping title, panel borders. |
| `ChatPanelTest.java` | Update `find()` calls to use `panel.messageArea()`/`panel.chunksArea()` instead of `panel` directly. Add tests: row part names, resizable columns, no `#` column, chunks header label, no `Details` component. |

No new files created. `ChunkRow.java` and `AppConfig.java` unchanged.

## Approach per requirement

### Req 1: Chunk table row color coding

Use `Grid.setPartNameGenerator()` to assign a CSS `::part()` name per
row based on the round's effective Vaadin color index:

```java
grid.setPartNameGenerator(row -> {
    var colorIndex = row.round() % MAX_COLOR_INDEX;
    return "round-color-%d".formatted(colorIndex);
});
```

**Simplification:** Change `MAX_COLOR_INDEX` from `9` to `7` to match
the actual number of Vaadin user colors (`--vaadin-user-color-0` through
`--vaadin-user-color-6`). The old value of 9 introduced a confusing
double-modulo (`% 9 % 7`) since `setUserColorIndex()` internally maps
`n % 7`. With `MAX_COLOR_INDEX = 7`, a single `% 7` produces color
indices 0–6 directly, matching both the avatar and the grid row. This
changes the avatar color sequence for rounds 9+ (cosmetic only).

In `styles.css`, 7 rules using `color-mix()` at 15% opacity:

```css
vaadin-grid::part(round-color-0) {
    background-color: color-mix(in srgb, var(--vaadin-user-color-0) 15%, transparent);
}
/* ... through round-color-6 */
```

`color-mix()` is baseline 2023 — fine for a conference demo app.
The 15% opacity produces a subtle tint that works in both light and dark
themes.

**Why `setPartNameGenerator` over `setClassNameGenerator`:** Grid rows
render inside the `vaadin-grid` shadow DOM. `setClassNameGenerator` sets
classes on `<tr>` elements inside the shadow root, which are not
targetable from the theme stylesheet. `setPartNameGenerator` exposes
CSS `::part()` names accessible from the light DOM — the correct Vaadin
pattern for styling grid rows from a theme stylesheet.

### Req 2: Resizable grid columns

Chain `.setResizable(true)` on every `addColumn()` call in
`createChunksGrid()`.

### Req 3: Remove `#` column

Delete the `grid.addColumn(ChunkRow::round).setHeader("#")...` call.
The `round` field on `ChunkRow` is retained — it's still needed by the
part name generator and `highlightMessageForRound()`.

### Req 4: Non-wrapping title

Add `LumoUtility.Whitespace.NOWRAP` to the `H2` in `createToolbar()`.
This uses a LumoUtility constant consistent with
[spec 001 decision #54](../001-three-mode-rag-demo/decisions.md#54-2026-07-23-0954-edt-response-to-chunks-via-color-coding-not-click-handlers)'s
style note ("Use `LumoUtility` class constants instead of raw CSS
string literals").

### Req 5: Panel borders

Add a private `updatePanelBorders()` method in `ChatView`, called from
`toggleMode()` after every add/remove. When `panels.size() >= 2`, add
vertical-only border classes (`LumoUtility.Border.START`,
`LumoUtility.Border.END`, and `LumoUtility.BorderColor.CONTRAST_10`) to
every panel's `messageArea()` and `chunksArea()`. When `< 2`, remove
those classes. Vertical-only separators avoid clashing with the
horizontal splitter that already separates top from bottom. Both halves
need borders because message areas and chunk areas are in different
containers — each needs its own vertical boundary.

### Req 6: Shared resizable split

**ChatView changes:**

- Replace `panelContainer` (`HorizontalLayout`) with three fields:
  `splitLayout` (`SplitLayout`), `messageContainer`
  (`HorizontalLayout`), `chunksContainer` (`HorizontalLayout`).
- Constructor builds the split:

  ```java
  this.splitLayout = new SplitLayout(Orientation.VERTICAL);
  this.splitLayout.setSizeFull();
  this.splitLayout.addToPrimary(this.messageContainer);
  this.splitLayout.addToSecondary(this.chunksContainer);
  this.splitLayout.setSplitterPosition(70);
  ```

- `toggleMode()` adds/removes `panel.messageArea()` to/from
  `messageContainer` and `panel.chunksArea()` to/from
  `chunksContainer`.

**ChatPanel changes:**

- Constructor builds two internal `VerticalLayout` containers:
  - `messageArea`: `MessageList` (expanded) + `MessageInput` (full
    width). Full size, no padding, no spacing.
  - `chunksArea`: header `Span` (full width) + `Grid<ChunkRow>`
    (expanded). Full size, no padding, no spacing.
- Remove `Details chunksDetails` field and its import entirely.
- `addChunks()` updates the header span text instead of
  `Details.setSummaryText()`.
- Remove `setAllRowsVisible(true)` from the grid so it uses its own
  internal scrollbar within the constrained bottom half of the split.

### Req 7: Sticky toolbar

Add LumoUtility classes to the toolbar in `createToolbar()`:

```java
toolbar.addClassNames(
    "sticky-toolbar",
    Position.STICKY,
    Position.Top.NONE,
    ZIndex.SMALL,
    Border.BOTTOM,
    BorderColor.CONTRAST_10
);
```

All constants verified in `LumoUtility` (Vaadin 25.2.3):
- `Position.STICKY` → `"sticky"`
- `Position.Top.NONE` → `"top-0"`
- `ZIndex.SMALL` → `"z-20"` (available since 24.4.7)
- `Border.BOTTOM` → `"border-b"`
- `BorderColor.CONTRAST_10` → `"border-contrast-10"`

In `styles.css`, add a background rule so content doesn't bleed through
(Vaadin layouts are transparent by default):

```css
.sticky-toolbar {
    background-color: var(--lumo-base-color);
}
```

**Why `STICKY` over `FIXED`:** `position: sticky` keeps the toolbar in
normal flow until scrolled past, avoiding the need for manual padding.
Since `ChatView` is `setSizeFull()`, the toolbar is always at the top.

## CSS strategy (`styles.css`)

Final file will contain three groups:

1. **Existing** — `.highlighted` message rule (unchanged).
2. **Row coloring** — 7 `vaadin-grid::part(round-color-N)` rules using
   `color-mix(in srgb, var(--vaadin-user-color-N) 15%, transparent)`.
3. **Sticky toolbar** — `.sticky-toolbar` background color rule.

All borders and layout utilities use `LumoUtility` constants in Java —
no additional raw CSS needed for those.

## Implementation order

| Step | Req | Description |
|------|-----|-------------|
| 1 | 6 | Decompose ChatPanel, restructure ChatView with SplitLayout. Foundation for everything else. |
| 2 | 3 | Remove `#` column. |
| 3 | 2 | Make columns resizable. |
| 4 | 1 | Add part name generator + CSS rules for row coloring. |
| 5 | 4 | Non-wrapping title. |
| 6 | 5 | Panel borders with `updatePanelBorders()`. |
| 7 | 7 | Sticky toolbar. |
| 8 | — | Update tests for all requirements. |

## Test approach

Both test files continue to extend `QuarkusBrowserlessTest` with
`@QuarkusTest`. Assertions use AssertJ. The existing test patterns
(`navigate()`, `find()`, `test()`, `ComponentUtil.fireEvent()`) are
reused.

**ChatPanelTest updates:**
- `find()` calls change from `find(Component.class, panel)` to
  `find(Component.class, panel.messageArea())` or
  `find(Component.class, panel.chunksArea())`.
- `fireSubmit()` helper uses `panel.messageArea()` as the search root.
- New tests: `chunksGridHasNoRoundColumn`,
  `allColumnsAreResizable`, `chunkRowsGetPartNameByRound`,
  `chunksHeaderShowsCount`, `noDetailsComponentInChunksArea`.

**ChatViewTest updates:**
- Existing toggle tests remain valid (they use `view.panels()` and
  `view.toggleMode()`, which are unchanged).
- New tests: `layoutUsesSplitLayoutWithVerticalOrientation`,
  `splitLayoutDefaultPositionIs70`, `titleDoesNotWrap`,
  `singlePanelHasNoBorder`, `multiplePanelsHaveBorders`,
  `toolbarIsStickyAtTop`.

## Alternatives considered

| Alternative | Why rejected |
|-------------|-------------|
| Split ChatPanel into two new classes | Requires coupling mechanism between halves for `onSubmit` coordination. More complex, more files, no benefit. |
| Keep ChatPanel extending `VerticalLayout` | ChatPanel's children live in the SplitLayout halves, not in the panel itself. A Component with no children in the DOM is misleading. Plain class is cleaner. |
| `setClassNameGenerator` for row colors | Classes are inside Grid shadow DOM, not targetable from theme CSS. `setPartNameGenerator` is the correct API. |
| Inline styles via renderers for row colors | Heavy — requires wrapping every cell. `setPartNameGenerator` is purpose-built for this. |
| `position: fixed` for sticky toolbar | Removes element from flow, requires manual padding. `sticky` is simpler. |
| Per-panel SplitLayout | Rejected by [spec decision #3](decisions.md#3-2026-07-24-edt-splitter-orientation-and-scope). Shared splitter keeps comparison view aligned. |
