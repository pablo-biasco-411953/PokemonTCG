package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComandoJugarTrainerTest {

    private TableroJugador tablero;
    private Partida partida;

    @BeforeEach
    void setUp() {
        tablero = new TableroJugador();
        TableroJugador tableroOponente = new TableroJugador();
        partida = new Partida(tablero, tableroOponente);
    }

    private Card crearEntrenador(String id, String nombre, String subtype) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setSupertype("Trainer");
        if (subtype != null) {
            card.setSubtypes(List.of(subtype));
        }
        return card;
    }

    @Test
    void puedeEjecutar_cartaNoEnMano_retornaFalse() {
        Card card = crearEntrenador("t1", "Professor Sycamore", "Supporter");
        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, p -> {});

        assertFalse(comando.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_supporterYaJugado_retornaFalse() {
        Card card = crearEntrenador("t1", "Professor Sycamore", "Supporter");
        tablero.getMano().add(card);
        partida.setPlayedSupporterThisTurn(true);

        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, p -> {});
        assertFalse(comando.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_stadiumYaJugado_retornaFalse() {
        Card card = crearEntrenador("t1", "Fairy Garden", "Stadium");
        tablero.getMano().add(card);
        partida.setPlayedStadiumThisTurn(true);

        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, p -> {});
        assertFalse(comando.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_normal_retornaTrue() {
        Card supporter = crearEntrenador("t1", "Professor Sycamore", "Supporter");
        tablero.getMano().add(supporter);

        ComandoJugarTrainer cmd1 = new ComandoJugarTrainer(supporter, tablero, p -> {});
        assertTrue(cmd1.puedeEjecutar(partida));

        Card item = crearEntrenador("t2", "Super Potion", "Item");
        tablero.getMano().add(item);

        ComandoJugarTrainer cmd2 = new ComandoJugarTrainer(item, tablero, p -> {});
        assertTrue(cmd2.puedeEjecutar(partida));
    }

    @Test
    void ejecutar_puedeEjecutarFalse_lanzaIllegalStateException() {
        Card card = crearEntrenador("t1", "Professor Sycamore", "Supporter");
        // No está en la mano
        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, p -> {});

        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_supporter_marcaEstadoYDescarta() {
        Card card = crearEntrenador("t1", "Professor Sycamore", "Supporter");
        tablero.getMano().add(card);

        AtomicBoolean effectRun = new AtomicBoolean(false);
        Consumer<Partida> effectRunner = p -> effectRun.set(true);

        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, effectRunner);
        comando.ejecutar(partida);

        assertTrue(partida.isPlayedSupporterThisTurn());
        assertFalse(tablero.getMano().contains(card));
        assertTrue(tablero.getPilaDescarte().contains(card));
        assertTrue(effectRun.get());
    }

    @Test
    void ejecutar_stadium_marcaEstadoYReemplazaEstadio() {
        Card oldStadium = crearEntrenador("t0", "Shadow Circle", "Stadium");
        partida.setActiveStadium(oldStadium);

        Card newStadium = crearEntrenador("t1", "Fairy Garden", "Stadium");
        tablero.getMano().add(newStadium);

        AtomicBoolean effectRun = new AtomicBoolean(false);
        Consumer<Partida> effectRunner = p -> effectRun.set(true);

        ComandoJugarTrainer comando = new ComandoJugarTrainer(newStadium, tablero, effectRunner);
        comando.ejecutar(partida);

        assertTrue(partida.isPlayedStadiumThisTurn());
        assertFalse(tablero.getMano().contains(newStadium));
        assertEquals(newStadium, partida.getActiveStadium());
        assertTrue(tablero.getPilaDescarte().contains(oldStadium));
        assertTrue(effectRun.get());
    }

    @Test
    void ejecutar_tool_noDescartaDeInmediato() {
        // Test "Pokémon Tool" spelling
        Card card = crearEntrenador("t1", "Hard Charm", "Pokémon Tool");
        tablero.getMano().add(card);

        AtomicBoolean effectRun = new AtomicBoolean(false);
        Consumer<Partida> effectRunner = p -> {
            effectRun.set(true);
            tablero.getMano().remove(card);
        };

        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, effectRunner);
        comando.ejecutar(partida);

        assertFalse(tablero.getMano().contains(card));
        assertFalse(tablero.getPilaDescarte().contains(card));
        assertTrue(effectRun.get());

        // Test "Pokemon Tool" spelling to cover all branches
        Card card2 = crearEntrenador("t2", "Hard Charm 2", "Pokemon Tool");
        tablero.getMano().add(card2);
        AtomicBoolean effectRun2 = new AtomicBoolean(false);
        Consumer<Partida> effectRunner2 = p -> {
            effectRun2.set(true);
            tablero.getMano().remove(card2);
        };
        ComandoJugarTrainer comando2 = new ComandoJugarTrainer(card2, tablero, effectRunner2);
        comando2.ejecutar(partida);

        assertFalse(tablero.getMano().contains(card2));
        assertFalse(tablero.getPilaDescarte().contains(card2));
        assertTrue(effectRun2.get());
    }

    @Test
    void ejecutar_itemNormal_descartaYCorreEfecto() {
        Card card = crearEntrenador("t1", "Super Potion", "Item");
        tablero.getMano().add(card);

        AtomicBoolean effectRun = new AtomicBoolean(false);
        Consumer<Partida> effectRunner = p -> effectRun.set(true);

        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, effectRunner);
        comando.ejecutar(partida);

        assertFalse(tablero.getMano().contains(card));
        assertTrue(tablero.getPilaDescarte().contains(card));
        assertTrue(effectRun.get());
    }

    @Test
    void getNombre_retornaFormatoEsperado() {
        Card card = crearEntrenador("t1", "Super Potion", "Item");
        ComandoJugarTrainer comando = new ComandoJugarTrainer(card, tablero, p -> {});
        assertEquals("JugarTrainer[Super Potion]", comando.getNombre());
    }
}
