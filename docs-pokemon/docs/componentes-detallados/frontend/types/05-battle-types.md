---
sidebar_position: 5
title: ⚔️ Battle Types
---

# ⚔️ Battle Types - Tipos de Batalla

> Interfaces del sistema de batalla: `CartaEnJuego`, `TableroJugador`, `Partida`, `BattleActionCard`

---

## 📍 Ubicación

`frontend/src/app/shared/models/battle.ts`

---

## 📋 Interfaces

### CartaEnJuego

```typescript
export interface CartaEnJuego {
  card: Card;
  hpActual: number;
  energiasUnidas: Card[];
  puedeAtacar: boolean;
  condicionesEspeciales: string[];
  invulnerable?: boolean;
  bocaAbajo?: boolean;
}
```

Un Pokémon ya puesto en juego con su estado en tiempo real.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `card` | `Card` | Datos base de la carta |
| `hpActual` | `number` | HP restante |
| `energiasUnidas` | `Card[]` | Energías unidas a este Pokémon |
| `puedeAtacar` | `boolean` | Si puede atacar en este momento |
| `condicionesEspeciales` | `string[]` | Condiciones activas: `'ASLEEP'`, `'PARALYZED'`, `'CONFUSED'`, `'POISONED'`, `'BURNED'` |
| `invulnerable` | `boolean \| undefined` | Inmune a daño este turno |
| `bocaAbajo` | `boolean \| undefined` | Carta boca abajo (durante setup) |

---

### TableroJugador

```typescript
export interface TableroJugador {
  mazo: Card[];
  mano: Card[];
  premios: Card[];
  activo: CartaEnJuego | null;
  banca: CartaEnJuego[];
  pilaDescarte: Card[];
}
```

Zonas visibles de un jugador durante la partida.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `mazo` | `Card[]` | Deck restante (boca abajo) |
| `mano` | `Card[]` | Cartas en mano |
| `premios` | `Card[]` | 6 cartas de premio (boca abajo) |
| `activo` | `CartaEnJuego \| null` | Pokémon en posición activa |
| `banca` | `CartaEnJuego[]` | Pokémon en banca (0-5) |
| `pilaDescarte` | `Card[]` | Pila de descarte |

---

### Partida

Estado completo de una partida sincronizado con el backend.

```typescript
export interface Partida {
  id: string;
  jugador: TableroJugador;
  bot: TableroJugador;
  turnoActual: 'JUGADOR' | 'BOT';
  faseActual: FasePartida;
  numeroTurno?: number;
  yaSeRetiroEsteTurno: boolean;
  ultimasMonedasLanzadas: boolean[];
  jugadorUsername?: string;
  botUsername?: string;
  ganador?: string;
  razonFinPartida?: string;
  // Coin flip
  coinFlipped?: boolean;
  coinFlipWinner?: string;
  coinFlipResult?: string;
  coinFlipCallerUsername?: string;
  coinHandshakeJugadorPower?: number;
  coinHandshakeBotPower?: number;
  coinHandshakeJugadorHolding?: boolean;
  coinHandshakeBotHolding?: boolean;
  coinHandshakeComplete?: boolean;
  // Logs y loading
  turnLogs?: string[];
  jugadorLoadingPercentage?: number;
  botLoadingPercentage?: number;
  // Setup
  setupJugadorListo?: boolean;
  setupBotListo?: boolean;
  mulligansJugador?: number;
  mulligansBot?: number;
  cartasMulliganExtraPendientesJugador?: number;
  cartasMulliganExtraPendientesBot?: number;
  setupJugadorRoboExtraMulligan?: boolean;
  setupBotRoboExtraMulligan?: boolean;
}
```

#### Campos base

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `string` | UUID de la partida |
| `jugador` | `TableroJugador` | Tablero del jugador humano |
| `bot` | `TableroJugador` | Tablero del bot/oponente |
| `turnoActual` | `'JUGADOR' \| 'BOT'` | A quién le toca |
| `faseActual` | `FasePartida` | Fase del juego (ver tabla abajo) |
| `numeroTurno` | `number` | Número de turno |
| `yaSeRetiroEsteTurno` | `boolean` | El jugador ya usó la retirada este turno |
| `ultimasMonedasLanzadas` | `boolean[]` | Resultados de las últimas monedas (true = CARA) |

#### Ciclo de vida: las 14 fases

| `faseActual` | Descripción |
|-------------|-------------|
| `'INICIO'` | Partida recién creada |
| `'LANZAMIENTO_MONEDA'` | Coin flip para decidir quién empieza |
| `'SETUP_INITIAL_DRAW'` | Robo inicial de 7 cartas |
| `'SETUP_MULLIGAN_EVALUATION'` | Evaluar si hay Pokémon básico en mano |
| `'SETUP_MULLIGAN_REVEAL'` | Mostrar que se necesita mulligan |
| `'SETUP_PLACE_ACTIVE'` | Elegir Pokémon activo inicial |
| `'SETUP_PLACE_BENCH'` | Colocar Pokémon en banca |
| `'SETUP_PRIZE_PLACEMENT'` | Colocar 6 cartas de premio |
| `'SETUP_MULLIGAN_EXTRA_DRAW'` | Robo extra por mulligans del rival |
| `'SETUP_PLACE_BENCH_EXTRA'` | Banca extra post-mulligan |
| `'SETUP_REVEAL'` | Revelar setup al oponente |
| `'ESPERANDO_INTERACCION'` | Esperando acción del jugador o bot |
| `'TURNO_NORMAL'` | Turno de juego normal |
| `'FIN_PARTIDA'` | Partida terminada |

```
INICIO
  ↓
LANZAMIENTO_MONEDA
  ↓
SETUP_INITIAL_DRAW → SETUP_MULLIGAN_EVALUATION → (mulligan?) SETUP_MULLIGAN_REVEAL
  ↓                                                               ↓
SETUP_PLACE_ACTIVE ←──────────────────────────────────────────────
  ↓
SETUP_PLACE_BENCH → SETUP_PRIZE_PLACEMENT → (rival mulligan?) SETUP_MULLIGAN_EXTRA_DRAW
  ↓                                                                ↓
SETUP_REVEAL ←──────────────────────────────────────────────────────
  ↓
ESPERANDO_INTERACCION / TURNO_NORMAL (loop)
  ↓
FIN_PARTIDA
```

#### Campos de coin flip (opcionales)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `coinFlipped` | `boolean` | Ya se lanzó la moneda |
| `coinFlipWinner` | `string` | Username del ganador |
| `coinFlipResult` | `string` | `'CARA'` o `'CRUZ'` |
| `coinFlipCallerUsername` | `string` | Quién llamó la moneda |
| `coinHandshakeJugadorPower` | `number` | Potencia del handshake (0-100) |
| `coinHandshakeBotPower` | `number` | Potencia del bot |
| `coinHandshakeJugadorHolding` | `boolean` | Jugador sosteniendo el botón |
| `coinHandshakeBotHolding` | `boolean` | Bot sosteniendo el botón |
| `coinHandshakeComplete` | `boolean` | Handshake completado |

#### Campos de setup (opcionales)

| Campo | Descripción |
|-------|-------------|
| `setupJugadorListo` | El jugador confirmó su setup |
| `setupBotListo` | El bot confirmó su setup |
| `mulligansJugador` | Cantidad de mulligans del jugador |
| `mulligansBot` | Cantidad de mulligans del bot |
| `cartasMulliganExtraPendientesJugador` | Cartas extra pendientes de robar |
| `cartasMulliganExtraPendientesBot` | Cartas extra del bot pendientes |
| `setupJugadorRoboExtraMulligan` | El jugador ya robó las extras |
| `setupBotRoboExtraMulligan` | El bot ya robó las extras |

---

### BattleActionCard

```typescript
export interface BattleActionCard {
  id: string;
  nombre: string;
  tipo?: string;
  hp?: string;
  ataques?: Ataque[];
  supertype?: string;
  evolvesFrom?: string | null;
}
```

Vista simplificada de carta para operaciones de acción en batalla. Subconjunto de `Card` usado cuando el contexto no requiere todos los campos.

| Campo | Tipo | Diferencia vs `Card` |
|-------|------|---------------------|
| `id` | `string` | Igual |
| `nombre` | `string` | Igual |
| `tipo` | `string` | Opcional (en `Card` es requerido) |
| `hp` | `string` | Opcional (en `Card` es requerido) |
| `ataques` | `Ataque[]` | Igual |
| `supertype` | `string` | Opcional (igual) |
| `evolvesFrom` | `string \| null` | Igual |
| `imagen` | — | **No existe** |
| `debilidades` | — | **No existe** |

---

### StartBattleResponse

```typescript
export interface StartBattleResponse extends Partida {}
```

Alias vacío de `Partida` para tipado semántico del retorno de `startBattle`.

---

## 🔗 Relaciones

| Tipo | Consumido por |
|------|--------------|
| `CartaEnJuego` | `BattleBoardStateService`, `BattleBoardTurnService`, componentes |
| `TableroJugador` | Componentes del tablero |
| `Partida` | Todos los servicios BattleBoard, `BattleService` |
| `BattleActionCard` | `BattleBoardActionService`, `BattleBoardStateService` |

---

*Próximo: [06-battle-board-types.md](./06-battle-board-types.md)*
