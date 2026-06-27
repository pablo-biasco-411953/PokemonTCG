package com.pokemon.tcg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Habilidad;
import com.pokemon.tcg.model.battle.*;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.service.BotAIService;
import com.pokemon.tcg.service.BattleKoService;
import com.pokemon.tcg.service.battle.command.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HabilidadesIntegrityTest {

    private Partida crearPartidaBase() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.setFaseActual(Partida.Fase.TURNO_NORMAL);
        partida.setJugadorUsername("pablo");
        partida.setBotUsername("bot");
        return partida;
    }

    private Card crearCardConHabilidad(String id, String nombre, String abilityName, String abilityType) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp("100");
        Habilidad hab = new Habilidad();
        hab.setNombre(abilityName);
        hab.setType(abilityType);
        card.setHabilidades(Collections.singletonList(hab));
        return card;
    }

    @Test
    void testFurfrouFurCoat() {
        Partida partida = crearPartidaBase();
        Ataque ataque = new Ataque();
        ataque.setNombre("Tackle");
        ataque.setDanio(50);
        
        CartaEnJuego atacante = new CartaEnJuego(new Card());
        Card furfrou = crearCardConHabilidad("xy1-114", "Furfrou", "Fur Coat", "Ability");
        CartaEnJuego defensor = new CartaEnJuego(furfrou);
        
        BattleAttackService service = new BattleAttackService();
        service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);
        
        // 50 base damage - 20 (Fur Coat) = 30 damage. Remaining HP = 100 - 30 = 70.
        assertEquals(70, defensor.getHpActual());
    }

    @Test
    void testChesnaughtSpikyShield() {
        Partida partida = crearPartidaBase();
        Ataque ataque = new Ataque();
        ataque.setNombre("Tackle");
        ataque.setDanio(50);
        
        CartaEnJuego atacante = new CartaEnJuego(new Card());
        atacante.setHpActual(100);
        Card chesnaught = crearCardConHabilidad("xy1-14", "Chesnaught", "Spiky Shield", "Ability");
        CartaEnJuego defensor = new CartaEnJuego(chesnaught);
        
        BattleAttackService service = new BattleAttackService();
        service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);
        
        // Chesnaught should deal 30 counter-damage. Remaining HP of attacker = 70.
        assertEquals(70, atacante.getHpActual());
    }

    @Test
    void testVoltorbDestinyBurst() {
        Partida partida = crearPartidaBase();
        Ataque ataque = new Ataque();
        ataque.setNombre("Tackle");
        ataque.setDanio(50); // Voltorb has 50 HP, this will KO it
        
        CartaEnJuego atacante = new CartaEnJuego(new Card());
        atacante.setHpActual(100);
        Card voltorb = crearCardConHabilidad("xy1-44", "Voltorb", "Destiny Burst", "Ability");
        voltorb.setHp("50");
        CartaEnJuego defensor = new CartaEnJuego(voltorb);
        defensor.setHpActual(50);
        
        BattleAttackService service = new BattleAttackService();
        service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);
        
        assertFalse(partida.getUltimasMonedasLanzadas().isEmpty());
        boolean coinHeads = partida.getUltimasMonedasLanzadas().get(0);
        if (coinHeads) {
            assertEquals(50, atacante.getHpActual()); // 100 - 50 damage
        } else {
            assertEquals(100, atacante.getHpActual()); // 100 - 0 damage
        }
    }

    @Test
    void testTrevenantForestsCurse() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        TableroJugador oponente = partida.getBot();
        
        Card trevenant = crearCardConHabilidad("xy1-55", "Trevenant", "Forest's Curse", "Ability");
        oponente.setActivo(new CartaEnJuego(trevenant));
        
        Card item = new Card();
        item.setNombre("Super Potion");
        item.setSupertype("Trainer");
        item.setSubtypes(Collections.singletonList("Item"));
        jugador.getMano().add(item);
        
        ComandoJugarTrainer command = new ComandoJugarTrainer(item, jugador, (p) -> {});
        
        // Item cannot be played under Forest's Curse
        assertFalse(command.puedeEjecutar(partida));
    }

    @Test
    void testSlurpuffSweetVeil() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        
        Card pikachu = new Card();
        pikachu.setNombre("Pikachu");
        pikachu.setId("xy1-22");
        CartaEnJuego active = new CartaEnJuego(pikachu);
        active.agregarCondicion("Poisoned");
        jugador.setActivo(active);
        
        Card slurpuff = crearCardConHabilidad("xy1-95", "Slurpuff", "Sweet Veil", "Ability");
        jugador.getBanca().add(new CartaEnJuego(slurpuff));
        
        Card fairyEnergy = new Card();
        fairyEnergy.setTipo("Fairy");
        active.getEnergiasUnidas().add(fairyEnergy);
        
        BattleEngineService engine = new BattleEngineService(
            mock(JugadorRepository.class),
            mock(MazoRepository.class),
            mock(CardRepository.class),
            mock(BotAIService.class),
            mock(BattleAttackService.class),
            mock(BattleKoService.class)
        );
        engine.aplicarHabilidadesPasivas(partida);
        
        // Pikachu's Poisoned condition must be cured by Sweet Veil
        assertTrue(active.getCondicionesEspeciales().isEmpty());
    }

    @Test
    void testGreninjaWaterShuriken() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        TableroJugador oponente = partida.getBot();
        
        Card greninja = crearCardConHabilidad("xy1-41", "Greninja", "Water Shuriken", "Ability");
        CartaEnJuego active = new CartaEnJuego(greninja);
        jugador.setActivo(active);
        
        Card waterEnergy = new Card();
        waterEnergy.setTipo("Water");
        waterEnergy.setSupertype("Energy");
        waterEnergy.setId("xy1-134");
        jugador.getMano().add(waterEnergy);
        
        Card targetCard = new Card();
        targetCard.setId("target-1");
        CartaEnJuego target = new CartaEnJuego(targetCard);
        target.setHpActual(60);
        oponente.setActivo(target);
        
        BattleEngineService engineMock = mock(BattleEngineService.class);
        
        ComandoUsarHabilidad command = new ComandoUsarHabilidad(
                "xy1-41", "Water Shuriken", "target-1", null,
                jugador, oponente, engineMock
        );
        
        assertTrue(command.puedeEjecutar(partida));
        command.ejecutar(partida);
        
        assertFalse(jugador.getMano().contains(waterEnergy));
        assertTrue(jugador.getPilaDescarte().contains(waterEnergy));
        assertEquals(30, target.getHpActual());
        assertTrue(active.getHabilidadesUsadasEsteTurno().contains("Water Shuriken"));
    }

    @Test
    void testDelphoxMysticalFire() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        
        Card delphox = crearCardConHabilidad("xy1-26", "Delphox", "Mystical Fire", "Ability");
        CartaEnJuego active = new CartaEnJuego(delphox);
        jugador.setActivo(active);
        
        jugador.getMano().add(new Card());
        jugador.getMano().add(new Card());
        
        BattleEngineService engineMock = mock(BattleEngineService.class);
        
        ComandoUsarHabilidad command = new ComandoUsarHabilidad(
                "xy1-26", "Mystical Fire", null, null,
                jugador, partida.getBot(), engineMock
        );
        
        assertTrue(command.puedeEjecutar(partida));
        command.ejecutar(partida);
        
        verify(engineMock).robarCartas(jugador, 4);
        assertTrue(active.getHabilidadesUsadasEsteTurno().contains("Mystical Fire"));
    }

    @Test
    void testAromatisseFairyTransfer() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        
        Card aromatisse = crearCardConHabilidad("xy1-93", "Aromatisse", "Fairy Transfer", "Ability");
        CartaEnJuego active = new CartaEnJuego(aromatisse);
        jugador.setActivo(active);
        
        Card benchCard = new Card();
        benchCard.setId("bench-1");
        CartaEnJuego bench = new CartaEnJuego(benchCard);
        jugador.getBanca().add(bench);
        
        Card fairyEnergy = new Card();
        fairyEnergy.setTipo("Fairy");
        fairyEnergy.setId("energy-1");
        bench.getEnergiasUnidas().add(fairyEnergy);
        
        BattleEngineService engineMock = mock(BattleEngineService.class);
        
        ComandoUsarHabilidad command = new ComandoUsarHabilidad(
                "xy1-93", "Fairy Transfer", "xy1-93", "energy-1,bench-1",
                jugador, partida.getBot(), engineMock
        );
        
        assertTrue(command.puedeEjecutar(partida));
        command.ejecutar(partida);
        
        assertTrue(active.getEnergiasUnidas().contains(fairyEnergy));
        assertFalse(bench.getEnergiasUnidas().contains(fairyEnergy));
    }

    @Test
    void testSwellowDriveOff() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        TableroJugador oponente = partida.getBot();
        
        Card swellow = crearCardConHabilidad("xy1-103", "Swellow", "Drive Off", "Ability");
        jugador.setActivo(new CartaEnJuego(swellow));
        
        Card botActive = new Card();
        botActive.setId("bot-active");
        oponente.setActivo(new CartaEnJuego(botActive));
        
        Card botBench = new Card();
        botBench.setId("bot-bench");
        CartaEnJuego bench = new CartaEnJuego(botBench);
        oponente.getBanca().add(bench);
        
        BattleEngineService engineMock = mock(BattleEngineService.class);
        
        ComandoUsarHabilidad command = new ComandoUsarHabilidad(
                "xy1-103", "Drive Off", "bot-bench", null,
                jugador, oponente, engineMock
        );
        
        assertTrue(command.puedeEjecutar(partida));
        command.ejecutar(partida);
        
        assertEquals("bot-bench", oponente.getActivo().getCard().getId());
        assertEquals("bot-active", oponente.getBanca().get(0).getCard().getId());
    }

    @Test
    void testAegislashStanceChange() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        
        Card aegislash1 = crearCardConHabilidad("xy1-85", "Aegislash", "Stance Change", "Ability");
        CartaEnJuego active = new CartaEnJuego(aegislash1);
        active.setHpActual(80);
        jugador.setActivo(active);
        
        Card aegislash2 = crearCardConHabilidad("xy1-86", "Aegislash", "Stance Change", "Ability");
        jugador.getMano().add(aegislash2);
        
        BattleEngineService engineMock = mock(BattleEngineService.class);
        
        ComandoUsarHabilidad command = new ComandoUsarHabilidad(
                "xy1-85", "Stance Change", "xy1-86", null,
                jugador, partida.getBot(), engineMock
        );
        
        assertTrue(command.puedeEjecutar(partida));
        command.ejecutar(partida);
        
        assertEquals("xy1-86", jugador.getActivo().getCard().getId());
        assertEquals(80, jugador.getActivo().getHpActual());
        assertTrue(jugador.getMano().contains(aegislash1));
        assertFalse(jugador.getMano().contains(aegislash2));
    }

    @Test
    void testInkayUpsideDownEvolution() {
        Partida partida = crearPartidaBase();
        TableroJugador jugador = partida.getJugador();
        
        Card inkay = crearCardConHabilidad("xy1-74", "Inkay", "Upside-Down Evolution", "Ability");
        CartaEnJuego active = new CartaEnJuego(inkay);
        active.agregarCondicion("Confused");
        jugador.setActivo(active);
        
        Card malamar = new Card();
        malamar.setId("xy1-75");
        malamar.setNombre("Malamar");
        malamar.setEvolvesFrom("Inkay");
        malamar.setSupertype("Pokémon");
        jugador.getMazo().add(malamar);
        
        BattleEngineService engineMock = mock(BattleEngineService.class);
        
        ComandoUsarHabilidad command = new ComandoUsarHabilidad(
                "xy1-74", "Upside-Down Evolution", null, null,
                jugador, partida.getBot(), engineMock
        );
        
        assertTrue(command.puedeEjecutar(partida));
        command.ejecutar(partida);
        
        assertEquals("xy1-75", jugador.getActivo().getCard().getId());
        assertFalse(jugador.getActivo().getCondicionesEspeciales().contains("Confused"));
    }
}
