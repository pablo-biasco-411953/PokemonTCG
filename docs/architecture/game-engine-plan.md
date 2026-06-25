# Game Engine Plan

## Goal

Refactor the game rules so the backend is the only source of truth for:

- turn flow
- attack validation
- coin flips
- weakness and resistance
- special conditions
- prize flow
- KO resolution
- pending decisions
- temporary restrictions

The frontend must only render server state, send player intent, and animate results.

## Non-negotiable rules

1. No game calculation in frontend.
2. No hidden frontend-only rule state.
3. REST and WebSocket are transport only.
4. The engine must be testable without Angular, HTTP, or sockets.
5. Every card effect family must map to a backend rule component.

## Target architecture

### 1. Facade

`GameEngineFacade`

Single internal entry point used by controllers, room services, bot service, and persistence orchestration.

Responsibilities:

- start match
- restore match
- validate player action
- execute action
- emit domain events
- return updated authoritative state

Controllers should not know how attacks, conditions, prizes, or turn transitions work.

### 2. State

Two state axes:

- `MatchState`
  - `WAITING`
  - `SETUP`
  - `ACTIVE`
  - `FINISHED`
- `TurnState`
  - `DRAW`
  - `MAIN`
  - `ATTACK`
  - `BETWEEN_TURNS`
  - extra setup substates when needed

State objects decide:

- which commands are legal
- what happens next
- what automatic maintenance runs

### 3. Strategy

Use strategies for card behavior, not giant `if` chains.

Main families:

- `AttackEffectStrategy`
- `TrainerEffectStrategy`
- `SpecialConditionStrategy`
- `CoinResolutionStrategy`

Examples:

- draw cards
- discard attached energy
- search deck
- recover from discard
- switch active
- apply next-turn restriction
- bench damage

Each strategy should be backend-only and deterministic except where randomness is explicitly requested by the rules engine.

### 4. Chain of Responsibility

Attack resolution should be a backend pipeline matching the consigna:

1. energy validation
2. confusion / attack-start restrictions
3. player selections / pending decisions
4. pre-attack effects
5. modifiers
6. damage calculation
7. post-damage effects

Suggested handlers:

- `EnergyValidationHandler`
- `AttackStartRestrictionHandler`
- `TargetSelectionHandler`
- `PreAttackEffectHandler`
- `DamageModifierHandler`
- `DamageResolutionHandler`
- `PostDamageEffectHandler`
- `KoAndPrizeHandler`

This is the right place for:

- weakness and resistance
- all-or-nothing coin flips
- protections for next turn
- effects that change what happens after damage lands

### 5. Observer

Introduce explicit domain events from the engine:

- `AttackDeclared`
- `CoinFlipResolved`
- `DamageApplied`
- `ConditionApplied`
- `PokemonKnockedOut`
- `PrizeTaken`
- `PendingDecisionOpened`
- `TurnEnded`
- `MatchFinished`

WebSocket broadcasting should subscribe to these events. The engine should not know who is listening.

### 6. Repository

Repositories stay responsible for loading and saving:

- cards
- decks
- players
- persisted match snapshots

The engine should depend on repository interfaces, not transport or controller code.

## Current project direction

The project already has useful seeds:

- `Partida` as authoritative match state
- command-based attack resolution
- pending action support
- turn log stream
- backend coin storage in `ultimasMonedasLanzadas`

We should evolve this, not throw it away.

## Immediate refactor roadmap

### Phase 1: Stabilize source of truth

- move all remaining battle calculations out of frontend
- make pending selections fully backend-driven
- sync coin flip event metadata for attacker, rival, and spectator
- finish attack families already half-implemented

### Phase 2: Separate rule families

- extract attack effect families into strategies
- move temporary rule flags into explicit engine state
- isolate turn transitions into state classes

### Phase 3: Formalize attack pipeline

- convert current attack command flow into handler chain
- add tests per handler and per XY1 family
- stop resolving complex effects with scattered ad hoc checks

### Phase 4: Event-driven outbound updates

- emit domain events
- adapt WebSocket / room notifications as observers
- keep REST for command submission and state hydration

## Suggested folder direction

Possible long-term backend structure:

```text
backend/src/main/java/com/pokemon/tcg/game/
  engine/
    GameEngineFacade.java
  state/
    match/
    turn/
  strategy/
    attack/
    trainer/
    status/
  chain/
    attack/
  events/
  repository/
  model/
```

No need to rename everything immediately. The point is to move toward a stable boundary.

## Audit workflow

The file `docs/card-audit/xy1-effect-audit.tsv` is the working memory for card-by-card review.

Rules:

1. Review in the TSV, not in chat.
2. Mark `fully_reviewed=yes` only when:
   - backend family is identified
   - implementation path is clear
   - test expectation is known
3. Mark `verification_status=verified` only after backend test or manual proof.
4. Prefer closing families, not random isolated cards.

## Definition of done for a card effect

A card effect is only done when all are true:

- backend resolves it authoritatively
- frontend only renders server output
- logs and events are emitted correctly
- pending choices are validated by backend
- rival and spectator receive consistent state
- test exists for the effect family or the exact card

## Token-saving operating mode

To save session tokens:

- use this document as architecture memory
- use the TSV as progress memory
- use the backlog file as execution memory

Then future chats only need to read these files and continue from there.
