# Card Audit Files

This folder exists to save project memory outside the chat.

## Files

- `xy1-effect-audit.tsv`
  - working table for card-by-card review
- `xy1-source-of-truth.tsv`
  - exact extraction from `backend/src/main/resources/cards.json`
  - includes attacks, abilities, and rules with required engine capability
- `implementation-backlog.md`
  - execution backlog by effect family
- `foundation-gap-checklist.md`
  - foundational backend capabilities missing or needing verification
- `../architecture/game-engine-plan.md`
  - architecture target aligned with the course requirements

## How to use

Do not review cards from chat history.

Instead:

1. open the TSV
2. check exact source text in `xy1-source-of-truth.tsv`
3. identify missing engine capability from `foundation-gap-checklist.md`
4. implement in backend
5. add tests
6. mark rows as reviewed and verified

This keeps future sessions cheap in tokens and stable in direction.
