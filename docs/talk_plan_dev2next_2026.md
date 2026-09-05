# Garbage In, Insight Out: Talk Plan

dev2next, 75 minutes, no dedicated Q&A slot (people find you after).

Current deck: `garbage-in-insight-out.pptx` in this folder (37 slides). The
running punch-list of deck edits lives alongside it in `deck-todo.md`.

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

- **Every live-demo beat gets fallback slides showing the same real, captured
  result**, not a substitute for going live, insurance for it. The slide(s)
  right before a live moment get a speaker-note cue: `🔴 LIVE DEMO NEXT,
  switch to the app now`, with the exact mode and question to ask. The
  fallback slide(s) right after get: `📌 FALLBACK for the live query above`,
  with instructions for both cases (worked live: breeze through confirming
  what they saw; didn't: deliver it fresh, this is now primary content).
  This applies to every live beat: Act 2's verdict (Q3 again, A+B) and
  Act 3's knockout (Q5, A+B+C) get the same pairing as Act 1's.
- **Live beats also carry a visible on-slide badge.** Every slide that is
  itself a live moment shows a small red `🔴 LIVE` badge flush in the
  top-left corner (aligned with the footer branding, not with the indented
  title), so the audience and the presenter can see at a glance which beats
  are happening live rather than replayed. The word "live" was removed from
  the slide titles themselves once the badge carried that signal.
- One RAG pipeline, three modes, each changing exactly one variable from
  the previous mode (Mode A: Tika + naive chunker. Mode B: Docling
  extraction + same naive chunker. Mode C: Docling extraction + Docling
  hybrid chunker).
- **Code slides keep the package line, omit imports**, logic only. The
  package line locates the class (package + class name = the fully-qualified
  location); imports are omitted since they're noise that wraps awkwardly in
  the code block. The small file-attribution captions were dropped too,
  redundant once the package line is present. The package line is styled as
  code, matching the snippet's own font and coloring: the `package` keyword
  highlighted blue like the other keywords, the rest of the line (path +
  semicolon) in the default code color, with a little vertical space before
  the class declaration so the logic stays the hero. Applies to all code
  slides (Tika extractor, naive chunker, retrieval augmentor, Docling
  extractor swap, hybrid chunker).
- **The chunker stays identical across Modes A and B**, only the extractor
  changes between them (Act 3's hybrid chunker is the one actual chunker
  change, and it's Docling-side). Act 2's extractor-swap code slide does not
  re-show `NaiveChunker`. The "chunker unchanged" point is carried by the
  Mode B design slide (23: "the exact same sentence splitter as Mode A.
  Unchanged. On purpose.") and the spoken track; the extractor code comment
  only explains why it flattens Docling's structure to plain text and keeps
  provenance.
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

1. **Prose baseline** ("how many annotators, how long"): all three tie.
2. **Figure/chart** ("what % are Patents"): all three tie (confirmed
   empirically; the original hypothesis that this would differentiate
   modes didn't hold up, and that's fine, it became a "not
   cherry-picking" control instead).
3. **Author metadata** ("who wrote the paper"): A confidently wrong
   (confuses cited works with actual authors), B correct but verbose, C
   correct and direct (reads the ACM reference block on page 1). The
   spine question, asked three times across the whole talk.
4. **Table qualitative** (Table 2, which architecture won): all three
   get the label right, but evidence escalates: A bare assertion, B
   assertion + retrieval metadata, C assertion + actual per-class mAP
   scores.
5. **Table quantitative knockout** (mAP gap, Caption row: YOLOv5x6 77.7
   vs. FRCNN R101 70.1): A and B can't answer at all, C computes 7.6
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
| **Act 1: Mode A (naive)** ||||
| 5a | Practice & design | 3 | Tika + naive chunker, conceptual. Annotated failure screenshots live here: Table 2's caption stranded 90 lines from its own numbers by an intruding "5 EXPERIMENTS" header; Figure 2's pie-chart labels and percentages extracting as two disconnected sequences |
| 5b | **Live**: cold open, Q3 | 3 | Mode A only |
| 5c | Code: Tika extractor + naive chunker | 4 | |
| 5d | Stakes coda: Vegetative Electron Microscopy | 4 | Spoken beat + simple text slide or original mockup, not a screenshot of the actual retracted paper. Present the column-misread origin as one contested theory, not settled fact |
| 6 | Interlude: why not just a bigger LLM/VLM | 5 | Cost, latency, privacy |
| **Act 2: Mode B (Docling extraction)** ||||
| 7a | Twist + Docling Java origin story + license/OSS+SaaS aside | 3 | Three-slide section-break sequence (BJUACM-style big-logo breaks): "Meet Docling" → Docling + Python logos side by side (the setup: it all lives in Python) → docling-java payoff logo (the resolution). The "Java Champion problem" text (last time I checked, I'm a Java Champion; Java devs shouldn't be left out in the cold) is now narrated, not on-slide |
| 7b | Design: what changed | 3 | Extractor only, same chunker, conceptual |
| 7c | **Live**: Q3 again, A+B side by side | 3 | The verdict |
| 7d | Code: Docling extractor swap | 4 | |
| **Act 3: Mode C (hybrid chunking)** ||||
| 8a | Design: structure-aware chunking | 3 | |
| 8b | **Live**: Q3 callback (C debuts), then Q1/Q2 aside | 2 | All three tie on the control questions |
| 8c | **Live**: Q4, A+B+C | 3 | Same answer, escalating evidence |
| 8d | **Live**: Q5, A+B+C | 4 | The knockout, only C answers |
| 8e | Code: hybrid chunker config | 3 | |
| 8f | Ecosystem ("Not just a Quarkus thing") | 4 | docling-java as the common foundation, not tied to one framework: LangChain4j, Quarkus, Spring Boot (Arconia plugging into Spring AI's RAG pipeline). Built independently with Thomas Vitale, then joined forces |
| 9 | Roadmap | 3 | |
| 10 | Scorecard slide | 2 | All 5 questions x 3 modes, three-tier marks with a legend: ✗ couldn't, ✓ answered, ✓✓ answered with grounded evidence (page / table / scores). The ✓✓ tier is reserved for the quantitative-grounding rows (Q4/Q5, Mode C) so the differentiators read at a glance |
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
