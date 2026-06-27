package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MazoServiceExtendedTest {

    private MazoRepository mazoRepo;
    private JugadorRepository jugadorRepo;
    private CardRepository cardRepo;
    private MazoBackupService backupService;
    private MazoService service;

    @BeforeEach
    void setUp() {
        mazoRepo = mock(MazoRepository.class);
        jugadorRepo = mock(JugadorRepository.class);
        cardRepo = mock(CardRepository.class);
        backupService = mock(MazoBackupService.class);
        service = new MazoService(mazoRepo, jugadorRepo, cardRepo, backupService);
    }

    private Card basicPokemon(String id) {
        Card c = new Card();
        c.setId(id);
        c.setNombre("Pokemon " + id);
        c.setSupertype("Pokemon");
        c.setSubtypes(List.of("Basic"));
        return c;
    }

    private List<String> ids60(String cardId) {
        return Collections.nCopies(60, cardId);
    }

    /** Genera 60 IDs con max 4 copias de cada carta (15 cartas distintas x4). */
    private List<String> ids60Validos(List<Card> cartas) {
        List<String> ids = new ArrayList<>();
        for (Card c : cartas) {
            for (int i = 0; i < 4; i++) ids.add(c.getId());
        }
        return ids;
    }

    @Test
    void guardarMazo_jugadorNoEncontrado_lanzaExcepcion() {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.guardarMazo("Mazo", "noexiste", ids60("xy1-1")));
    }

    @Test
    void guardarMazo_cartasMenos60_lanzaExcepcion() {
        Jugador j = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        List<String> pocasCartas = Collections.nCopies(10, "xy1-1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarMazo("Mazo", "ash", pocasCartas));
        assertTrue(ex.getMessage().contains("60 cartas"));
    }

    @Test
    void guardarMazo_cartasMas60_lanzaExcepcion() {
        Jugador j = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        List<String> muchasCartas = Collections.nCopies(61, "xy1-1");
        assertThrows(IllegalArgumentException.class,
                () -> service.guardarMazo("Mazo", "ash", muchasCartas));
    }

    @Test
    void guardarMazo_cartasNull_lanzaExcepcion() {
        Jugador j = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);

        assertThrows(IllegalArgumentException.class,
                () -> service.guardarMazo("Mazo", "ash", null));
    }

    @Test
    void guardarMazo_cartaNoEncontradaEnBD_lanzaExcepcion() {
        Jugador j = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(cardRepo.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.guardarMazo("Mazo", "ash", ids60("xy1-999")));
    }

    @Test
    void guardarMazo_sinPokemonBasico_lanzaExcepcion() {
        Jugador j = new Jugador("ash");
        Card energia = new Card();
        energia.setId("xy1-e1");
        energia.setSupertype("Energy");
        energia.setSubtypes(List.of("Basic"));

        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(cardRepo.findById("xy1-e1")).thenReturn(Optional.of(energia));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.guardarMazo("Mazo", "ash", ids60("xy1-e1")));
        assertTrue(ex.getMessage().contains("Pokemon Basico"));
    }

    @Test
    void guardarMazo_llamaBackup() {
        Jugador j = new Jugador("ash");
        // Crear 15 Pokémon básicos distintos (4 copias cada uno = 60 cartas, cumple la regla)
        List<Card> pokemons = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Card c = basicPokemon("xy1-" + i);
            pokemons.add(c);
            when(cardRepo.findById("xy1-" + i)).thenReturn(Optional.of(c));
        }
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(mazoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.guardarMazo("Mazo", "ash", ids60Validos(pokemons));

        verify(backupService).backupAll();
    }

    @Test
    void listarMazos_jugadorNoEncontrado_lanzaExcepcion() {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.listarMazos("noexiste"));
    }

    @Test
    void listarMazos_retornaMazosDelJugador() {
        Jugador j = new Jugador("ash");
        Mazo m = new Mazo("Bosque", j);
        m.setCartas(new ArrayList<>());
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(mazoRepo.findByJugador(j)).thenReturn(List.of(m));

        List<Mazo> result = service.listarMazos("ash");

        assertEquals(1, result.size());
        assertEquals("Bosque", result.get(0).getNombre());
    }

    @Test
    void eliminarMazo_mazoNoExiste_lanzaExcepcion() {
        when(mazoRepo.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.eliminarMazo(99L));
    }

    @Test
    void eliminarMazo_exitoso_llamaDelete() {
        when(mazoRepo.existsById(1L)).thenReturn(true);

        service.eliminarMazo(1L);

        verify(mazoRepo).deleteById(1L);
        verify(backupService).backupAll();
    }

    @Test
    void actualizarMazo_mazoNoEncontrado_lanzaExcepcion() {
        when(mazoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.actualizarMazo(99L, "Nuevo", ids60("xy1-1")));
    }

    @Test
    void actualizarMazo_cartasMenos60_lanzaExcepcion() {
        Mazo mazo = new Mazo("Viejo", new Jugador("ash"));
        when(mazoRepo.findById(1L)).thenReturn(Optional.of(mazo));

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarMazo(1L, "Nuevo", Collections.nCopies(10, "xy1-1")));
    }

    @Test
    void debugInyectarCarta_noAdmin_lanzaSecurityException() {
        Jugador j = new Jugador("ash");
        Mazo mazo = new Mazo("Bosque", j);
        when(mazoRepo.findById(1L)).thenReturn(Optional.of(mazo));

        assertThrows(SecurityException.class,
                () -> service.debugInyectarCarta(1L, "xy1-5", null));
    }

    @Test
    void debugInyectarCarta_mazoNoEncontrado_lanzaExcepcion() {
        when(mazoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.debugInyectarCarta(99L, "xy1-5", null));
    }

    @Test
    void debugInyectarCarta_cartaNueva_noEncontrada_lanzaExcepcion() {
        Jugador j = new Jugador("ash");
        j.setAdmin(true);
        Mazo mazo = new Mazo("Bosque", j);
        mazo.setCartas(new ArrayList<>());

        when(mazoRepo.findById(1L)).thenReturn(Optional.of(mazo));
        when(cardRepo.findById("xy1-999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.debugInyectarCarta(1L, "xy1-999", null));
    }

    @Test
    void debugInyectarCarta_mazoConMenos60_agrega() {
        Jugador j = new Jugador("ash");
        j.setAdmin(true);
        Mazo mazo = new Mazo("Bosque", j);
        mazo.setCartas(new ArrayList<>());

        Card c = basicPokemon("xy1-5");
        when(mazoRepo.findById(1L)).thenReturn(Optional.of(mazo));
        when(cardRepo.findById("xy1-5")).thenReturn(Optional.of(c));
        when(mazoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mazo result = service.debugInyectarCarta(1L, "xy1-5", null);

        assertEquals(1, result.getCartas().size());
    }
}
