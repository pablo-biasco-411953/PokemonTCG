---
sidebar_position: 5
title: 🎴 MazoController
---

# 🎴 MazoController - Gestión de Mazos

> Endpoints para crear, actualizar, eliminar y listar mazos (decks) de batalla

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/controller/MazoController.java`

---

## 🏗️ Clase Principal

```java
@RestController
@RequestMapping("/api/mazos")
public class MazoController {
    
    private final MazoService mazoService;
    
    public MazoController(MazoService mazoService) {
        this.mazoService = mazoService;
    }
}
```

**Responsabilidades**:
- ✅ Crear nuevos mazos
- ✅ Listar mazos de un jugador
- ✅ Actualizar mazos existentes
- ✅ Eliminar mazos
- ✅ Inyectar cartas (debug)

---

## 📡 Endpoints

### 1. POST /api/mazos/guardar

**Crear un nuevo mazo (deck)**

```java
@PostMapping("/guardar")
public ResponseEntity<?> guardarMazo(@RequestBody GuardarMazoRequest request) {
    try {
        Mazo mazo = mazoService.guardarMazo(
            request.getNombre(), 
            request.getUsername(), 
            request.getCartas()
        );
        return ResponseEntity.ok(mazo);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

**Request**:
```json
{
  "nombre": "Fuego Rápido",
  "username": "Pikachu123",
  "cartas": [
    { "id": "001", "cantidad": 4 },
    { "id": "045", "cantidad": 3 },
    { "id": "089", "cantidad": 4 },
    { "id": "120", "cantidad": 2 }
  ]
}
```

**Response (200)** - Mazo creado:
```json
{
  "id": 1,
  "nombre": "Fuego Rápido",
  "username": "Pikachu123",
  "cartas": [
    { "id": "001", "cantidad": 4 },
    { "id": "045", "cantidad": 3 },
    { "id": "089", "cantidad": 4 },
    { "id": "120", "cantidad": 2 }
  ],
  "totalCartas": 13,
  "createdAt": "2026-06-08T10:30:00Z"
}
```

**Response (400)** - Validación fallida:
```json
"La cantidad total de cartas debe ser 60"
```

**Validaciones en MazoService**:
- ✅ Total de cartas = 60
- ✅ Máximo 4 copias por carta (excepto energía)
- ✅ Mínimo 1 copia de Pokémon activo
- ✅ Jugador posee todas las cartas

---

### 2. GET /api/mazos/listar/`{username}`

**Obtener todos los mazos de un jugador**

```java
@GetMapping("/listar/{username}")
public ResponseEntity<List<Mazo>> listarMazos(@PathVariable String username) {
    return ResponseEntity.ok(mazoService.listarMazos(username));
}
```

**Request**:
```
GET /api/mazos/listar/Pikachu123
```

**Response (200)**:
```json
[
  {
    "id": 1,
    "nombre": "Fuego Rápido",
    "username": "Pikachu123",
    "totalCartas": 60,
    "createdAt": "2026-06-08T10:30:00Z"
  },
  {
    "id": 2,
    "nombre": "Agua Control",
    "username": "Pikachu123",
    "totalCartas": 60,
    "createdAt": "2026-06-08T11:15:00Z"
  }
]
```

---

### 3. PUT /api/mazos/actualizar/`{id}`

**Actualizar un mazo existente**

```java
@PutMapping("/actualizar/{id}")
public ResponseEntity<?> actualizarMazo(@PathVariable Long id, 
                                       @RequestBody ActualizarMazoRequest request) {
    try {
        Mazo mazo = mazoService.actualizarMazo(
            id, 
            request.getNombre(), 
            request.getCartas()
        );
        return ResponseEntity.ok(mazo);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error al actualizar el mazo: " + e.getMessage());
    }
}
```

**Request**:
```json
{
  "nombre": "Fuego Rápido v2",
  "cartas": [
    { "id": "001", "cantidad": 4 },
    { "id": "045", "cantidad": 4 },
    { "id": "089", "cantidad": 4 }
  ]
}
```

**Response (200)**:
```json
{
  "id": 1,
  "nombre": "Fuego Rápido v2",
  "cartas": [...],
  "totalCartas": 60,
  "updatedAt": "2026-06-08T12:00:00Z"
}
```

---

### 4. DELETE /api/mazos/eliminar/`{id}`

**Eliminar un mazo**

```java
@DeleteMapping("/eliminar/{id}")
public ResponseEntity<?> eliminarMazo(@PathVariable Long id) {
    try {
        mazoService.eliminarMazo(id);
        return ResponseEntity.ok().build();
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error al eliminar el mazo: " + e.getMessage());
    }
}
```

**Request**:
```
DELETE /api/mazos/eliminar/1
```

**Response (200)**:
```
OK
```

**Response (404)**:
```json
"Mazo no encontrado"
```

---

### 5. POST /api/mazos/`{id}`/debug/inject-card *(Debug Only)*

**Debug: Inyectar carta en un mazo**

```java
@PostMapping("/{id}/debug/inject-card")
public ResponseEntity<?> debugInyectarCarta(@PathVariable Long id, 
                                           @RequestBody DebugInjectCardRequest request) {
    try {
        Mazo mazo = mazoService.debugInyectarCarta(
            id, 
            request.getCartaId(), 
            request.getCartaAReemplazarId()
        );
        return ResponseEntity.ok(mazo);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error al inyectar carta en el mazo: " + e.getMessage());
    }
}
```

**Request**:
```json
{
  "cartaId": "025",
  "cartaAReemplazarId": "001"
}
```

⚠️ **Solo en desarrollo** - Propósito: testing y debugging

---

## 🔄 Flujo Completo: Crear y Listar Mazos

```
Cliente              MazoController        MazoService          BD
   │                     │                     │               │
   ├─ POST /api/mazos/guardar              │               │
   │ { "nombre": "...", "cartas": [...] }  │               │
   │────────────────────→ │                     │               │
   │                      ├─ guardarMazo() ───→ │               │
   │                      │                     │               │
   │                      │     Validar:        │               │
   │                      │     - 60 cartas     │               │
   │                      │     - 4 máx/carta   │               │
   │                      │     - Cartas posees │               │
   │                      │                     │               │
   │                      │     Guardar ─────→ ├─ INSERT... ──→ │
   │                      │              ← Mazo ─────────────┤
   │                      │                     │               │
   │ 200 OK + Mazo ← ─ ─ ┤                     │               │
   │                      │                     │               │
   ├─ GET /api/mazos/listar/user              │               │
   │────────────────────→ │                     │               │
   │                      ├─ listarMazos() ───→ │               │
   │                      │                     ├─ SELECT... ──→ │
   │                      │              ← List<Mazo> ─────────┤
   │ 200 OK + [Mazos] ← ─ ┤                     │               │
```

---

## 📋 Estructura de un Mazo

```java
public class Mazo {
    private Long id;              // ID único
    private String nombre;        // Nombre del mazo
    private String username;      // Propietario
    private List<CartaEnMazo> cartas; // Cartas incluidas
    private Integer totalCartas;  // Suma total (siempre 60)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public class CartaEnMazo {
    private String id;           // ID de la carta
    private Integer cantidad;    // Copias de esta carta (1-4)
}
```

---

## 🎯 Validaciones de Mazo

```
Crear/Actualizar Mazo:
├─ Total de cartas = 60 ✓
├─ Ninguna carta > 4 copias ✓
│  Excepto:
│  └─ Energía básica (máximo sin límite)
├─ Mínimo 1 Pokémon (para jugar) ✓
├─ Jugador posee todas las cartas ✓
└─ Nombres de cartas válidos ✓
```

---

## ⚡ Performance

- **Listado**: O(n) - donde n = cantidad de mazos del usuario (típicamente < 50)
- **Búsqueda**: O(1) - por ID (indexed)
- **Validación**: O(k) - donde k = cartas en mazo (siempre 60)
- **Creación**: O(60) - validar y guardar 60 cartas

---

## 🔐 Seguridad

✅ Autorización: Solo el propietario puede editar/eliminar
✅ Validación de entrada (DTOs)
✅ Verificación de propiedad de cartas
✅ Sin exposición de datos internos

---

## 🔗 Relaciones

```
MazoController
    ├─ MazoService (lógica, validación)
    │   └─ MazoRepository
    │       └─ Mazo entity
    │           ├─ List<CartaEnMazo>
    │           └─ Jugador (propietario)
    └─ Card (validación de posesión)
```

---

## 📝 DTOs Usadas

```java
// Request - Crear
class GuardarMazoRequest {
    String nombre;
    String username;
    List<CartaEnMazo> cartas;
}

// Request - Actualizar
class ActualizarMazoRequest {
    String nombre;
    List<CartaEnMazo> cartas;
}

// Request - Debug
class DebugInjectCardRequest {
    String cartaId;
    String cartaAReemplazarId;
}

// Response
class Mazo {
    Long id;
    String nombre;
    String username;
    List<CartaEnMazo> cartas;
    Integer totalCartas;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

---

*Próximo: [SobreController](/docs/componentes-detallados/backend/controllers/06-sobre-controller)*
