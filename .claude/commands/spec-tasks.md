---
description: Break an approved plan into an ordered, checkable task list. Does not write application code.
---
Given a spec slug:

1. Read `specs/<slug>/plan.md`. If Status is not `Approved`, stop and ask
   the user to review it first.
2. Write `specs/<slug>/tasks.md`: Status (Draft), a numbered checklist of
   small, independently verifiable tasks (`- [ ] ...`), ordered by
   dependency.
3. Show the user the file and STOP. Ask them if they'd like to review.
4. If there are any ambiguities or questions, start a question/answer session with the user,
    1 question at a time.
5. Once all questions are resolved, tell the user to set Status to
   Approved, or run `/spec-implement` once ready.
