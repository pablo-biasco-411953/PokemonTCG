package com.pokemon.tcg.service;

import com.pokemon.tcg.dto.lobby.LobbyRoomRequest;
import com.pokemon.tcg.dto.lobby.LobbyRoomSnapshot;
import com.pokemon.tcg.dto.lobby.LobbyRoomStartResponse;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LobbyRoomServiceTest {

    private BattleEngineService battleEngineService;
    private MazoRepository mazoRepository;
    private LobbyRoomService service;

    @BeforeEach
    void setUp() {
        battleEngineService = mock(BattleEngineService.class);
        mazoRepository = mock(MazoRepository.class);
        service = new LobbyRoomService(battleEngineService, mazoRepository);
    }

    private Card card(String id) {
        Card c = new Card();
        c.setId(id);
        return c;
    }

    private Mazo mazoBase(String owner) {
        Jugador j = new Jugador(owner);
        Mazo m = new Mazo("Bosque", j);
        List<Card> cartas = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cartas.add(card("xy1-" + (i + 1)));
        }
        m.setCartas(cartas);
        return m;
    }

    private LobbyRoomRequest request(String username, Long mazoId) {
        LobbyRoomRequest req = new LobbyRoomRequest();
        req.setUsername(username);
        req.setMazoId(mazoId);
        req.setRoomName("Sala Test");
        return req;
    }

    // =================== listRooms ===================

    @Test
    void listRooms_sinRooms_retornaListaVacia() {
        List<LobbyRoomSnapshot> result = service.listRooms();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listRooms_conUsername_retornaListaVacia() {
        List<LobbyRoomSnapshot> result = service.listRooms("ash");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =================== getRoom ===================

    @Test
    void getRoom_noExiste_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getRoom("noexiste"));
    }

    @Test
    void getRoom_conUsername_noExiste_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getRoom("noexiste", "ash"));
    }

    // =================== getRoomByMatchId ===================

    @Test
    void getRoomByMatchId_noExiste_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getRoomByMatchId("noexiste"));
    }

    // =================== createRoom ===================

    @Test
    void createRoom_sinUsername_lanzaExcepcion() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomRequest req = request(null, 1L);
        assertThrows(IllegalArgumentException.class,
                () -> service.createRoom(null, req));
    }

    @Test
    void createRoom_sinMazo_lanzaExcepcion() {
        when(mazoRepository.findById(99L)).thenReturn(Optional.empty());

        LobbyRoomRequest req = request("ash", 99L);
        assertThrows(IllegalArgumentException.class,
                () -> service.createRoom("ash", req));
    }

    @Test
    void createRoom_exitoso_retornaSnapshot() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomRequest req = request("ash", 1L);
        LobbyRoomSnapshot result = service.createRoom("ash", req);

        assertNotNull(result);
        assertEquals("ash", result.getOwnerUsername());
    }

    @Test
    void createRoom_apareceEnListRooms() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        List<LobbyRoomSnapshot> rooms = service.listRooms();

        assertEquals(1, rooms.size());
    }

    @Test
    void createRoom_conPassword_retornaLocked() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomRequest req = request("ash", 1L);
        req.setPassword("secret");
        LobbyRoomSnapshot result = service.createRoom("ash", req);

        assertTrue(result.isLocked());
    }

    @Test
    void createRoom_sinPassword_retornaNoLocked() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        LobbyRoomSnapshot result = service.createRoom("ash", request("ash", 1L));

        assertFalse(result.isLocked());
    }

    // =================== joinRoom ===================

    @Test
    void joinRoom_roomNoExiste_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.joinRoom("noexiste", "misty", new LobbyRoomRequest()));
    }

    @Test
    void joinRoom_exitoso_agrega_guest() {
        Mazo mazoOwner = mazoBase("ash");
        Mazo mazoGuest = mazoBase("misty");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazoOwner));
        when(mazoRepository.findById(2L)).thenReturn(Optional.of(mazoGuest));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        LobbyRoomRequest joinReq = request("misty", 2L);
        LobbyRoomSnapshot result = service.joinRoom(roomId, "misty", joinReq);

        assertEquals("misty", result.getGuestUsername());
    }

    @Test
    void joinRoom_salaConGuest_terceroIntenta_lanzaExcepcion() {
        Mazo mazoOwner = mazoBase("ash");
        Mazo mazoGuest = mazoBase("misty");
        Mazo mazoBrock = mazoBase("brock");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazoOwner));
        when(mazoRepository.findById(2L)).thenReturn(Optional.of(mazoGuest));
        when(mazoRepository.findById(3L)).thenReturn(Optional.of(mazoBrock));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();
        service.joinRoom(roomId, "misty", request("misty", 2L));

        assertThrows(RuntimeException.class,
                () -> service.joinRoom(roomId, "brock", request("brock", 3L)));
    }

    // =================== leaveRoom ===================

    @Test
    void leaveRoom_propietario_eliminaSala() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        service.leaveRoom(roomId, "ash");

        assertTrue(service.listRooms().isEmpty());
    }

    @Test
    void leaveRoom_noExiste_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.leaveRoom("noexiste", "ash"));
    }

    // =================== addBot ===================

    @Test
    void addBot_exitoso_agrega_bot() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        LobbyRoomSnapshot result = service.addBot(roomId, "ash", "NORMAL");

        assertTrue(result.isGuestBot());
    }

    @Test
    void addBot_noEsPropietario_lanzaExcepcion() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        assertThrows(RuntimeException.class,
                () -> service.addBot(roomId, "misty", "NORMAL"));
    }

    // =================== isSpectator ===================

    @Test
    void isSpectator_usernameNull_retornaFalse() {
        assertFalse(service.isSpectator("match-123", null));
    }

    @Test
    void isSpectator_noExisteMatch_retornaFalse() {
        assertFalse(service.isSpectator("noexiste", "spectator"));
    }

    // =================== setReady ===================

    @Test
    void setReady_propietario_marcaListo() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        LobbyRoomSnapshot result = service.setReady(roomId, "ash", true, 1L);

        assertTrue(result.isOwnerReady());
    }

    @Test
    void setReady_roomNoExiste_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.setReady("noexiste", "ash", true, 1L));
    }

    // =================== kickGuest ===================

    @Test
    void kickGuest_sinGuest_noFalla() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        LobbyRoomSnapshot result = service.kickGuest(roomId, "ash");
        assertNull(result.getGuestUsername());
    }

    // =================== addChat ===================

    @Test
    void addChat_exitoso_retornaSnapshot() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        LobbyRoomSnapshot result = service.addChat(roomId, "ash", "Hola!");

        assertNotNull(result);
        assertFalse(result.getChat().isEmpty());
    }

    @Test
    void addChat_roomNoExiste_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addChat("noexiste", "ash", "Hola!"));
    }

    // =================== updateSettings ===================

    @Test
    void updateSettings_propietario_actualizaTiempoTurno() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        LobbyRoomSnapshot result = service.updateSettings(roomId, "ash", 60, null);

        assertEquals(60, result.getTurnTimeSeconds());
    }

    @Test
    void updateSettings_noEsPropietario_lanzaExcepcion() {
        Mazo mazo = mazoBase("ash");
        when(mazoRepository.findById(1L)).thenReturn(Optional.of(mazo));

        service.createRoom("ash", request("ash", 1L));
        String roomId = service.listRooms().get(0).getId();

        assertThrows(RuntimeException.class,
                () -> service.updateSettings(roomId, "misty", 60, null));
    }
}
