---
sidebar_position: 3
title: 👤 Jugador Types
---

# 👤 Jugador Types - Tipos de Jugador

> Interfaces del modelo de jugador: `Jugador`, `JugadorDatosResponse`

---

## 📍 Ubicación

`frontend/src/app/shared/models/jugador.ts`

---

## 📋 Interfaces

### Jugador

```typescript
export interface Jugador {
  id?: number;
  username: string;
  sobresDisponibles: number;
  santoCoins?: number;
  cantidadCartas?: number;
  nivel?: number;
  coleccion?: Card[];
  cartasObtenidas?: Card[];
  characterId?: string;
  skinColor?: string;
  hairColor?: string;
  eyeColor?: string;
  height?: number;
  pikachuCompanion?: boolean;
}
```

Estado principal del jugador dentro del frontend.

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `id` | `number` | No | ID interno del jugador |
| `username` | `string` | Sí | Nombre de usuario (único) |
| `sobresDisponibles` | `number` | Sí | Cantidad de sobres sin abrir |
| `santoCoins` | `number` | No | Moneda del juego |
| `cantidadCartas` | `number` | No | Total de cartas en colección |
| `nivel` | `number` | No | Nivel del jugador |
| `coleccion` | `Card[]` | No | Colección completa de cartas |
| `cartasObtenidas` | `Card[]` | No | Cartas recientemente obtenidas |
| `characterId` | `string` | No | ID del personaje (avatar) |
| `skinColor` | `string` | No | Color de piel del avatar |
| `hairColor` | `string` | No | Color de pelo del avatar |
| `eyeColor` | `string` | No | Color de ojos del avatar |
| `height` | `number` | No | Altura del avatar |
| `pikachuCompanion` | `boolean` | No | Tiene Pikachu como compañero |

---

### JugadorDatosResponse

```typescript
export interface JugadorDatosResponse {
  username: string;
  sobresDisponibles: number;
  santoCoins?: number;
  cantidadCartas: number;
  cartasObtenidas?: Card[];
  characterId?: string;
  skinColor?: string;
  hairColor?: string;
  eyeColor?: string;
  height?: number;
  pikachuCompanion?: boolean;
}
```

Respuesta resumida del backend para el header del lobby.

| Campo | Diferencia respecto a `Jugador` |
|-------|--------------------------------|
| `username` | Igual, pero requerido |
| `cantidadCartas` | **Requerido** (en `Jugador` es opcional) |
| `id` | **No existe** en este tipo |
| `nivel` | **No existe** en este tipo |
| `coleccion` | **No existe** en este tipo |

---

## 🔗 Relaciones

| Donde se usa | Tipo | Cómo |
|-------------|------|------|
| Auth responses (login) | `Jugador` | Objeto completo del jugador autenticado |
| Lobby header | `JugadorDatosResponse` | Datos rápidos para mostrar en la barra |
| `localStorage['jugador']` | `Jugador` | Persiste el usuario en sesión |
| `LobbyRoomService.headers()` | — | Lee `jugador.username` para el header `X-Username` |
| `BattleService.getHeaders()` | — | Lee `jugador.username` para el header `X-Username` |

---

*Próximo: [04-mazo-types.md](./04-mazo-types.md)*
