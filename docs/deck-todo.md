# Deck To-Do — Garbage In, Insight Out (v29)

Running punch-list. Nothing here gets applied until Eric says go. Edits
are batched, then the file is rendered/verified and re-opened+resaved in
PowerPoint to normalize (see [[garbage-in-insight-out-talk-deck]] caveat
about python-pptx re-editing).

Status key: `[ ]` open · `[~]` triaged/verified real · `[x]` done · `[-]` dropped

## Open (from talk plan)

- [ ] **Title slide** — plain-text placeholder; Eric swaps in the Gemini
  illustration himself. (Eric owns; leave as-is until then.)
- [ ] **Act 2 verdict slide — Mode B / Q3 wording is thin.** Needs either
  a screenshot (matching how Q3/Mode A was sourced) or a clearly-labeled
  paraphrase. Decide which.
- [ ] **Tika code slide** — a few lines wrap awkwardly at current font
  size. Readable but not pretty. Minor.
- [ ] **Rehearse Act 3 live calls** end-to-end at least once (six calls
  clustered back-to-back). Eric owns; not a file edit.

## Captured this session (narrated by Eric)

- [~] **Code-slide attribution → package line in snippet, drop caption.**
  Decided. On all 5 code slides (15 `TikaExtractor`, 16 `NaiveChunker`,
  17 `ExtendedContentRetrievalAugmentorBuilder`, 26 `DoclingNaiveExtractor`,
  33 `DoclingHybridExtractor`):
  - Add the `package …;` line as the **first line of the code block**,
    styled in the **same dim gray as the comments** (de-emphasized so the
    logic stays the hero). Fits on one line — 57 chars in a 9.02in /
    10.5pt Courier block, no wrap risk. Packages:
    - 15/26/33 → `dev.ericdeandrea.docling.ai.ingestion.extraction`
    - 16 → `dev.ericdeandrea.docling.ai.ingestion.chunking`
    - 17 → `dev.ericdeandrea.docling.ai.rag`
  - **Remove the `FileAttribution` caption shape** (now redundant:
    package + class name = fully-qualified location; imports still omitted).
  - **Docs to update:** the code-slide mechanic note in `talk-plan.md`
    (bundle copy) and the demo repo's `docs/talk_plan_dev2next_2026.md` —
    change "omit package declarations and imports" → "package line kept
    (locates the class), imports omitted (noise that wraps)."

- [~] **Slide 17 code fixes** (`ExtendedContentRetrievalAugmentorBuilder`).
  Two verified bugs vs the real source + the deck's own highlighting
  convention (blue=keywords/modifiers, green=LangChain4j types,
  757575=comments):
  - **Highlighting:** line 0 `class` is dark-gray but should be blue
    (every other code slide colors `class` blue). Also restore the real
    `final` modifier → `final class …` (`final class` blue).
  - **Method declaration:** currently `RetrievalAugmentor build(…) {` —
    `static` missing + params elided to `(…)` (slide 17 is the only code
    slide using the elision; 26/33 show full sigs). Fix: restore `static`
    (blue) and show the **full real params** wrapped to 2 lines:
    `static RetrievalAugmentor build(EmbeddingStore<TextSegment> embeddingStore,`
    `                                EmbeddingModel embeddingModel, DemoConfig config) {`
    Highlight `EmbeddingStore`/`TextSegment`/`EmbeddingModel` green
    (LangChain4j), `DemoConfig` gray (app type), param names gray.
  - **Fit:** code block bottom (4.59in) nearly touches the Caption
    (4.70in); adding the package line + 2 signature lines overflows at
    10.5pt. Drop this slide's code font to **9.5pt** (already used on
    26/33) and/or tighten line spacing. **Verify at full resolution
    after** (per the deck's own code-slide QA lesson).

- [~] **Slide 22 ("The Java Champion problem") — logo as hero.** Decision:
  "Logo as hero, drop title text." The docling-java logo (`image30.png`,
  duck+Duke illustration, 2048x2048, no wordmark) is currently a tiny
  0.52in icon bottom-left. Make it the visual anchor:
  - **Enlarge `DoclingJavaLogo`** to a large square on the right side,
    spanning roughly the body/card vertical zone (~2.5-3in sq, keep 1:1
    aspect). High-res source scales cleanly.
  - **Drop the standalone "Docling Java" title** from `DoclingJavaCard`
    (redundant: URL spells the name + it's said aloud). **Keep the
    credibility line** verbatim: "The official Java integration, one of
    the project's own listed key repositories, not a scrappy side fork."
    Reflow the card to just that line, left of the logo.
  - **Keep** the Title, the Body quote, and the `DoclingJavaURL`
    (`docling-project.github.io/docling-java`) — URL now carries the name.
  - **QR (`DoclingJavaQR`, image31):** not in the chosen mockup; keep it
    but reposition near the URL/logo (confirm at verify — drop if it
    clutters the hero layout).
  - Layout change → **verify visually (full-res render) after.**

- [~] **Slide 26 (`DoclingNaiveExtractor`) — trim the flatten comment.**
  Decision: drop the chunker cross-reference, keep the *why*. Slide 23
  (Card2: "The exact same sentence splitter as Mode A. Unchanged. On
  purpose.") already owns the "chunker didn't change" point ~3 slides
  back, so `for the SAME NaiveChunker` is redundant here. The flatten +
  provenance motivation is *not* redundant (it's the "Naive" in the class
  name + Act 3 setup), so keep it. Change the comment:
  - from `// Flatten to text for the SAME NaiveChunker — but keep provenance.`
  - to   `// Flatten Docling's structure to plain text, but keep provenance.`
    (comma, not em-dash — see [[avoid-em-dashes]])
  - **Doc reconcile:** this softens the talk-plan mechanic note
    (`talk-plan.md:36-39`, bundle copy) that deliberately wanted the
    chunker called out *on this code slide*. Update that note to say the
    "chunker unchanged" point is carried by slide 23 (design) + the spoken
    track, and the code comment now only explains the flatten/provenance
    intent. (Same doc also edited by the code-slide attribution item —
    batch both edits together.)

- [~] **Slide titles must not end in a period.** Seven headers do. Strip
  the trailing period on all; two two-clause ones also get their internal
  period replaced with a comma (no em-dashes, per [[avoid-em-dashes]]).
  Edit only the `Heading` shape text, preserve existing run styling:
  - 17: `Retrieval: the boring part. On purpose.` → `Retrieval: the boring part, on purpose`
  - 25: `Live: "Who wrote the paper?" again.` → `Live: "Who wrote the paper?" again`
  - 30: `The easy questions tie too.` → `The easy questions tie too`
  - 31: `Same answer. Wildly different evidence.` → `Same answer, wildly different evidence`
  - 32: `Only Mode C can answer.` → `Only Mode C can answer`
  - 33: `Mode C: extract and chunk, one call.` → `Mode C: extract and chunk, one call`
  - 36: `Five questions, three modes, all real.` → `Five questions, three modes, all real`
  - (25/29 use curly quotes around the question; keep them. 31/17 collapse
    two runs into one clause — keep the first run's styling.)

- [~] **Em-dash sweep (deck-wide).** Replace every em-dash in shipping
  content per [[avoid-em-dashes]]. Not a blind find/replace: use the
  right punctuation per context (colon for card `label: detail`, parens
  for the list aside, comma/period otherwise). Preserve run styling.
  Slide 26's comment is already covered by its own item above.
  **Slide content (11):**
  - S9 DocText: `…Staar — IBM Research, KDD 2022` → `…Staar (IBM Research, KDD 2022)`
  - S9 DocText: `…figures, footnotes — everything our abstract complains about.` → `…footnotes: everything our abstract complains about.`
  - S9 DocText: `Not cherry-picked — this is the same PDF…` → `Not cherry-picked: this is the same PDF…`
  - S11 Card1: `Apache Tika — plain text, no layout awareness` → `Apache Tika: plain text, no layout awareness`
  - S11 Card2: `Sentence splitter — fixed rules, no idea what a table is` → `Sentence splitter: fixed rules, no idea what a table is`
  - S16 CodeBlock: `// attachMetadata() tries to attach provenance — there isn't any here.` → `// attachMetadata() tries to attach provenance, but there isn't any here.`
  - S23 Card1: `Docling instead of Tika — structured extraction, layout & table aware.` → `Docling instead of Tika: structured extraction, layout & table aware.`
  - S23 Caption: `Everything downstream of extraction — embedding model, vector store, retrieval, prompt, LLM — stays identical to Mode A.` → `Everything downstream of extraction (embedding model, vector store, retrieval, prompt, LLM) stays identical to Mode A.`
  - S24 Card1: same as S23 Card1
  - S24 Caption: same as S23 Caption
  - S33 Caption: `Simplified for the slide — real method inlines this logic rather than calling named helpers. Same behavior.` → `Simplified for the slide: the real method inlines this logic rather than calling named helpers. Same behavior.`

  **Speaker notes (2):**
  - S11 notes & S24 notes: `🔴 LIVE DEMO NEXT — switch to the app now.` → `🔴 LIVE DEMO NEXT: switch to the app now.`

- [~] **Act 3 live-demo cues + LIVE badge.** Interleave by narrative beat
  (not per-query), with Q5 isolated for max drama → 4 app round-trips:
  Q3 / Q1+Q2 / Q4 / Q5. Cues follow the deck's existing 🔴/📌 mechanic
  (talk-plan.md), colon not em-dash (per [[avoid-em-dashes]], matches the
  notes fixes on slides 11/24). **Append** these to each slide's existing
  speaker notes (don't replace what's there):

  - **Slide 28** (design), append:
    `🔴 LIVE DEMO NEXT: switch to the app now. Ask Q3 ("Who wrote the paper?") in Mode C. A and B already answered this in Acts 1 & 2 — leave their panels as captured; only C is new. This is the callback, let it breathe.`
  - **Slide 29** (Q3), append:
    `📌 FALLBACK for the Q3 live call above. Worked live: breeze through, confirm C's page-1 ACM Reference answer against B's verbosity. Didn't: present these captured panels as primary content.`
    `🔴 LIVE DEMO NEXT: back to the app. Ask Q1 ("how many annotators, how long") then Q2 ("what % are Patents") across A+B+C. Both tie — keep it fast (~30-45s), this is the "not cherry-picking" beat, not suspense.`
  - **Slide 30** (Q1/Q2), append:
    `📌 FALLBACK for the Q1 + Q2 live calls above. Worked live: confirm the ties fast. Didn't: present both rows as captured.`
    `🔴 LIVE DEMO NEXT: back to the app. Ask Q4 (Table 2, which architecture won) across A+B+C. Same answer, escalating evidence.`
  - **Slide 31** (Q4), append:
    `📌 FALLBACK for the Q4 live call above. Worked live: let the audience notice Mode C's actual numbers before you point them out. Didn't: present these panels as captured.`
    `🔴 LIVE DEMO NEXT: back to the app, one last time. Ask Q5 (the knockout — mAP gap, Caption row: YOLOv5x6 77.7 vs FRCNN R101 70.1) across A+B+C. Isolated on purpose, this is the closer.`
  - **Slide 32** (Q5), append:
    `📌 FALLBACK for the Q5 live call above. The knockout. Worked live: let A and B's "can't answer" sit for a beat, then C computing 7.6 points. Didn't: deliver these panels fresh as primary content. Close the app after — this is the last live call.`
  - (Note: one em-dash appears inside the S31 cue "the knockout — mAP gap".
    Use a colon there too: "the knockout: mAP gap". No em-dashes anywhere.)

  **LIVE badge (audience-facing tell):** add a small, consistent "LIVE"
  badge to the four live-result slides (29, 30, 31, 32) — NOT the design
  slide 28. Same shape/size/position on all four (top-right, clear of the
  Heading). A red indicator dot + `LIVE` text reads as a broadcast-style
  live marker. **Pick a color that doesn't collide with the Mode A/B/C
  palette** (verify against the panel label colors). Match deck label
  styling; confirm size/placement at full-res. Rationale: Acts 1 & 2 carry
  "live" in the slide *title*; Act 3's titles are thematic (kept, per the
  earlier decision), so the badge is Act 3's equivalent live signal.
  - *Optional follow-up (not in this batch):* for full visual uniformity,
    the same badge could be added to Acts 1/2's live slides (12/13/14/25)
    too — flag to Eric later, don't do preemptively.

- [~] **Slide 34 ("Not just a Java thing") — three fixes.**
  1. **Icon centering.** Text boxes are correctly centered under each
     card; the three icons are each ~0.098in left of card center (uniform
     systematic offset). Nudge each icon right so icon-center = card-center:
     - `LangChain4jIcon` x 1.42 → 1.519
     - `QuarkusIcon`     x 4.617 → 4.716
     - `SpringBootIcon`  x 7.814 → 7.913
     (y/size unchanged; vertical placement is fine.)
  2. **Spring Boot icon pixelated.** `SpringBootIcon` source is only
     42×42 px (vs Quarkus 512×512, LangChain4j 150×150) → mush at 0.569in.
     Replace with a high-res official Spring Boot logo (≥512px, transparent
     PNG). Check the demo repo `docs/images/` for a better asset first;
     else source the official logo (confirm source at execution). Keep the
     same display size + centered position (post-fix x 7.913).
  3. **Introduce Thomas + trim the Story block (too text-heavy).**
     Decision: name Thomas in a *trimmed* one-line story + credit on Card3;
     move the dev-services bonus to speaker notes. (Supersedes the earlier
     "add 'Fellow Java Champion Thomas Vitale'" idea — that made it longer.)
     - `Story` — **replace the whole two-paragraph block** with one line:
       `Thomas Vitale and I built the same idea independently, then joined forces: docling-java, one foundation under all three.`
       (~18 words vs ~55; names Thomas; no em-dash. **Keep the blue
       highlight on `docling-java`** as in the original.)
     - **Move to slide 34 speaker notes** (the dev-services bonus that was
       the Story's 2nd paragraph): `Quarkus and Spring Boot both get zero-config Testcontainers dev services for Docling Serve.`
     - `Card3Text` desc: `Arconia Docling, plugs into Spring AI's RAG pipeline` → `Arconia Docling by Thomas Vitale, plugs into Spring AI's RAG pipeline` (parallels Card2's "co-maintained with Alex Soto"; also reinforces who Thomas is). **Overflow risk** — Card3Text box is 1.258in / 2 lines; the added "by Thomas Vitale" may push to 3 lines. **Verify at full-res**; if it wraps badly, shrink the desc font a touch or drop "Docling" → "Arconia by Thomas Vitale, plugs into Spring AI's RAG pipeline".

- [~] **Slide 37 URL unreadable — remove the hyperlink.** Root cause: the
  `RepoURL` runs are white (`FFFFFF`) but carry `<a:hlinkClick r:id="rId4">`,
  and PowerPoint paints hyperlink text with the theme hlink color
  (`0097A7`, dark teal), overriding the white — low contrast on the blue
  layout bg (`4695EB`). Decision: **just slide 37** (the only readability
  crisis; the other four URLs are teal-but-readable on light bg, left
  clickable). Fix:
  - Remove `<a:hlinkClick>` from both `RepoURL` runs so the white fill
    renders. Keep the white `solidFill` and the underline (`u="sng"`) so it
    still reads as a URL. The QR (`RepoQR`) remains the scan/click path.
  - Clean up the now-orphaned `rId4` relationship in `slide37.xml.rels`
    (unused external hyperlink rel) to avoid a dangling relationship.
  - Verify in **PowerPoint**, not just a LibreOffice render (LO already
    honors the white fill, so it won't reproduce or confirm the fix).

## Captured — round 2 (2026-09-02, post-batch-1 review)

- [~] **Slide 11 — chunk card to one line. DECIDED.** `Card2` desc run
  (Arial 14pt, box w=4.495in, usable ~287.6pt). Change:
  - from `Sentence splitter: fixed rules, no idea what a table is` (309pt, wraps)
  - to   `Sentence splitter: fixed rules, no table sense` (275pt, fits — mirrors
    Card1's "no layout awareness")
  Keep 14pt to match Card1 (don't shrink font). No em-dash
  (per [[avoid-em-dashes]]). Verify one-line at render.
  (Rejected: Eric's first "no clue about tables" = 309pt, still wraps.)

- [~] **Slide 36 ("Five questions, three modes, all real") — scorecard is
  confusing. DECIDED (add a ✓✓ grounded-evidence tier).** Problem: it's a
  5-question × 3-mode ✓/✗ "did it answer" matrix, but (a) Table 2 (row 4)
  is ✓✓✓ — a flat tie sitting between two gap rows — so the difficulty ramp
  reads as random, and (b) binary ✓ hides the talk's thesis ("same answer,
  wildly different evidence", slide 31): on Table 2 all three answer but only
  C brings the scores, yet ✓✓✓ shows a tie and under-sells C.
  **Fix — 3-tier marks in the deck palette (Eric chose ✓✓ over a ★ star):**
  - ✗ red `C0392B` = couldn't answer (unchanged)
  - ✓ blue `4285F4` = answered (unchanged)
  - **✓✓ green `7CB342` bold** = answered WITH grounded evidence (page / table
    / scores). Green = deck's "notable" accent; keeps one check vocabulary +
    makes 1-vs-2 legible from the back.
  - **Cell changes (C column only): Table 2 / Mode C → ✓✓; mAP / Mode C → ✓✓.**
    Q3 ("Who wrote the paper?") Mode C stays PLAIN ✓ (both B & C answered;
    reserve ✓✓ for the quantitative-grounding rows so the two greens cluster
    at the bottom as the crescendo). Everything else unchanged. Final C
    column: ✓ ✓ ✓ ✓✓ ✓✓.
  - Marks are `Mark<row>_<xcol>` shapes (20pt bold). The two changed C cells
    are `Mark3_7211700` and `Mark4_7211700` → text `✓✓`, color 7CB342.
  - **Add a legend line under the matrix** (matrix ends row 4 at y=4.53;
    room ~y=5.0 before slide bottom 5.625): `✓ answered   ✓✓ answered with
    grounded evidence (page / table / scores)   ✗ couldn't` — style the
    glyphs in their tier colors. No em-dash (per [[avoid-em-dashes]]).
  - **Verify at render:** green ✓✓ legibility + two-glyph column spacing
    (C cells centered at x~7.21); nudge if the pair overflows the cell.
  - **Notes tweak:** current notes say "ties on the left, gaps opening on the
    right" — update to reflect the new read (Table 2 is no longer a tie; C's
    green double-checks are the tell). Keep the "land on the last row" beat.

- [~] **Slides 29-32 — redo the LIVE badge. DECIDED (🔴 emoji + LIVE,
  top-left).** Eric dislikes the current badge (drawn red dot + red "LIVE"
  text, top-right at x=8.742/8.983, y~0.08). New design uses the SAME 🔴
  (U+1F534) emoji as the speaker-note cues ("🔴 LIVE DEMO NEXT"), so notes
  and audience view match:
  - **Remove** `LiveBadgeDot` (drawn oval) on all 4 slides.
  - **Repurpose `LiveBadgeText`:** text "LIVE" → `🔴 LIVE`; recolor "LIVE"
    stays red `D32F2F` bold; drop to **11pt** (from 12) to fit the top strip.
  - **Move top-left as an eyebrow above the heading:** left-aligned to
    heading (x=0.492), y ~0.05 in the strip above the title (heading is
    y=0.241 h=0.525). Widen box (~0.95in) for emoji+text.
  - **Caveats to verify:** (1) top strip is only ~0.24in tall — confirm at
    render the badge clears the heading; nudge size/pos if tight. (2) Emoji
    rendering must be checked IN POWERPOINT (LibreOffice may show 🔴 as a
    hollow circle / tofu — like the slide 37 URL, LO can't confirm this).
  Applies to 29/30/31/32 only (NOT 28). Mechanical → Sonnet on go.

- [~] **Slides 21→22 — kill the bottom-left footer "jump" on transition.
  DECIDED (mirror S21 footer on S22).** S21 footer = `[DoclingLogo]
  [DoclingQR][DoclingURL]` from x=0.459. S22 dropped the small footer logo
  (it became the hero on the right), so its QR+URL slid one slot left and up
  → the high-contrast QR square visibly hops on transition. Pin S22's footer
  to S21's exact EMU so nothing moves:
  - **Add** `DoclingJavaFooterLogo` (PICTURE, reuse S22's existing
    `image30.png` = docling-java mark) at `left=420000 top=4700000
    w=380000 h=380000` (= S21 DoclingLogo slot).
  - **Move** `DoclingJavaQR` → `left=850000 top=4700000` (keep 380000² size;
    was ~left=419k/top=4650k).
  - **Move** `DoclingJavaURL` → `left=1280000 top=4790000` (keep S22's own
    width — the docling-java URL is longer than S21's "docling.ai").
  Net: `[logo][QR][URL]` at identical coords on both slides. Footer logo
  cross-fades python→java in place (same spot/size); QR/URL don't move.
  **Verify** both slides render with the cluster pixel-aligned, then eyeball
  the 21→22 transition. Small layout add → route to Sonnet on go.

- [~] **Slide 21 ("Meet Docling") — align the blue card headers per row.**
  2×3 card grid (Card1-6, all same size, rows top-aligned at y=1.258 /
  2.925). Each card = blue header p0 (13pt bold `4285F4`) + description p1
  (11.5pt). All six are vertically **center-anchored** (`anchor=ctr`).
  Confirmed misalignment: descriptions that wrap to 2 lines (Card1, Card3,
  Card5) make those blocks taller, so centering pushes their headers UP vs
  the 1-line cards (2, 4, 6) — headers don't line up across a row.
  **Fix:** set `anchor="t"` (top) on all 6 card text frames. Headers then
  start at a fixed offset from each card top → aligned per-row AND across
  rows, robust to any wrap. 1-line cards get a little bottom whitespace
  (normal card look). Pure anchor change, no text/geometry edits.
  Mechanical → route to Sonnet on go. Verify visually at render.

- [x] **Slide 17 — de-squish + align params. DONE + overflow fix (2026-09-02).**
  Batch 2 applied 10.5pt + aligned params, but the render showed the class-closing
  `}` (line 23) spilling just below the box border into the caption. Root cause:
  para[0] kept old 1.25 lnSpc, paras[1-22] were 1.05, and 23 lines don't fit that
  loose in the vertical budget (no autofit; insets 0.20in top+bottom). Fix:
  set ALL 23 code paras to 100% lnSpc; CodeBlock top=0.800 h=4.200 (bottom 5.000);
  Caption top=5.050. Re-render confirms both `}` inside the box, params still align
  (`EmbeddingModel` under `EmbeddingStore`), caption clear below. VERIFIED at 150dpi.

- [x] **Slide 33 — `}` overflow fixed (2026-09-02).** All 21 code lines were still
  1.25 lnSpc (batch 2 only restyled its package line); the class `}` spilled below
  the box into the caption, same defect as slide 17. Fix: set all 21 lines to 1.15
  lnSpc (fills the existing box top=0.820 h=4.265 without a big bottom gap that 1.0
  would leave). Re-render confirms both `}` inside the box, caption clear below.
  VERIFIED at 150dpi.

- [-] **[superseded] Slide 17 — de-squish + align params. DECIDED (restore 10.5pt).**
  Slide 10×5.625in. Current CodeBlock y=0.875 h=3.718 (bottom 4.593),
  Caption y=4.703 (bottom 5.249); 23 code lines at 9.5pt / 100% lnSpc (the
  cramped feel — batch 1 forced 9.5pt + 100% to clear overflow). Now:
  - **Font 9.5 → 10.5pt** on ALL slide-17 code runs (matches 15/16/26/33).
  - **Grow the CodeBlock:** top 0.875 → ~0.80, height 3.718 → ~4.08 (bottom
    ~4.88); **move Caption down** y 4.703 → ~4.98 (bottom ~5.53, ~0.10in
    slide-edge margin). Keep the ~0.10in gap between block bottom and caption.
  - **Line spacing 100% → ~105%** on the code paragraphs (measured: 23 lines
    at 10.5pt/105% ≈ 3.52in text vs ~3.68in usable in the grown box — fits
    with margin; drop to 100% if render overflows, or nudge up toward 110% if
    there's room).
  - **Align wrapped params:** L4 leading spaces 32 → **34** so
    `EmbeddingModel embeddingModel, DemoConfig config) {` sits directly under
    `EmbeddingStore` on L3 (first param starts at col 34; Courier is
    monospaced so +2 spaces aligns exactly).
  - **Interacts with the package-as-code item below** — both rewrite slide
    17's L0/geometry. Apply as ONE coherent slide-17 change: package line on
    17 becomes 10.5pt too (NOT 9.5pt). **Verify no caption overflow at
    full-res** (the code-slide QA lesson).

- [~] **Code slides 15/16/17/26/33 — style the package line AS CODE, not a
  comment.** Reverses the batch-1 "de-emphasize the package line" styling:
  Eric says the muted/bold/italic gray look is weird next to the code.
  Current L0 on all 5: `Courier New, 9.5pt, bold=True, italic=True, 757575`.
  Change to match the deck's Java syntax highlighting:
  - Split L0 into 2 runs: `package` → keyword blue `4285F4` (same as
    `public`/`class`/`final`); the ` <fqn>;` remainder → default code gray
    `212121`.
  - **Remove bold, remove italic.** Keep Courier New.
  - **Font size = that slide's code size: 10.5pt on ALL FIVE** (15/16/26/33
    always were; 17 is now restored to 10.5pt by the de-squish item above, so
    its package line goes to 10.5pt too — the earlier "9.5pt on 17" is void).
  - **No blank line** between package and class (keep tight to avoid caption
    overflow — the reason 17 is already 9.5pt). Eric may opt in to a blank
    line later; would need a fit re-check.
  - **Verify no caption overflow at full-res** on 15/16/26/33 (package line
    9.5→10.5pt adds a little height); 17 unchanged in height.
  - **Doc reconcile:** talk-plan.md package-line note currently says the
    line is styled muted gray — update to "styled as code (package keyword
    highlighted), matching the snippet's font/coloring."
  Good mechanical batch → route to Sonnet on go.

## New finding (from batch-1 verification — RESOLVED, no change needed)

- [-] **Slide 34 "Quarkus" logo — false alarm, my misidentification.**
  I flagged `QuarkusIcon` (`/ppt/media/image14.png`) as the docling-java
  brand mark. It is NOT. `image14.png` is the current official Quarkus icon
  (cube-in-a-star inside the speech-bubble frame) — Quarkus's modern
  commonhaus-era mark. Verified 2026-09-02 by downloading the official icon
  from quarkus.io/brand (commonhaus/artwork
  `quarkus_icon_1024px_default.png`) and diffing: identical design, ~5.9/255
  mean per-channel difference (pure resample/antialias noise). The docling
  mark is actually the duck+Duke illustration (`image30.png`, slide 22),
  which I conflated with the cube+star. No edit made — the card is correct.
  (`image14.png` is shared with slide 3, so had a swap been needed it would
  have required a new media part, not a blob overwrite.)

## Batch 1 — APPLIED & VERIFIED (2026-09-02)

All 9 "Captured this session" items applied to canonical
`garbage-in-insight-out-v29.pptx` and independently verified (structural
XML + full-res render crops of slides 17/22/29/34):
- Items 1,4,5,6,7,9 confirmed structurally clean (37 slides/notes intact,
  package lines + captions removed, headers de-periodized, zero em-dashes,
  badges on 29-32 only, S37 hyperlink + rId4 gone).
- Item 2 (S17): full-res render shows `final class`/`static`/full wrapped
  params, correct green/gray highlighting, 9.5pt, no overflow into caption.
- Item 3 (S22): logo hero on right, "Docling Java" text gone, credibility
  line + quote + URL/QR intact. Verified visually.
- Item 8 (S34): icons centered, Spring Boot logo now 1024² (crisp), Story
  trimmed with `docling-java` highlight, dev-services moved to notes,
  Card3 used the 2-line fallback wording ("Arconia by Thomas Vitale…").
- Docs: `talk-plan.md` package-line + chunker-reconcile notes updated. Demo
  repo doc has no code-slide mechanic wording → nothing to change there.
- Backup: `archive/garbage-in-insight-out-v29-prebatch-20260902-141412.pptx`.
- **STILL OWED (Eric):** open + resave in PowerPoint to normalize, and
  eyeball slide 37's URL in PowerPoint (LibreOffice can't confirm the
  hyperlink-color fix).

## Captured — round 3 (2026-09-02, in-PowerPoint review)

- [x] **Code slides 15/16/17/26/33 — package keyword renders BLACK in PowerPoint
  (BUG).** Root cause: the batch-2 run split wrote rPr children in invalid OOXML
  order (`<a:latin>` before `<a:solidFill>`); PowerPoint drops the mis-placed
  color, `package` falls back to black. LibreOffice is lenient and rendered it
  blue, so the render "verified" clean = missed it. Scan found EXACTLY 10 bad runs
  (the 2 runs on each of the 5 package lines); nothing else in the deck is
  mis-ordered. Fix: reorder each so `solidFill` precedes `latin`. VERIFY the fix
  by checking XML child order is schema-valid (NOT by LibreOffice render, which
  masks it). Eric confirmed via PowerPoint screenshot.
- [x] **Code slides 15/16/17/26/33 — add a little vertical space between the
  package line and the class line.** Use paragraph spacing (spcAft ~6pt on para 0)
  not a blank line, to stay cheap on the dense slides. Re-check 17 (23 lines,
  box h=4.20, ls=1.0) and 33 (21 lines, box h=4.265, ls=1.15) don't re-overflow.
- [x] **LIVE marker — unify across all live-demo slides. DONE (2026-09-02).**
  - Placement: **full top-left corner**, flush to the left edge (NOT x=0.492 which
    aligns with the indented title and made it look stacked above the title). Match
    the bottom-left footer-logo left margin. Keep y near top (~0.08).
  - Slides 29-32: MOVE existing `LiveBadgeText` ("🔴 LIVE") to the full-left x.
  - Slides 12/13/14/25: ADD an identical `LiveBadgeText` badge at the same spot.
  - Titles (strip "live", Eric picked "Strip it, add badge"):
    - 12/13/14: `Mode A, live: "Who wrote the paper?"` → `Mode A: "Who wrote the paper?"`
    - 25: `Live: "Who wrote the paper?" again` → `"Who wrote the paper?" again`
  - Badge style = same as 29-32: `🔴 LIVE`, red D32F2F, bold, 11pt.

## Notes

- Canonical file: `garbage-in-insight-out-v29.pptx` (PowerPoint-repaired,
  content-verified, 37 slides). Pre-repair originals in `archive/`.
- After any python-pptx batch: render (LibreOffice) to eyeball, then open
  + resave in PowerPoint to normalize before treating as final.

## Captured — round 4 (2026-09-02, font-consistency review)

- [x] **Heading font outliers — S17 + S36 not Kalam. DONE (2026-09-02).** Every Heading is Kalam
  2800 bold except: S17 (`INHERIT`/1800) and S36 (`INHERIT`/2000). Fix: set run
  latin=Kalam, sz=2800 on both. Verify 28pt fits one line (S17 38ch w=9.016;
  S36 37ch w=9.318; siblings of similar length fit).
- [x] **S21 blue card headers not Kalam. DONE (2026-09-02).** Card1-6 blue headers are `INHERIT`/1300
  (Arial); should be Kalam like slide 11's card headers (Kalam/1700 blue). Fix:
  latin=Kalam on the 6 header runs, keep sz=1300. Verify long ones
  ("Document intelligence", "Unified representation") don't wrap at Kalam 13pt;
  drop ~1pt if they do.
- [-] **S1 title not Kalam — LEFT ALONE.** Title slide is Eric's (Gemini
  illustration swap pending); styling the placeholder is moot.
- Non-fixes (correctly Arial, not headers): blue body emphasis S9/S18/S22/S24/S34,
  S14 score numbers, S36 checkmarks + legend, all URLs.

## Captured — round 6 (2026-09-02, in-PowerPoint review)

- [x] **S34 title mismatch. DONE (2026-09-02).** "Not just a Java thing" was wrong
  (every card is a JVM framework; Story lands on framework-independence, not
  language-independence). Retitled **"Not just a Quarkus thing"**. Verified: single
  line, Kalam, clear of cards.
- [x] **S36 "Table 2 winner" row wrapped to 2 lines. DONE (2026-09-02).** Text
  measured 3.14in but Label3 box was 3.24in with 0.2in insets (3.04in usable), so
  it overflowed by ~0.1in. Fix: widened Label3 box 3.24->3.50in (invisible box;
  visible text still ends ~x=3.73, far left of Mode A checkmark at x=4.80). Verified
  one line, no collision.

## Captured — round 5 (2026-09-02, in-PowerPoint review)

- [x] **S36 legend too close to the rows + wrong item order. DONE (2026-09-02).**
  Legend was top=5.000, overlapping RowBg4 (bottom=5.162). Fix: moved
  ScorecardLegend to top=5.20 and reordered runs worst→best:
  `✗ couldn't   ✓ answered   ✓✓ answered with grounded evidence (page / table / scores)`.
  Verified via render: clear gap below "mAP difference" row, no collision with
  footer logo or social icons.
