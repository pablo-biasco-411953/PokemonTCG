---
sidebar_position: 2
title: 🃏 CardCatalogService
---

# 🃏 CardCatalogService - Gestión del Catálogo

> Servicio de carga, caché y normalización del catálogo de cartas

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/service/CardCatalogService.java`

---

## 🏗️ Clase Principal

```java
@Service
public class CardCatalogService {
    
    private final CardRepository cardRepo;
    private final ObjectMapper objectMapper;
    
    public CardCatalogService(CardRepository cardRepo, ObjectMapper objectMapper) {
        this.cardRepo = cardRepo;
        this.objectMapper = objectMapper;
    }
}
```

**Responsabilidades**:
- ✅ Cargar catálogo desde BD o JSON
- ✅ Filtrar cartas jugables (XY + Pokémon/Energía)
- ✅ Normalizar tipos de energía
- ✅ Caché en memoria

---

## 📡 Métodos Principales

### 1. getCatalogo()

**Obtener catálogo completo (con caché)**

```java
@Transactional
public `List<Card>` getCatalogo() {
    // 1. Si catálogo en BD, usar ese
    List<Card> cartas = filtrarCartasJugables(cardRepo.findAll());
    if (!cartas.isEmpty()) {
        normalizarEnergiasXy(cartas);
        return cartas;
    }
    
    // 2. Si no, sincronizar desde JSON
    return sincronizarDesdeJson();
}
```

**Flujo**:
```
1. Consultar todas las cartas en BD
2. Filtrar solo cartas jugables (XY)
3. Si hay cartas:
   - Normalizar energías
   - Devolver
4. Si no hay:
   - Sincronizar desde /cards.json
   - Guardar en BD
   - Devolver
```

**Performance**:
- **Complejidad**: O(n) donde n = cartas en BD
- **Caché**: Las cartas se cachean en memoria de Spring
- **First call**: Lento (carga JSON)
- **Subsequent calls**: Rápido (BD)

---

### 2. sincronizarDesdeJson()

**Cargar catálogo desde JSON embebido**

```java
@Transactional
public `List<Card>` sincronizarDesdeJson() {
    // 1. Leer cartas desde /cards.json
    List<Card> cartas = leerCardsJson();
    
    // 2. Filtrar solo jugables
    cartas = filtrarCartasJugables(cartas);
    if (cartas.isEmpty()) {
        throw new IllegalStateException("cards.json no contiene cartas XY jugables.");
    }
    
    // 3. Normalizar tipos de energía
    normalizarEnergiasXy(cartas);
    
    // 4. Guardar en BD
    cardRepo.saveAll(cartas);
    cardRepo.flush();
    
    // 5. Releer de BD (para asegurar IDs)
    List<Card> guardadas = filtrarCartasJugables(cardRepo.findAll());
    normalizarEnergiasXy(guardadas);
    return guardadas;
}
```

**Pasos**:
1. Parsear `cards.json` con Jackson ObjectMapper
2. Filtrar: solo set XY + (Pokémon o Energía)
3. Normalizar tipos de energía
4. Guardar en BD
5. Releer desde BD para validar

---

### 3. leerCardsJson()

**Parsear cards.json embebido**

```java
private `List<Card>` leerCardsJson() {
    try (InputStream inputStream = getClass().getResourceAsStream("/cards.json")) {
        if (inputStream == null) {
            throw new IllegalStateException("No se encontro /cards.json dentro del build.");
        }
        return objectMapper.readValue(inputStream, new TypeReference<List<Card>>() {});
    } catch (IOException e) {
        throw new IllegalStateException("No se pudo leer cards.json: " + e.getMessage(), e);
    }
}
```

**Ubicación del archivo**: `src/main/resources/cards.json`

**Formato esperado**:
```json
[
  {
    "id": "xy1-001",
    "nombre": "Bulbasaur",
    "supertype": "Pokemon",
    "tipo": "Grass",
    "hp": "40",
    "subtypes": ["Basic"],
    ...
  },
  ...
]
```

---

### 4. filtrarCartasJugables(`List<Card>` cartas)

**Filtrar: solo XY + (Pokémon o Energía)**

```java
private List<Card> filtrarCartasJugables(List<Card> cartas) {
    return cartas.stream()
        .filter(this::esSetXy)           // Solo set XY
        .filter(card -> esPokemon(card) || esEnergia(card)) // Solo Pokémon o Energía
        .toList();
}

private boolean esSetXy(Card card) {
    String id = normalizar(card.getId());
    return id.startsWith("xy");  // ID como "xy1-001"
}

private boolean esPokemon(Card card) {
    return "pokemon".equals(normalizar(card.getSupertype()));
}

private boolean esEnergia(Card card) {
    return "energy".equals(normalizar(card.getSupertype()));
}
```

**Criterios de Filtrado**:
- ✓ ID comienza con "xy" (ej: "xy1-001")
- ✓ Supertype = "Pokemon" O "Energy"
- ✗ Entrenador, Objeto, Supporter (no jugables)

---

### 5. normalizarEnergiasXy(`List<Card>` cartas)

**Asegurar tipos de energía correctos**

```java
private void normalizarEnergiasXy(List<Card> cartas) {
    // Mapa de IDs → tipos de energía
    Map<String, String> tiposPorId = Map.ofEntries(
        Map.entry("xy1-132", "Grass"),
        Map.entry("xy1-133", "Fire"),
        Map.entry("xy1-134", "Water"),
        Map.entry("xy1-135", "Lightning"),
        Map.entry("xy1-136", "Psychic"),
        Map.entry("xy1-137", "Fighting"),
        Map.entry("xy1-138", "Darkness"),
        Map.entry("xy1-139", "Metal"),
        Map.entry("xy1-140", "Fairy")
    );
    
    boolean hayCambios = false;
    for (Card card : cartas) {
        // 1. Buscar en mapa (ID exacto)
        String tipoCorrecto = tiposPorId.get(card.getId());
        
        // 2. Si no, inferir por nombre
        if (tipoCorrecto == null) {
            tipoCorrecto = inferirTipoEnergiaBasica(card);
        }
        
        // 3. Si tipo incorrecto, actualizar
        if (tipoCorrecto != null && !tipoCorrecto.equalsIgnoreCase(card.getTipo())) {
            card.setTipo(tipoCorrecto);
            hayCambios = true;
        }
    }
    
    // 4. Guardar cambios si los hay
    if (hayCambios) {
        cardRepo.saveAll(cartas);
        cardRepo.flush();
    }
}
```

**Lógica de Normalización**:
1. Mapa de IDs → tipos (energías básicas XY)
2. Si no en mapa, inferir por nombre
3. Comparar con tipo actual
4. Si diferente, actualizar y guardar

---

### 6. inferirTipoEnergiaBasica(Card card)

**Inferir tipo de energía por nombre**

```java
private String inferirTipoEnergiaBasica(Card card) {
    // Validar que es energía básica
    if (!esEnergia(card) || card.getNombre() == null || card.getSubtypes() == null) 
        return null;
    
    boolean esBasica = card.getSubtypes().stream()
        .anyMatch(s -> "basic".equals(normalizar(s)));
    if (!esBasica) return null;
    
    // Buscar en nombre
    String nombre = normalizar(card.getNombre());
    if (nombre.contains("grass energy")) return "Grass";
    if (nombre.contains("fire energy")) return "Fire";
    if (nombre.contains("water energy")) return "Water";
    if (nombre.contains("lightning energy")) return "Lightning";
    if (nombre.contains("psychic energy")) return "Psychic";
    if (nombre.contains("fighting energy")) return "Fighting";
    if (nombre.contains("darkness energy")) return "Darkness";
    if (nombre.contains("metal energy")) return "Metal";
    if (nombre.contains("fairy energy")) return "Fairy";
    return null;
}
```

**Tipos de Energía Soportados**:
1. Grass
2. Fire
3. Water
4. Lightning
5. Psychic
6. Fighting
7. Darkness
8. Metal
9. Fairy

---

## 🔄 Flujo: Primera Carga

```
Cliente              CardCatalogService      BD              Recurso
   │                       │                 │               │
   ├─ getCatalogo()       │                 │               │
   │──────────────────→   │                 │               │
   │                      ├─ findAll() ────→ │               │
   │                      │             ← Empty ─────────┤
   │                      │                 │               │
   │                      ├─ sincronizarDesdeJson() ──────→ │
   │                      │                 │      Read /cards.json
   │                      │                 │               │
   │                      │ ← List<Card> ─────────────────┤
   │                      │                 │               │
   │                      ├─ filtrarCartasJugables()       │
   │                      │ (solo XY)       │               │
   │                      │                 │               │
   │                      ├─ normalizarEnergiasXy()       │
   │                      │                 │               │
   │                      ├─ saveAll() ────→ │               │
   │                      │ ~500-1000 cartas│               │
   │                      │                 ├─ INSERT... ──→│
   │                      │                 │               │
   │ ← List<Card> ────────┤                 │               │
```

---

## 📊 Complejidad

- **getCatalogo()**: O(n) - donde n = cartas jugables (~500-1000)
- **sincronizarDesdeJson()**: O(n log n) - lectura JSON + sort + insert
- **filtrarCartasJugables()**: O(n)
- **normalizarEnergiasXy()**: O(n) - iterar cartas
- **Caché**: Primera llamada: lenta; subsiguientes: rápidas

---

## 💾 Caché Strategy

```
Primera llamada:
1. Consultar BD (vacía)
2. Sincronizar desde JSON (~100ms)
3. Guardar en BD (~200ms)
4. Spring cachea automáticamente

Llamadas siguientes:
1. BD devuelve cartas cacheadas
2. Filtrar (O(n))
3. Devolver (~5ms)
```

---

## 🔗 Relaciones

```
CardCatalogService
    ├─ CardRepository (acceso BD)
    │   └─ Card entity
    │       ├─ id (unique)
    │       ├─ nombre
    │       ├─ tipo (energía)
    │       ├─ supertype (Pokemon/Energy)
    │       └─ hp
    │
    └─ ObjectMapper (Jackson)
        └─ Parsea cards.json
```

---

## 🎯 Casos de Uso

### 1. Primer inicio (BD vacía)
```
1. GET /api/cards
2. CardCatalogService.getCatalogo()
3. BD vacía → sincronizarDesdeJson()
4. Carga /cards.json (~1000 cartas)
5. Filtra XY + jugables (~500)
6. Normaliza energías
7. Guarda en BD
8. Devuelve a cliente
```

### 2. Cargas posteriores (BD poblada)
```
1. GET /api/cards
2. CardCatalogService.getCatalogo()
3. BD no vacía → devolver
4. Spring cachea
5. Respuesta rápida
```

---

## ⚠️ Excepciones

```java
IllegalStateException:
  - "No se encontro /cards.json dentro del build."
  - "No se pudo leer cards.json: {error}"
  - "cards.json no contiene cartas XY jugables."
  - "La base de datos no tiene suficientes cartas cargadas."
```

---

*Próximo: [MazoService](/docs/componentes-detallados/backend/services/03-mazo-service)*
