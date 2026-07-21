# Spec 001: Three-Mode RAG Demo

Status: Draft

## Summary

A RAG chatbot used three times across the talk to prove two theses in
sequence: (1) bad document extraction, not the LLM, is usually why RAG
answers are wrong, and (2) once extraction is clean, chunking strategy is
a second, separate lever on retrieval quality.

## Motivation

Three demo beats, same app throughout:
- Cold open (Mode A): show a wrong/hallucinated answer, blame the model,
  then reveal the real cause.
- Closing verdict (Mode A vs. B): same question, right answer this time,
  extraction is the only thing that changed.
- Advanced payoff (Mode B vs. C): a second question that only Mode C
  answers correctly, chunking is a second, separate lever.

## Requirements

1. ONE RAG pipeline shape: extract -> chunk -> embed -> store -> retrieve
   -> generate.
2. Three modes, each changing exactly one variable relative to the
   previous one:
   - Mode A (`naive`): Apache Tika/PDFBox plain-text extraction, naive
     fixed-size/token chunker.
   - Mode B (`docling-naive-chunk`): Docling Java structured extraction,
     the *same* naive chunker as Mode A.
   - Mode C (`docling-hybrid-chunk`): Docling Java structured extraction,
     Docling's own structure-aware/hybrid chunker.
3. Between A and B: embedding model, vector store, retrieval top-k, prompt
   template, and LLM must be IDENTICAL. Between B and C: everything held
   constant except the chunker. No other variable may change within a
   comparison pair.
4. Each mode has its own pre-built vector index/collection, built ahead of
   time, not re-ingested live during the demo.
5. Fixture document: the DocLayNet paper (arXiv:2206.01062v1), at
   `fixtures/doclaynet-2206.01062v1.pdf`. Chosen deliberately: it's the same PDF
   Docling's own team uses internally to demo naive-extraction failures,
   and it's a paper about document-layout analysis.
6. UI: a simple chat interface. Must show retrieved chunks alongside the
   generated answer, not just the final answer, this is the visual proof
   of the thesis, not just an assertion. Must support switching between
   modes (A/B/C) live, at runtime, through the UI itself, without editing,
   recompiling, or hot-reloading any application code, a restart-free but
   code-edit-triggered reload (e.g. Quarkus dev mode picking up a changed
   constant) does not satisfy this requirement.
7. Ingestion code must be simple/readable enough to show on screen on its
   own, separately from the chat demo.

### Planted questions, Set 1 (Mode A vs. B, verified against real naive
extraction via `pdftotext`, do not change without re-verifying):

1. "How many List-item annotations are in the DocLayNet dataset?" ->
   185,660 (Table 1). Naive extraction turns the table into two
   disconnected flat lists, with an unrelated Figure 3 caption and two
   paragraphs of body text spliced into the middle of the column stream.
2. "What does Table 2 show, and what network architecture won overall?"
   -> YOLOv5x6 wins overall with 76.8 mAP. Naive extraction separates
   Table 2's numeric grid from its own caption by ~90 lines, with an
   unrelated section header ("5 EXPERIMENTS") intruding between them.
3. "What percentage of DocLayNet pages are Financial Reports?" -> 32%
   (Figure 2, a pie chart). Naive extraction produces two disconnected,
   interleaved sequences of labels and percentages with no link between
   them.
4. "What's the source of the AAPL example in footnote 2?" ->
   https://www.annualreports.com/. Naive extraction strands the footnote
   marker a full page away from the footnote text.

Pick 2-3 of these four for the actual cold-open/verdict pair. #2 (Table 2)
is the strongest single exhibit if only one is used.

### Planted question, Set 2 (Mode B vs. C, unverified hypothesis):

5. "By how many mAP points does YOLOv5x outperform Faster R-CNN overall?"
   -> 3.4 points (76.8 vs. 73.4, Table 2's `All` row). Intended failure
   mode: if Mode B's fixed-size chunker slices Table 2's grid across a
   chunk boundary, retrieval only surfaces part of it. Mode C's chunker
   should keep the table intact as one retrievable unit.

## Out of scope

- Re-ingesting live during the on-stage demo (indices are pre-built)
- Any UI polish or branding beyond a functional chat + retrieved-chunks view

## Open questions

- Whether Mode B's chunker config actually fragments Table 2 at the
  chosen chunk size is UNVERIFIED. Before using planted question 5 on
  stage, chunk the Docling-extracted text both ways and confirm Mode B
  genuinely splits the table. If it doesn't, either shrink the chunk size
  or find a different section that reliably straddles a boundary. Resolve
  this during the plan or tasks phase, not left for demo day.
