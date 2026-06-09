---
sidebar_position: 4
title: 🗄️ Diseño de Base de Datos
---

# 🗄️ Diseño de Base de Datos - Esquema Relacional

---

## 📊 Entidades Principales

### JUGADOR
```sql
CREATE TABLE jugador (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  wins INT DEFAULT 0,
  losses INT DEFAULT 0,
  elo_rating INT DEFAULT 1200
);
```

### CARTA
```sql
CREATE TABLE carta (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  tipo ENUM('FUEGO','AGUA','PLANTA','ELECTRICO','..') NOT NULL,
  hp INT NOT NULL,
  rareza ENUM('COMUN','NO_COMUN','RARA','HOLO_RARA') NOT NULL,
  ataques JSON,
  habilidad JSON,
  evolucion_de_id BIGINT REFERENCES carta(id),
  image_url VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### MAZO
```sql
CREATE TABLE mazo (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  jugador_id BIGINT FOREIGN KEY REFERENCES jugador(id),
  nombre VARCHAR(100) NOT NULL,
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fecha_modificacion TIMESTAMP
);

CREATE TABLE mazo_cartas (
  mazo_id BIGINT FOREIGN KEY REFERENCES mazo(id),
  carta_id BIGINT FOREIGN KEY REFERENCES carta(id),
  cantidad INT DEFAULT 1,
  PRIMARY KEY (mazo_id, carta_id)
);
```

### PARTIDA
```sql
CREATE TABLE partida (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  jugador1_id BIGINT FOREIGN KEY REFERENCES jugador(id),
  jugador2_id BIGINT FOREIGN KEY REFERENCES jugador(id),
  ganador_id BIGINT FOREIGN KEY REFERENCES jugador(id),
  estado ENUM('LOBBY','STARTED','FINISHED') NOT NULL,
  fecha_inicio TIMESTAMP,
  fecha_fin TIMESTAMP,
  estado_json JSON  -- Estado actual de la partida
);
```

---

## 🔗 Relaciones

```
JUGADOR
  ├─ 1:N MAZO (un jugador tiene muchos mazos)
  └─ 1:N PARTIDA (un jugador juega muchas partidas)

MAZO
  └─ N:M CARTA (a través de MAZO_CARTAS)

PARTIDA
  ├─ N:1 JUGADOR (jugador1)
  └─ N:1 JUGADOR (jugador2)
```

---

## 📈 Índices Principales

```sql
CREATE INDEX idx_jugador_username ON jugador(username);
CREATE INDEX idx_jugador_email ON jugador(email);
CREATE INDEX idx_mazo_jugador ON mazo(jugador_id);
CREATE INDEX idx_partida_estado ON partida(estado);
CREATE INDEX idx_partida_jugador1 ON partida(jugador1_id);
CREATE INDEX idx_partida_jugador2 ON partida(jugador2_id);
```

---

*Próximo: [API Endpoints](/docs/tecnica/api-endpoints)*
