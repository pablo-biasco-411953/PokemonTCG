---
sidebar_position: 8
title: 📡 WebSocket y Lobby
---

# 📡 WebSocket y Lobby - Comunicación en Tiempo Real

---

## Configuración WebSocket

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(battleHandler(), "/ws/battle/{battleId}")
            .setAllowedOrigins("*");
    }
    
    @Bean
    public BattleWebSocketHandler battleHandler() {
        return new BattleWebSocketHandler();
    }
}
```

---

## Lobby Flow

```
Jugador A                  Servidor               Jugador B
   |
   | 1. Buscar partida
   |─────────────────→
   |                    Esperar oponente
   |                         ✓ Encontrado
   |←─────────────────────────────
   | 2. Sala encontrada
   |
   | 3. Conectar WebSocket
   |─────────────────→ /ws/battle/123
   |                         │
   |                    Conectar J B
   |←─────────────────────────┤
   | 4. Estado sincronizado
   |
   | 5. Decidir primer turno
   |─────────────────→ CHOOSE_TURN
   |                         │
   |←─────────────────────────┤
   | 6. Batalla inicia
   |
```

---

## Mensajes WebSocket

```json
// Acción
{
  "type": "ACTION",
  "action": {
    "type": "JUGAR_POKEMON",
    "pokemonId": "025"
  }
}

// Estado
{
  "type": "STATE",
  "state": {
    "currentPlayer": 1,
    "phase": "MAIN",
    "player1Prizes": 3
  }
}

// Error
{
  "type": "ERROR",
  "message": "Acción inválida"
}
```

---

*Próximo: [Autenticación](/docs/tecnica/autenticacion)*
