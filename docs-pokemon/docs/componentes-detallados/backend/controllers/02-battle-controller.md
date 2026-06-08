---
sidebar_position: 2
title: ⚔️ BattleController
---

# ⚔️ BattleController - Gestión de Batalla

> Endpoints para iniciar, gestionar y controlar batallas

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/controller/BattleController.java`

---

## 📡 Endpoints Principales

### POST /api/battle/start/`{username}`

**Iniciar batalla contra IA**

```java
@PostMapping("/start/{username}")
public ResponseEntity<?> startBattle(@PathVariable String username,
                                     @RequestBody StartBattleRequest request) {
    try {
        // username: jugador
        // request.getMazoId(): mazo a usar
        Partida partida = battleEngine.startBattle(username, 
                                                   request.getMazoId());
        return ResponseEntity.ok(partida);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

**Request**:
```json
{
  "mazoId": 123
}
```

**Response (200)**:
```json
{
  "matchId": "match-uuid-123",
  "estado": "INICIANDO",
  "jugador1": "Pikachu123",
  "jugador2": "IA-Bot",
  "turno": 1
}
```

---

### POST /api/battle/start-online

**Iniciar batalla multijugador**

```java
@PostMapping("/start-online")
public ResponseEntity<?> startBattleOnline(
    @RequestBody StartBattleOnlineRequest request) {
    
    try {
        Partida partida = battleEngine.startBattleOnline(
            request.getPlayer1(),      // Nombre jugador 1
            request.getPlayer1MazoId(), // Mazo jugador 1
            request.getPlayer2(),      // Nombre jugador 2
            request.getPlayer2MazoId() // Mazo jugador 2
        );
        return ResponseEntity.ok(partida);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

---

### GET /api/battle/state/`{matchId}`

**Obtener estado actual de la batalla**

```java
@GetMapping("/state/{matchId}")
public ResponseEntity<?> getEstadoPartida(
    @PathVariable String matchId,
    @RequestHeader(value = "X-Username", required = false) String username) {
    
    Partida partida = battleEngine.getEstadoPartida(matchId, username);
    if (partida == null) return ResponseEntity.notFound().build();
    
    // Si es espectador, vista limitada
    if (lobbyRoomService.isSpectator(matchId, username)) {
        return ResponseEntity.ok(toSpectatorView(partida));
    }
    
    // Si es bot, invertir perspectiva
    if (username != null && username.equals(partida.getBotUsername())) {
        return ResponseEntity.ok(swapPerspective(partida));
    }
    
    return ResponseEntity.ok(partida);
}
```

---

### POST /api/battle/`{matchId}`/evolve

**Evolucionar Pokémon**

```java
@PostMapping("/{matchId}/evolve")
public ResponseEntity<?> evolucionarPokemon(
    @PathVariable String matchId,
    @RequestHeader(value = "X-Username", required = false) String username,
    @RequestBody EvolveRequest request) {
    
    try {
        battleEngine.evolucionarPokemon(
            matchId,
            request.getCartaManoId(),    // Carta evolución en mano
            request.getCartaTableroId(), // Pokémon base en tablero
            username
        );
        return ResponseEntity.ok().build();
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

**Request**:
```json
{
  "cartaManoId": "charmeleon-001",
  "cartaTableroId": "charmander-002"
}
```

---

### POST /api/battle/`{matchId}`/play-pokemon

**Jugar Pokémon desde mano**

```
POST /api/battle/{matchId}/play-pokemon
{
  "cardId": "pikachu-001",
  "position": "bench" // "active" o "bench"
}
```

---

### POST /api/battle/`{matchId}`/attach-energy

**Unir energía**

```
POST /api/battle/{matchId}/attach-energy
{
  "energyId": "electric-energy-001",
  "pokemonId": "pikachu-001"
}
```

---

### POST /api/battle/`{matchId}`/attack

**Ejecutar ataque**

```
POST /api/battle/{matchId}/attack
{
  "attackName": "Thunderbolt",
  "targetId": "squirtle-001"
}
```

---

### POST /api/battle/`{matchId}`/surrender

**Rendirse**

```java
@PostMapping("/{matchId}/surrender")
public ResponseEntity<?> rendirse(
    @PathVariable String matchId,
    @RequestHeader(value = "X-Username", required = false) String username) {
    
    try {
        Partida partida = battleEngine.rendirse(matchId, username);
        if (username != null && username.equals(partida.getBotUsername())) {
            return ResponseEntity.ok(swapPerspective(partida));
        }
        return ResponseEntity.ok(partida);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

---

## 🎯 Responsabilidades

✅ Recibir peticiones HTTP  
✅ Extraer parámetros (path, body, headers)  
✅ Delegar a BattleEngineService  
✅ Manejar perspectiva (bot, espectador)  
✅ Devolver respuesta JSON  

---

## 🔄 Flujo: Ejecutar Ataque

```
Cliente                BattleController    BattleEngineService    BD
   │                        │                      │                │
   ├─ POST /attack         │                      │                │
   │  { attackName }       │                      │                │
   │───────────────────→   │                      │                │
   │                       │                      │                │
   │                       ├─ executeAction() ──→ │                │
   │                       │                      │                │
   │                       │                      ├─ Validar acción
   │                       │                      ├─ Calcular daño
   │                       │                      ├─ Aplicar efectos
   │                       │                      │                │
   │                       │                      ├─ UPDATE ... ──→│
   │                       │                      │   WHERE matchId
   │                       │                      │                │
   │                       │ ← Partida actualizada            │
   │                       │                      │                │
   │ 200 OK + Estado ← ─ ─ ┤                      │                │
   │ {turno, hp, estado}   │                      │                │
```

---

*Próximo: [CardController](/docs/componentes-detallados/backend/controllers/03-card-controller)*
