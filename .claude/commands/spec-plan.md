---
description: Turn an approved spec into a technical design. Does not write application code.
---
Given a spec slug or path:

1. Read `specs/<slug>/spec.md`. If its Status is not `Approved`, stop and
   tell the user to review and approve the spec first.
2. Write `specs/<slug>/plan.md`: Status (Draft), architecture/approach,
   files to be created or changed, key interfaces/classes, and any
   tradeoffs or alternatives considered.
3. Show the user the file and STOP.
4. Go through it yourself one more time looking for any outstanding
   questions, ambiguities, or missing requirements. If you find any, 
   ask the user for clarification 1 item at a time, and record those clarifications.
5. Ask them to review, resolve any open
   questions from the spec if the plan can settle them, and set Status to
   Approved, or ask for `/spec-plan` again with feedback.
