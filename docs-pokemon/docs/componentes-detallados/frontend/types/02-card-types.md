---
sidebar_position: 2
title: 🃏 Card Types
---

# 🃏 Card Types - Tipos de Carta

> Interfaces base del sistema de cartas: `Card`, `Ataque`, `CardWeakness`

---

## 📍 Ubicación

`frontend/src/app/shared/models/card.ts`

---

## 📋 Interfaces

### CardWeakness

```typescript
export interface CardWeakness {
  tipo: string;
  valor?: string;
}
```

Representa una debilidad o resistencia expresada por tipo de energía.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `tipo` | `string` | Tipo de energía (`'Fire'`, `'Water'`, etc.) |
| `valor` | `string \| undefined` | Modificador (ej: `'×2'`, `'-20'`) |

---

### Ataque

```typescript
export interface Ataque {
  nombre: string;
  danio: number;
  texto: string;
  costo: string[];
}
```

Forma simplificada de un ataque consumida por el frontend (mapeada desde el JSON del backend).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `nombre` | `string` | Nombre del ataque (ej: `'Thunderbolt'`) |
| `danio` | `number` | Daño base del ataque |
| `texto` | `string` | Descripción/efecto del ataque (puede contener coin flips, condiciones) |
| `costo` | `string[]` | Array de tipos de energía requeridos (ej: `['Fire', 'Colorless']`) |

---

### Card

```typescript
export interface Card {
  id: string;
  nombre: string;
  tipo: string;
  hp: string;
  imagen: string;
  supertype?: string;
  evolvesFrom?: string | null;
  costoRetirada?: number;
  pokemonId?: number;
  attacks?: string;
  ataques?: Ataque[];
  subtypes?: string[];
  debilidades?: CardWeakness[];
  resistencias?: CardWeakness[];
  rarity?: string;
}
```

Modelo base de una carta tal como llega desde el backend.

| Campo | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `id` | `string` | — | Identificador único (ej: `'xy1-1'`, `'base1-4'`) |
| `nombre` | `string` | — | Nombre de la carta |
| `tipo` | `string` | — | Tipo primario de energía |
| `hp` | `string` | — | HP como string (ej: `'100'`) |
| `imagen` | `string` | — | Ruta de la imagen |
| `supertype` | `string \| undefined` | — | Categoría: `'Pokémon'`, `'Energy'`, `'Trainer'` |
| `evolvesFrom` | `string \| null \| undefined` | — | Nombre del Pokémon pre-evolución |
| `costoRetirada` | `number \| undefined` | — | Energías necesarias para retirarse |
| `pokemonId` | `number \| undefined` | — | Número de Pokédex |
| `attacks` | `string \| undefined` | — | JSON raw de ataques (legado) |
| `ataques` | `Ataque[] \| undefined` | — | Ataques mapeados |
| `subtypes` | `string[] \| undefined` | — | Subtipos (ej: `['Basic']`, `['Stage 1']`) |
| `debilidades` | `CardWeakness[] \| undefined` | — | Debilidades del Pokémon |
| `resistencias` | `CardWeakness[] \| undefined` | — | Resistencias del Pokémon |
| `rarity` | `string \| undefined` | — | Rareza (ej: `'Common'`, `'Rare Holo'`) |

### Valores comunes de supertype

| supertype | Descripción |
|-----------|-------------|
| `'Pokémon'` | Carta de Pokémon (con acento) |
| `'Pokemon'` | Variante sin acento (legacy) |
| `'Energy'` | Carta de energía |
| `'Trainer'` | Carta de entrenador |

---

## 🔗 Relaciones

`Card` es el tipo base utilizado por prácticamente todo el sistema:

| Donde se usa | Cómo |
|-------------|------|
| `Mazo.cartas` | Array de cartas del mazo |
| `CartaEnJuego.card` | Carta que está en juego |
| `CartaEnJuego.energiasUnidas` | Energías unidas al Pokémon |
| `TableroJugador.mano` | Mano del jugador |
| `TableroJugador.mazo` | Deck restante |
| `TableroJugador.premios` | Cartas de premio |
| `TableroJugador.pilaDescarte` | Descarte |
| `Jugador.coleccion` | Colección completa del jugador |
| `BattleBoardAttack` | Extiende `Ataque` con campo `dano` |

---

*Próximo: [03-jugador-types.md](./03-jugador-types.md)*
