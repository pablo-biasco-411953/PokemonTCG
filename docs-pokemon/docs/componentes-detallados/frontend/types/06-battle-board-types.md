---
sidebar_position: 6
title: 🖥️ BattleBoard Types
---

# 🖥️ BattleBoard Types - Tipos de UI del Tablero

> Tipos de estado visual para el tablero de batalla: monedas, overlays, partículas y cartas hovereadas

---

## 📍 Ubicación

`frontend/src/app/features/battle/battle-board.types.ts`

---

## 🏗️ Características

Este archivo define tipos exclusivamente de UI. No modelan el estado del juego en el backend, sino el estado visual del componente del tablero:

- Animaciones de monedas
- Overlays entre turnos
- Efectos de partículas
- Estado hover de cartas
- Glosario de condiciones
- Extensiones de `Ataque` para el UI

---

## 📋 Tipos

### CoinSide

```typescript
export type CoinSide = '' | 'CARA' | 'CRUZ';
```

Resultado de una moneda:
- `''` — No lanzada / estado inicial
- `'CARA'` — Resultado cara
- `'CRUZ'` — Resultado cruz

---

### BattleBoardSide

```typescript
export type BattleBoardSide = 'jugador' | 'bot';
```

Identifica qué lado del tablero está involucrado.

---

### OverlaySide

```typescript
export type OverlaySide = BattleBoardSide | 'neutral';
```

Extiende `BattleBoardSide` con `'neutral'` para overlays que no pertenecen a ningún lado (ej: coin flip inicial).

---

### CoinFaceState

```typescript
export type CoinFaceState = 'girando' | 'cara' | 'cruz';
```

Estado visual de una moneda individual durante la animación.

| Estado | Descripción |
|--------|-------------|
| `'girando'` | La moneda está animada (girando) |
| `'cara'` | La moneda cayó en cara |
| `'cruz'` | La moneda cayó en cruz |

---

### DamageNumberState

```typescript
export interface DamageNumberState {
  valor: number;
  esCuracion: boolean;
}
```

Número de daño o curación que flota sobre un Pokémon.

| Campo | Descripción |
|-------|-------------|
| `valor` | Cantidad de daño o HP curado |
| `esCuracion` | `true` = curación (mostrar en verde), `false` = daño (en rojo) |

---

### CoinVisualState

```typescript
export interface CoinVisualState {
  estado: CoinFaceState;
}
```

Estado visual de una moneda individual en el overlay de coin flip.

---

### AttackCoinFlipState

```typescript
export interface AttackCoinFlipState {
  nombreAtaque: string;
  descripcion: string;
  cantidadMonedas: number;
  danioBase: number;
  danioExtraPorCara: number;
  monedas: CoinVisualState[];
  danioTotal: number;
  terminado: boolean;
  progreso: number;
  esSoloEstado: boolean;
}
```

Estado completo del overlay de animación de monedas para un ataque con coin flip.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `nombreAtaque` | `string` | Nombre del ataque o fase (ej: `'FASE DE MANTENIMIENTO'`) |
| `descripcion` | `string` | Texto descriptivo del efecto |
| `cantidadMonedas` | `number` | Cuántas monedas se lanzan |
| `danioBase` | `number` | Daño garantizado |
| `danioExtraPorCara` | `number` | Daño extra por cada CARA |
| `monedas` | `CoinVisualState[]` | Estado de cada moneda individual |
| `danioTotal` | `number` | Daño total calculado post-animación |
| `terminado` | `boolean` | La animación terminó |
| `progreso` | `number` | Índice de la moneda actual (0-based) |
| `esSoloEstado` | `boolean` | El efecto es solo condición, no daño numérico |

---

### InterTurnOverlayState

```typescript
export interface InterTurnOverlayState {
  titulo: string;
  subtitulo?: string;
  fase: string;
  tipo: OverlaySide;
  duracion: number;
}
```

Estado del overlay que aparece entre turnos o fases.

| Campo | Descripción |
|-------|-------------|
| `titulo` | Texto principal (ej: `'Turno del Bot'`) |
| `subtitulo` | Texto secundario opcional |
| `fase` | Identificador de la fase que provocó el overlay |
| `tipo` | A qué lado pertenece (`'jugador'`, `'bot'`, `'neutral'`) |
| `duracion` | Duración en ms |

---

### ParticleVisualState

```typescript
export interface ParticleVisualState {
  color: string;
  tx: number;
  ty: number;
  size: number;
  duracion: number;
}
```

Estado de una partícula visual en efectos de combate.

| Campo | Descripción |
|-------|-------------|
| `color` | Color CSS de la partícula |
| `tx` | Desplazamiento X final (translateX en px) |
| `ty` | Desplazamiento Y final (translateY en px) |
| `size` | Tamaño en px |
| `duracion` | Duración de la animación en ms |

---

### HoveredBattleCard

```typescript
export type HoveredBattleCard = Card | BattleActionCard | CartaEnJuego;
```

Tipo unión para representar cualquier carta que puede estar en estado hover en el tablero. Permite que el panel de detalle de carta acepte cualquier tipo.

---

### CardGlossaryEntry

```typescript
export interface CardGlossaryEntry {
  nombre: string;
  desc: string;
  css: string;
}
```

Entrada del glosario de condiciones especiales mostrado en la UI.

| Campo | Descripción |
|-------|-------------|
| `nombre` | Nombre de la condición (ej: `'Paralizado'`) |
| `desc` | Descripción corta del efecto |
| `css` | Clase CSS para el badge (ej: `'kw-paralyze'`) |

---

### BattleBoardAttack

```typescript
export type BattleBoardAttack = Ataque & {
  dano?: number | string;
};
```

Extensión de `Ataque` con campo adicional `dano` (sin acento, alternativa a `danio`). Usada para tolerar variaciones del backend donde el campo puede llegar con nombre diferente.

---

## 🔗 Relaciones

| Tipo | Consumido por |
|------|--------------|
| `CoinSide` | `BattleBoardTurnService` |
| `AttackCoinFlipState` | `BattleBoardTurnService.crearEstadoCoinFlip` |
| `CoinVisualState` | `AttackCoinFlipState.monedas` |
| `HoveredBattleCard` | Componentes del tablero (panel hover) |
| `CardGlossaryEntry` | `BattleBoardUiService.extraerGlosario` |
| `BattleBoardAttack` | Componentes de ataque del tablero |
| `InterTurnOverlayState` | Componentes de overlay |
| `ParticleVisualState` | Componentes de efectos visuales |

---

*Próximo: [07-lobby-types.md](./07-lobby-types.md)*
