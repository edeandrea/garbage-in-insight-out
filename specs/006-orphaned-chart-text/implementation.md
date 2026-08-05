# Spec 006: Orphaned Chart Text — Implementation Notes

## Task 1: Refactor captionTextFor to accept List<RefItem>

Change signature from `captionTextFor(TableItem)` to
`captionTextFor(List<RefItem>)`. Update `resolvedCaptionFor` to pass
`table.getCaptions()`. Update DoclingExtractor callers. Update tests.

## Task 2: Add orphanedChildrenOf to DocItemIndex

Filter text items by parent ref and exclusion from referenced set.

## Task 3: Create synthetic picture segments

Post-process in extractAndChunk(): collect referenced refs, iterate
pictures, call orphanedChildrenOf, build synthetic TextSegments with
caption + orphan text. Used proper imports (PictureItem, Set) instead
of inline fully-qualified references.

## Task 4: Integration test

Added test verifying at least one PICTURE segment contains "Patents"
and "8%" from Figure 2 chart labels.

## Task 5: Browser verification

All 5 questions tested successfully:
- Q1 (Table 2 winner): all modes answer correctly ✓
- Q2 (mAP difference): only Mode C answers (7.6 mAP) ✓
- Q3 (prose control): all modes answer equally ✓
- Q4 (Patents %): all modes answer 8% — Mode C now retrieves the
  synthetic PICTURE segment ✓
- Q5 (largest category): all modes answer Financial Reports 32% ✓
