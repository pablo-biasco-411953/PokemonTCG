---
sidebar_position: 5
title: "🔌 Flujo WebSocket"
---

# Flujo WebSocket - Lobby Multiplayer

> Conexion WebSocket para sincronizar jugadores en el lobby 3D

---

## Arquitectura WebSocket

```mermaid
graph TD
    subgraph Clientes
        C1["Jugador A (Browser)"]
        C2["Jugador B (Browser)"]
        C3["Jugador C (Browser)"]
    end

    subgraph "Backend (Spring WebSocket)"
        WC[WebSocketConfig<br/>/lobby-ws]
        LWH[LobbyWebSocketHandler]
        SESSIONS["sessions Map<br/>WebSocketSession → username"]
    end

    C1 -->|ws://host/lobby-ws| WC
    C2 -->|ws://host/lobby-ws| WC
    C3 -->|ws://host/lobby-ws| WC
    WC --> LWH
    LWH --> SESSIONS
```

---

## Ciclo de Conexion

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant WS as WebSocket /lobby-ws
    participant LWH as LobbyWebSocketHandler
    participant OTHERS as Otros Clientes

    FE->>WS: Conectar ws://host/lobby-ws
    WS->>LWH: afterConnectionEstablished(session)
    LWH->>LWH: Registrar session

    FE->>LWH: JOIN mensaje (username, characterId, colors)
    LWH->>LWH: Mapear session → username
    LWH->>OTHERS: Broadcast: nuevo jugador conectado

    loop Cada 50ms (movimiento)
        FE->>LWH: MOVE (x, y, z, rotation, animation)
        LWH->>OTHERS: Broadcast posicion a todos
    end

    Note over FE,OTHERS: El jugador se desconecta

    WS->>LWH: afterConnectionClosed(session)
    LWH->>LWH: Esperar 2.5s (grace period)
    alt No se reconecta
        LWH->>OTHERS: Broadcast: jugador desconectado
        LWH->>LWH: Limpiar session
    else Se reconecta
        LWH->>LWH: Restaurar session
    end
```

---

## Tipos de Mensaje

Todos los mensajes se envian como texto plano con formato `TIPO|payload`.

### Cliente a Servidor

| Tipo | Formato | Descripcion |
|------|---------|-------------|
| `JOIN` | `JOIN\|username\|characterId\|skinColor\|hairColor\|eyeColor` | Unirse al lobby |
| `MOVE` | `MOVE\|x\|y\|z\|rotationY\|animation` | Actualizar posicion |
| `CHAT` | `CHAT\|username\|mensaje` | Enviar mensaje de chat |
| `EMOTE` | `EMOTE\|username\|emoteId` | Enviar emote |
| `CHALLENGE` | `CHALLENGE\|fromUser\|toUser` | Desafiar a otro jugador |

### Servidor a Cliente (Broadcast)

| Tipo | Formato | Descripcion |
|------|---------|-------------|
| `PLAYER_JOIN` | `PLAYER_JOIN\|username\|characterId\|colors...` | Nuevo jugador en el lobby |
| `PLAYER_MOVE` | `PLAYER_MOVE\|username\|x\|y\|z\|rotY\|anim` | Posicion actualizada |
| `PLAYER_LEAVE` | `PLAYER_LEAVE\|username` | Jugador salio del lobby |
| `CHAT` | `CHAT\|username\|mensaje` | Mensaje de chat |
| `EMOTE` | `EMOTE\|username\|emoteId` | Emote recibido |
| `CHALLENGE` | `CHALLENGE\|fromUser\|toUser` | Desafio recibido |

---

## Sincronizacion de Jugadores

```mermaid
graph TD
    subgraph "Jugador A (Local)"
        LA[Avatar Local] --> |Teclado/Mouse| POS_A[Posicion Local]
        POS_A --> |MOVE mensaje| WS_OUT[WebSocket Send]
    end

    subgraph Backend
        WS_IN[WebSocket Receive] --> BC[Broadcast a todos]
    end

    subgraph "Jugador B (Remoto en pantalla de A)"
        WS_RCV[WebSocket Receive] --> INTERP[Interpolacion]
        INTERP --> NPC[OtherPlayerNPC]
        NPC --> |Render| SCENE[Three.js Scene]
    end

    WS_OUT --> WS_IN
    BC --> WS_RCV
```

### Interpolacion

Los jugadores remotos no se teletransportan. El frontend usa **interpolacion lineal** para suavizar el movimiento:

```typescript
// Cada frame de render
otherPlayer.root.position.lerp(otherPlayer.targetPosition, 0.15);
```

---

## Grace Period de Desconexion

```mermaid
graph TD
    A[Conexion perdida] --> B[Iniciar timer 2.5s]
    B --> C{Se reconecto?}
    C -->|Si| D[Restaurar session]
    C -->|No, timeout| E[Broadcast PLAYER_LEAVE]
    E --> F[Limpiar recursos]
```

El grace period de **2.5 segundos** evita que recargas rapidas de pagina o micro-desconexiones muestren al jugador saliendo y entrando.

---

## Chat y Burbujas

```mermaid
sequenceDiagram
    participant A as Jugador A
    participant S as Servidor
    participant B as Jugador B

    A->>S: CHAT|Pablo|Hola!
    S->>A: CHAT|Pablo|Hola!
    S->>B: CHAT|Pablo|Hola!

    Note over A: Muestra burbuja sobre avatar propio
    Note over B: Muestra burbuja sobre avatar de Pablo
```

Los mensajes de chat aparecen como **burbujas 3D** flotando sobre el avatar del jugador que los envio, visibles para todos los jugadores en el lobby.

---

## Configuracion del Servidor

**Archivo**: `config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lobbyWebSocketHandler(), "/lobby-ws")
                .setAllowedOrigins("*");
    }
}
```

- Endpoint: `/lobby-ws`
- CORS: Abierto (`*`)
- Protocolo: WebSocket nativo (no STOMP ni SockJS)
