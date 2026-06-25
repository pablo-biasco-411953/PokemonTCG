---
sidebar_position: 4
title: 👤 JugadorController
---

# 👤 JugadorController - Gestión de Jugadores

> Endpoints para consultar y administrar datos, monedas, sobres y personalización de jugadores

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/controller/JugadorController.java`

---

## 🏗️ Clase Principal

```java
@RestController
@RequestMapping("/api/jugadores")
@Tag(name = "Jugadores", description = "Endpoints para consultar y administrar jugadores...")
public class JugadorController {
    
    private final JugadorRepository jugadorRepo;
    
    public JugadorController(JugadorRepository jugadorRepo) {
        this.jugadorRepo = jugadorRepo;
    }
}
```

**Responsabilidades**:
- ✅ Obtener datos de jugadores
- ✅ Administrar puntos (Santoropoints)
- ✅ Administrar sobres disponibles
- ✅ Gestionar compras de packs
- ✅ Personalización de avatar
- ✅ Sistema de misiones (Santoro)
- ✅ Intercambio de cartas (trading)

---

## 📡 Endpoints

### 1. GET /api/jugadores/`{username}`/datos

**Obtener datos completos de un jugador**

```java
@GetMapping("/{username}/datos")
public ResponseEntity<?> obtenerDatos(@PathVariable String username) {
    try {
        Jugador j = jugadorRepo.findByUsername(username);
        if (j == null) return ResponseEntity.status(404).body("Jugador no encontrado");
        
        JugadorDatosResponse response = new JugadorDatosResponse(
            j.getUsername(), 
            j.getSobresDisponibles(),
            highlightCardCount(j),
            j.getSantoroPoints(),
            j.getCharacterId(),
            j.getSkinColor(),
            j.getHairColor(),
            j.getEyeColor(),
            j.getHeight(),
            j.isPikachuCompanion()
        );
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
    }
}
```

**Request**:
```
GET /api/jugadores/Pikachu123/datos
```

**Response (200)**:
```json
{
  "username": "Pikachu123",
  "sobresDisponibles": 5,
  "coleccionSize": 42,
  "santoroPoints": 150,
  "characterId": "char_001",
  "skinColor": "#FFCC00",
  "hairColor": "#000000",
  "eyeColor": "#0066CC",
  "height": 1.75,
  "pikachuCompanion": true
}
```

**Response (404)**:
```json
"Jugador no encontrado"
```

---

### 2. POST /api/jugadores/`{username}`/personalizacion

**Guardar personalización del avatar**

```java
@PostMapping("/{username}/personalizacion")
public ResponseEntity<?> guardarPersonalizacion(@PathVariable String username,
                                               @RequestBody PersonalizacionRequest request) {
    try {
        Jugador j = jugadorRepo.findByUsername(username);
        if (j == null) return ResponseEntity.status(404).body("Jugador no encontrado");
        
        j.setCharacterId(request.getCharacterId());
        j.setSkinColor(request.getSkinColor());
        j.setHairColor(request.getHairColor());
        j.setEyeColor(request.getEyeColor());
        j.setHeight(request.getHeight());
        j.setPikachuCompanion(request.isPikachuCompanion());
        
        jugadorRepo.save(j);
        return ResponseEntity.ok().build();
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error al guardar personalización: " + e.getMessage());
    }
}
```

**Request**:
```json
{
  "characterId": "char_002",
  "skinColor": "#FF6B6B",
  "hairColor": "#FFA500",
  "eyeColor": "#4285F4",
  "height": 1.80,
  "pikachuCompanion": false
}
```

**Response (200)**:
```
OK
```

---

### 3. GET /api/jugadores/`{username}`/coleccion

**Obtener colección de cartas del jugador**

```java
@GetMapping("/{username}/coleccion")
public ResponseEntity<?> obtenerColeccion(@PathVariable String username) {
    try {
        Jugador j = jugadorRepo.findByUsername(username);
        if (j == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(j.getColeccion());
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error en coleccion: " + e.getMessage());
    }
}
```

**Request**:
```
GET /api/jugadores/Pikachu123/coleccion
```

**Response (200)**:
```json
[
  {
    "id": "001",
    "name": "Pikachu",
    "type": "ELECTRIC",
    "hp": 35,
    "rarity": "COMMON"
  },
  {
    "id": "025",
    "name": "Pikachu Holo",
    "type": "ELECTRIC",
    "hp": 35,
    "rarity": "HOLO_RARE"
  }
]
```

---

### 4. POST /api/jugadores/`{username}`/coins/reward

**Acreditar puntos (Santoropoints) a un jugador**

```java
@PostMapping("/{username}/coins/reward")
public ResponseEntity<?> rewardCoins(@PathVariable String username,
                                     @RequestBody(required = false) Map<String, Object> payload) {
    // Leer amount, sumar a santoroPoints, guardar
    int amount = readPositiveAmount(payload);
    jugador.setSantoroPoints(jugador.getSantoroPoints() + amount);
    jugadorRepo.save(jugador);
    return ResponseEntity.ok(toDatosResponse(jugador));
}
```

**Request**:
```json
{
  "amount": 50
}
```

**Response (200)**:
```json
{
  "username": "Pikachu123",
  "santoroPoints": 200,
  "sobresDisponibles": 5,
  ...
}
```

---

### 5. POST /api/jugadores/`{username}`/coins/spend

**Gastar monedas (validando saldo)**

```java
@PostMapping("/{username}/coins/spend")
public ResponseEntity<?> spendCoins(@PathVariable String username,
                                    @RequestBody(required = false) Map<String, Object> payload) {
    int amount = readPositiveAmount(payload);
    if (jugador.getSantoroPoints() < amount) {
        return ResponseEntity.badRequest().body("Santoropoints insuficientes");
    }
    jugador.setSantoroPoints(jugador.getSantoroPoints() - amount);
    jugadorRepo.save(jugador);
    return ResponseEntity.ok(toDatosResponse(jugador));
}
```

**Request**:
```json
{
  "amount": 30
}
```

**Response (400)** - Saldo insuficiente:
```json
"Santoropoints insuficientes"
```

---

### 6. POST /api/jugadores/`{username}`/packs/buy

**Comprar sobres con monedas**

```java
@PostMapping("/{username}/packs/buy")
public ResponseEntity<?> buyPacks(@PathVariable String username,
                                  @RequestBody(required = false) Map<String, Object> payload) {
    int amount = readPositiveAmount(payload);
    int cost = switch (amount) {
        case 1 -> 80;   // 1 pack = 80 coins
        case 3 -> 200;  // 3 packs = 200 coins
        case 5 -> 300;  // 5 packs = 300 coins
        default -> throw new IllegalArgumentException("Bundle no disponible");
    };
    
    if (jugador.getSantoroPoints() < cost) {
        return ResponseEntity.badRequest().body("Santoropoints insuficientes");
    }
    
    jugador.setSantoroPoints(jugador.getSantoroPoints() - cost);
    jugador.setSobresDisponibles(jugador.getSobresDisponibles() + amount);
    jugadorRepo.save(jugador);
    return ResponseEntity.ok(toDatosResponse(jugador));
}
```

**Request** - Comprar 3 sobres:
```json
{
  "amount": 3
}
```

**Response (200)**:
```json
{
  "username": "Pikachu123",
  "santoroPoints": 50,
  "sobresDisponibles": 8,
  ...
}
```

**Tabla de Precios**:
| Cantidad | Precio (coins) | Costo por Sobre |
|----------|---|---|
| 1 | 80 | 80 |
| 3 | 200 | ~67 |
| 5 | 300 | 60 |

---

### 7. GET /api/jugadores/`{username}`/quests/santoro

**Obtener estado de misión Santoro**

```java
@GetMapping("/{username}/quests/santoro")
public ResponseEntity<?> getSantoroQuest(@PathVariable String username) {
    try {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) return ResponseEntity.status(404).body("Jugador no encontrado");
        return ResponseEntity.ok(toSantoroQuestResponse(jugador));
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error al cargar mision de Santoro: " + e.getMessage());
    }
}
```

**Response (200)**:
```json
{
  "giftClaimed": false,
  "tracking": true,
  "state": "AVAILABLE",
  "sobresDisponibles": 5
}
```

---

### 8. POST /api/jugadores/`{username}`/quests/santoro/tracking

**Activar/desactivar seguimiento de misión**

```java
@PostMapping("/{username}/quests/santoro/tracking")
public ResponseEntity<?> setSantoroTracking(@PathVariable String username,
                                           @RequestBody SantoroTrackingRequest request) {
    boolean tracking = request.isTracking() && !jugador.isSantoroGiftClaimed();
    jugador.setSantoroQuestTracking(tracking);
    if (tracking && (jugador.getSantoroQuestState() == null || jugador.getSantoroQuestState().isBlank())) {
        jugador.setSantoroQuestState("AVAILABLE");
    }
    jugadorRepo.save(jugador);
    return ResponseEntity.ok(toSantoroQuestResponse(jugador));
}
```

---

### 9. POST /api/jugadores/`{username}`/quests/santoro/claim

**Reclamar premio de misión Santoro (+10 sobres)**

```java
@PostMapping("/{username}/quests/santoro/claim")
public ResponseEntity<?> claimSantoroGift(@PathVariable String username) {
    if (!jugador.isSantoroGiftClaimed()) {
        jugador.setSobresDisponibles(jugador.getSobresDisponibles() + 10);
        jugador.setSantoroGiftClaimed(true);
        jugador.setSantoroQuestTracking(false);
        jugador.setSantoroQuestState("COMPLETED");
        jugadorRepo.save(jugador);
    }
    return ResponseEntity.ok(toSantoroQuestResponse(jugador));
}
```

**Response (200)**:
```json
{
  "giftClaimed": true,
  "tracking": false,
  "state": "COMPLETED",
  "sobresDisponibles": 15
}
```

---

### 10. POST /api/jugadores/trade/execute

**Ejecutar intercambio de cartas entre dos jugadores**

```java
@PostMapping("/trade/execute")
@Transactional
public ResponseEntity<?> executeTrade(@RequestBody TradeExecutionRequest request) {
    Jugador jA = jugadorRepo.findByUsername(request.getPlayerA());
    Jugador jB = jugadorRepo.findByUsername(request.getPlayerB());
    
    if (jA == null || jB == null) return ResponseEntity.status(404).body("Jugador no encontrado");
    
    List<Card> colA = new ArrayList<>(jA.getColeccion());
    List<Card> colB = new ArrayList<>(jB.getColeccion());
    
    // Intercambiar cartas de A -> B
    for (String cardId : request.getPlayerACardIds()) {
        Card found = colA.stream().filter(c -> c.getId().equals(cardId)).findFirst().orElse(null);
        if (found == null) return ResponseEntity.badRequest().body("Carta no encontrada");
        colA.remove(found);
        colB.add(found);
    }
    
    // Intercambiar cartas de B -> A
    for (String cardId : request.getPlayerBCardIds()) {
        Card found = colB.stream().filter(c -> c.getId().equals(cardId)).findFirst().orElse(null);
        if (found == null) return ResponseEntity.badRequest().body("Carta no encontrada");
        colB.remove(found);
        colA.add(found);
    }
    
    jA.setColeccion(colA);
    jB.setColeccion(colB);
    jugadorRepo.save(jA);
    jugadorRepo.save(jB);
    
    return ResponseEntity.ok().build();
}
```

**Request**:
```json
{
  "playerA": "Pikachu123",
  "playerACardIds": ["001", "025"],
  "playerB": "Charizard456",
  "playerBCardIds": ["042", "050"]
}
```

**Response (200)**:
```
OK (trade completado)
```

---

### 11. POST /api/jugadores/`{username}`/debug/sobres *(Debug Only)*

**Debug: Establecer cantidad de sobres**

```java
@PostMapping("/{username}/debug/sobres")
public ResponseEntity<?> debugSetSobres(@PathVariable String username,
                                        @RequestBody DebugSetSobresRequest request) {
    int cantidad = Math.max(0, request.getCantidad());
    jugador.setSobresDisponibles(cantidad);
    jugadorRepo.save(jugador);
    return ResponseEntity.ok(toDatosResponse(jugador));
}
```

⚠️ **Solo en desarrollo** - Propósito: testing y debugging

---

## 🔄 Flujo Completo: Obtener Datos + Comprar Sobres

```
Cliente              JugadorController      JugadorRepository        BD
   │                      │                       │                 │
   ├─ GET /api/jugadores/user/datos             │                 │
   │────────────────────→ │                       │                 │
   │                      ├─ findByUsername() ──→ │                 │
   │                      │                       ├─ SELECT... ───→ │
   │                      │                   ← Jugador ─────────┤
   │ 200 OK + Datos ← ─ ─ ┤                       │                 │
   │                      │                       │                 │
   ├─ POST /api/jugadores/user/packs/buy        │                 │
   │ { "amount": 3 }      │                       │                 │
   │────────────────────→ │                       │                 │
   │                      ├─ Validar coins       │                 │
   │                      ├─ Restar 200 coins    │                 │
   │                      ├─ Sumar 3 sobres      │                 │
   │                      │                       │                 │
   │                      ├─ save(jugador) ─────→ │                 │
   │                      │                       ├─ UPDATE... ───→ │
   │                      │                   ← OK ──────────────┤
   │ 200 OK + Stats ← ─ ─ ┤                       │                 │
```

---

## 📋 DTOs Usadas

```java
// Response
class JugadorDatosResponse {
    String username;
    Integer sobresDisponibles;
    Integer coleccionSize;
    Integer santoroPoints;
    String characterId;
    String skinColor;
    String hairColor;
    String eyeColor;
    Double height;
    boolean pikachuCompanion;
}

// Request
class PersonalizacionRequest {
    String characterId;
    String skinColor;
    String hairColor;
    String eyeColor;
    Double height;
    boolean pikachuCompanion;
}

// Request
class SantoroTrackingRequest {
    boolean tracking;
}

// Request
class TradeExecutionRequest {
    String playerA;
    List<String> playerACardIds;
    String playerB;
    List<String> playerBCardIds;
}
```

---

## 🎯 Validaciones

```
Compra de Sobres:
├─ Amount > 0 ✓
├─ Amount en [1, 3, 5] ✓
├─ Saldo de coins suficiente ✓
└─ Actualizar sobres + coins

Trading:
├─ Ambos jugadores existen ✓
├─ Jugador A posee sus cartas ✓
├─ Jugador B posee sus cartas ✓
├─ Transacción atómica (@Transactional)
└─ Ambos guardan cambios
```

---

## ⚡ Performance

- **Búsqueda**: O(1) - por username (indexed)
- **Trading**: O(n) - busca cartas en colección (típicamente n < 200)
- **Caché**: Consideración futura para datos frecuentes

---

## 🔐 Seguridad

✅ Autenticación necesaria para acceso (en producción)
✅ Validación de inputs
✅ Restricción de acceso a datos propios
✅ Trading: verificación de propiedad antes de transferencia
✅ @Transactional para integridad de trading

---

## 🔗 Relaciones

```
JugadorController
    ├─ JugadorRepository (acceso a BD)
    └─ Jugador entity
        ├─ coleccion (List<Card>)
        ├─ sobresDisponibles
        └─ santoroPoints
```

---

*Próximo: [MazoController](/docs/componentes-detallados/backend/controllers/05-mazo-controller)*
