# Arquitectura del Sistema

## Visión general

```
Angular FE  ←──── REST API ────→  Spring Boot BE  ←──→  Base de datos
                    │
              WebSocket (lobby)
```

El FE y el BE son proyectos independientes. Se comunican vía:
- **REST API**: operaciones CRUD, autenticación, lógica de juego por turnos.
- **WebSocket**: comunicación en tiempo real para el lobby multijugador (`LobbyWebSocketHandler`).

## Backend — capas

| Paquete | Rol |
|---------|-----|
| `controller/` | Endpoints REST — recibe y responde requests |
| `service/` | Lógica de negocio — reglas del juego, validaciones |
| `repository/` | Acceso a datos — Spring Data JPA |
| `model/` | Entidades del dominio (Jugador, Card, Mazo, Partida) |
| `dto/` | Objetos de transferencia para requests y responses |
| `config/` | Configuración de CORS, WebSocket, carga inicial de datos |

## Frontend — features

| Feature | Descripción |
|---------|-------------|
| `battle/` | Tablero de batalla, turnos, ataques, panel de debug |
| `lobby/` | Sala de espera, apertura de sobres, matchmaking |
| `deck-builder/` | Constructor de mazos |
| `login/` | Autenticación y registro |

## Flujo de una partida

1. Jugador entra al lobby (WebSocket).
2. Se arma una partida (`BattleService.startBattle()`).
3. Cada turno: el jugador elige acción → FE llama al endpoint → BE valida y ejecuta → devuelve estado actualizado.
4. Si un Pokémon queda KO (`BattleKoService`), se resuelve la eliminación.
5. La partida termina cuando un jugador no tiene más Pokémon activos.

## Bot IA

`BotAIService` implementa un oponente automático. Toma decisiones basadas en el estado actual del tablero. Se activa cuando la partida se inicia con `isBotGame = true`.
