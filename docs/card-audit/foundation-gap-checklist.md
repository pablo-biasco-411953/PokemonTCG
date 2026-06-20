# Foundation Gap Checklist

This file tracks engine capabilities that must exist before many XY1 cards can work correctly.

## Rule clarifications

These are **not** default player actions:

- attaching Energy to a Benched Pokemon
- attacking a Benched Pokemon
- switching a rival Bench target of your choice
- reordering deck cards
- choosing optional branches of an effect

They only happen when a card effect explicitly enables them.

That means they belong in the backend engine as effect-driven capabilities, not as general frontend controls.

## Current foundational gaps to verify and close

### 1. Bench targeting

- [ ] Backend can select exactly 1 rival Benched Pokemon as attack target.
- [ ] Backend can select 2 rival Benched Pokemon when an effect requires it.
- [ ] Backend can apply bench damage without Weakness/Resistance by default unless text says otherwise.
- [ ] Frontend can only present legal targets coming from backend.

Used by examples:

- `Ledian - Mach Punch`
- `M Blastoise-EX - Hydro Bombard`
- `Yveltal - Oblivion Wing` style families when extended

### 2. Effect-driven attach to bench

- [ ] Backend can attach Energy to Bench only when an effect opens that action.
- [ ] Backend can validate legal destination Bench slots.
- [ ] Backend can support choosing one or multiple Bench targets.
- [ ] Frontend can render backend-issued pending selection for Bench attach.

Used by examples:

- Water Energy from discard to Benched Pokemon
- Fairy Energy search and attach to chosen Bench Pokemon

### 3. Forced switch / gust effects

- [ ] Backend can choose opponent Bench target and promote it.
- [ ] Backend can choose own Bench target and switch active.
- [ ] Backend can support chained switch effects in one resolution.

Used by examples:

- `Volbeat - Luring Glow`
- `Blastoise-EX - Rapid Spin`

### 4. Deck ordering and inspection

- [ ] Backend can expose top-N deck cards as temporary hidden decision state.
- [ ] Backend can accept reordered result and commit it authoritatively.
- [ ] Frontend must not reorder locally without backend-issued pending action.

Used by examples:

- `Braixen - Clairvoyant Eye`

### 5. Optional decisions

- [ ] Backend can issue yes/no optional-effect prompts.
- [ ] Backend can branch effect resolution based on player decision.

Used by examples:

- attacks with `You may do 20 more damage...`
- attacks with `You may discard...`

### 6. Per-attack restriction state

- [ ] Backend can block one specific named attack on next turn.
- [ ] Backend can block an entire attack attempt pending coin flip.
- [ ] Restriction state expires at correct timing.

Used by examples:

- `Simisage - Torment`
- attacks with `If the Defending Pokemon tries to attack during your opponent's next turn...`

### 7. Observer/event correctness

- [ ] Coin flips emit authoritative events.
- [ ] Rival and spectator see the same event sequence.
- [ ] No client can advance action flow while backend-issued flip event is unresolved in UI.

## Recommended execution order

1. Bench targeting
2. Forced switch
3. Optional decision prompts
4. Deck ordering
5. Effect-driven Bench attach
6. Per-attack restriction state
7. Re-run XY1 audit rows affected by each capability
