---
sidebar_position: 3
title: "📊 Modelo de Datos"
---

# Modelo de Datos - Diagrama ER

> Esquema completo de la base de datos generado por Hibernate/JPA

---

## Diagrama Entidad-Relacion

```mermaid
erDiagram
    jugadores {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        int sobres_disponibles
        varchar character_id
        varchar skin_color
        varchar hair_color
        varchar eye_color
        double height
        boolean pikachu_enabled
        varchar password_reset_token_hash
    }

    cards {
        varchar id PK
        varchar nombre
        varchar hp
        varchar tipo
        varchar imagen
        varchar supertype
        varchar evolves_from
        varchar rarity
    }

    card_ataques {
        bigint id PK
        varchar nombre
        varchar danio
        varchar texto
        bigint card_id FK
    }

    card_subtypes {
        bigint card_id FK
        varchar subtype
    }

    card_rules {
        bigint card_id FK
        varchar rule
    }

    card_debilidades {
        bigint card_id FK
        varchar tipo
        varchar valor
    }

    card_resistencias {
        bigint card_id FK
        varchar tipo
        varchar valor
    }

    ataque_costo {
        bigint ataque_id FK
        varchar tipo_energia
    }

    mazos {
        bigint id PK
        varchar nombre
        bigint jugador_id FK
    }

    jugador_card {
        bigint jugador_id FK
        varchar card_id FK
    }

    mazo_card {
        bigint mazo_id FK
        varchar card_id FK
    }

    jugadores ||--o{ mazos : "tiene"
    jugadores }o--o{ cards : "coleccion (jugador_card)"
    mazos }o--o{ cards : "contiene (mazo_card)"
    cards ||--o{ card_ataques : "tiene"
    cards ||--o{ card_subtypes : "tiene"
    cards ||--o{ card_rules : "tiene"
    cards ||--o{ card_debilidades : "tiene"
    cards ||--o{ card_resistencias : "tiene"
    card_ataques ||--o{ ataque_costo : "requiere"
```

---

## Tablas Principales

### jugadores

Almacena los datos del jugador y su perfil de avatar.

| Columna | Tipo | Constraint | Descripcion |
|---------|------|-----------|-------------|
| `id` | BIGINT | PK, AUTO | ID generado |
| `username` | VARCHAR | UNIQUE | Nombre de usuario |
| `email` | VARCHAR | UNIQUE | Email para recovery |
| `password_hash` | VARCHAR | | SHA-256 del password |
| `sobres_disponibles` | INT | | Sobres por abrir |
| `character_id` | VARCHAR | | Modelo 3D elegido |
| `skin_color` | VARCHAR | | Color de piel (hex) |
| `hair_color` | VARCHAR | | Color de pelo (hex) |
| `eye_color` | VARCHAR | | Color de ojos (hex) |
| `height` | DOUBLE | | Altura del avatar |
| `pikachu_enabled` | BOOLEAN | | Mostrar Pikachu mascota |
| `password_reset_token_hash` | VARCHAR | | Token temporal de recovery |

### cards

Catalogo de cartas Pokemon del set XY.

| Columna | Tipo | Constraint | Descripcion |
|---------|------|-----------|-------------|
| `id` | VARCHAR | PK | ID del catalogo (ej: "xy1-1") |
| `nombre` | VARCHAR | | Nombre de la carta |
| `hp` | VARCHAR | | Puntos de vida |
| `tipo` | VARCHAR | | Tipo de energia |
| `imagen` | VARCHAR | | URL de la imagen |
| `supertype` | VARCHAR | | Pokemon / Energy |
| `evolves_from` | VARCHAR | | Pokemon previo |
| `rarity` | VARCHAR | | Rareza |

### mazos

Mazos de 60 cartas de cada jugador.

| Columna | Tipo | Constraint | Descripcion |
|---------|------|-----------|-------------|
| `id` | BIGINT | PK, AUTO | ID generado |
| `nombre` | VARCHAR | | Nombre del mazo |
| `jugador_id` | BIGINT | FK | Dueno del mazo |

---

## Tablas de Relacion

### jugador_card (Coleccion)

Relacion `@ManyToMany` entre Jugador y Card. Un jugador puede tener multiples copias de la misma carta.

### mazo_card (Cartas del Mazo)

Relacion `@ManyToMany` entre Mazo y Card. Siempre contiene exactamente 60 registros por mazo.

---

## Tablas de Coleccion (ElementCollection)

Estas tablas almacenan listas de valores simples asociados a una entidad padre:

| Tabla | Padre | Contenido |
|-------|-------|-----------|
| `card_subtypes` | Card | Subtipos: "Basic", "Stage 1", "EX" |
| `card_rules` | Card | Reglas especiales de la carta |
| `card_debilidades` | Card | Debilidades (tipo + valor) |
| `card_resistencias` | Card | Resistencias (tipo + valor) |
| `ataque_costo` | Ataque | Tipos de energia requeridos |

---

## Datos In-Memory (No Persistidos)

Estos datos viven en `ConcurrentHashMap` durante la ejecucion:

```mermaid
graph TD
    subgraph "In-Memory (ConcurrentHashMap)"
        P["Partida<br/>UUID → Estado completo"]
        LR["LobbyRoom<br/>UUID → Sala + jugadores"]
        WS["WebSocket Sessions<br/>sessionId → username"]
    end

    subgraph "Dentro de Partida"
        TJ1["TableroJugador (jugador)"]
        TJ2["TableroJugador (bot)"]
        TJ1 --> ACTIVO["CartaEnJuego (activo)"]
        TJ1 --> BANCA["CartaEnJuego[] (banca)"]
        TJ1 --> MANO["Card[] (mano)"]
        TJ1 --> PREMIOS["Card[] (premios)"]
        TJ1 --> DESC["Card[] (descarte)"]
    end
```

---

## Estrategia DDL

| Entorno | `ddl-auto` | Efecto |
|---------|-----------|--------|
| Desarrollo | `update` | Crea/modifica tablas automaticamente |
| Produccion | `validate` (recomendado) | Solo valida, no modifica |
