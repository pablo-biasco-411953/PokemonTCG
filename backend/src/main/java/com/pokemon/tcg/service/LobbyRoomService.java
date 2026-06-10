package com.pokemon.tcg.service;

import com.pokemon.tcg.dto.lobby.LobbyRoomRequest;
import com.pokemon.tcg.dto.lobby.LobbyRoomSnapshot;
import com.pokemon.tcg.dto.lobby.LobbyRoomStartResponse;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.lobby.LobbyRoom;
import com.pokemon.tcg.model.lobby.LobbyRoomChatMessage;
import com.pokemon.tcg.model.lobby.LobbyRoomReaction;
import com.pokemon.tcg.model.lobby.LobbyRoomStatus;
import com.pokemon.tcg.repository.MazoRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyRoomService {
    private final Map<String, LobbyRoom> rooms = new ConcurrentHashMap<>();
    private final BattleEngineService battleEngineService;
    private final MazoRepository mazoRepository;

    public LobbyRoomService(BattleEngineService battleEngineService, MazoRepository mazoRepository) {
        this.battleEngineService = battleEngineService;
        this.mazoRepository = mazoRepository;
    }

    public List<LobbyRoomSnapshot> listRooms() {
        return listRooms(null);
    }

    public List<LobbyRoomSnapshot> listRooms(String username) {
        rooms.values().removeIf(room -> !syncRoomStatus(room));
        return rooms.values().stream()
                .sorted(Comparator.comparing(LobbyRoom::getUpdatedAt).reversed())
                .map(room -> toSnapshot(room, username))
                .toList();
    }

    public LobbyRoomSnapshot getRoom(String roomId) {
        return getRoom(roomId, null);
    }

    public LobbyRoomSnapshot getRoom(String roomId, String username) {
        LobbyRoom room = requireRoom(roomId);
        if (!syncRoomStatus(room)) {
            rooms.remove(roomId);
            throw new IllegalArgumentException("La sala ya finalizo.");
        }
        return toSnapshot(room, username);
    }

    public LobbyRoomSnapshot getRoomByMatchId(String matchId) {
        return getRoomByMatchId(matchId, null);
    }

    public LobbyRoomSnapshot getRoomByMatchId(String matchId, String username) {
        LobbyRoom room = rooms.values().stream()
                .filter(r -> matchId != null && matchId.equals(r.getMatchId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sala de la partida no encontrada."));
        if (!syncRoomStatus(room)) {
            rooms.remove(room.getId());
            throw new IllegalArgumentException("La sala ya finalizo.");
        }
        return toSnapshot(room, username);
    }

    public LobbyRoomSnapshot createRoom(String username, LobbyRoomRequest request) {
        requireUsername(username);
        ensureUserCanEnterRoom(username, null);
        Mazo deck = requireDeck(request.getMazoId());

        LobbyRoom room = new LobbyRoom();
        room.setName(cleanRoomName(request.getRoomName(), username));
        room.setOwnerUsername(username);
        room.setOwnerMazoId(deck.getId());
        room.setOwnerDeckName(deck.getNombre());
        room.setOwnerReady(false);
        String password = normalizePassword(request.getPassword());
        room.setHasPassword(password != null);
        room.setPasswordHash(password == null ? null : hashPassword(password));
        room.getChat().add(new LobbyRoomChatMessage("SISTEMA", username + " creo la sala.", true));
        touch(room);
        rooms.put(room.getId(), room);
        return toSnapshot(room);
    }

    public LobbyRoomSnapshot joinRoom(String roomId, String username, LobbyRoomRequest request) {
        requireUsername(username);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            ensureUserCanEnterRoom(username, roomId);
            assertPassword(room, request.getPassword());
            if (username.equals(room.getOwnerUsername()) || username.equals(room.getGuestUsername())) {
                return toSnapshot(room);
            }
            if (room.getStatus() == LobbyRoomStatus.IN_PROGRESS) {
                if (room.getSpectators().add(username)) {
                    room.getChat().add(new LobbyRoomChatMessage("SISTEMA", username + " esta mirando la partida.", true));
                    trimChat(room);
                    touch(room);
                }
                return toSnapshot(room, username);
            }
            if (room.getStatus() != LobbyRoomStatus.OPEN) {
                throw new IllegalStateException("La sala ya finalizo.");
            }
            if (room.getGuestUsername() != null && !username.equals(room.getGuestUsername())) {
                throw new IllegalStateException("La sala ya esta completa.");
            }
            Mazo deck = requireDeck(request.getMazoId());
            room.setGuestUsername(username);
            room.setGuestMazoId(deck.getId());
            room.setGuestDeckName(deck.getNombre());
            room.setGuestReady(false);
            room.setGuestBot(false);
            room.getChat().add(new LobbyRoomChatMessage("SISTEMA", username + " se unio a la sala.", true));
            touch(room);
            return toSnapshot(room);
        }
    }

    public LobbyRoomSnapshot leaveRoom(String roomId, String username) {
        requireUsername(username);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            boolean participant = username.equals(room.getOwnerUsername())
                    || username.equals(room.getGuestUsername());
            if (participant && room.getStatus() == LobbyRoomStatus.IN_PROGRESS) {
                if (room.getMatchId() != null) {
                    battleEngineService.rendirse(room.getMatchId(), username);
                }
                room.setStatus(LobbyRoomStatus.FINISHED);
                room.getChat().add(new LobbyRoomChatMessage(
                        "SISTEMA",
                        username + " abandono la partida. La sala fue cerrada.",
                        true
                ));
                touch(room);
                rooms.remove(roomId);
                return toSnapshot(room);
            }
            if (username.equals(room.getOwnerUsername())) {
                room.setStatus(LobbyRoomStatus.FINISHED);
                rooms.remove(roomId);
                room.getChat().add(new LobbyRoomChatMessage("SISTEMA", "La sala fue cerrada.", true));
                touch(room);
                return toSnapshot(room);
            }
            if (username.equals(room.getGuestUsername())) {
                room.getChat().add(new LobbyRoomChatMessage("SISTEMA", username + " salio de la sala.", true));
                room.setGuestUsername(null);
                room.setGuestMazoId(null);
                room.setGuestDeckName(null);
                room.setGuestReady(false);
            }
            room.getSpectators().remove(username);
            touch(room);
            return toSnapshot(room);
        }
    }

    public LobbyRoomSnapshot kickGuest(String roomId, String ownerUsername) {
        requireUsername(ownerUsername);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            requireOwner(room, ownerUsername);
            if (room.getGuestUsername() != null) {
                room.getChat().add(new LobbyRoomChatMessage("SISTEMA", room.getGuestUsername() + " fue retirado de la sala.", true));
            }
            room.setGuestUsername(null);
            room.setGuestMazoId(null);
            room.setGuestDeckName(null);
            room.setGuestReady(false);
            room.setGuestBot(false);
            touch(room);
            return toSnapshot(room);
        }
    }

    public LobbyRoomSnapshot addBot(String roomId, String ownerUsername, String difficulty) {
        requireUsername(ownerUsername);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            requireOwner(room, ownerUsername);
            if (room.getStatus() != LobbyRoomStatus.OPEN) {
                throw new IllegalStateException("La partida ya empezo.");
            }
            if (room.getGuestUsername() != null) {
                throw new IllegalStateException("La sala ya tiene rival.");
            }
            String normalizedDifficulty = normalizeBotDifficulty(difficulty);
            room.setGuestUsername("BOT");
            room.setGuestDeckName("Mazo bot " + normalizedDifficulty.toLowerCase());
            room.setGuestMazoId(null);
            room.setGuestReady(true);
            room.setGuestBot(true);
            room.setBotDifficulty(normalizedDifficulty);
            room.getChat().add(new LobbyRoomChatMessage(
                    "SISTEMA",
                    "Se sumo un bot " + normalizedDifficulty.toLowerCase() + " listo para jugar.",
                    true
            ));
            touch(room);
            return toSnapshot(room);
        }
    }

    public LobbyRoomSnapshot setReady(String roomId, String username, boolean ready, Long mazoId) {
        requireUsername(username);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            if (room.getStatus() != LobbyRoomStatus.OPEN) {
                throw new IllegalStateException("La partida ya empezo.");
            }
            if (username.equals(room.getOwnerUsername())) {
                if (mazoId != null) {
                    Mazo deck = requireDeck(mazoId);
                    room.setOwnerMazoId(deck.getId());
                    room.setOwnerDeckName(deck.getNombre());
                }
                room.setOwnerReady(ready);
            } else if (username.equals(room.getGuestUsername())) {
                if (mazoId != null) {
                    Mazo deck = requireDeck(mazoId);
                    room.setGuestMazoId(deck.getId());
                    room.setGuestDeckName(deck.getNombre());
                }
                room.setGuestReady(ready);
            } else {
                throw new IllegalStateException("No estas dentro de esta sala.");
            }
            room.getChat().add(new LobbyRoomChatMessage("SISTEMA", username + (ready ? " esta listo." : " dejo de estar listo."), true));
            trimChat(room);
            touch(room);
            return toSnapshot(room);
        }
    }

    public LobbyRoomStartResponse startRoom(String roomId, String ownerUsername) {
        requireUsername(ownerUsername);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            requireOwner(room, ownerUsername);
            if (room.getStatus() != LobbyRoomStatus.OPEN) {
                return new LobbyRoomStartResponse(toSnapshot(room), room.getMatchId());
            }
            if (room.getGuestUsername() == null || (!room.isGuestBot() && room.getGuestMazoId() == null)) {
                throw new IllegalStateException("Falta un rival para empezar.");
            }
            if (!room.isOwnerReady() || !room.isGuestReady()) {
                throw new IllegalStateException("Ambos jugadores deben estar listos.");
            }

            Partida partida = room.isGuestBot()
                    ? battleEngineService.startBattle(
                            room.getOwnerUsername(),
                            room.getOwnerMazoId(),
                            room.getBotDifficulty()
                    )
                    : battleEngineService.startBattleOnline(
                            room.getOwnerUsername(), room.getOwnerMazoId(),
                            room.getGuestUsername(), room.getGuestMazoId()
                    );
            room.setMatchId(partida.getId());
            room.setStatus(LobbyRoomStatus.IN_PROGRESS);
            room.getChat().add(new LobbyRoomChatMessage("SISTEMA", "La batalla comenzo.", true));
            touch(room);
            return new LobbyRoomStartResponse(toSnapshot(room), partida.getId());
        }
    }

    public LobbyRoomStartResponse spectateRoom(String roomId, String username, String password) {
        requireUsername(username);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            assertPassword(room, password);
            if (room.getStatus() != LobbyRoomStatus.OPEN && room.getStatus() != LobbyRoomStatus.IN_PROGRESS) {
                throw new IllegalStateException("La sala ya finalizo.");
            }
            if (username.equals(room.getOwnerUsername()) || username.equals(room.getGuestUsername())) {
                throw new IllegalStateException("Los jugadores deben entrar como participantes, no como espectadores.");
            }
            if (room.getSpectators().add(username)) {
                room.getChat().add(new LobbyRoomChatMessage("SISTEMA", username + " esta mirando la sala.", true));
                trimChat(room);
                touch(room);
            }
            return new LobbyRoomStartResponse(toSnapshot(room, username), room.getMatchId());
        }
    }

    public boolean isSpectator(String matchId, String username) {
        if (matchId == null || username == null || username.isBlank()) return false;
        return rooms.values().stream()
                .anyMatch(room -> matchId.equals(room.getMatchId()) && room.getSpectators().contains(username));
    }

    public LobbyRoomSnapshot addChat(String roomId, String username, String text) {
        requireUsername(username);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            boolean participant = username.equals(room.getOwnerUsername())
                    || username.equals(room.getGuestUsername())
                    || room.getSpectators().contains(username);
            if (!participant) throw new IllegalStateException("No estas dentro de esta sala.");

            String clean = text == null ? "" : text.trim();
            if (!clean.isEmpty()) {
                if (clean.length() > 160) clean = clean.substring(0, 160);
                room.getChat().add(new LobbyRoomChatMessage(username, clean, false));
                trimChat(room);
                touch(room);
            }
            return toSnapshot(room);
        }
    }

    public LobbyRoomSnapshot addReaction(String roomId, String username, String reaction) {
        requireUsername(username);
        LobbyRoom room = requireRoom(roomId);
        synchronized (room) {
            String clean = reaction == null ? "" : reaction.trim();
            if (clean.isEmpty()) clean = "spark";
            if (clean.length() > 24) clean = clean.substring(0, 24);
            room.getReactions().add(new LobbyRoomReaction(UUID.randomUUID().toString(), username, clean, System.currentTimeMillis()));
            trimReactions(room);
            touch(room);
            return toSnapshot(room);
        }
    }

    public LobbyRoomSnapshot addReactionByMatchId(String matchId, String username, String reaction) {
        LobbyRoom room = rooms.values().stream()
                .filter(r -> matchId != null && matchId.equals(r.getMatchId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sala de la partida no encontrada."));
        return addReaction(room.getId(), username, reaction);
    }

    private LobbyRoomSnapshot toSnapshot(LobbyRoom room) {
        return toSnapshot(room, null);
    }

    private LobbyRoomSnapshot toSnapshot(LobbyRoom room, String username) {
        LobbyRoomSnapshot s = new LobbyRoomSnapshot();
        s.setId(room.getId());
        s.setName(room.getName());
        s.setStatus(room.getStatus());
        s.setLocked(room.isHasPassword());
        s.setOwnerUsername(room.getOwnerUsername());
        s.setOwnerDeckName(room.getOwnerDeckName());
        s.setOwnerReady(room.isOwnerReady());
        s.setGuestUsername(room.getGuestUsername());
        s.setGuestDeckName(room.getGuestDeckName());
        s.setGuestReady(room.isGuestReady());
        s.setGuestBot(room.isGuestBot());
        s.setBotDifficulty(room.getBotDifficulty());
        s.setPlayerCount(room.getGuestUsername() == null ? 1 : 2);
        s.setSpectatorCount(room.getSpectators().size());
        s.setMatchId(room.getMatchId());
        s.setCanJoin(room.getStatus() == LobbyRoomStatus.OPEN && room.getGuestUsername() == null);
        s.setCanSpectate(room.getStatus() == LobbyRoomStatus.OPEN || room.getStatus() == LobbyRoomStatus.IN_PROGRESS);
        s.setCurrentUserSpectator(username != null && room.getSpectators().contains(username));
        s.setUpdatedAt(room.getUpdatedAt());
        int from = Math.max(0, room.getChat().size() - 30);
        s.setChat(new java.util.ArrayList<>(room.getChat().subList(from, room.getChat().size())));
        trimReactions(room);
        s.setReactions(new java.util.ArrayList<>(room.getReactions()));
        return s;
    }

    private String normalizeBotDifficulty(String difficulty) {
        if (difficulty == null) return "NORMAL";
        return switch (difficulty.trim().toUpperCase()) {
            case "EASY" -> "EASY";
            case "HARD" -> "HARD";
            default -> "NORMAL";
        };
    }

    private LobbyRoom requireRoom(String roomId) {
        LobbyRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Sala no encontrada.");
        return room;
    }

    private Mazo requireDeck(Long mazoId) {
        if (mazoId == null) throw new IllegalArgumentException("Selecciona un mazo.");
        Mazo deck = mazoRepository.findById(mazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado."));
        if (deck.getCartas() == null || deck.getCartas().size() < 60) {
            throw new IllegalStateException("El mazo debe tener 60 cartas.");
        }
        return deck;
    }

    private void ensureUserCanEnterRoom(String username, String allowedRoomId) {
        rooms.values().stream()
                .filter(room -> room.getStatus() != LobbyRoomStatus.FINISHED)
                .filter(room -> allowedRoomId == null || !room.getId().equals(allowedRoomId))
                .filter(room -> username.equals(room.getOwnerUsername())
                        || username.equals(room.getGuestUsername())
                        || room.getSpectators().contains(username))
                .findFirst()
                .ifPresent(room -> {
                    throw new IllegalStateException("Ya estas en una sala activa. Sali de esa sala antes de crear o entrar a otra.");
                });
    }

    private void requireOwner(LobbyRoom room, String username) {
        if (!username.equals(room.getOwnerUsername())) {
            throw new IllegalStateException("Solo el dueno de la sala puede hacer eso.");
        }
    }

    private void assertPassword(LobbyRoom room, String password) {
        if (!room.isHasPassword()) return;
        String clean = normalizePassword(password);
        if (clean == null || !hashPassword(clean).equals(room.getPasswordHash())) {
            throw new IllegalArgumentException("Clave incorrecta.");
        }
    }

    private void requireUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Usuario requerido.");
        }
    }

    private String cleanRoomName(String requested, String username) {
        String clean = requested == null ? "" : requested.trim();
        if (clean.isEmpty()) clean = "Sala de " + username;
        return clean.length() > 42 ? clean.substring(0, 42) : clean;
    }

    private String normalizePassword(String password) {
        String clean = password == null ? "" : password.trim();
        return clean.isEmpty() ? null : clean;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger la clave de sala.", e);
        }
    }

    private void touch(LobbyRoom room) {
        room.setUpdatedAt(System.currentTimeMillis());
    }

    private void trimChat(LobbyRoom room) {
        while (room.getChat().size() > 50) {
            room.getChat().remove(0);
        }
    }

    private void trimReactions(LobbyRoom room) {
        long cutoff = System.currentTimeMillis() - 6000;
        room.getReactions().removeIf(reaction -> reaction.getSentAt() < cutoff);
        while (room.getReactions().size() > 18) {
            room.getReactions().remove(0);
        }
    }

    private boolean syncRoomStatus(LobbyRoom room) {
        if (room.getStatus() == LobbyRoomStatus.FINISHED) return false;
        if (room.getStatus() != LobbyRoomStatus.IN_PROGRESS || room.getMatchId() == null) return true;
        try {
            Partida partida = battleEngineService.getEstadoPartida(room.getMatchId());
            if (partida == null || partida.getFaseActual() == Partida.Fase.FIN_PARTIDA) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
