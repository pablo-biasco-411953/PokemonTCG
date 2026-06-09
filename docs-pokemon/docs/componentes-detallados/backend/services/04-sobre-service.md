---
sidebar_position: 4
title: 🎁 SobreService
---

# 🎁 SobreService - Generación de Booster Packs

> Servicio de apertura de sobres y generación de cartas aleatorias

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/service/SobreService.java`

---

## 🏗️ Clase Principal

```java
@Service
public class SobreService {
    
    private final JugadorRepository jugadorRepo;
    private final CardCatalogService cardCatalogService;
    private final Random random = new Random();
    
    public SobreService(JugadorRepository jugadorRepo, CardCatalogService cardCatalogService) {
        this.jugadorRepo = jugadorRepo;
        this.cardCatalogService = cardCatalogService;
    }
}
```

**Responsabilidades**:
- ✅ Abrir sobres
- ✅ Generar cartas aleatorias
- ✅ Validar disponibilidad
- ✅ Actualizar colección del jugador

---

## 📡 Método Principal

### abrirSobre(`String` username)

**Abrir un sobre y obtener 10 cartas**

```java
public `List<Card>` abrirSobre(`String` username) {
    // 1. Encontrar jugador
    Jugador jugador = jugadorRepo.findByUsername(username);
    if (jugador == null) {
        throw new IllegalArgumentException("Jugador no encontrado: " + username);
    }
    
    // 2. Validar que tiene sobres disponibles
    if (jugador.getSobresDisponibles() <= 0) {
        throw new IllegalStateException("No hay sobres disponibles para el jugador: " + username);
    }
    
    // 3. Obtener catálogo
    List<Card> todasLasCartas = cardCatalogService.getCatalogo();
    
    // 4. Separar energías y pokémon
    List<Card> energias = todasLasCartas.stream()
        .filter(this::esEnergia)
        .toList();
    
    List<Card> pokemones = todasLasCartas.stream()
        .filter(this::esPokemon)
        .toList();
    
    // 5. Si faltan cartas, sincronizar
    if (energias.isEmpty() || pokemones.isEmpty()) {
        todasLasCartas = cardCatalogService.sincronizarDesdeJson();
        energias = todasLasCartas.stream()
            .filter(this::esEnergia)
            .toList();
        pokemones = todasLasCartas.stream()
            .filter(this::esPokemon)
            .toList();
        
        if (energias.isEmpty() || pokemones.isEmpty()) {
            throw new IllegalStateException("La base de datos no tiene suficientes cartas cargadas.");
        }
    }
    
    // 6. Generar sobre
    // Random entre 2-5 energías (2-5 cartas)
    int cantEnergias = random.nextInt(4) + 2;  // 2 a 5
    int cantPokemones = 10 - cantEnergias;     // 5 a 8
    
    List<Card> sobreGenerado = new ArrayList<>();
    
    // 7. Agregar energías aleatorias
    List<Card> energiasMezcladas = new ArrayList<>(energias);
    Collections.shuffle(energiasMezcladas);
    sobreGenerado.addAll(energiasMezcladas.subList(0, Math.min(cantEnergias, energiasMezcladas.size())));
    
    // 8. Agregar pokémon aleatorios
    List<Card> pokemonesMezclados = new ArrayList<>(pokemones);
    Collections.shuffle(pokemonesMezclados);
    sobreGenerado.addAll(pokemonesMezclados.subList(0, Math.min(cantPokemones, pokemonesMezclados.size())));
    
    // 9. Mezclar orden final
    Collections.shuffle(sobreGenerado);
    
    // 10. Actualizar jugador
    jugador.getColeccion().addAll(sobreGenerado);
    jugador.setSobresDisponibles(jugador.getSobresDisponibles() - 1);
    jugadorRepo.save(jugador);
    
    return sobreGenerado;
}
```

**Pasos Principales**:
1. Validar jugador existe
2. Validar sobres disponibles > 0
3. Obtener catálogo (todas las cartas)
4. Separar en energías y pokémon
5. Generar cantidad aleatoria: 2-5 energías + 5-8 pokémon
6. Seleccionar cartas aleatorias de cada grupo
7. Mezclar orden
8. Agregar a colección del jugador
9. Decrementar sobres disponibles
10. Guardar en BD

---

## 🎲 Algoritmo de Generación

### Composición del Sobre

```
Total: 10 cartas

Energías: Random entre 2 y 5 (inclusive)
  - random.nextInt(4) → 0-3
  - + 2 → 2-5

Pokémon: 10 - cantEnergías
  - Si 5 energías → 5 pokémon
  - Si 2 energías → 8 pokémon
```

### Distribución

| Caso | Energías | Pokémon | Total |
|------|----------|---------|-------|
| Min energía | 2 | 8 | 10 |
| Max energía | 5 | 5 | 10 |
| Promedio | 3.5 | 6.5 | 10 |

### Aleatoriedad

```
1. Copiar lista de energías
2. Collections.shuffle() - mezcla in-place
3. Tomar subList de primeras N
4. Repetir con pokémon
5. Shuffle final de todas las cartas
```

---

## 🔍 Métodos Auxiliares

### esEnergia(Card card)

**Detectar si carta es energía**

```java
private boolean esEnergia(Card card) {
    String supertype = normalizar(card.getSupertype());
    String nombre = normalizar(card.getNombre());
    String hp = card.getHp();
    
    return "energy".equals(supertype)
        || (supertype.isBlank() && nombre.contains("energy"))
        || (supertype.isBlank() && "0".equals(hp) && nombre.contains("energia"));
}
```

**Criterios**:
- Supertype = "energy" (explícito)
- O supertype vacío + nombre contiene "energy"
- O supertype vacío + hp = "0" + nombre contiene "energia"

### esPokemon(Card card)

**Detectar si carta es pokémon**

```java
private boolean esPokemon(Card card) {
    String supertype = normalizar(card.getSupertype());
    String hp = card.getHp();
    
    return "pokemon".equals(supertype)
        || (supertype.isBlank() && hp != null && !"0".equals(hp));
}
```

**Criterios**:
- Supertype = "pokemon" (explícito)
- O supertype vacío + hp existe y no es "0"

### normalizar(String value)

**Normalizar string para comparación**

```java
private String normalizar(String value) {
    if (value == null) return "";
    return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
}
```

---

## 🔄 Flujo: Abrir Sobre

```
Cliente              SobreService         CardCatalogService    BD
   │                     │                      │              │
   ├─ abrirSobre(user)  │                      │              │
   │───────────────────→ │                      │              │
   │                     ├─ findByUsername() ───────────────────→
   │                     │                      │        ← Jugador
   │                     ├─ Validar sobres > 0 │              │
   │                     │                      │              │
   │                     ├─ getCatalogo() ─────→ │              │
   │                     │                      ├─ findAll() ───→
   │                     │                      │      ← Cartas
   │                     │                      │              │
   │                     ├─ Separar energías/pokémon         │
   │                     ├─ Random 2-5 energías               │
   │                     ├─ Random pokémon (10 - energías)    │
   │                     ├─ Shuffle final                     │
   │                     │                                     │
   │                     ├─ Actualizar colección              │
   │                     ├─ Decrementar sobres                │
   │                     ├─ save(jugador) ───────────────────→
   │                     │                      │    ← UPDATE
   │                     │                      │              │
   │ ← 10 cartas ────────┤                      │              │
```

---

## 📊 Complejidad

- **abrirSobre()**: O(n log n) donde n = cartas en catálogo (~500-1000)
  - getCatalogo(): O(n)
  - shuffle(): O(n log n) - Timsort de Collections
  - Total: O(n log n)
- **esEnergia() / esPokemon()**: O(1) per card
- **Memoria**: O(10) - solo 10 cartas en sobre

**Performance**: ~50-100ms típicamente

---

## 🔐 Seguridad

✅ Validación de jugador
✅ Validación de sobres disponibles
✅ Actualización atómica (save garantiza consistencia)
✅ Sin predicción de cartas (Random.nextInt())

⚠️ **Mejora**: Usar SecureRandom para mejor aleatoriedad

---

## ⚠️ Excepciones

```java
IllegalArgumentException:
  - "Jugador no encontrado: {username}"

IllegalStateException:
  - "No hay sobres disponibles para el jugador: {username}"
  - "La base de datos no tiene suficientes cartas cargadas."
```

---

## 🎯 Casos de Uso

### Caso 1: Abrir sobre normal
```
1. Jugador tiene 5 sobres
2. POST /api/sobres/abrir/username
3. Genera: 3 energías + 7 pokémon (aleatorio)
4. Actualiza:
   - Colección: +10 cartas
   - Sobres: 5 → 4
5. Devuelve las 10 cartas
```

### Caso 2: Sin sobres
```
1. Jugador tiene 0 sobres
2. POST /api/sobres/abrir/username
3. ❌ IllegalStateException
4. "No hay sobres disponibles"
```

### Caso 3: BD vacía (primer inicio)
```
1. POST /api/sobres/abrir/username
2. getCatalogo() retorna vacío
3. sincronizarDesdeJson() carga /cards.json
4. Genera sobre normalmente
```

---

## 💡 Mejoras Futuras

- [ ] Usar SecureRandom en vez de Random
- [ ] Garantizar rareza mínima (1 rara por sobre)
- [ ] Animación progresiva (frontend)
- [ ] Estadísticas de sobres (cantidad, rarezas)
- [ ] Limitación de tasa (máximo sobres/hora)
- [ ] Seed determinístico para testing

---

*Próximo: [BattleEngineService](/docs/componentes-detallados/backend/services/05-battle-engine-service)*
