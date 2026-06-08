---
sidebar_position: 2
title: 🃏 Card Entity
---

# 🃏 Card Entity - Modelo de Carta

> Entidad JPA que representa una carta en el catálogo

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/model/Card.java`

---

## 🏗️ Definición de la Entidad

```java
@Entity
@Table(name = "cards")
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignora props desconocidas en JSON
public class Card {
    @Id
    private String id;  // ID único (ej: "xy1-001"), no auto-generado
    
    // ... campos
}
```

**Mapeo BD**: Tabla `cards`
**Origen datos**: Cargado desde `/cards.json` (externa API)

---

## 📋 Campos Principales

### Identidad y Metadata

```java
@Id
private String id;               // ID único (ej: "xy1-001")

private String nombre;           // Nombre de la carta
private String hp;               // Puntos de vida (String, puede ser "—")
private String tipo;             // Tipo de energía (Fire, Water, etc)
private String imagen;           // URL a imagen de la carta
```

| Campo | Tipo | PK | Descripción |
|-------|------|----|----|
| `id` | `String` | ✅ | ID único del set (ej: "xy1-001") |
| `nombre` | `String` | | Nombre de la carta |
| `hp` | `String` | | HP (String porque puede ser "—" sin HP) |
| `tipo` | `String` | | Tipo de energía (Grass, Fire, Water, etc) |
| `imagen` | `String` | | URL a imagen de carta |

---

### Características Especiales

```java
@JsonProperty("costoRetirada")
private int costoRetirada;       // Costo de retirar (energías necesarias)

@JsonProperty("supertype")
private String supertype;        // Tipo superior (Pokemon, Energy, Trainer)

@JsonProperty("evolvesFrom")
private String evolvesFrom;      // ID de carta base (si es evolución)
```

| Campo | Tipo | Descripción |
|-------|------|-----------|
| `costoRetirada` | `int` | Energías necesarias para retirar |
| `supertype` | `String` | Pokemon, Energy, Trainer, Supporter |
| `evolvesFrom` | `String` | ID de carta base (null si no evoluciona) |

---

### Subtypes y Reglas

```java
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "card_subtypes", 
    joinColumns = @JoinColumn(name = "card_id"))
@Column(name = "subtype")
private List<String> subtypes = new ArrayList<>();

@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "card_rules", 
    joinColumns = @JoinColumn(name = "card_id"))
@Column(name = "rule", length = 2000)
private List<String> reglas = new ArrayList<>();
```

**Subtypes** (ej: Pokémon):
- "Basic" (Pokémon básico)
- "Stage 1" (Primera evolución)
- "Stage 2" (Segunda evolución)
- "GX", "EX" (Tipos especiales)

**Reglas** (ej: Pokémon):
- Descripción de habilidades
- Texto largo (hasta 2000 caracteres)

---

### Ataques

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@JoinColumn(name = "card_id")
private List<Ataque> ataques = new ArrayList<>();
```

**Relación**:
- Type: One-to-Many
- Cascade: ALL (eliminar ataques con carta)
- Orphan removal: true
- Fetch: LAZY

**Estructura BD**:
```
ataques
├── id (PK)
├── card_id (FK → cards.id)
├── nombre
├── danio
└── ...
```

---

### Debilidades y Resistencias

```java
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "card_debilidades", 
    joinColumns = @JoinColumn(name = "card_id"))
private List<CardAttribute> debilidades = new ArrayList<>();

@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "card_resistencias", 
    joinColumns = @JoinColumn(name = "card_id"))
private List<CardAttribute> resistencias = new ArrayList<>();
```

**CardAttribute**:
```java
public class CardAttribute {
    private String tipo;           // Tipo de energía (Fire, Water, etc)
    private int valor;             // Multiplicador (+20, -20, etc)
}
```

**Ejemplo**:
```
Debilidades:
  - Fire: +20 (recibe 20 más de daño Fire)
  
Resistencias:
  - Water: -20 (recibe 20 menos de daño Water)
```

---

## 🔄 Setters con @JsonProperty

### setSupertype()

```java
@JsonProperty("supertype")
public void setSupertype(String supertype) {
    this.supertype = supertype;
    // Debug log
    if (supertype != null) {
        System.out.println("✅ [CARD LOAD] " + this.nombre + " es de tipo: " + supertype);
    }
}
```

**Propósito**: Mapeo explícito de JSON → Java + debug

---

### cargarAtaquesDesdeJson()

```java
@JsonProperty("ataques")
public void cargarAtaquesDesdeJson(List<Map<String, Object>> jsonAtaques) {
    List<Ataque> nuevaLista = new ArrayList<>();
    if (jsonAtaques != null) {
        for (Map<String, Object> map : jsonAtaques) {
            Ataque atk = new Ataque();
            atk.setNombre((String) map.get("nombre"));
            
            // Parsear daño (puede ser "20" o "20+")
            Object dmgObj = map.get("dano") != null ? map.get("dano") : map.get("damage");
            if (dmgObj != null) {
                String dmgStr = String.valueOf(dmgObj).replaceAll("[^0-9]", "");
                atk.setDanio(dmgStr.isEmpty() ? 0 : Integer.parseInt(dmgStr));
            }
            
            // Costo de energía
            List<String> costo = (List<String>) map.get("costo");
            atk.setTiposEnergia(costo != null ? costo : new ArrayList<>());
            
            // Texto del ataque
            String textoAtk = (String) map.get("texto");
            atk.setTexto(textoAtk != null ? textoAtk : "");
            
            nuevaLista.add(atk);
        }
    }
    this.ataques = nuevaLista;
}
```

**Lógica**:
1. Iterar sobre lista de JSON maps
2. Crear objeto Ataque para cada uno
3. Parsear daño (extrae solo números)
4. Asignar costo (lista de tipos de energía)
5. Asignar texto del ataque

---

## 📊 Schema BD

```sql
CREATE TABLE cards (
    id VARCHAR(255) PRIMARY KEY,
    nombre VARCHAR(255),
    hp VARCHAR(10),
    tipo VARCHAR(50),
    imagen VARCHAR(2048),
    costoRetirada INT,
    supertype VARCHAR(50),
    evolvesFrom VARCHAR(255)
);

CREATE TABLE card_subtypes (
    card_id VARCHAR(255) NOT NULL,
    subtype VARCHAR(255),
    FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE TABLE card_rules (
    card_id VARCHAR(255) NOT NULL,
    rule VARCHAR(2000),
    FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE TABLE card_debilidades (
    card_id VARCHAR(255) NOT NULL,
    tipo VARCHAR(50),
    valor INT,
    FOREIGN KEY (card_id) REFERENCES cards(id)
);

CREATE TABLE card_resistencias (
    card_id VARCHAR(255) NOT NULL,
    tipo VARCHAR(50),
    valor INT,
    FOREIGN KEY (card_id) REFERENCES cards(id)
);
```

---

## 🔐 Getters

```java
public String getId()
public String getNombre()
public String getHp()
public String getTipo()
public String getImagen()
public int getCostoRetirada()
public String getSupertype()
public String getEvolvesFrom()
public List<String> getSubtypes()
public List<String> getReglas()
public List<Ataque> getAtaques()
public List<CardAttribute> getDebilidades()
public List<CardAttribute> getResistencias()
```

---

## 🎯 Casos de Uso

### Buscar carta por ID
```java
Card c = cardRepo.findById("xy1-001");
// Pikachu
```

### Obtener ataques
```java
Card c = cardRepo.findById("xy1-001");
List<Ataque> ataques = c.getAtaques();
// Thunder Shock (20 damage), Thunderbolt (50 damage)
```

### Verificar debilidades
```java
Card c = cardRepo.findById("xy1-001");  // Pikachu (Electric)
List<CardAttribute> debils = c.getDebilidades();
// Ground: +20
```

---

## ⚠️ Notas Importantes

**Origen de datos**:
- ✅ Cargadas desde `/cards.json` (external API)
- ✅ Parseadas con Jackson (@JsonProperty)
- ✅ Validadas y normalizadas en CardCatalogService

**Relaciones**:
- N:M con Jugador (colección)
- N:M con Mazo (cartas en deck)
- 1:N con Ataque (ataques disponibles)

**Performance**:
- ✅ HP como String (mejor flexibilidad)
- ⚠️ LAZY loading en ataques (cuidado con LazyInitializationException)
- ✅ @JsonIgnoreProperties(ignoreUnknown = true) = robustez

---

*Próximo: [Mazo Entity](/docs/componentes-detallados/backend/models/03-mazo-entity)*
