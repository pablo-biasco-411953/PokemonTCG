---
sidebar_position: 4
title: 🗃️ Mazo Types
---

# 🗃️ Mazo Types - Tipos de Mazo

> Interfaces del modelo de mazo: `Mazo`, `MazoJugadorResumen`

---

## 📍 Ubicación

`frontend/src/app/shared/models/mazo.ts`

---

## 📋 Interfaces

### MazoJugadorResumen

```typescript
export interface MazoJugadorResumen {
  id: number;
  username: string;
  sobresDisponibles: number;
}
```

Resumen mínimo del propietario de un mazo, embebido en `Mazo`.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `number` | ID del jugador |
| `username` | `string` | Nombre de usuario |
| `sobresDisponibles` | `number` | Cantidad de sobres disponibles |

---

### Mazo

```typescript
export interface Mazo {
  id: number;
  nombre: string;
  cartas: Card[];
  jugador?: MazoJugadorResumen;
}
```

Modelo de mazo usado por el lobby y el deck builder.

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `id` | `number` | Sí | ID único del mazo |
| `nombre` | `string` | Sí | Nombre del mazo |
| `cartas` | `Card[]` | Sí | Cartas que componen el mazo |
| `jugador` | `MazoJugadorResumen` | No | Propietario del mazo |

---

## 🔗 Relaciones

| Donde se usa | Cómo |
|-------------|------|
| `MazoService` | CRUD de mazos (`getMazos`, `createMazo`, etc.) |
| `LobbyRoomService.createRoom` | `mazoId: number` para identificar el mazo usado |
| `LobbyRoomService.joinRoom` | `mazoId: number` |
| `LobbyRoomService.setReady` | `mazoId: number \| null` |
| `BattleService.startBattle` | `mazoId: number` |
| Deck Builder | Edición y visualización del mazo |
| Lobby (lista de salas) | `ownerDeckName`, `guestDeckName` vienen de `Mazo.nombre` |

---

*Próximo: [05-battle-types.md](./05-battle-types.md)*
