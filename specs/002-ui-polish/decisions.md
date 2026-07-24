# Spec 002: UI Polish — Decisions

## 1. [2026-07-24 EDT]: Chunk table color coding approach

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

## 2. [2026-07-24 EDT]: Three-panel arrangement

**Question:** When all three modes are active simultaneously, how should
the panels be arranged?

**Options considered:**
1. Three side by side (~33% width each).
2. Two-and-one stacked layout.
3. Side-by-side with horizontal scroll for the third.

**Decision:** Three side by side, each getting ~33% width.

---

## 3. [2026-07-24 EDT]: Splitter orientation and scope

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

## 4. [2026-07-24 EDT]: Default split ratio

**Question:** What should the default vertical split ratio be between the
message list area and the chunks table area?

**Options considered:**
1. 75/25 — messages very dominant.
2. 70/30 — messages dominant.
3. 60/40 — balanced.

**Decision:** 70/30. Messages get 70% of the vertical space, chunks get
30%.

---

## 5. [2026-07-24 EDT]: Click-to-highlight interaction with per-round coloring

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

## 6. [2026-07-24 EDT]: MessageInput placement in shared split layout

**Question:** With the shared split layout separating message lists from
chunk tables, where should each panel's `MessageInput` go?

**Options considered:**
1. Below its `MessageList` in the top half of the split.
2. Below the chunks grid in the bottom half.
3. A shared row of inputs at the splitter boundary.

**Decision:** Below its `MessageList` in the top half. Each panel's input
stays with its message list, above the shared splitter.

---

## 7. [2026-07-24 EDT]: Details collapsible wrapper for chunks grid

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

## 8. [2026-07-24 EDT]: Reason for removing `#` column

**Question:** Why is the round-number (`#`) column being removed from the
chunks grid?

**Decision:** The round is conveyed by the row color coding (requirement
1), making the `#` column redundant. Confirmed by the user.
