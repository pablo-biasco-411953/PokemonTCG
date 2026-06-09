---
sidebar_position: 1
title: 📐 TypeScript Models — Índice
---

# 📐 TypeScript Models — Índice de Tipos

> Índice de todas las interfaces y tipos TypeScript del frontend

---

## 📍 Ubicación Base

```
frontend/src/app/shared/models/
frontend/src/app/features/battle/battle-board.types.ts      (UI types)
frontend/src/app/features/lobby/services/lobby-room.service.ts  (inline types)
```

---

## 📋 Documentos por Archivo

| Doc | Archivo | Tipos incluidos |
|-----|---------|----------------|
| [02-card-types.md](./02-card-types.md) | `shared/models/card.ts` | `Card`, `Ataque`, `CardWeakness` |
| [03-jugador-types.md](./03-jugador-types.md) | `shared/models/jugador.ts` | `Jugador`, `JugadorDatosResponse` |
| [04-mazo-types.md](./04-mazo-types.md) | `shared/models/mazo.ts` | `Mazo`, `MazoJugadorResumen` |
| [05-battle-types.md](./05-battle-types.md) | `shared/models/battle.ts` | `CartaEnJuego`, `TableroJugador`, `Partida`, `BattleActionCard` |
| [06-battle-board-types.md](./06-battle-board-types.md) | `battle-board.types.ts` | `CoinSide`, `AttackCoinFlipState`, `InterTurnOverlayState`, y más |
| [07-lobby-types.md](./07-lobby-types.md) | `lobby-room.service.ts` (inline) | `LobbyRoomSnapshot`, `LobbyRoomStatus`, y más |

---

## 🗺️ Diagrama de Relaciones

```
Card
 ├── usada en → Mazo.cartas
 ├── usada en → CartaEnJuego.card
 ├── usada en → CartaEnJuego.energiasUnidas
 ├── usada en → TableroJugador.mano / mazo / premios / pilaDescarte
 └── usada en → Jugador.coleccion

Ataque (parte de Card)
 └── extendida por → BattleBoardAttack (battle-board.types.ts)

CartaEnJuego
 └── usada en → TableroJugador.activo / banca

TableroJugador
 └── usada en → Partida.jugador / Partida.bot

Jugador
 └── persiste en → localStorage['jugador']
 └── variante resumida → JugadorDatosResponse

Mazo
 └── tiene → MazoJugadorResumen (dueño)

LobbyRoomSnapshot
 └── tiene → LobbyRoomChatMessage[]
 └── tiene → LobbyRoomReaction[]
 └── incluida en → LobbyRoomStartResponse

AttackCoinFlipState
 └── tiene → CoinVisualState[] (estado de cada moneda)
```

---

## 🔗 Tipos por Categoría

### Dominio del juego (shared/models)
- `Card` — carta base
- `Ataque` — ataque de un Pokémon
- `CardWeakness` — debilidad/resistencia
- `CartaEnJuego` — Pokémon en juego con HP y condiciones
- `TableroJugador` — zonas de un jugador (mano, banca, activo, etc.)
- `Partida` — estado completo de la partida
- `BattleActionCard` — vista simplificada de carta para acciones
- `Jugador` — jugador con colección y personalización
- `JugadorDatosResponse` — resumen para el header del lobby
- `Mazo` — mazo con cartas
- `MazoJugadorResumen` — propietario de un mazo

### UI del tablero (battle-board.types.ts)
- `CoinSide`, `CoinFaceState` — estado de monedas
- `CoinVisualState`, `AttackCoinFlipState` — overlay de coin flip
- `DamageNumberState` — número de daño flotante
- `InterTurnOverlayState` — overlay entre turnos
- `ParticleVisualState` — efectos de partículas
- `HoveredBattleCard` — carta en hover
- `CardGlossaryEntry` — entrada de glosario
- `BattleBoardAttack` — extensión de Ataque
- `BattleBoardSide`, `OverlaySide` — identificadores de lado

### Tipos de lobby (inline en lobby-room.service.ts)
- `LobbyRoomStatus` — estado de la sala
- `LobbyRoomChatMessage` — mensaje de chat
- `LobbyRoomReaction` — reacción emoji
- `LobbyRoomSnapshot` — estado completo de la sala
- `LobbyRoomStartResponse` — respuesta al iniciar/espectar

---

*Ver también: [Frontend Services](../services/)*
