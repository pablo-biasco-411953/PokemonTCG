---
sidebar_position: 7
title: 🏟️ LobbyRoomService
---

# 🏟️ LobbyRoomService - Servicio de Sala de Lobby

> Gestión completa de salas multijugador: creación, unión, chat, reacciones y arranque de partidas

---

## 📍 Ubicación

`frontend/src/app/features/lobby/services/lobby-room.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class LobbyRoomService {
  private base = `${getBackendUrl()}/api/lobby-rooms`;

  constructor(private http: HttpClient) {}
}
```

**Tipo**: Servicio raíz  
**Dependencias**: HttpClient, HttpHeaders  
**Feature**: Lobby (Salas multijugador)  
**Autenticación**: Header `X-Username` extraído de `localStorage`

---

## 🔑 Patrón de Autenticación: X-Username desde localStorage

Todos los métodos del servicio invocan el helper privado `headers()` que construye el header de autenticación sin token JWT, usando directamente el `localStorage`:

```typescript
private headers(): { headers: HttpHeaders } {
  let username = '';
  try {
    const data = localStorage.getItem('jugador');
    username = data ? JSON.parse(data).username || '' : '';
  } catch {
    username = '';
  }
  return { headers: new HttpHeaders({ 'X-Username': username }) };
}
```

**Por qué importa**:
- El backend identifica al jugador por el header `X-Username`, no por un token Bearer
- Si `localStorage` no tiene el campo `jugador`, el header se envía vacío (`''`)
- El bloque `try/catch` absorbe silenciosamente errores de parse de JSON
- Este patrón es idéntico al de `BattleService` y es el estándar para todo el módulo de lobby/battle

---

## 📋 Tipos Inline

Los tipos de este servicio están declarados **en el mismo archivo** `lobby-room.service.ts`, no en `shared/models/`.

### LobbyRoomStatus

```typescript
export type LobbyRoomStatus = 'OPEN' | 'IN_PROGRESS' | 'FINISHED';
```

| Valor | Descripción |
|-------|-------------|
| `'OPEN'` | Sala abierta, acepta jugadores |
| `'IN_PROGRESS'` | Partida en curso |
| `'FINISHED'` | Partida finalizada |

### LobbyRoomChatMessage

```typescript
export interface LobbyRoomChatMessage {
  sender: string;
  text: string;
  sentAt: number;    // Unix timestamp (ms)
  system: boolean;   // true = mensaje del sistema
}
```

### LobbyRoomReaction

```typescript
export interface LobbyRoomReaction {
  id: string;
  sender: string;
  reaction: string;  // emoji o código de reacción
  sentAt: number;    // Unix timestamp (ms)
}
```

### LobbyRoomSnapshot

Estado completo de una sala en un momento dado:

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
  updatedAt: number;            // Unix timestamp (ms)
  chat: LobbyRoomChatMessage[];
  reactions: LobbyRoomReaction[];
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `string` | UUID de la sala |
| `name` | `string` | Nombre visible de la sala |
| `status` | `LobbyRoomStatus` | Estado actual |
| `locked` | `boolean` | Sala protegida con contraseña |
| `ownerUsername` | `string` | Dueño de la sala |
| `ownerDeckName` | `string` | Nombre del mazo del dueño |
| `ownerReady` | `boolean` | El dueño marcó listo |
| `guestUsername` | `string \| null \| undefined` | Invitado (si hay) |
| `guestDeckName` | `string \| null \| undefined` | Mazo del invitado |
| `guestReady` | `boolean` | El invitado marcó listo |
| `guestBot` | `boolean` | El invitado es un bot |
| `playerCount` | `number` | Jugadores en sala (0-2) |
| `spectatorCount` | `number` | Espectadores activos |
| `matchId` | `string \| null \| undefined` | ID de partida activa |
| `canJoin` | `boolean` | El usuario actual puede unirse |
| `canSpectate` | `boolean` | El usuario actual puede espectear |
| `currentUserSpectator` | `boolean` | El usuario ya es espectador |
| `updatedAt` | `number` | Timestamp de última actualización |
| `chat` | `LobbyRoomChatMessage[]` | Historial de chat |
| `reactions` | `LobbyRoomReaction[]` | Historial de reacciones |

### LobbyRoomStartResponse

```typescript
export interface LobbyRoomStartResponse {
  room: LobbyRoomSnapshot;
  matchId: string;
}
```

Retornado al iniciar o espectar una partida. Contiene el snapshot actualizado de la sala y el `matchId` para redirigir al tablero de batalla.

---

## 📡 Métodos

### Tabla de Referencia Rápida

| Método | Firma | Endpoint HTTP | Retorno |
|--------|-------|--------------|---------|
| `listRooms` | `()` | `GET /api/lobby-rooms` | `Observable<LobbyRoomSnapshot[]>` |
| `getRoomByMatch` | `(matchId: string)` | `GET /api/lobby-rooms/match/:matchId` | `Observable<LobbyRoomSnapshot>` |
| `createRoom` | `(roomName, mazoId, deckName, password?)` | `POST /api/lobby-rooms` | `Observable<LobbyRoomSnapshot>` |
| `joinRoom` | `(roomId, mazoId, deckName, password?)` | `POST /api/lobby-rooms/:id/join` | `Observable<LobbyRoomSnapshot>` |
| `leaveRoom` | `(roomId)` | `POST /api/lobby-rooms/:id/leave` | `Observable<LobbyRoomSnapshot>` |
| `kickGuest` | `(roomId)` | `POST /api/lobby-rooms/:id/kick` | `Observable<LobbyRoomSnapshot>` |
| `addBot` | `(roomId)` | `POST /api/lobby-rooms/:id/bot` | `Observable<LobbyRoomSnapshot>` |
| `setReady` | `(roomId, ready, mazoId)` | `POST /api/lobby-rooms/:id/ready` | `Observable<LobbyRoomSnapshot>` |
| `startRoom` | `(roomId)` | `POST /api/lobby-rooms/:id/start` | `Observable<LobbyRoomStartResponse>` |
| `spectateRoom` | `(roomId, password?)` | `POST /api/lobby-rooms/:id/spectate` | `Observable<LobbyRoomStartResponse>` |
| `sendChat` | `(roomId, text)` | `POST /api/lobby-rooms/:id/chat` | `Observable<LobbyRoomSnapshot>` |
| `sendReaction` | `(roomId, reaction)` | `POST /api/lobby-rooms/:id/reaction` | `Observable<LobbyRoomSnapshot>` |
| `sendMatchReaction` | `(matchId, reaction)` | `POST /api/lobby-rooms/match/:matchId/reaction` | `Observable<LobbyRoomSnapshot>` |

---

### listRooms()

**Listar todas las salas públicas disponibles**

```typescript
listRooms(): Observable<LobbyRoomSnapshot[]>
```

**Endpoint**: `GET /api/lobby-rooms`  
**Headers**: `X-Username`  
**Body**: ninguno

**Uso típico**:
```typescript
this.lobbyRoomService.listRooms().subscribe(rooms => {
  this.rooms = rooms.filter(r => r.status === 'OPEN');
});
```

---

### getRoomByMatch(matchId)

**Obtener sala por ID de partida activa**

```typescript
getRoomByMatch(matchId: string): Observable<LobbyRoomSnapshot>
```

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `matchId` | `string` | ID de la partida en curso |

**Endpoint**: `GET /api/lobby-rooms/match/:matchId`

---

### createRoom(roomName, mazoId, deckName, password?)

**Crear una nueva sala**

```typescript
createRoom(
  roomName: string,
  mazoId: number,
  deckName: string,
  password = ''
): Observable<LobbyRoomSnapshot>
```

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `roomName` | `string` | — | Nombre visible de la sala |
| `mazoId` | `number` | — | ID del mazo a usar |
| `deckName` | `string` | — | Nombre del mazo (display) |
| `password` | `string` | `''` | Contraseña (vacío = pública) |

**Endpoint**: `POST /api/lobby-rooms`  
**Body**: `{ roomName, mazoId, deckName, password }`

---

### joinRoom(roomId, mazoId, deckName, password?)

**Unirse a una sala existente como invitado**

```typescript
joinRoom(
  roomId: string,
  mazoId: number,
  deckName: string,
  password = ''
): Observable<LobbyRoomSnapshot>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/join`  
**Body**: `{ mazoId, deckName, password }`

---

### leaveRoom(roomId)

**Salir de una sala**

```typescript
leaveRoom(roomId: string): Observable<LobbyRoomSnapshot>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/leave`  
**Body**: `{}`

---

### kickGuest(roomId)

**Expulsar al invitado (solo el dueño puede hacerlo)**

```typescript
kickGuest(roomId: string): Observable<LobbyRoomSnapshot>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/kick`  
**Body**: `{}`

---

### addBot(roomId)

**Agregar un bot como invitado**

```typescript
addBot(roomId: string): Observable<LobbyRoomSnapshot>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/bot`  
**Body**: `{}`  
**Efecto**: `guestBot = true` en el snapshot resultante

---

### setReady(roomId, ready, mazoId)

**Marcar/desmarcar listo**

```typescript
setReady(
  roomId: string,
  ready: boolean,
  mazoId: number | null
): Observable<LobbyRoomSnapshot>
```

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `roomId` | `string` | ID de la sala |
| `ready` | `boolean` | `true` = listo, `false` = no listo |
| `mazoId` | `number \| null` | Mazo seleccionado (puede actualizarse aquí) |

**Endpoint**: `POST /api/lobby-rooms/:roomId/ready`  
**Body**: `{ ready, mazoId }`

---

### startRoom(roomId)

**Iniciar la partida (requiere ambos jugadores listos)**

```typescript
startRoom(roomId: string): Observable<LobbyRoomStartResponse>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/start`  
**Retorno**: `LobbyRoomStartResponse` con el `matchId` para navegar al tablero

---

### spectateRoom(roomId, password?)

**Unirse como espectador**

```typescript
spectateRoom(roomId: string, password = ''): Observable<LobbyRoomStartResponse>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/spectate`  
**Body**: `{ password }`  
**Retorno**: `LobbyRoomStartResponse` para acceder al tablero en modo lectura

---

### sendChat(roomId, text)

**Enviar mensaje de chat a la sala**

```typescript
sendChat(roomId: string, text: string): Observable<LobbyRoomSnapshot>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/chat`  
**Body**: `{ text }`

---

### sendReaction(roomId, reaction)

**Enviar reacción en sala de lobby**

```typescript
sendReaction(roomId: string, reaction: string): Observable<LobbyRoomSnapshot>
```

**Endpoint**: `POST /api/lobby-rooms/:roomId/reaction`  
**Body**: `{ text: reaction }` *(nota: el campo se llama `text` en el body)*

---

### sendMatchReaction(matchId, reaction)

**Enviar reacción durante una partida activa**

```typescript
sendMatchReaction(matchId: string, reaction: string): Observable<LobbyRoomSnapshot>
```

**Endpoint**: `POST /api/lobby-rooms/match/:matchId/reaction`  
**Body**: `{ text: reaction }`  
**Diferencia con `sendReaction`**: usa `matchId` en lugar de `roomId`, permite reaccionar durante la batalla sin perder la referencia a la sala

---

## 🔄 Flujo Típico de una Sala

```
Jugador A                        Jugador B
   │                                 │
   │ createRoom(name, mazoId, deck)  │
   │ ────────────────────────────>   │
   │                                 │ joinRoom(roomId, mazoId, deck)
   │                                 │ <──────────────────────────────
   │ setReady(roomId, true, mazoId)  │
   │ ────────────────────────────>   │
   │                                 │ setReady(roomId, true, mazoId)
   │                                 │ <──────────────────────────────
   │ startRoom(roomId)               │
   │ ──> LobbyRoomStartResponse      │
   │     { room, matchId }           │
   │                                 │
   [Navegar a /battle/:matchId]
```

---

## 🔗 Relaciones

- **Usado por**: Componentes del módulo `lobby` (lista de salas, detalle de sala)
- **Tipos**: `LobbyRoomSnapshot`, `LobbyRoomStartResponse`, `LobbyRoomStatus` (inline en este archivo)
- **Ver también**: [07-lobby-types.md](../types/07-lobby-types.md) para descripción detallada de los tipos

---

*Próximo: [08-battle-board-state-service.md](./08-battle-board-state-service.md)*
