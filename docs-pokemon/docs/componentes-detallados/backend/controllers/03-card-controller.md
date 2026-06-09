---
sidebar_position: 3
title: 🃏 CardController
---

# 🃏 CardController - Gestión del Catálogo de Cartas

> Endpoints para obtener información completa del catálogo de cartas

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/controller/CardController.java`

---

## 🏗️ Clase Principal

```java
@RestController
@RequestMapping("/api/cards")
public class CardController {
    
    private final CardCatalogService cardCatalogService;
    
    public CardController(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }
}
```

**Responsabilidades**:
- ✅ Servir el catálogo completo de cartas
- ✅ Delegar lógica a `CardCatalogService`
- ✅ Manejar excepciones y devolver errores apropiados

---

## 📡 Endpoints

### 1. GET /api/cards

**Obtener todas las cartas del catálogo**

```java
@GetMapping
public ResponseEntity<List<Card>> getAll() {
    try {
        List<Card> cards = cardCatalogService.getCatalogo();
        return ResponseEntity.ok(cards);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
```

**Request**:
```
GET /api/cards
```

**Response (200)**:
```json
[
  {
    "id": "001",
    "name": "Pikachu",
    "type": "ELECTRIC",
    "hp": 35,
    "rarity": "COMMON",
    "imageUrl": "https://..."
  },
  {
    "id": "002",
    "name": "Charizard",
    "type": "FIRE",
    "hp": 120,
    "rarity": "RARE",
    "imageUrl": "https://..."
  }
]
```

**Response (500)**:
```
Internal Server Error
```

---

## 🔄 Flujo Completo: Obtener Catálogo

```
Cliente                CardController        CardCatalogService      BD
   │                         │                      │                │
   ├─ GET /api/cards        │                      │                │
   │────────────────────────→ │                      │                │
   │                          │                      │                │
   │                          ├─ getCatalogo() ───→ │                │
   │                          │                      │                │
   │                          │                      ├─ SELECT... ──→ │
   │                          │                      │   ALL CARDS    │
   │                          │                      │                │
   │                          │              ← List<Card> ──────────┤
   │                          │                      │                │
   │                          │ ← List<Card> ─────────┤                │
   │                          │                      │                │
   │ 200 OK + Cards List ← ─ ┤                      │                │
   │ [Card[], Card[], ...]    │                      │                │
   │                          │                      │                │
```

---

## 📋 Modelo Card

```java
public class Card {
    private String id;           // Identificador único (ej: "001")
    private String name;         // Nombre de la carta
    private String type;         // Tipo de energía (FIRE, WATER, GRASS, etc)
    private Integer hp;          // Puntos de vida
    private String rarity;       // Rareza (COMMON, UNCOMMON, RARE, etc)
    private String imageUrl;     // URL a imagen de la carta
    private List<Attack> attacks; // Ataques disponibles
    private Ability ability;      // Habilidad especial (opcional)
}
```

---

## 🎯 Validaciones

```
GET /api/cards
├─ Verificar autenticación (opcional)
├─ Llamar CardCatalogService
├─ Manejo de excepciones
└─ Devolver lista de cartas o error 500
```

---

## ⚡ Performance

- **Caché**: `CardCatalogService` cachea el catálogo en memoria
- **Complejidad**: O(1) - devuelve lista cacheada
- **Tamaño**: ~500-1000 cartas típicamente
- **Overhead**: Minimal - no hay cálculos, solo serialización JSON

---

## 🔐 Seguridad

✅ Validación de excepciones
✅ Respuesta neutral para errores (no expone detalles internos)
✅ Sin datos sensibles en respuesta
✅ Endpoint GET (read-only)

---

## 🔗 Relaciones

```
CardController
    ↓
    └─ CardCatalogService
        ├─ Carga cartas en memoria
        └─ Devuelve List<Card>
```

---

## 📝 DTOs Usadas

```java
// Response
class Card {
    String id;
    String name;
    String type;
    Integer hp;
    String rarity;
    String imageUrl;
    List<Attack> attacks;
    Ability ability;
}
```

---

*Próximo: [JugadorController](/docs/componentes-detallados/backend/controllers/04-jugador-controller)*
