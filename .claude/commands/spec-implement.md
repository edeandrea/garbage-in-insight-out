---
description: Implement an approved task list one task at a time, checking off progress as it goes.
---
Given a spec slug:

1. Read `specs/<slug>/tasks.md`. If Status is not `Approved`, stop and ask
   the user to approve it first.
2. Work through unchecked tasks in order. For each task:
   a. Before implementing, write the implementation approach to
      `specs/<slug>/implementation.md` (keyed by task number).
   b. Implement the task.
   c. Add or update tests covering the behavior this task introduces or
      changes (per CLAUDE.md's testing rule, running the pre-existing
      suite alone is not enough).
   d. Run the full build/test suite.
   e. Mark it `- [x]` in tasks.md only once everything passes and the
      new behavior is actually covered.
3. If a task turns out to need a design decision not covered by plan.md,
   stop and ask rather than improvising. Record the decision in
   `specs/<slug>/decisions.md` and suggest updating plan.md if needed.
4. Go through everything yourself one more time looking for any outstanding
   questions, ambiguities, or missing requirements. If you find any, 
   ask the user for clarification 1 item at a time, and record those clarifications.
5. When all tasks are checked, summarize what was built and note anything
   that deviated from the plan.