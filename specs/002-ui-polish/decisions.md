# Spec 002: UI Polish — Decisions

## 1. [2026-07-24 12:15 EDT]: Chunk table color coding approach

**Question:** Should each conversation round get its own distinct color
in the chunks table (multi-color palette), or should only the
currently-selected round be highlighted?

**Options considered:**
1. Distinct color per round — each round (1, 2, 3…) gets a different
   background color in the grid rows, matching the assistant message
   avatar color.
2. Highlight selected only — only the currently-clicked round is
   highlighted in both the message list and chunks table.

**Decision:** Distinct color per round. The avatar colors already exist
via `setUserColorIndex()` using Vaadin's `--vaadin-user-color-{0..6}`
palette. Grid rows must use the same per-round color as a passive
always-visible indicator. The existing click-to-highlight interaction
coexists with this. This completes the intent of
[spec 001 decision #54](../001-three-mode-rag-demo/decisions.md#54-2026-07-23-0954-edt-response-to-chunks-via-color-coding-not-click-handlers).

---

## 2. [2026-07-24 12:15 EDT]: Three-panel arrangement

**Question:** When all three modes are active simultaneously, how should
the panels be arranged?

**Options considered:**
1. Three side by side (~33% width each).
2. Two-and-one stacked layout.
3. Side-by-side with horizontal scroll for the third.

**Decision:** Three side by side, each getting ~33% width.

---

## 3. [2026-07-24 12:15 EDT]: Splitter orientation and scope

**Question:** Should the resizable splitter between the message list and
chunks section be per-panel (each panel has its own independent
splitter), or a single shared splitter across all panels?

**Options considered:**
1. Per-panel vertical splitter — each ChatPanel gets its own independent
   draggable divider.
2. Shared vertical splitter — one divider controls the split for all
   panels simultaneously, keeping message lists and chunk tables aligned
   across panels.

**Decision:** Shared vertical splitter. All panels' message lists sit
side by side above the divider; all panels' chunk tables sit side by side
below it. This keeps the comparison view aligned.

---

## 4. [2026-07-24 12:20 EDT]: Default split ratio

**Question:** What should the default vertical split ratio be between the
message list area and the chunks table area?

**Options considered:**
1. 75/25 — messages very dominant.
2. 70/30 — messages dominant.
3. 60/40 — balanced.

**Decision:** 70/30. Messages get 70% of the vertical space, chunks get
30%.

---

## 5. [2026-07-24 12:20 EDT]: Click-to-highlight interaction with per-round coloring

**Question:** With per-round color coding always visible on grid rows,
should the existing click-to-highlight interaction (clicking a chunk row
highlights the corresponding assistant message) remain?

**Options considered:**
1. Keep both — per-round colors are always visible AND clicking a row
   still highlights the matching message.
2. Remove click-to-highlight — per-round colors are sufficient visual
   linkage.

**Decision:** Keep both. The click-to-highlight interaction remains
unchanged and coexists with the passive per-round row coloring.

---

## 6. [2026-07-24 12:30 EDT]: MessageInput placement in shared split layout

**Question:** With the shared split layout separating message lists from
chunk tables, where should each panel's `MessageInput` go?

**Options considered:**
1. Below its `MessageList` in the top half of the split.
2. Below the chunks grid in the bottom half.
3. A shared row of inputs at the splitter boundary.

**Decision:** Below its `MessageList` in the top half. Each panel's input
stays with its message list, above the shared splitter.

---

## 7. [2026-07-24 12:30 EDT]: Details collapsible wrapper for chunks grid

**Question:** The chunks grid is currently inside a collapsible `Details`
component ("Retrieved Chunks (8)"). With the shared resizable splitter,
should the `Details` wrapper remain?

**Options considered:**
1. Keep `Details` collapsible — each panel's chunks section stays in a
   collapsible wrapper inside the bottom half.
2. Remove `Details`, show grid directly — the splitter replaces the need
   for collapse; show the grid directly with a header/label.

**Decision:** Remove `Details`, show the grid directly. A "Retrieved
Chunks (N)" label/header appears above each panel's grid to preserve
the count display.

---

## 8. [2026-07-24 12:35 EDT]: Reason for removing `#` column

**Question:** Why is the round-number (`#`) column being removed from the
chunks grid?

**Decision:** The round is conveyed by the row color coding (requirement
1), making the `#` column redundant. Confirmed by the user.

---

## 9. [2026-07-24 13:05 EDT]: ChatPanel decomposition — plain class vs. Component

**Question:** With the shared SplitLayout, ChatPanel's children
(`messageArea` and `chunksArea`) live in different halves of the split,
not inside ChatPanel itself. Should ChatPanel remain a Vaadin
`VerticalLayout` (Component) or become a plain coordinator class?

**Options considered:**
1. Plain class (not a Component) — cleaner, since ChatPanel is a
   coordinator, not a layout. Tests use `panel.messageArea()` /
   `panel.chunksArea()` as search roots.
2. Keep extending `VerticalLayout` — less churn in tests, but a
   Component with no children in the DOM is misleading.

**Decision:** Plain class. ChatPanel becomes a coordinator that owns the
state/event logic and exposes two `VerticalLayout` getters. It is not
added to the DOM tree.

---

## 10. [2026-07-24 13:05 EDT]: Simplify MAX_COLOR_INDEX from 9 to 7

**Question:** `MAX_COLOR_INDEX` is currently 9, but Vaadin only defines 7
user colors (0–6). `setUserColorIndex(n)` internally maps `n % 7`. This
means the part name generator for grid rows would need a confusing
double-modulo (`% 9 % 7`). Should we simplify `MAX_COLOR_INDEX` to 7?

**Options considered:**
1. Simplify to 7 — cleaner code, one modulo. Slightly changes the
   avatar color sequence for rounds 9+ (purely cosmetic).
2. Keep 9 with double-modulo — preserves existing color sequence
   exactly, but more complex code.

**Decision:** Simplify to 7. A single `% 7` directly produces valid
color indices for both the avatar and the grid row, with no
double-modulo.

---

## 11. [2026-07-24 13:05 EDT]: Panel border style — full box vs. vertical separators

**Question:** Should panel borders be full box borders (`Border.ALL`,
all 4 sides) or vertical-only separators (left/right borders)?

**Options considered:**
1. Full box borders — each panel area is a complete bordered card. But
   the top/bottom borders would butt up against the SplitLayout
   splitter, looking cluttered.
2. Vertical separators only — `Border.START` + `Border.END` between
   adjacent panels. Cleaner with the horizontal splitter already
   separating top from bottom.

**Decision:** Vertical separators only. Avoids visual clutter at the
splitter boundary.

---

## 12. [2026-07-24 13:05 EDT]: Grid scrolling behavior in constrained SplitLayout

**Question:** The grid currently uses `setAllRowsVisible(true)` to
expand and show all rows without scrolling. In the new layout, the grid
is constrained to the bottom 30% of a SplitLayout. Should we keep this
behavior (letting the container scroll) or remove it (letting the grid
use its own internal scrollbar)?

**Options considered:**
1. Remove `setAllRowsVisible(true)` — grid scrolls internally within
   its allocated space. Standard behavior for constrained containers.
2. Keep it — grid expands to show all rows, the `chunksArea`
   `VerticalLayout` scrolls instead.

**Decision:** Remove it. The grid uses its own internal scrollbar within
the constrained bottom half of the SplitLayout.

---

## 13. [2026-07-24 13:17 EDT]: Chunks header label test location

**Question:** The "Retrieved Chunks (N)" header label test was listed
under `ChatViewTest` (task 10), but the plan placed it in
`ChatPanelTest` as `chunksHeaderShowsCount`. Which test class should own
this test?

**Options considered:**
1. `ChatPanelTest` — already mocks `AssistantService` and fires submits,
   making it the natural home alongside other ChatPanel behavior tests.
2. `ChatViewTest` — test it as part of the SplitLayout integration tests.

**Decision:** `ChatPanelTest`. The header label is ChatPanel behavior,
not layout structure.

---

## 14. [2026-07-24 14:12 EDT]: Two-row toolbar with smaller title font

**Question:** The title "Garbage In, Insight Out" still wraps despite
`Whitespace.NOWRAP` and `flex-shrink: 0` when all three mode buttons are
visible. How should the toolbar be restructured?

**Decision:** Restructure the toolbar into two centered rows:
- Row 1: title (centered, smaller font `--lumo-font-size-xl`) + theme
  toggle (right-justified).
- Row 2: mode buttons (centered).

The toolbar changes from a single `HorizontalLayout` to a
`VerticalLayout` containing two `HorizontalLayout` rows. This is a
refinement of reqs 4 (non-wrapping title) and 7 (sticky toolbar).

---

## 15. [2026-07-24 15:03 EDT]: Defer metadata label/type investigation to separate spec

**Question:** During implementation, we discovered that `elementLabel` is
always null for Mode B (hardcoded `null` in
`DoclingExtractor.toProvenanceEntry()`) and `elementType` is always null
for Mode C (hybrid chunker API doesn't expose it). Should we fix this in
spec 002 or defer?

**Decision:** Defer to a separate spec. The fix involves the ingestion
pipeline (DoclingExtractor, provenance model, possibly DoclingServeApi
options), which is outside the scope of UI polish. The investigation
found that `TableItem.getCaptions()` in the Docling document model
contains usable labels (e.g., "Table 1", "Table 2") that could populate
`elementLabel` for Mode B, but resolving `$ref` caption references
requires cross-referencing against `doc.getTexts()`. A new spec should
cover this along with investigating `DoclingServeApi` options.
