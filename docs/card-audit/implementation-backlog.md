# XY1 Implementation Backlog

## Purpose

Track effect families instead of discussing individual cards from memory.

Main source files:

- `docs/card-audit/xy1-effect-audit.tsv`
- `docs/architecture/game-engine-plan.md`

## Current baseline

- Card catalog audit base generated from XY1 attack text.
- Backend already has partial support for:
  - damage
  - some coin flips
  - draw
  - search deck
  - healing
  - self-damage
  - discard attached energy
  - discard to top deck
  - one attack restriction family

## Priority order

### P0 - Source of truth integrity

- [ ] Remove remaining frontend rule decisions.
- [ ] Ensure every coin-flip result shown in UI comes from backend state.
- [ ] Ensure spectator/rival see the same resolution timing.
- [ ] Ensure no turn can advance while a backend-triggered coin event is unresolved in UI.

### P1 - Attack pipeline alignment

- [ ] Formalize attack-resolution handler chain.
- [ ] Move pre-attack restrictions into dedicated chain step.
- [ ] Centralize weakness and resistance in backend damage step.
- [ ] Centralize all-or-nothing attack logic in backend.

### P2 - XY1 families by batch

#### Family: coin-flip

- [ ] heads for extra damage
- [ ] damage times number of heads
- [ ] until tails
- [ ] heads/tails split outcomes
- [ ] flip before defending Pokemon can attack

#### Family: search / guided selection

- [ ] search deck to hand
- [ ] search deck and attach
- [ ] choose target from bench
- [ ] choose card from discard
- [ ] reorder top of deck

#### Family: discard / recovery

- [ ] discard one attached energy
- [ ] discard typed energy
- [ ] discard all attached energy
- [ ] recover from discard to hand
- [ ] recover from discard to top of deck

#### Family: turn restrictions

- [x] active Pokemon cannot attack during its next turn (Rhyperior xy1-62, Yveltal xy1-78)
- [x] active Pokemon cannot use a specific attack during its next turn (Aegislash xy1-86)
- [x] defending Pokemon cannot attack during opponent's next turn (Wigglytuff xy1-89)
- [ ] defending Pokemon cannot use a chosen attack (Simisage xy1-11)
- [ ] attack fails on tails during next turn
- [ ] supporter lock and similar future families

#### Family: board impact

- [x] bench damage to opponent's benched (Ledian xy1-7, M Blastoise-EX xy1-30, Trevenant xy1-55, Stoutland xy1-110, Xerneas-EX xy1-97)
- [x] self bench damage (Dugtrio xy1-59)
- [x] bench scaling damage (Raichu xy1-43)
- [ ] switch effects
- [ ] forced switch on opponent

### P3 - Trainer cards and non-attack effects

- [ ] Audit Item effects
- [ ] Audit Supporter effects
- [ ] Audit Stadium effects
- [ ] Decide whether each uses Strategy, PendingAction, or dedicated chain step

## Review statuses

Use these values in the TSV:

- `implementation_status`
  - `todo`
  - `partial`
  - `partial-or-recent`
  - `implemented`
  - `blocked`
- `verification_status`
  - `not-reviewed`
  - `needs-test`
  - `manual-pass`
  - `verified`
- `fully_reviewed`
  - `no`
  - `yes`

## Review protocol for one batch

1. Filter one family in the TSV.
2. Confirm expected official behavior from XY1 rule text and card text.
3. Map family to backend owner:
   - `Strategy.AttackEffect`
   - `Chain.PreAttack`
   - `Chain.DamageModifiers`
   - `PendingAction.*`
4. Implement in backend.
5. Add or update tests.
6. Mark affected rows:
   - `implementation_status=implemented`
   - `verification_status=verified`
   - `fully_reviewed=yes`

## Recommended first batches

1. `attack-restriction`
2. `recover-discard-topdeck`
3. `bench-damage`
4. `deck-ordering`
5. `optional-effect`

These are the families most likely to keep causing weird edge cases if they remain ad hoc.
