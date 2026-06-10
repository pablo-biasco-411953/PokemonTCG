---
sidebar_position: 7
title: 🏟️ Lobby Types
---

# 🏟️ Lobby Types - Tipos de Lobby

> Tipos de sala multijugador: `LobbyRoomSnapshot`, `LobbyRoomStatus`, `LobbyRoomChatMessage`, `LobbyRoomReaction`, `LobbyRoomStartResponse`

---

## ⚠️ Ubicación Especial

> **Estos tipos están declarados inline en `lobby-room.service.ts`, no en `shared/models/`.**

```
frontend/src/app/features/lobby/services/lobby-room.service.ts
```

Esto es una excepción al patrón del proyecto donde los tipos compartidos viven en `shared/models/`. Los tipos de lobby están colocalizados con su servicio porque solo son consumidos por los componentes del módulo lobby.

---

## 📋 Tipos

### LobbyRoomStatus

```typescript
export type LobbyRoomStatus = 'OPEN' | 'IN_PROGRESS' | 'FINISHED';
```

Estado del ciclo de vida de una sala.

| Valor | Descripción |
|-------|-------------|
| `'OPEN'` | Sala disponible, acepta jugadores |
| `'IN_PROGRESS'` | Partida en curso |
| `'FINISHED'` | Partida finalizada |

---

### LobbyRoomChatMessage

```typescript
export interface LobbyRoomChatMessage {
  sender: string;
  text: string;
  sentAt: number;
  system: boolean;
}
```

Mensaje de chat dentro de una sala.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `sender` | `string` | Username del emisor |
| `text` | `string` | Contenido del mensaje |
| `sentAt` | `number` | Unix timestamp en ms |
| `system` | `boolean` | `true` = mensaje del sistema (ej: "Juan se unió"), `false` = mensaje de jugador |

---

### LobbyRoomReaction

```typescript
export interface LobbyRoomReaction {
  id: string;
  sender: string;
  reaction: string;
  sentAt: number;
}
```

Reacción emoji dentro de una sala o partida.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `string` | ID único de la reacción |
| `sender` | `string` | Username del emisor |
| `reaction` | `string` | Código o emoji de la reacción |
| `sentAt` | `number` | Unix timestamp en ms |

---

### LobbyRoomSnapshot

```typescript
export interface LobbyRoomSnapshot {
  id: string;
  name: string;
  status: LobbyRoomStatus;
  locked: boolean;
  ownerUsername: string;
  ownerDeckName: string;
  ownerReady: boolean;
  guestUsername?: string | null;
  guestDeckName?: string | null;
  guestReady: boolean;
  guestBot: boolean;
  playerCount: number;
  spectatorCount: number;
  matchId?: string | null;
  canJoin: boolean;
  canSpectate: boolean;
  currentUserSpectator: boolean;
  updatedAt: number;
  chat: LobbyRoomChatMessage[];
  reactions: LobbyRoomReaction[];
}
```

Estado completo de una sala en un momento dado. Es el tipo principal del módulo de lobby.

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `id` | `string` | Sí | UUID de la sala |
| `name` | `string` | Sí | Nombre de la sala |
| `status` | `LobbyRoomStatus` | Sí | Estado actual |
| `locked` | `boolean` | Sí | Protegida con contraseña |
| `ownerUsername` | `string` | Sí | Dueño de la sala |
| `ownerDeckName` | `string` | Sí | Nombre del mazo del dueño |
| `ownerReady` | `boolean` | Sí | Dueño marcó listo |
| `guestUsername` | `string \| null` | No | Invitado actual |
| `guestDeckName` | `string \| null` | No | Mazo del invitado |
| `guestReady` | `boolean` | Sí | Invitado marcó listo |
| `guestBot` | `boolean` | Sí | El invitado es un bot |
| `playerCount` | `number` | Sí | Cantidad de jugadores (0-2) |
| `spectatorCount` | `number` | Sí | Espectadores activos |
| `matchId` | `string \| null` | No | ID de la partida activa |
| `canJoin` | `boolean` | Sí | El usuario actual puede unirse |
| `canSpectate` | `boolean` | Sí | El usuario puede espectear |
| `currentUserSpectator` | `boolean` | Sí | El usuario ya es espectador |
| `updatedAt` | `number` | Sí | Unix timestamp de última actualización |
| `chat` | `LobbyRoomChatMessage[]` | Sí | Historial de mensajes |
| `reactions` | `LobbyRoomReaction[]` | Sí | Historial de reacciones |

---

### LobbyRoomStartResponse

```typescript
export interface LobbyRoomStartResponse {
  room: LobbyRoomSnapshot;
  matchId: string;
}
```

Respuesta al iniciar o espectar una partida.

| Campo | Descripción |
|-------|-------------|
| `room` | Snapshot actualizado de la sala |
| `matchId` | ID para navegar a `/battle/:matchId` |

Retornado por `startRoom` y `spectateRoom`.

---

## 🔄 Diagrama de Estados de Sala

```
OPEN ──(otro jugador se une)──> OPEN (playerCount=2, canJoin=false)
  │                                │
  │                                │ (ambos setReady + startRoom)
  │                                ▼
  │                          IN_PROGRESS
  │                                │
  │                                │ (partida termina)
  │                                ▼
  └──(alguien deja)──────────> FINISHED
```

Estados completos del campo `status`:

| Status | playerCount | Descripción |
|--------|-------------|-------------|
| `'OPEN'` | 0-2 | Sala abierta; `canJoin=true` si `playerCount < 2` |
| `'IN_PROGRESS'` | 2 | Partida activa; `matchId` tiene valor |
| `'FINISHED'` | — | Partida terminada; sala cerrada |

---

## 🔗 Relaciones

| Tipo | Retornado por |
|------|--------------|
| `LobbyRoomSnapshot` | `listRooms`, `createRoom`, `joinRoom`, `leaveRoom`, `kickGuest`, `addBot`, `setReady`, `sendChat`, `sendReaction`, `sendMatchReaction` |
| `LobbyRoomStartResponse` | `startRoom`, `spectateRoom` |

---

*Ver también: [07-lobby-room-service.md](../services/07-lobby-room-service.md)*
