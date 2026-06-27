package com.pokemon.tcg.service;

import com.pokemon.tcg.dto.lobby.LobbyRoomRequest;
import com.pokemon.tcg.dto.lobby.LobbyRoomSnapshot;
import com.pokemon.tcg.dto.lobby.LobbyRoomStartResponse;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.lobby.LobbyRoomStatus;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LobbyRoomServiceCoverageTest {

    private BattleEngineService battleEngineService;
    private MazoRepository mazoRepository;
    private LobbyRoomService service;

    @BeforeEach
    void setUp() {
        battleEngineService = mock(BattleEngineService.class);
        mazoRepository = mock(MazoRepository.class);
        service = new LobbyRoomService(battleEngineService, mazoRepository);
    }

    private Mazo mazoBase(String owner, Long id) {
        Jugador j = new Jugador(owner);
        Mazo m = new Mazo("Deck", j);
        m.setId(id);
        List<Card> cartas = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Card c = new Card();
            c.setId("xy1-" + (i + 1));
            cartas.add(c);
        }
        m.setCartas(cartas);
        return m;
    }

    private LobbyRoomRequest request(String pass, Long mazoId) {
        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setPassword(pass);
        req.setMazoId(mazoId);
        return req;
    }

    @Test
    void syncRoomStatus_whenPartidaFinished_removesRoom() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.addBot(snap.getId(), "p1", "EASY");
        service.setReady(snap.getId(), "p1", true, 1L);
        
        Partida partidaMock = mock(Partida.class);
        when(partidaMock.getId()).thenReturn("match-123");
        when(battleEngineService.startBattle(any(), any(), any())).thenReturn(partidaMock);
        
        service.startRoom(snap.getId(), "p1");
        
        // Match in progress. Now get room should work.
        when(partidaMock.getFaseActual()).thenReturn(Partida.Fase.INICIO);
        when(battleEngineService.getEstadoPartida("match-123")).thenReturn(partidaMock);
        assertNotNull(service.getRoom(snap.getId()));
        
        // Now mock getEstadoPartida to return FIN_PARTIDA
        when(partidaMock.getFaseActual()).thenReturn(Partida.Fase.FIN_PARTIDA);
        when(battleEngineService.getEstadoPartida("match-123")).thenReturn(partidaMock);
        
        // Trigger syncRoomStatus via getRoom
        assertThrows(IllegalArgumentException.class, () -> service.getRoom(snap.getId()));
    }

    @Test
    void syncRoomStatus_whenPartidaException_removesRoom() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.addBot(snap.getId(), "p1", "EASY");
        service.setReady(snap.getId(), "p1", true, 1L);
        
        Partida partidaMock = mock(Partida.class);
        when(partidaMock.getId()).thenReturn("match-123");
        when(battleEngineService.startBattle(any(), any(), any())).thenReturn(partidaMock);
        
        service.startRoom(snap.getId(), "p1");
        
        when(battleEngineService.getEstadoPartida("match-123")).thenThrow(new RuntimeException("DB error"));
        
        assertThrows(IllegalArgumentException.class, () -> service.getRoom(snap.getId()));
    }

    @Test
    void listRooms_removesFinishedRooms() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.addBot(snap.getId(), "p1", "EASY");
        service.setReady(snap.getId(), "p1", true, 1L);
        Partida partidaMock = mock(Partida.class);
        when(partidaMock.getId()).thenReturn("match-123");
        when(battleEngineService.startBattle(any(), any(), any())).thenReturn(partidaMock);
        service.startRoom(snap.getId(), "p1");
        
        when(partidaMock.getFaseActual()).thenReturn(Partida.Fase.FIN_PARTIDA);
        when(battleEngineService.getEstadoPartida("match-123")).thenReturn(partidaMock);

        List<LobbyRoomSnapshot> rooms = service.listRooms("p1");
        assertTrue(rooms.isEmpty());
    }

    @Test
    void getRoomByMatchId_throwsExceptionIfSyncFails() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.addBot(snap.getId(), "p1", "EASY");
        service.setReady(snap.getId(), "p1", true, 1L);
        Partida partidaMock = mock(Partida.class);
        when(partidaMock.getId()).thenReturn("match-123");
        when(battleEngineService.startBattle(any(), any(), any())).thenReturn(partidaMock);
        service.startRoom(snap.getId(), "p1");
        
        when(partidaMock.getFaseActual()).thenReturn(Partida.Fase.FIN_PARTIDA);
        when(battleEngineService.getEstadoPartida("match-123")).thenReturn(partidaMock);

        assertThrows(IllegalArgumentException.class, () -> service.getRoomByMatchId("match-123", "p1"));
    }
    
    @Test
    void updateSettings_validBotDifficultyAndTurnTime() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.addBot(snap.getId(), "p1", "EASY");
        
        LobbyRoomSnapshot updated = service.updateSettings(snap.getId(), "p1", 60, "HARD");
        
        assertEquals(60, updated.getTurnTimeSeconds());
        assertEquals("HARD", updated.getBotDifficulty());
        assertEquals("Mazo bot hard", updated.getGuestDeckName());
    }

    @Test
    void updateSettings_invalidDifficultyDefaultsToNormal() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.addBot(snap.getId(), "p1", "EASY");
        
        LobbyRoomSnapshot updated = service.updateSettings(snap.getId(), "p1", null, "WEIRD");
        
        assertEquals("NORMAL", updated.getBotDifficulty());
    }

    @Test
    void addChat_longMessageIsTrimmed() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        
        String longText = "a".repeat(200);
        LobbyRoomSnapshot updated = service.addChat(snap.getId(), "p1", longText);
        
        String savedChat = updated.getChat().get(updated.getChat().size() - 1).getText();
        assertEquals(160, savedChat.length());
    }

    @Test
    void cleanRoomName_longNameIsTrimmed() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomRequest req = request(null, 1L);
        req.setRoomName("a".repeat(50));
        
        LobbyRoomSnapshot snap = service.createRoom("p1", req);
        assertEquals(42, snap.getName().length());
    }

    @Test
    void addReactionByMatchId_works() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.addBot(snap.getId(), "p1", "EASY");
        service.setReady(snap.getId(), "p1", true, 1L);
        Partida partidaMock = mock(Partida.class);
        when(partidaMock.getId()).thenReturn("match-123");
        when(battleEngineService.startBattle(any(), any(), any())).thenReturn(partidaMock);
        service.startRoom(snap.getId(), "p1");
        
        LobbyRoomSnapshot updated = service.addReactionByMatchId("match-123", "p1", "smile");
        assertEquals("smile", updated.getReactions().get(0).getReaction());
    }
    
    @Test
    void joinRoom_whenStatusNotOpen_throwsException() {
        Mazo mazo = mazoBase("p1", 1L);
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot snap = service.createRoom("p1", request(null, 1L));
        service.leaveRoom(snap.getId(), "p1"); // Finished
        
        assertThrows(IllegalArgumentException.class, () -> service.joinRoom(snap.getId(), "p2", request(null, 1L)));
    }
}
