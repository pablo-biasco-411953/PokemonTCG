package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Habilidad;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.BattleEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComandoUsarHabilidadTest {

    private Partida partida;
    private TableroJugador jugador;
    private TableroJugador oponente;
    private BattleEngineService battleEngine;

    @BeforeEach
    void setUp() {
        jugador = new TableroJugador();
        oponente = new TableroJugador();
        partida = new Partida(jugador, oponente);
        battleEngine = Mockito.mock(BattleEngineService.class);
    }

    private CartaEnJuego setupPokemon(String id, String nombre, String abilityName, TableroJugador tablero) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setHp("100");
        Habilidad h = new Habilidad();
        h.setNombre(abilityName);
        c.setHabilidades(List.of(h));
        CartaEnJuego cej = new CartaEnJuego(c);
        if (tablero.getActivo() == null) {
            tablero.setActivo(cej);
        } else {
            tablero.getBanca().add(cej);
        }
        return cej;
    }

    @Test
    void puedeEjecutar_FalseIfPokemonNotFound() {
        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("no-existe", "Water Shuriken", null, null, jugador, oponente, battleEngine);
        assertFalse(cmd.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_TrueIfAbilityExistsAndNotUsed() {
        setupPokemon("p1", "Greninja", "Water Shuriken", jugador);
        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Water Shuriken", null, null, jugador, oponente, battleEngine);
        assertTrue(cmd.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_FalseIfAbilityAlreadyUsedThisTurn() {
        CartaEnJuego cej = setupPokemon("p1", "Greninja", "Water Shuriken", jugador);
        cej.registrarUsoHabilidad("Water Shuriken");
        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Water Shuriken", null, null, jugador, oponente, battleEngine);
        assertFalse(cmd.puedeEjecutar(partida));
    }

    @Test
    void ejecutar_WaterShuriken_DiscardsEnergyAndDealsDamage() {
        setupPokemon("p1", "Greninja", "Water Shuriken", jugador);
        CartaEnJuego target = setupPokemon("p2", "Enemy", "Ninguna", oponente);
        
        Card waterEnergy = new Card();
        waterEnergy.setId("e1");
        waterEnergy.setSupertype("Energy");
        waterEnergy.setTipo("Water");
        jugador.getMano().add(waterEnergy);

        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Water Shuriken", "p2", null, jugador, oponente, battleEngine);
        cmd.ejecutar(partida);

        assertEquals(70, target.getHpActual()); // 100 - 30
        assertTrue(jugador.getMano().isEmpty());
        assertEquals(1, jugador.getPilaDescarte().size());
    }

    @Test
    void ejecutar_MysticalFire_DrawsUntil6() {
        setupPokemon("p1", "Delphox", "Mystical Fire", jugador);
        // Player has 2 cards, should draw 4
        jugador.getMano().add(new Card());
        jugador.getMano().add(new Card());
        for (int i=0; i<10; i++) jugador.getMazo().add(new Card());

        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Mystical Fire", null, null, jugador, oponente, battleEngine);
        cmd.ejecutar(partida);
        
        Mockito.verify(battleEngine, Mockito.times(1)).robarCartas(jugador, 4);
    }

    @Test
    void ejecutar_DriveOff_ForcesOpponentSwitch() {
        setupPokemon("p1", "Aromatisse", "Drive Off", jugador);
        CartaEnJuego activeEnemy = setupPokemon("p2", "EnemyActive", "Ninguna", oponente);
        CartaEnJuego benchEnemy = setupPokemon("p3", "EnemyBench", "Ninguna", oponente);

        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Drive Off", "p3", null, jugador, oponente, battleEngine);
        cmd.ejecutar(partida);

        assertEquals("p3", oponente.getActivo().getCard().getId());
        assertEquals(1, oponente.getBanca().size());
        assertEquals("p2", oponente.getBanca().get(0).getCard().getId());
    }

    @Test
    void ejecutar_StanceChange_SwitchesWithHand() {
        CartaEnJuego active = setupPokemon("p1", "Aegislash", "Stance Change", jugador);
        Card handAegislash = new Card();
        handAegislash.setId("p2");
        handAegislash.setNombre("Aegislash");
        jugador.getMano().add(handAegislash);

        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Stance Change", null, null, jugador, oponente, battleEngine);
        cmd.ejecutar(partida);

        assertEquals("p2", jugador.getActivo().getCard().getId());
        assertEquals(1, jugador.getMano().size());
        assertEquals("p1", jugador.getMano().get(0).getId());
    }

    @Test
    void ejecutar_FairyTransfer_MovesFairyEnergy() {
        CartaEnJuego origin = setupPokemon("p1", "Aromatisse", "Fairy Transfer", jugador);
        CartaEnJuego target = setupPokemon("p2", "Bench", "Ninguna", jugador);
        
        Card fairy = new Card();
        fairy.setId("e1");
        fairy.setTipo("Fairy");
        origin.getEnergiasUnidas().add(fairy);

        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Fairy Transfer", "p2", "e1,p1", jugador, oponente, battleEngine);
        cmd.ejecutar(partida);

        assertTrue(origin.getEnergiasUnidas().isEmpty());
        assertEquals(1, target.getEnergiasUnidas().size());
    }

    @Test
    void ejecutar_UpsideDownEvolution_EvolvesInkay() {
        CartaEnJuego inkay = setupPokemon("p1", "Inkay", "Upside-Down Evolution", jugador);
        inkay.agregarCondicion("Confused");

        Card evolution = new Card();
        evolution.setId("malamar1");
        evolution.setNombre("Malamar");
        evolution.setEvolvesFrom("Inkay");
        evolution.setHp("100");
        jugador.getMazo().add(evolution);

        ComandoUsarHabilidad cmd = new ComandoUsarHabilidad("p1", "Upside-Down Evolution", null, null, jugador, oponente, battleEngine);
        cmd.ejecutar(partida);

        assertEquals("malamar1", jugador.getActivo().getCard().getId());
    }
}
