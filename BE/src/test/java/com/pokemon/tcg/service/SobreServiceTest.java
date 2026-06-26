package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SobreServiceTest {

    private JugadorRepository jugadorRepo;
    private CardCatalogService cardCatalogService;
    private SobreService sobreService;

    @BeforeEach
    void setUp() {
        jugadorRepo = mock(JugadorRepository.class);
        cardCatalogService = mock(CardCatalogService.class);
        sobreService = new SobreService(jugadorRepo, cardCatalogService);
    }

    private Card pokemon(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Pokemon");
        c.setHp("60");
        return c;
    }

    private Card energia(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre + " Energy");
        c.setSupertype("Energy");
        c.setHp("0");
        return c;
    }

    private List<Card> catalogoBase() {
        List<Card> cartas = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            cartas.add(pokemon("xy1-" + i, "Pokemon" + i));
        }
        for (int i = 11; i <= 15; i++) {
            cartas.add(energia("xy1-" + i, "Fire"));
        }
        return cartas;
    }

    @Test
    void abrirSobre_exitoso_retornaListaDeCartas() {
        Jugador jugador = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(cardCatalogService.getCatalogo()).thenReturn(catalogoBase());
        when(jugadorRepo.save(any())).thenReturn(jugador);

        List<Card> resultado = sobreService.abrirSobre("ash");

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertTrue(resultado.size() <= 10);
    }

    @Test
    void abrirSobre_retorna10Cartas() {
        Jugador jugador = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(cardCatalogService.getCatalogo()).thenReturn(catalogoBase());
        when(jugadorRepo.save(any())).thenReturn(jugador);

        List<Card> resultado = sobreService.abrirSobre("ash");

        assertEquals(10, resultado.size());
    }

    @Test
    void abrirSobre_decrementaSobresDisponibles() {
        Jugador jugador = new Jugador("ash");
        int sobresInicial = jugador.getSobresDisponibles();
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(cardCatalogService.getCatalogo()).thenReturn(catalogoBase());
        when(jugadorRepo.save(any())).thenReturn(jugador);

        sobreService.abrirSobre("ash");

        assertEquals(sobresInicial - 1, jugador.getSobresDisponibles());
    }

    @Test
    void abrirSobre_agregaCartasAColeccion() {
        Jugador jugador = new Jugador("ash");
        int coleccionInicial = jugador.getColeccion().size();
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(cardCatalogService.getCatalogo()).thenReturn(catalogoBase());
        when(jugadorRepo.save(any())).thenReturn(jugador);

        sobreService.abrirSobre("ash");

        assertTrue(jugador.getColeccion().size() > coleccionInicial);
    }

    @Test
    void abrirSobre_guardaJugador() {
        Jugador jugador = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(cardCatalogService.getCatalogo()).thenReturn(catalogoBase());
        when(jugadorRepo.save(any())).thenReturn(jugador);

        sobreService.abrirSobre("ash");

        verify(jugadorRepo).save(jugador);
    }

    @Test
    void abrirSobre_jugadorNoExiste_lanzaExcepcion() {
        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sobreService.abrirSobre("noexiste"));
        assertTrue(ex.getMessage().contains("Jugador no encontrado: noexiste"));
    }

    @Test
    void abrirSobre_sinSobres_lanzaExcepcion() {
        Jugador jugador = new Jugador("ash");
        jugador.setSobresDisponibles(0);
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> sobreService.abrirSobre("ash"));
        assertTrue(ex.getMessage().contains("No hay sobres disponibles"));
    }

    @Test
    void abrirSobre_catalogoVacioSincroniza() {
        Jugador jugador = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(cardCatalogService.getCatalogo()).thenReturn(List.of());
        when(cardCatalogService.sincronizarDesdeJson()).thenReturn(catalogoBase());
        when(jugadorRepo.save(any())).thenReturn(jugador);

        List<Card> resultado = sobreService.abrirSobre("ash");

        assertFalse(resultado.isEmpty());
        verify(cardCatalogService).sincronizarDesdeJson();
    }

    @Test
    void abrirSobre_catalogo_sinPokemones_lanzaExcepcion() {
        Jugador jugador = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);

        List<Card> soloEnergias = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            soloEnergias.add(energia("xy1-" + i, "Fire"));
        }
        when(cardCatalogService.getCatalogo()).thenReturn(List.of());
        when(cardCatalogService.sincronizarDesdeJson()).thenReturn(soloEnergias);

        assertThrows(IllegalStateException.class, () -> sobreService.abrirSobre("ash"));
    }
}
