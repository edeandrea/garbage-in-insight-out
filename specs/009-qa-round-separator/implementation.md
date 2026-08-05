# Spec 009: Q&A Round Separator — Implementation Notes

CSS-only change in `styles.css`. Added `border-top`, `margin-top`,
and `padding-top` to `vaadin-message.current-user` to create a visual
separator above each user message (start of a new round). The
`:first-child` pseudo-class removes the separator from the first
message. Uses `--lumo-contrast-20pct` for the border color (works in
both light and dark themes) and `--lumo-space-s` for spacing.
