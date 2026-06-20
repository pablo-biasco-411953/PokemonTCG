---
sidebar_position: 1
title: 👤 Jugador Entity
---

# 👤 Jugador Entity - Modelo de Jugador

> Entidad JPA que representa a un jugador en el sistema

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/model/Jugador.java`

---

## 🏗️ Definición de la Entidad

```java
@Entity
@Table(name = "jugadores")
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ... campos
}
```

**Mapeo BD**: Tabla `jugadores`

---

## 📋 Campos Principales

### Identificación

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;                    // ID único, auto-generado

@Column(unique = true)
private String username;            // Nombre de usuario (único)

@Column(unique = true)
private String email;               // Email (único, para recuperación)
```

| Campo | Tipo | Constraints | Descripción |
|-------|------|-----------|-----------|
| `id` | `Long` | PK, auto-increment | Identificador único |
| `username` | `String` | UNIQUE | Nombre de usuario |
| `email` | `String` | UNIQUE | Email para recuperación |

---

### Autenticación

```java
@JsonIgnore
@Column(length = 128)
private String passwordHash;        // SHA-256 hash (no retorna en JSON)

@JsonIgnore
@Column(length = 128)
private String passwordResetTokenHash;  // Token hash para reset

@JsonIgnore
private Long passwordResetTokenExpiresAt;  // Expiracion del token
```

| Campo | Tipo | Constraints | Descripción |
|-------|------|-----------|-----------|
| `passwordHash` | `String` | 128 chars, @JsonIgnore | SHA-256 del password |
| `passwordResetTokenHash` | `String` | 128 chars, @JsonIgnore | Token hash para reset |
| `passwordResetTokenExpiresAt` | `Long` | @JsonIgnore | Timestamp de expiración |

**Seguridad**:
- ✅ Passwords hasheadas (no plaintext)
- ✅ Nunca incluido en JSON responses
- ✅ Token con expiración

---

### Recursos del Jugador

```java
private int sobresDisponibles;      // Cantidad de sobres sin abrir

@Column(nullable = false)
private int santoroPoints = 200;       // Puntos del juego (inicio: 200)
```

| Campo | Tipo | Default | Descripción |
|-------|------|---------|-----------|
| `sobresDisponibles` | `int` | 10 | Sobres disponibles (regalados al registrar) |
| `santoroPoints` | `int` | 200 | Puntos para comprar sobres |

**Getter con validación**:
```java
public void setSantoroPoints(int santoroPoints) {
    this.santoroPoints = Math.max(0, santoroPoints);  // Nunca negativo
}
```

---

### Colección de Cartas

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "jugador_card",
    joinColumns = @JoinColumn(name = "jugador_id"),
    inverseJoinColumns = @JoinColumn(name = "card_id")
)
private List<Card> coleccion = new ArrayList<>();
```

**Relación**:
- Type: Many-to-Many
- Join table: `jugador_card`
- Fetch: LAZY (carga bajo demanda)
- Default: lista vacía

**Estructura BD**:
```
jugador_card
├── jugador_id (FK → jugadores.id)
└── card_id (FK → cards.id)
```

---

### Personalización de Avatar

```java
private String characterId;        // ID del personaje (ej: "char_001")
private String skinColor;          // Color de piel (ej: "#FFCC00")
private String hairColor;          // Color de cabello
private String eyeColor;           // Color de ojos
private double height = 1.0;       // Altura (metros, default: 1.0)
private boolean pikachuCompanion = true;  // ¿Tiene Pikachu acompañante?
```

| Campo | Tipo | Default | Descripción |
|-------|------|---------|-----------|
| `characterId` | `String` | null | ID del personaje |
| `skinColor` | `String` | null | Hex color (#RRGGBB) |
| `hairColor` | `String` | null | Hex color |
| `eyeColor` | `String` | null | Hex color |
| `height` | `double` | 1.0 | Altura en metros |
| `pikachuCompanion` | `boolean` | true | ¿Acompañado por Pikachu? |

---

### Sistema de Misiones (Santoro)

```java
private boolean santoroGiftClaimed = false;    // ¿Reclamó el regalo?
private boolean santoroQuestTracking = false;  // ¿Sigue la misión?
private String santoroQuestState = "AVAILABLE"; // Estado: AVAILABLE, COMPLETED
```

| Campo | Tipo | Default | Descripción |
|-------|------|---------|-----------|
| `santoroGiftClaimed` | `boolean` | false | ¿Ya recibió el regalo (+10 sobres)? |
| `santoroQuestTracking` | `boolean` | false | ¿Está siguiendo la misión? |
| `santoroQuestState` | `String` | "AVAILABLE" | Estado actual de la misión |

**Estados Misión**:
```
AVAILABLE    → Puede reclamar
COMPLETED    → Ya reclamó
```

**Reward**: +10 sobres una sola vez

---

## 🏗️ Constructores

### Constructor por defecto (JPA)

```java
public Jugador() {}
```

Requerido por JPA para instanciación por reflexión.

---

### Constructor con username (Dominio)

```java
public Jugador(String username) {
    this.username = username;
    this.sobresDisponibles = 10;  // Regalo inicial
}
```

**Uso**: Crear nuevo jugador
```java
Jugador j = new Jugador("Pikachu123");
// username: "Pikachu123"
// sobresDisponibles: 10
// santoroPoints: 200 (default)
```

---

## 📊 Valores Iniciales

```
NEW Jugador("user"):
├── username: "user"
├── sobresDisponibles: 10 (regalo inicial)
├── santoroPoints: 200 (gift inicial)
├── coleccion: [] (vacío)
├── characterId: null
├── pikachuCompanion: true
├── santoroGiftClaimed: false
├── santoroQuestState: "AVAILABLE"
└── email: null (set después en register)
```

---

## 🔄 Relaciones

```
Jugador (1) ──────────────── (N) Mazo
    │
    └──────── (N:M) ────────── Card (colección)
                    (jugador_card)
```

**Cascadas**:
- Mazo: Sin especificar (manual delete)
- Colección: Sin cascada (manual manage)

---

## 💾 Estrategia de Carga

```java
@ManyToMany(fetch = FetchType.LAZY)
private List<Card> coleccion;
```

**LAZY**: La colección se carga bajo demanda
- ✅ Mejor performance (no carga si no se usa)
- ⚠️ Riesgo de LazyInitializationException si se accede fuera de transacción

---

## 🔐 Getters y Setters

```java
// Identidad
public Long getId()                     // Lectura solo
public String getUsername()
public void setUsername(String u)
public String getEmail()
public void setEmail(String e)

// Autenticación
public String getPasswordHash()
public void setPasswordHash(String h)
public String getPasswordResetTokenHash()
public void setPasswordResetTokenHash(String h)
public Long getPasswordResetTokenExpiresAt()
public void setPasswordResetTokenExpiresAt(Long t)

// Recursos
public int getSobresDisponibles()
public void setSobresDisponibles(int s)
public int getSantoroPoints()
public void setSantoroPoints(int points)     // Con validación Math.max(0, points)

// Colección
public List<Card> getColeccion()
public void setColeccion(List<Card> c)

// Avatar
public String getCharacterId()
public void setCharacterId(String c)
public String getSkinColor()
public void setSkinColor(String s)
public String getHairColor()
public void setHairColor(String h)
public String getEyeColor()
public void setEyeColor(String e)
public double getHeight()
public void setHeight(double h)
public boolean isPikachuCompanion()
public void setPikachuCompanion(boolean p)

// Misiones
public boolean isSantoroGiftClaimed()
public void setSantoroGiftClaimed(boolean b)
public boolean isSantoroQuestTracking()
public void setSantoroQuestTracking(boolean b)
public String getSantoroQuestState()
public void setSantoroQuestState(String s)
```

---

## 📊 Schema BD

```sql
CREATE TABLE jugadores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    passwordHash VARCHAR(128),
    passwordResetTokenHash VARCHAR(128),
    passwordResetTokenExpiresAt BIGINT,
    sobresDisponibles INT,
    santoroPoints INT NOT NULL DEFAULT 200,
    characterId VARCHAR(255),
    skinColor VARCHAR(255),
    hairColor VARCHAR(255),
    eyeColor VARCHAR(255),
    height DOUBLE DEFAULT 1.0,
    pikachuCompanion BOOLEAN DEFAULT true,
    santoroGiftClaimed BOOLEAN DEFAULT false,
    santoroQuestTracking BOOLEAN DEFAULT false,
    santoroQuestState VARCHAR(255) DEFAULT 'AVAILABLE'
);

CREATE TABLE jugador_card (
    jugador_id BIGINT NOT NULL,
    card_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (jugador_id, card_id),
    FOREIGN KEY (jugador_id) REFERENCES jugadores(id),
    FOREIGN KEY (card_id) REFERENCES cards(id)
);
```

---

## 🎯 Casos de Uso

### Crear nuevo jugador
```java
Jugador j = new Jugador("Pikachu123");
j.setEmail("pikachu@example.com");
j.setPasswordHash(hashPassword("MyPass123!"));
jugadorRepo.save(j);
```

### Agregar cartas a colección
```java
Jugador j = jugadorRepo.findByUsername("Pikachu123");
j.getColeccion().addAll(cartasDelSobre);
jugadorRepo.save(j);
```

### Comprar sobres
```java
Jugador j = jugadorRepo.findByUsername("Pikachu123");
j.setSantoroPoints(j.getSantoroPoints() - 80);
j.setSobresDisponibles(j.getSobresDisponibles() + 1);
jugadorRepo.save(j);
```

---

## ⚠️ Considerar

**Ventajas**:
- ✅ Normalizado (unique constraints)
- ✅ @JsonIgnore protege datos sensibles
- ✅ Validación en setters (Math.max para coins)
- ✅ Valores por defecto (sobres, coins, altura)

**Riesgos**:
- ⚠️ `coleccion` es LAZY → riesgo de LazyInitializationException
- ⚠️ passwordResetToken sin rotación automática
- ⚠️ Sin auditoría (createdAt, updatedAt)

---

*Próximo: [Card Entity](/docs/componentes-detallados/backend/models/02-card-entity)*
