---
sidebar_position: 5
title: 📡 API Endpoints - REST
---

# 📡 API Endpoints - Referencia Completa

---

## 🔐 Auth

```
POST /api/auth/register
  Body: { username, email, password }
  Response: { token, user }

POST /api/auth/login
  Body: { email, password }
  Response: { token, user }

POST /api/auth/refresh
  Headers: Authorization: Bearer <token>
  Response: { newToken }

POST /api/auth/logout
  Headers: Authorization: Bearer <token>
```

---

## 👤 Jugador

```
GET /api/jugador/perfil
  Headers: Authorization: Bearer <token>
  Response: { id, username, wins, losses, elo }

PUT /api/jugador/{id}
  Body: { username, bio, avatar }
  Response: { updatedUser }

GET /api/jugador/{id}
  Response: { publicProfile }
```

---

## 🃏 Cartas

```
GET /api/cartas
  Query: ?tipo=FUEGO&rareza=RARA&page=0&size=20
  Response: { content[], totalPages, totalElements }

GET /api/cartas/{id}
  Response: { id, nombre, hp, tipo, ataques[] }

GET /api/cartas/tipo/{tipo}
  Response: { cartas[] }
```

---

## 🛠️ Mazos

```
POST /api/mazos
  Body: { nombre, cartas[] }
  Response: { id, mazo }

GET /api/mazos/{id}
  Response: { mazo }

PUT /api/mazos/{id}
  Body: { nombre, cartas[] }
  Response: { updatedMazo }

POST /api/mazos/{id}/validar
  Response: { isValid, errors[] }
```

---

## ⚔️ Batalla

```
POST /api/batalla/iniciar
  Body: { jugador1Id, jugador2Id }
  Response: { partida }

GET /api/batalla/{id}
  Response: { estado, jugador1, jugador2 }

POST /api/batalla/{id}/accion
  Body: { type, payload }
  Response: { nuevoEstado }
```

---

## 🎁 Sobres

```
POST /api/sobres/abrir
  Body: { cantidad: 1 }
  Response: { cartas[], totalCoste }

GET /api/jugador/sobres
  Response: { saldoMonedas, historialAbertura }
```

---

## 🔌 WebSocket

```
WS /ws/batalla/{battleId}

Mensajes:
├─ ACTION { type, data }
├─ STATE_UPDATE { estado }
└─ ERROR { mensaje }
```

---

*Próximo: [Patrones de Diseño](/docs/tecnica/patrones-diseño)*
