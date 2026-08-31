# Decisions — 012 Reuse mode panels

## 1. [2026-08-31 15:27 EDT]: Panel placement when a mode is re-shown

**Question:** When a hidden mode is toggled back on, where does its panel appear
among the visible panels?

**Options:** (a) fixed `Mode` order (A, B, C) regardless of toggle sequence;
(b) append to the end of the currently-visible panels.

**Decision:** Fixed `Mode` order (spec Req 4). A re-shown panel returns to its
A/B/C slot. Chosen for a stable, predictable layout during the live demo.

## 2. [2026-08-31 15:27 EDT]: Behavior when a mode is hidden mid-stream

**Question:** If a mode is toggled off while its LLM response is still streaming,
what happens?

**Options:** (a) keep streaming to the hidden panel and show the completed result
on return; (b) leave it out of scope (assume modes toggled only while idle).

**Decision:** Keep streaming, show on return (spec Req 8). Most faithful to the
"preserve history" goal and drives the reuse mechanism choice below.

## 3. [2026-08-31 15:27 EDT]: Reuse mechanism — setVisible vs. detach-and-cache

**Question:** How to reuse a panel across toggles?

**Options:** (a) `setVisible(false/true)` on the panel's two layouts, leaving them
attached; (b) detach (remove from container) into a cache map and re-add on show.

**Decision:** `setVisible`. A detached component leaves the UI tree, so live
`ui.access` streaming updates and `executeJs` scroll calls would not render,
breaking Req 8. `setVisible(false)` keeps the panel attached so streaming keeps
flowing, and makes fixed ordering trivial since children never move.

## 4. [2026-08-31 15:35 EDT]: Source of truth for a mode's visibility

**Question:** How do we answer "is this mode currently shown"?

**Options:** (a) read `panel.messageArea().isVisible()`; (b) read the toggle
button's `LUMO_PRIMARY` theme variant (`button.hasThemeName("primary")`);
(c) maintain a separate `EnumSet<Mode> visibleModes` field as the single logical
source, driving both the button variant and `setVisible` from it.

**Decision:** Read `panel.messageArea().isVisible()` directly (option a).

The shown/hidden state is inherently mirrored across two components — the button
variant and the two layouts — since both are outputs of `toggleMode`. The real
choice is which mirror to read. Layout visibility wins because:

- it is the exact property `applyBordersBetween` already filters on
  (`Component::isVisible`), so no translation from a styling token to logical
  state is needed;
- it is semantic ("panel is shown") rather than stylistic ("button is styled
  primary"), so restyling the active button (a different variant, a CSS class,
  an icon) cannot silently break the visibility query;
- it adds no state — `setVisible(...)` must be called anyway, so reading it back
  is free.

Option (b) was considered and rejected: it infers logical state from a
presentation choice and is stringly-typed against a Lumo variant name. Option
(c) is the clean way to have one explicit logical source but adds a field that
must be kept in sync with the components. The `panels` `EnumMap` is repurposed as
the cache of *created* panels; the `panels()` accessor keeps its signature but its
meaning shifts from "active" to "created".
