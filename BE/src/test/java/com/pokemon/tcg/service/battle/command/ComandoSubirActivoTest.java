package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComandoSubirActivoTest {

    private TableroJugador tablero;
    private Partida partida;

    @BeforeEach
    void setUp() {
        tablero = new TableroJugador();
        TableroJugador tableroOponente = new TableroJugador();
        partida = new Partida(tablero, tableroOponente);
    }

    private Card crearCardPokemon(String id, String nombre) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setTipo("Basic Pokemon");
        return card;
    }

    @Test
    void puedeEjecutar_activoNullYBancaConPokemon_retornaTrue() {
        tablero.setActivo(null);
        tablero.getBanca().add(new CartaEnJuego(crearCardPokemon("1", "Pikachu")));

        ComandoSubirActivo comando = new ComandoSubirActivo("1", tablero);
        assertTrue(comando.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_conActivoYaPresente_retornaFalse() {
        tablero.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander")));
        tablero.getBanca().add(new CartaEnJuego(crearCardPokemon("1", "Pikachu")));

        ComandoSubirActivo comando = new ComandoSubirActivo("1", tablero);
        assertFalse(comando.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_bancaVacia_retornaFalse() {
        tablero.setActivo(null);
        tablero.getBanca().clear();

        ComandoSubirActivo comando = new ComandoSubirActivo("1", tablero);
        assertFalse(comando.puedeEjecutar(partida));
    }

    @Test
    void ejecutar_conActivoYaPresente_lanzaIllegalStateException() {
        tablero.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander")));
        tablero.getBanca().add(new CartaEnJuego(crearCardPokemon("1", "Pikachu")));

        ComandoSubirActivo comando = new ComandoSubirActivo("1", tablero);
        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_pokemonNoEncontradoEnBanca_lanzaIllegalArgumentException() {
        tablero.setActivo(null);
        tablero.getBanca().add(new CartaEnJuego(crearCardPokemon("1", "Pikachu")));

        // Buscamos la carta "99" que no existe en banca
        ComandoSubirActivo comando = new ComandoSubirActivo("99", tablero);
        assertThrows(IllegalArgumentException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_flujoNormalExitoso() {
        tablero.setActivo(null);
        CartaEnJuego pikachu = new CartaEnJuego(crearCardPokemon("1", "Pikachu"));
        tablero.getBanca().add(pikachu);

        ComandoSubirActivo comando = new ComandoSubirActivo("1", tablero);
        comando.ejecutar(partida);

        assertEquals(pikachu, tablero.getActivo());
        assertTrue(tablero.getBanca().isEmpty());
    }

    @Test
    void getNombre_retornaFormatoEsperado() {
        ComandoSubirActivo comando = new ComandoSubirActivo("1", tablero);
        assertEquals("SubirActivo", comando.getNombre());
    }
}
