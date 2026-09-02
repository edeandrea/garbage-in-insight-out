# Garbage In, Insight Out — Talk Plan

dev2next, 75 minutes, no dedicated Q&A slot (people find you after).

## The throughline

We explain what RAG is, then spend three acts proving one claim by
building it live in front of the audience: bad extraction, not the
model, is why RAG hallucinates, and once extraction is clean, chunking
is a second, independent lever on top of it. Each act adds exactly one
mode, freezes everything upstream of that addition on screen, and
re-asks a recurring question so the audience watches the same query get
better answers for reasons they can see, not reasons they're told. It
closes with a scorecard that turns the whole session into one glance,
then the ask.

## The document

The DocLayNet paper (arXiv:2206.01062v1), a real KDD 2022 paper from IBM
Research (Pfitzmann, Auer, Dolfi, Nassar, Staar). Chosen deliberately:
it's a paper *about* document-layout analysis, which makes getting its
own layout wrong a built-in joke, and it's the same PDF Docling's own
team already uses internally to demonstrate naive-extraction failure, so
it's not a cherry-picked example.

## The mechanics (apply throughout, not just once)

- **Code slides omit package declarations and imports**, logic only.
  Cleaner focus, and it avoids long import lines wrapping awkwardly in
  the code block. Applies to the remaining code slides (Docling
  extractor swap, hybrid chunker config) as much as the ones already
  built.
- **The chunker stays identical across Modes A and B**, only the
  extractor changes between them (Act 3's hybrid chunker is the one
  actual chunker change, and it's Docling-side, not this class). Act 2's
  "Docling extractor swap" code slide (7d) should not re-show
  `NaiveChunker`, it's unchanged from what Act 1 already showed. Say so
  explicitly on that slide rather than leaving it ambiguous whether
  something changed there too.
- **Every live-demo beat gets fallback slides showing the same real, captured
  result**, not a substitute for going live, insurance for it. The slide(s)
  right before a live moment get a speaker-note cue: `🔴 LIVE DEMO NEXT —
  switch to the app now`, with the exact mode and question to ask. The
  fallback slide(s) right after get: `📌 FALLBACK for the live query above`,
  with instructions for both cases (worked live: breeze through confirming
  what they saw; didn't: deliver it fresh, this is now primary content).
  This applies to every future live beat too: Act 2's verdict (Q3 again,
  A+B) and Act 3's knockout (Q5, A+B+C) both need this same pairing when
  those slides get built, not just Act 1's.
- One RAG pipeline, three modes, each changing exactly one variable from
  the previous mode (Mode A: Tika + naive chunker. Mode B: Docling
  extraction + same naive chunker. Mode C: Docling extraction + Docling
  hybrid chunker).
- Once a mode's panel answers a question, it stays static. Never
  re-query an old mode on an old question, non-determinism would put
  the callback's own proof at risk.
- All three panels equal visual weight throughout, show/hide togglable
  rather than any built-in de-emphasis.
- Consistent color-coding per mode across every slide and every panel,
  A is always the same color everywhere, so the audience tracks modes
  across gaps without re-explanation.
- Same question order within every panel once populated: Q3, Q1, Q2,
  Q4, Q5, so scrolling any panel down the line lands on the same
  question in the same position. Falls out naturally from Act 3's
  sequencing (Q3 callback before the Q1/Q2 aside), not a separate rule
  to enforce.

## The five planted questions

1. **Prose baseline** ("how many annotators, how long") — all three tie.
2. **Figure/chart** ("what % are Patents") — all three tie (confirmed
   empirically; the original hypothesis that this would differentiate
   modes didn't hold up, and that's fine, it became a "not
   cherry-picking" control instead).
3. **Author metadata** ("who wrote the paper") — A confidently wrong
   (confuses cited works with actual authors), B correct but verbose, C
   correct and direct (reads the ACM reference block on page 1). The
   spine question, asked three times across the whole talk.
4. **Table qualitative** (Table 2, which architecture won) — all three
   get the label right, but evidence escalates: A bare assertion, B
   assertion + retrieval metadata, C assertion + actual per-class mAP
   scores.
5. **Table quantitative knockout** (mAP gap, Caption row: YOLOv5x6 77.7
   vs. FRCNN R101 70.1) — A and B can't answer at all, C computes 7.6
   points correctly. Verified empirically via `ChunkSizeSimulationTest`
   that `maxTokens=300` is what causes Mode B's chunker to fragment
   Table 2.

## Full segment table

Note: the title slide sits first in the actual file, it's what's on
screen while the room fills before the talk starts, and the natural home
for a swappable conference logo. The pre-tease is still your first
*spoken* beat, the cold open, triggered by your first click forward from
the title slide, not by the file's resting position.

| # | Segment | Min | Notes |
|---|---|---|---|
| 1 | Pre-tease | 1 | Screenshot flash of Q3's wrong answer, no explanation, "hold that thought." A captured image, not a live call. |
| 2 | Open: hook + who am I | 3 | Java Champion, Spring + Quarkus dual credibility, docling-java/quarkus-docling lead |
| 3 | RAG primer | 5 | Existing content |
| 4 | Meet the document | 3 | Page 1 flash (title/authors/abstract), the meta-joke, the "Docling's own team already uses this file" credibility beat |
| **Act 1 — Mode A (naive)** ||||
| 5a | Practice & design | 3 | Tika + naive chunker, conceptual. Annotated failure screenshots live here: Table 2's caption stranded 90 lines from its own numbers by an intruding "5 EXPERIMENTS" header; Figure 2's pie-chart labels and percentages extracting as two disconnected sequences |
| 5b | **Live**: cold open, Q3 | 3 | Mode A only |
| 5c | Code: Tika extractor + naive chunker | 4 | |
| 5d | Stakes coda: Vegetative Electron Microscopy | 4 | Spoken beat + simple text slide or original mockup, not a screenshot of the actual retracted paper. Present the column-misread origin as one contested theory, not settled fact |
| 6 | Interlude: why not just a bigger LLM/VLM | 5 | Cost, latency, privacy |
| **Act 2 — Mode B (Docling extraction)** ||||
| 7a | Twist + Docling Java origin story + license/OSS+SaaS aside | 3 | |
| 7b | Design: what changed | 3 | Extractor only, same chunker, conceptual |
| 7c | **Live**: Q3 again, A+B side by side | 3 | The verdict |
| 7d | Code: Docling extractor swap | 4 | |
| **Act 3 — Mode C (hybrid chunking)** ||||
| 8a | Design: structure-aware chunking | 3 | |
| 8b | **Live**: Q3 callback (C debuts), then Q1/Q2 aside | 2 | All three tie on the control questions |
| 8c | **Live**: Q4, A+B+C | 3 | Same answer, escalating evidence |
| 8d | **Live**: Q5, A+B+C | 4 | The knockout, only C answers |
| 8e | Code: hybrid chunker config | 3 | |
| 8f | Ecosystem montage | 4 | Quarkus, LangChain4j, Spring AI, Arconia |
| 9 | Roadmap | 3 | |
| 10 | Scorecard slide | 2 | All 5 questions x 3 modes, checkmarks, differentiators highlighted |
| 11 | Close: recap + call to action | 3 | |

**Total: 71 minutes. ~4 minutes slack against 75.**

## Screenshot/asset plan

- **Clean baseline shots** (meet the document): unannotated, "here's
  what a human sees."
- **Annotated failure shots** (Act 1 design beat): highlight the actual
  verified breaks with a consistent annotation style/color across the
  whole deck. Real extraction output, not mockups.
- **App UI screenshots** (`docs/images/*.png`): live-demo fallback
  insurance, conceptually separate from document-illustration shots
  even though they'll live in the same deck.
- VEM: no screenshot of the actual paper or the Retraction Watch piece,
  spoken/text-slide/original mockup only.

## Open items

- **This session's full batch applied and validated** (still 36 slides).
  Slide 4 divider retitled "Quick Primer"; slide 7's ingestion diagram
  centered; Mode A/B/C design slides (11, both Mode B steps, Mode C)
  converted from bullets to cards, matching the primer/interlude card
  language; asides restyled as small gray captions rather than
  full-size italic competing with the bullets; all four code slides got
  more line spacing (had to dial the longest one, DoclingHybridExtractor,
  back from 145% to 125% after catching real overflow on first render);
  DoclingNaiveExtractor gained a caption noting doclingServeApi calls
  Docling Serve as a separate service; slide 21 lost its self-host
  bullet (folded into slide 20's Privacy card instead) and its Docling
  Java bullet became a card; the montage slide was fully rebuilt as an
  icon card grid (reusing LangChain4j/Quarkus/Spring Boot logos already
  in the deck), "co-maintained with Alex Soto," plus a line noting
  Quarkus and Spring Boot both get Testcontainers-based dev services for
  Docling Serve.
- **Font consistency pass, deck-wide**: short labels (card titles, panel
  headers like "Mode A/B/C," verdict labels like "Correct. Verbose.,"
  "7.6 mAP points.") now explicitly set Kalam; body/explanatory text
  explicitly set Arial, matching the theme's own defined default rather
  than relying on inheritance. Applied across every slide with label-
  style text, not just the ones already being touched for other reasons,
  code slides deliberately excluded since blue there means syntax
  highlighting, not a label.
- **Confirmed non-issue, no fix needed**: hyperlinks appeared
  non-clickable in PowerPoint's edit view. Verified via direct XML
  inspection that the relationships and hlinkClick structure are
  correct; this is standard PowerPoint behavior (edit view needs
  Ctrl/Cmd+click, slideshow view works with a plain click), not a file
  problem.

- **Deck is now 36 slides.** This session's full batch, all implemented
  and validated: slide 21's paragraph split into two slides (setup line,
  then a duplicate adding the bold blue payoff line and the "or it's
  hallucination" spoken aside); capitalization fixes (the/One/One);
  "layout & table aware" wording; slide 23's caption shortened with the
  full explanation moved to speaker notes; `@ApplicationScoped` dropped
  from slide 15; VEM QR code added; a new "Meet Docling" primer slide
  (6-card grid, content adapted from Docling's own official overview
  deck) inserted right after the Act 2 divider, with the Docling
  logo/QR/URL moved onto it; the Java Champion slide trimmed to drop the
  now-redundant Docling bullet, with Docling Java converted to a card;
  a "Takeaways" divider, a real Scorecard slide (5 questions x 3 modes,
  all real data, checkmarks), and a Thank You slide (real repo QR,
  clearly-marked placeholder for the not-yet-real slides QR) added at
  the close.
- **Three real bugs caught and fixed during this session's own
  validation, before delivery**: an unescaped `&` in the Scorecard
  slide's XML, a notes-file naming collision (reused the same filename
  for two different slides' notes), and a dash-before-fill element-
  ordering violation on the Thank You slide's placeholder box, the same
  *class* of schema bug as the earlier corruption incident, different
  element type, caught this time by the same direct-schema-validation
  process rather than shipping blind. All three confirmed fixed via
  direct validation against the real ISO OOXML schemas (165 files
  checked, zero violations) before this file was delivered.

- **Speaker notes reformatted deck-wide as real bulleted lists** (actual
  bullet formatting in the Notes pane, not just short sentences), plus
  the Act 1 divider (position 10) got notes for the first time, it had
  none before, inconsistent with the other three dividers.
- **Slide 11**: "sentence splitter" capitalized to "Sentence splitter."
  Deliberately left lowercase on slide 21 ("the exact same sentence
  splitter as Mode A"), that's genuine mid-sentence prose, not a label,
  so capitalizing just that one word there would read as a typo rather
  than consistency. Flag if this reasoning doesn't hold.
- **Slide 16**: the attachMetadata() explanation moved from a standalone
  caption into an inline code comment, matching the pattern already used
  on the other three code slides. Caption shape removed.
- **Slide 15 (VEM)**: added a spoken transition cue to the notes, read
  right before advancing in: "And this isn't just a problem for my demo.
  Here's what document-parsing chaos actually causes, out in the real
  world."

- **File is now genuinely, directly schema-validated, not just "the tool
  said passed."** Discovered the shared validator's schema check only
  matches files sitting directly inside `ppt/`, not nested ones like
  `ppt/slides/slideN.xml`, so every slide/layout/master in this project
  has been silently skipped by schema validation the whole time. That's
  how a real corruption bug (font tags duplicated/misordered on 13
  slides, from a repeat of the earlier fill-before-font mistake, this
  time hitting slides that already had it fixed and inserting a second
  copy) made it into a delivered file without any tool flagging it.
  Also found and fixed a second, older bug: `sldIdLst` and
  `notesMasterIdLst` were swapped in `presentation.xml`, present since
  slides were first added, unrelated to the batch session.
  **Going forward**: any batch of XML edits gets validated directly
  against the real ISO OOXML schemas
  (`/mnt/skills/public/pptx/scripts/office/schemas/ISO-IEC29500-4_2016/`)
  before delivery, not just the shared validate.py tool, since that
  tool's "all validations passed" doesn't cover slide content.

- **Deck is at 31 slides, everything through Act 3 processed and clean.**
  Tonight's batch, all implemented and verified: new divider between
  intro and the start of the talk; deck-wide title-size/font consistency
  (18 slides); six headings shortened where the size bump caused
  wrapping; chunker heading simplified and both forward-reference
  comments removed (plus its caption, which had the same issue);
  capitalization matched across the "Mode X: The Y" headings; all four
  code slides rebuilt with 2-space indent, LangChain4j classes
  highlighted green, comments in bold gray instead of muted green (fixes
  the earlier color collision); the extractor's loadDocument call now
  fits on one line; DoclingNaiveExtractor's Uni.createFrom() wrapping
  restored and its missing return fixed; QR code added to the DocLayNet
  slide (arxiv.org/abs/2206.01062); VEM line-wrapping fixed and a
  Retraction Watch reference link added; the "why not a bigger model"
  interlude rebuilt as a 2x2 card grid; Docling and docling-java logos,
  URLs, and QR codes added to the twist/origin slide.
- **Next session starts fresh on Act 3 specifically.** Everything above
  applied deck-wide, including to Act 3's code slide and headings, since
  those were already-agreed global rules, not new Act 3 content review.
  Act 3's actual content (design, Q1/Q2/Q4/Q5, montage) hasn't had a
  dedicated pass yet the way Act 1 and 2 did.

- **Act 1, Act 2, and Act 3 are all fully complete, no placeholders
  remaining anywhere** (30 slides total). The Act 2 verdict slide and
  Act 3's Q3 full-circle slide both got their real Mode B/C data from
  `docs/images/q5-who-wrote-paper-results.png` in the repo. One nuance
  worth remembering: that screenshot's own Mode A answer differs from
  the one used throughout the rest of the deck (different wrong paper,
  different names), that's just LLM non-determinism across separate
  runs. The deck deliberately keeps the original captured Mode A text
  everywhere for consistency with the pre-tease callback; don't let this
  screenshot's Mode A text creep in anywhere by mistake.
- **Class name correction, matters for Act 3 too:** there is no single
  `DoclingExtractor` class (the README's architecture diagram is stale on
  this). It's actually two separate classes: `DoclingNaiveExtractor`
  (Mode B, used for Act 2's code slide) and `DoclingHybridExtractor`
  (Mode C). Act 3's "hybrid chunker config" code slide should probably
  show `DoclingHybridExtractor` specifically, not a separate standalone
  chunker class, worth confirming when that slide gets built since
  hybrid chunking may happen server-side as part of extraction rather
  than in a local chunker class the way Mode A/B's NaiveChunker works.
- **Real data for Q1, Q2, Q4, and Q5 is no longer blocked.** The repo
  README now documents verbatim captured outputs across all three modes
  for these four questions, not just Q3. Only Act 2's verdict slide
  (Mode B's exact wording for Q3) is still genuinely thin, the README
  describes it qualitatively ("works through the extended content and
  arXiv metadata") but doesn't quote it. Either get a screenshot for that
  one slide, matching how Q3/Mode A was sourced, or accept a clearly-
  labeled paraphrase there specifically.
- Format: **pptx**, confirmed.
- Title slide is currently plain text as a placeholder. Eric will
  generate a custom illustrated title image (Gemini, matching the style
  of the sample deck's opener) and swap it in himself; leave as-is until
  then.
- Rehearsal-test the six live calls clustered in Act 3 at least once end
  to end before relying on them back to back.
- Minor polish, not urgent: a few lines in the Tika code slide wrap
  awkwardly at the current font size, readable but not pretty.
