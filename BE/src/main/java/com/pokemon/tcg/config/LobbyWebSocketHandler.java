package com.pokemon.tcg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.dto.LobbyMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {

    // Almacena las sesiones de red activas mapeadas por nombre de usuario
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // Almacena el último estado conocido (posición, colores, avatar) de cada jugador
    private final Map<String, LobbyMessage> playersState = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Conexión TCP abierta, esperamos el paquete JOIN del cliente para registrar su nombre
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        LobbyMessage lobbyMsg = objectMapper.readValue(payload, LobbyMessage.class);
        
        if (lobbyMsg == null || lobbyMsg.getUsername() == null) return;
        
        String username = lobbyMsg.getUsername();
        String type = lobbyMsg.getType();

        if ("JOIN".equals(type)) {
            // Guardar el nombre en los atributos de sesión para identificarlo al desconectarse
            session.getAttributes().put("username", username);
            sessions.put(username, session);
            playersState.put(username, lobbyMsg);

            System.out.println("Jugador conectado al Lobby Online: " + username);

            // 1. Difundir este ingreso al resto de los jugadores conectados
            broadcastToOthers(username, payload);

            // 2. Enviar los estados de todos los jugadores que YA estaban conectados al nuevo jugador
            for (Map.Entry<String, LobbyMessage> entry : playersState.entrySet()) {
                if (!entry.getKey().equals(username)) {
                    try {
                        String existingPlayerJson = objectMapper.writeValueAsString(entry.getValue());
                        session.sendMessage(new TextMessage(existingPlayerJson));
                    } catch (IOException e) {
                        System.err.println("Error enviando estado de jugador existente a " + username + ": " + e.getMessage());
                    }
                }
            }
        } 
        else if ("MOVE".equals(type)) {
            // Actualizar la última posición física y variables de animación conocidas
            LobbyMessage cached = playersState.get(username);
            if (cached != null) {
                cached.setX(lobbyMsg.getX());
                cached.setY(lobbyMsg.getY());
                cached.setZ(lobbyMsg.getZ());
                cached.setRotY(lobbyMsg.getRotY());
                cached.setAnimation(lobbyMsg.getAnimation());
            } else {
                playersState.put(username, lobbyMsg);
            }

            // Difundir la actualización de movimiento al resto de los jugadores
            broadcastToOthers(username, payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            System.out.println("Jugador desconectado del Lobby Online: " + username);
            sessions.remove(username);
            playersState.remove(username);

            // Difundir evento LEAVE a los demás para remover su malla 3D de inmediato
            LobbyMessage leaveMsg = new LobbyMessage();
            leaveMsg.setType("LEAVE");
            leaveMsg.setUsername(username);
            
            String leaveJson = objectMapper.writeValueAsString(leaveMsg);
            broadcastToOthers(username, leaveJson);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // En caso de error de red, forzar cierre para disparar afterConnectionClosed y limpiar recursos
        try {
            session.close();
        } catch (IOException e) {
            // Ignorar fallas al cerrar conexión ya corrupta
        }
    }

    /**
     * Reenvía un mensaje JSON a todas las sesiones activas, excepto a la del emisor.
     */
    private void broadcastToOthers(String senderUsername, String jsonPayload) {
        TextMessage textMessage = new TextMessage(jsonPayload);
        sessions.forEach((username, session) -> {
            if (!username.equals(senderUsername) && session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    System.err.println("Falla al difundir socket a " + username + ": " + e.getMessage());
                }
            }
        });
    }
}
