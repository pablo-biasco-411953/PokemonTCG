package com.pokemon.tcg.controller;

import com.pokemon.tcg.config.LobbyWebSocketHandler;
import com.pokemon.tcg.dto.lobby.LobbyRoomRequest;
import com.pokemon.tcg.dto.lobby.LobbyRoomSnapshot;
import com.pokemon.tcg.dto.lobby.LobbyRoomStartResponse;
import com.pokemon.tcg.service.LobbyRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lobby-rooms")
public class LobbyRoomController {
    private final LobbyRoomService lobbyRoomService;
    private final LobbyWebSocketHandler lobbyWebSocketHandler;

    public LobbyRoomController(LobbyRoomService lobbyRoomService, LobbyWebSocketHandler lobbyWebSocketHandler) {
        this.lobbyRoomService = lobbyRoomService;
        this.lobbyWebSocketHandler = lobbyWebSocketHandler;
    }

    @GetMapping
    public ResponseEntity<?> listRooms(@RequestHeader(value = "X-Username", required = false) String headerUsername) {
        return ResponseEntity.ok(lobbyRoomService.listRooms(headerUsername));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId,
                                     @RequestHeader(value = "X-Username", required = false) String headerUsername) {
        try {
            return ResponseEntity.ok(lobbyRoomService.getRoom(roomId, headerUsername));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<?> getRoomByMatch(@PathVariable String matchId,
                                            @RequestHeader(value = "X-Username", required = false) String headerUsername) {
        try {
            return ResponseEntity.ok(lobbyRoomService.getRoomByMatchId(matchId, headerUsername));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestHeader(value = "X-Username", required = false) String headerUsername,
                                        @RequestBody LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.createRoom(resolveUsername(headerUsername, request), request);
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId,
                                      @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                      @RequestBody LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.joinRoom(roomId, resolveUsername(headerUsername, request), request);
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId,
                                       @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                       @RequestBody(required = false) LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.leaveRoom(roomId, resolveUsername(headerUsername, request));
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/kick")
    public ResponseEntity<?> kickGuest(@PathVariable String roomId,
                                       @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                       @RequestBody(required = false) LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.kickGuest(roomId, resolveUsername(headerUsername, request));
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/bot")
    public ResponseEntity<?> addBot(@PathVariable String roomId,
                                    @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                    @RequestBody(required = false) LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.addBot(
                    roomId,
                    resolveUsername(headerUsername, request),
                    request == null ? null : request.getBotDifficulty()
            );
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/ready")
    public ResponseEntity<?> setReady(@PathVariable String roomId,
                                      @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                      @RequestBody LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.setReady(roomId, resolveUsername(headerUsername, request), request.isReady(), request.getMazoId());
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<?> startRoom(@PathVariable String roomId,
                                       @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                       @RequestBody(required = false) LobbyRoomRequest request) {
        try {
            LobbyRoomStartResponse response = lobbyRoomService.startRoom(roomId, resolveUsername(headerUsername, request));
            broadcastRoomsUpdated(response.getRoom());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/spectate")
    public ResponseEntity<?> spectateRoom(@PathVariable String roomId,
                                          @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                          @RequestBody LobbyRoomRequest request) {
        try {
            LobbyRoomStartResponse response = lobbyRoomService.spectateRoom(roomId, resolveUsername(headerUsername, request), request.getPassword());
            broadcastRoomsUpdated(response.getRoom());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/chat")
    public ResponseEntity<?> addChat(@PathVariable String roomId,
                                     @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                     @RequestBody LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.addChat(roomId, resolveUsername(headerUsername, request), request.getText());
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/reaction")
    public ResponseEntity<?> addReaction(@PathVariable String roomId,
                                         @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                         @RequestBody LobbyRoomRequest request) {
        try {
            String username = resolveUsername(headerUsername, request);
            String reaction = request != null && request.getText() != null ? request.getText().trim() : "";
            LobbyRoomSnapshot room = lobbyRoomService.addReaction(roomId, username, reaction);
            lobbyWebSocketHandler.broadcastToAll(Map.of(
                    "type", "ROOM_REACTION",
                    "roomId", roomId,
                    "username", username == null ? "Spectator" : username,
                    "reaction", reaction == null || reaction.isBlank() ? "spark" : reaction
            ));
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/match/{matchId}/reaction")
    public ResponseEntity<?> addMatchReaction(@PathVariable String matchId,
                                              @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                              @RequestBody LobbyRoomRequest request) {
        try {
            String username = resolveUsername(headerUsername, request);
            String reaction = request != null && request.getText() != null ? request.getText().trim() : "";
            LobbyRoomSnapshot room = lobbyRoomService.addReactionByMatchId(matchId, username, reaction);
            lobbyWebSocketHandler.broadcastToAll(Map.of(
                    "type", "ROOM_REACTION",
                    "roomId", room.getId(),
                    "matchId", matchId,
                    "username", username == null ? "Spectator" : username,
                    "reaction", reaction == null || reaction.isBlank() ? "spark" : reaction
            ));
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable String roomId,
                                            @RequestHeader(value = "X-Username", required = false) String headerUsername,
                                            @RequestBody LobbyRoomRequest request) {
        try {
            LobbyRoomSnapshot room = lobbyRoomService.updateSettings(
                    roomId,
                    resolveUsername(headerUsername, request),
                    request.getTurnTimeSeconds(),
                    request.getBotDifficulty()
            );
            broadcastRoomsUpdated(room);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String resolveUsername(String headerUsername, LobbyRoomRequest request) {
        if (headerUsername != null && !headerUsername.isBlank()) return headerUsername;
        return request == null ? null : request.getUsername();
    }

    private void broadcastRoomsUpdated(LobbyRoomSnapshot room) {
        lobbyWebSocketHandler.broadcastToAll(Map.of("type", "ROOMS_UPDATED", "room", room));
    }
}
