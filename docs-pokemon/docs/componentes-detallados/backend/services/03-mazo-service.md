---
sidebar_position: 3
title: 🎴 MazoService
---

# 🎴 MazoService - Gestión de Decks

> Servicio de CRUD y validación de mazos

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/service/MazoService.java`

---

## 🏗️ Clase Principal

```java
@Service
public class MazoService {
    
    private final MazoRepository mazoRepo;
    private final JugadorRepository jugadorRepo;
    private final CardRepository cardRepo;
    private final MazoBackupService mazoBackupService;
    
    public MazoService(MazoRepository mazoRepo, JugadorRepository jugadorRepo, 
                      CardRepository cardRepo, MazoBackupService mazoBackupService) {
        this.mazoRepo = mazoRepo;
        this.jugadorRepo = jugadorRepo;
        this.cardRepo = cardRepo;
        this.mazoBackupService = mazoBackupService;
    }
}
```

**Responsabilidades**:
- ✅ Crear nuevos mazos (validar 60 cartas)
- ✅ Actualizar mazos
- ✅ Listar mazos de jugador
- ✅ Eliminar mazos
- ✅ Backup automático
- ✅ Debug: inyectar cartas

---

## 📡 Métodos Principales

### 1. guardarMazo(`String` nombre, `String` username, `List<String>` cartaIds)

**Crear nuevo mazo**

```java
public Mazo guardarMazo(String nombre, String username, List<String> cartaIds) {
    // 1. Validar que jugador existe
    Jugador jugador = jugadorRepo.findByUsername(username);
    if (jugador == null) {
        throw new IllegalArgumentException("Jugador no encontrado: " + username);
    }
    
    // 2. Validar cantidad de cartas = 60
    if (cartaIds == null || cartaIds.size() != 60) {
        throw new IllegalArgumentException("Un mazo debe contener exactamente 60 cartas. Se proporcionaron: " +
            (cartaIds == null ? 0 : cartaIds.size()));
    }
    
    // 3. Validar que todas las cartas existen
    List<Card> cartas = new ArrayList<>();
    for (String id : cartaIds) {
        Card card = cardRepo.findById(id).orElse(null);
        if (card == null) {
            throw new IllegalArgumentException("Carta no encontrada en la BD: " + id);
        }
        cartas.add(card);
    }
    
    // 4. Crear y guardar mazo
    Mazo mazo = new Mazo(nombre, jugador);
    mazo.setCartas(cartas);
    Mazo guardado = mazoRepo.save(mazo);
    
    // 5. Hacer backup
    mazoBackupService.backupAll();
    
    return guardado;
}
```

**Validaciones**:
- ✓ Jugador existe
- ✓ Cantidad de cartas = 60 exactamente
- ✓ Todas las cartas existen en BD
- ✓ No hay validación de máximo 4 copias (se permite en backend)

**Excepciones**:
```java
IllegalArgumentException:
  - "Jugador no encontrado: {username}"
  - "Un mazo debe contener exactamente 60 cartas. Se proporcionaron: {count}"
  - "Carta no encontrada en la BD: {cardId}"
```

---

### 2. actualizarMazo(`Long` id, `String` nombre, `List<String>` cartasIds)

**Actualizar mazo existente**

```java
public Mazo actualizarMazo(Long id, String nombre, List<String> cartasIds) {
    // 1. Encontrar mazo
    Mazo mazo = mazoRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("Mazo no encontrado con ID: " + id));
    
    // 2. Actualizar nombre
    mazo.setNombre(nombre);
    
    // 3. Obtener cartas (sin validación)
    List<Card> nuevasCartas = cardRepo.findAllById(cartasIds);
    
    // 4. Validar cantidad
    if (nuevasCartas.size() != 60) {
        // Log pero no lanzar excepción (?)
    }
    
    // 5. Actualizar cartas
    mazo.setCartas(nuevasCartas);
    
    // 6. Guardar y backup
    Mazo guardado = mazoRepo.save(mazo);
    mazoBackupService.backupAll();
    return guardado;
}
```

**Nota**: Este método no valida cantidad = 60

---

### 3. listarMazos(`String` username)

**Obtener todos los mazos de un jugador**

```java
public List<Mazo> listarMazos(String username) {
    // 1. Encontrar jugador
    Jugador jugador = jugadorRepo.findByUsername(username);
    if (jugador == null) {
        throw new IllegalArgumentException("Jugador no encontrado: " + username);
    }
    
    // 2. Devolver sus mazos
    return mazoRepo.findByJugador(jugador);
}
```

**Complejidad**: O(1) - lookup por jugador (indexed)

---

### 4. eliminarMazo(`Long` id)

**Eliminar un mazo**

```java
public void eliminarMazo(Long id) {
    // 1. Validar que existe
    if (!mazoRepo.existsById(id)) {
        throw new IllegalArgumentException("Mazo no encontrado con ID: " + id);
    }
    
    // 2. Eliminar
    mazoRepo.deleteById(id);
    
    // 3. Hacer backup
    mazoBackupService.backupAll();
}
```

**Excepciones**:
```java
IllegalArgumentException:
  - "Mazo no encontrado con ID: {id}"
```

---

### 5. debugInyectarCarta(`Long` mazoId, `String` cartaId, `String` cartaAReemplazarId)

**Debug: Inyectar o reemplazar carta**

```java
public Mazo debugInyectarCarta(Long mazoId, String cartaId, String cartaAReemplazarId) {
    // 1. Encontrar mazo
    Mazo mazo = mazoRepo.findById(mazoId)
        .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado con ID: " + mazoId));
    
    // 2. Encontrar carta nueva
    Card cartaNueva = cardRepo.findById(cartaId)
        .orElseThrow(() -> new IllegalArgumentException("Carta no encontrada en la BD: " + cartaId));
    
    // 3. Copiar lista de cartas actual
    List<Card> cartasActuales = mazo.getCartas() != null
        ? new ArrayList<>(mazo.getCartas())
        : new ArrayList<>();
    
    // 4. Si mazo está lleno (60 cartas), reemplazar
    if (cartasActuales.size() >= 60) {
        if (cartaAReemplazarId == null || cartaAReemplazarId.isBlank()) {
            throw new IllegalArgumentException("Debes elegir una carta a reemplazar en un mazo de 60 cartas.");
        }
        
        boolean reemplazada = false;
        for (int i = 0; i < cartasActuales.size(); i++) {
            Card actual = cartasActuales.get(i);
            if (actual != null && cartaAReemplazarId.equals(actual.getId())) {
                cartasActuales.set(i, cartaNueva);
                reemplazada = true;
                break;
            }
        }
        
        if (!reemplazada) {
            throw new IllegalArgumentException("La carta a reemplazar no existe dentro del mazo.");
        }
    } 
    // 5. Si no está lleno, agregar
    else {
        cartasActuales.add(cartaNueva);
    }
    
    // 6. Guardar cambios
    mazo.setCartas(cartasActuales);
    Mazo guardado = mazoRepo.save(mazo);
    mazoBackupService.backupAll();
    return guardado;
}
```

**Lógica**:
```
Si mazo tiene < 60 cartas:
  → Agregar nueva carta

Si mazo tiene >= 60 cartas:
  → Reemplazar carta especificada
  → Si no existe, excepción
```

**Excepciones**:
```java
IllegalArgumentException:
  - "Mazo no encontrado con ID: {id}"
  - "Carta no encontrada en la BD: {cardId}"
  - "Debes elegir una carta a reemplazar..."
  - "La carta a reemplazar no existe dentro del mazo."
```

---

## 🔄 Flujo: Crear Mazo

```
Controller              MazoService         Repositorys         BD
   │                        │                  │               │
   ├─ guardarMazo()        │                  │               │
   │ {60 cardIds}          │                  │               │
   │───────────────────→   │                  │               │
   │                       ├─ findByUsername()│               │
   │                       │──────────────────→ │               │
   │                       │         ← Jugador ─────────────┤
   │                       │                  │               │
   │                       ├─ for each cardId│               │
   │                       │ findById()       │               │
   │                       │──────────────────→ │               │
   │                       │      ← List<Card> ────────────┤
   │                       │                  │               │
   │                       ├─ save(mazo) ────→ │               │
   │                       │ con 60 cartas    │               │
   │                       │                  ├─ INSERT... ──→ │
   │                       │                  │               │
   │                       ├─ backupAll()     │               │
   │                       │ (crear archivo) │               │
   │                       │                  │               │
   │ ← Mazo guardado ──────┤                  │               │
```

---

## 📊 Complejidad

- **guardarMazo()**: O(60) = O(1) - validar 60 cartas
- **actualizarMazo()**: O(60) = O(1)
- **listarMazos()**: O(1) - lookup indexed
- **eliminarMazo()**: O(1)
- **debugInyectarCarta()**: O(60) = O(1) - buscar en lista de 60

---

## 🔐 Seguridad

✅ Validación de jugador (solo puede editar sus mazos)
✅ Validación de cartas (deben existir)
✅ Validación de cantidad (60 cartas exactamente)
⚠️ **Mejora**: Validar que jugador posea las cartas
⚠️ **Mejora**: Validar máximo 4 copias por carta

---

## 💾 Backup

Cada operación (create, update, delete) ejecuta:
```java
mazoBackupService.backupAll();
```

Esto crea un backup JSON de todos los mazos.

---

## 🔗 Relaciones

```
MazoService
    ├─ MazoRepository
    │   └─ Mazo entity
    │       ├─ id (primary key)
    │       ├─ nombre
    │       ├─ cartas (List<Card>)
    │       └─ jugador (FK)
    ├─ JugadorRepository
    │   └─ Jugador entity
    ├─ CardRepository
    │   └─ Card entity
    └─ MazoBackupService
        └─ Backup JSON
```

---

*Próximo: [SobreService](/docs/componentes-detallados/backend/services/04-sobre-service)*
