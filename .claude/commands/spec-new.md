---
description: Start a new feature spec through requirements gathering. Does not write application code.
---
You are starting a new feature spec. Do not write or edit any application
source code in this command.

1. If the user's request doesn't already make it clear, ask clarifying
   questions about scope, constraints, and what's explicitly out of scope.
   One question at a time is fine.
2. Once you have enough to write a clear spec, create
   `specs/<next-number>-<slug>/spec.md` with these sections: Status
   (Draft), Summary, Motivation, Requirements (numbered, testable
   statements), Out of scope, Open questions.
3. Show the user the file and STOP. Do not proceed to planning or
   implementation.
4. Go through it yourself one more time looking for any outstanding
   questions, ambiguities, or missing requirements. If you find any, 
   ask the user for clarification 1 item at a time, and record those clarifications.
5. Ask them to review it and either ask for changes, or
   run `/spec-plan <slug>` once they've set Status to Approved.
