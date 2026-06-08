---
sidebar_position: 3
title: 🎴 Mazo Entity
---

# 🎴 Mazo Entity - Modelo de Deck

> Entidad JPA que representa un deck (mazo) construido por un jugador

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/model/Mazo.java`

---

## 🏗️ Definición de la Entidad

```java
@Entity
@Table(name = "mazos")
public class Mazo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    
    @ManyToOne
    @JoinColumn(name = "jugador_id")
    @JsonIgnoreProperties({"coleccion", "mazos"})
    private Jugador jugador;
    
    @ManyToMany
    @JoinTable(
        name = "mazo_card",
        joinColumns = @JoinColumn(name = "mazo_id"),
        inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> cartas;
}
```

**Mapeo BD**: Tabla `mazos`

---

## 📋 Campos Principales

### Identidad

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;                // ID único, auto-generado
```

| Campo | Tipo | Constraints |
|-------|------|-----------|
| `id` | `Long` | PK, auto-increment |

---

### Nombre del Mazo

```java
private String nombre;          // Nombre del deck (ej: "Fuego Rápido")
```

---

### Propietario (Jugador)

```java
@ManyToOne
@JoinColumn(name = "jugador_id")
@JsonIgnoreProperties({"coleccion", "mazos"})
private Jugador jugador;
```

**Relación**:
- Type: Many-to-One
- Foreign Key: `jugador_id` → `jugadores.id`
- JSON: Ignora colección y mazos para evitar serialización circular

**Semantica**:
- Un mazo pertenece a exactamente un jugador
- Un jugador puede tener múltiples mazos

---

### Cartas en el Mazo

```java
@ManyToMany
@JoinTable(
    name = "mazo_card",
    joinColumns = @JoinColumn(name = "mazo_id"),
    inverseJoinColumns = @JoinColumn(name = "card_id")
)
private List<Card> cartas;
```

**Relación**:
- Type: Many-to-Many
- Join Table: `mazo_card`
- Semantica: Un mazo tiene N cartas, una carta está en N mazos

**Estructura BD**:
```
mazo_card (junction table)
├── mazo_id (FK → mazos.id)
├── card_id (FK → cards.id)
└── PK: (mazo_id, card_id)
```

---

## 🏗️ Constructores

### Constructor por defecto (JPA)

```java
public Mazo() {}
```

Requerido por JPA.

---

### Constructor con parámetros

```java
public Mazo(String nombre, Jugador jugador) {
    this.nombre = nombre;
    this.jugador = jugador;
}
```

**Uso**: Crear nuevo mazo
```java
Mazo mazo = new Mazo("Fuego Rápido", jugador);
```

---

## 🔐 Getters y Setters

```java
public Long getId()                 // Lectura solo
public String getNombre()
public void setNombre(String nombre)

public Jugador getJugador()
public void setJugador(Jugador jugador)

public List<Card> getCartas()
public void setCartas(List<Card> cartas)
```

---

## 📊 Schema BD

```sql
CREATE TABLE mazos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255),
    jugador_id BIGINT NOT NULL,
    FOREIGN KEY (jugador_id) REFERENCES jugadores(id)
);

CREATE TABLE mazo_card (
    mazo_id BIGINT NOT NULL,
    card_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (mazo_id, card_id),
    FOREIGN KEY (mazo_id) REFERENCES mazos(id),
    FOREIGN KEY (card_id) REFERENCES cards(id)
);
```

---

## 🔄 Relaciones

```
Jugador (1) ─ ManyToOne ─ (N) Mazo
              @JoinColumn(name = "jugador_id")

Mazo (N) ─ ManyToMany ─ (M) Card
           @JoinTable(name = "mazo_card")
```

**Cardinalidad**:
- Jugador → Mazos: 1:N (un jugador, múltiples mazos)
- Mazos → Cartas: N:M (un mazo tiene múltiples cartas, una carta está en múltiples mazos)

---

## 🎯 Casos de Uso

### Crear mazo nuevo
```java
Jugador j = jugadorRepo.findByUsername("Pikachu123");
Mazo mazo = new Mazo("Fuego Rápido", j);
mazo.setCartas(Arrays.asList(card1, card2, ...));  // 60 cartas
mazoRepo.save(mazo);
```

### Obtener mazos de jugador
```java
Jugador j = jugadorRepo.findByUsername("Pikachu123");
List<Mazo> mazos = mazoRepo.findByJugador(j);
```

### Actualizar cartas de mazo
```java
Mazo m = mazoRepo.findById(1L);
List<Card> nuevasCartas = Arrays.asList(...);  // 60 cartas
m.setCartas(nuevasCartas);
mazoRepo.save(m);
```

### Eliminar mazo
```java
mazoRepo.deleteById(1L);
```

---

## 📋 Validaciones (en MazoService)

```
Guardar mazo:
├─ Jugador existe ✓
├─ Cantidad cartas = 60 exactamente ✓
├─ Todas las cartas existen en BD ✓
└─ Cartas son válidas ✓
```

**Nota**: La entidad NO implementa validaciones (sin @Min, @Max)
- Validaciones están en MazoService (mejor: separación de concerns)

---

## ⚠️ Notas Importantes

**Circular references**:
```java
@JsonIgnoreProperties({"coleccion", "mazos"})
private Jugador jugador;
```
- Sin esto: JSON serialization infinita (Jugador → Mazo → Jugador → ...)

**Sin cascade**:
```java
@ManyToOne
private Jugador jugador;  // Sin cascade
```
- Eliminar mazo NO elimina jugador
- Eliminar jugador NO elimina mazos (orphan=true necesario)

**Sin timestamps**:
- Sin createdAt, updatedAt
- Mejor para auditoría en versión futura

---

*Próximo: [Backend Models Complete](/docs/componentes-detallados/backend/models/)*
