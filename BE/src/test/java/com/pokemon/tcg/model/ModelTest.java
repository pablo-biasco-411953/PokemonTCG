package com.pokemon.tcg.model;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    // =================== Jugador ===================

    @Test
    void jugador_settersGetters() {
        Jugador j = new Jugador();
        j.setId(1L);
        j.setUsername("ash");
        j.setEmail("ash@pokemon.com");
        j.setAdmin(true);
        j.setSobresDisponibles(5);
        j.setSantoroPoints(100);
        j.setPikachuCompanion(true);
        j.setCharacterId("char1");
        j.setSkinColor("#fff");
        j.setHairColor("#000");
        j.setEyeColor("#blue");
        j.setHeight(1.75);

        assertEquals(1L, j.getId());
        assertEquals("ash", j.getUsername());
        assertEquals("ash@pokemon.com", j.getEmail());
        assertTrue(j.isAdmin());
        assertEquals(5, j.getSobresDisponibles());
        assertEquals(100, j.getSantoroPoints());
        assertTrue(j.isPikachuCompanion());
        assertEquals("char1", j.getCharacterId());
        assertEquals("#fff", j.getSkinColor());
        assertEquals("#000", j.getHairColor());
        assertEquals("#blue", j.getEyeColor());
        assertEquals(1.75, j.getHeight());
    }

    @Test
    void jugador_santoroPointsNoNegativo() {
        Jugador j = new Jugador();
        j.setSantoroPoints(-50);
        assertEquals(0, j.getSantoroPoints());
    }

    @Test
    void jugador_passwordReset() {
        Jugador j = new Jugador();
        j.setPasswordResetTokenHash("hash123");
        j.setPasswordResetTokenExpiresAt(9999999L);

        assertEquals("hash123", j.getPasswordResetTokenHash());
        assertEquals(9999999L, j.getPasswordResetTokenExpiresAt());
    }

    @Test
    void jugador_santoroQuestFields() {
        Jugador j = new Jugador();
        j.setSantoroGiftClaimed(true);
        j.setSantoroQuestTracking(true);
        j.setSantoroQuestState("STEP_2");

        assertTrue(j.isSantoroGiftClaimed());
        assertTrue(j.isSantoroQuestTracking());
        assertEquals("STEP_2", j.getSantoroQuestState());
    }

    // =================== Mazo ===================

    @Test
    void mazo_settersGetters() {
        Mazo m = new Mazo();
        m.setId(1L);
        m.setNombre("Deck Eléctrico");
        m.setCartas(List.of(new Card()));

        assertEquals(1L, m.getId());
        assertEquals("Deck Eléctrico", m.getNombre());
        assertEquals(1, m.getCartas().size());
    }

    // =================== Card ===================

    @Test
    void card_settersGetters() {
        Card c = new Card();
        c.setId("xy1-1");
        c.setNombre("Pikachu");
        c.setSupertype("Pokemon");
        c.setSubtypes(List.of("Basic"));
        c.setHp("60");
        c.setTipo("Lightning");
        c.setCostoRetirada(1);
        c.setEvolvesFrom(null);

        assertEquals("xy1-1", c.getId());
        assertEquals("Pikachu", c.getNombre());
        assertEquals("Pokemon", c.getSupertype());
        assertEquals(List.of("Basic"), c.getSubtypes());
        assertEquals("60", c.getHp());
        assertEquals("Lightning", c.getTipo());
        assertEquals(1, c.getCostoRetirada());
        assertNull(c.getEvolvesFrom());
    }

    @Test
    void card_debilidades_resistencias() {
        Card c = new Card();
        CardAttribute deb = new CardAttribute("Fire", "×2");
        c.setDebilidades(List.of(deb));

        CardAttribute res = new CardAttribute("Water", "-20");
        c.setResistencias(List.of(res));

        assertEquals(1, c.getDebilidades().size());
        assertEquals("Fire", c.getDebilidades().get(0).getType());
        assertEquals(1, c.getResistencias().size());
        assertEquals("Water", c.getResistencias().get(0).getType());
    }

    // =================== CartaEnJuego ===================

    @Test
    void cartaEnJuego_condiciones() {
        Card card = new Card();
        card.setId("p1");
        card.setNombre("Pikachu");
        card.setHp("60");
        CartaEnJuego c = new CartaEnJuego(card);

        c.agregarCondicion("Poisoned");
        c.agregarCondicion("Burned");
        assertEquals(2, c.getCondicionesEspeciales().size());

        c.limpiarCondiciones();
        assertTrue(c.getCondicionesEspeciales().isEmpty());
    }

    @Test
    void cartaEnJuego_hpInicialDesdeCarta() {
        Card card = new Card();
        card.setId("p1");
        card.setNombre("Bulbasaur");
        card.setHp("45");
        CartaEnJuego c = new CartaEnJuego(card);

        assertEquals(45, c.getHpActual());
    }

    @Test
    void cartaEnJuego_energiasYHerramientas() {
        Card card = new Card();
        card.setId("p1");
        card.setNombre("Pikachu");
        card.setHp("60");
        CartaEnJuego c = new CartaEnJuego(card);

        Card energia = new Card();
        energia.setId("e1");
        energia.setNombre("Lightning Energy");
        c.getEnergiasUnidas().add(energia);

        Card tool = new Card();
        tool.setId("t1");
        tool.setNombre("Hard Charm");
        c.setAttachedTools(List.of(tool));

        assertEquals(1, c.getEnergiasUnidas().size());
        assertEquals(1, c.getAttachedTools().size());
    }

    @Test
    void cartaEnJuego_flagsDeControl() {
        Card card = new Card();
        card.setId("p1");
        card.setNombre("Pikachu");
        card.setHp("60");
        CartaEnJuego c = new CartaEnJuego(card);

        c.setInvulnerable(true);
        c.setBocaAbajo(true);
        c.setDebeLanzarMonedaSiAtaca(true);
        c.setNoPuedeAtacarSiguienteTurno(true);
        c.setNoPuedeAtacarYaConsumido(true);
        c.setAtaqueBloqueadoSiguienteTurno("Thunder");
        c.setAtaqueBloqueadoYaConsumido(true);
        c.setPreventDamageThreshold(30);
        c.setPreventDamageThresholdYaConsumido(true);
        c.setDanioExtraSiguienteTurno(40);
        c.setAtaquePotenciadoSiguienteTurno("Thunderbolt");
        c.setReduccionDanioRecibido(20);
        c.setAumentoDanioCausado(10);
        c.setTurnoEntrada(2);

        assertTrue(c.isInvulnerable());
        assertTrue(c.isBocaAbajo());
        assertTrue(c.isDebeLanzarMonedaSiAtaca());
        assertTrue(c.isNoPuedeAtacarSiguienteTurno());
        assertTrue(c.isNoPuedeAtacarYaConsumido());
        assertEquals("Thunder", c.getAtaqueBloqueadoSiguienteTurno());
        assertTrue(c.isAtaqueBloqueadoYaConsumido());
        assertEquals(30, c.getPreventDamageThreshold());
        assertTrue(c.isPreventDamageThresholdYaConsumido());
        assertEquals(40, c.getDanioExtraSiguienteTurno());
        assertEquals("Thunderbolt", c.getAtaquePotenciadoSiguienteTurno());
        assertEquals(20, c.getReduccionDanioRecibido());
        assertEquals(10, c.getAumentoDanioCausado());
        assertEquals(2, c.getTurnoEntrada());
    }

    // =================== TableroJugador ===================

    @Test
    void tableroJugador_settersGetters() {
        TableroJugador t = new TableroJugador();
        t.setTurnosJugados(3);

        assertEquals(3, t.getTurnosJugados());
        assertNotNull(t.getMano());
        assertNotNull(t.getMazo());
        assertNotNull(t.getBanca());
        assertNotNull(t.getPremios());
        assertNotNull(t.getPilaDescarte());
    }

    @Test
    void tableroJugador_activoYBanca() {
        TableroJugador t = new TableroJugador();
        Card card = new Card();
        card.setId("p1");
        card.setNombre("Pikachu");
        card.setHp("60");
        CartaEnJuego activo = new CartaEnJuego(card);
        t.setActivo(activo);

        assertNotNull(t.getActivo());
        assertEquals("Pikachu", t.getActivo().getCard().getNombre());
    }

    // =================== Partida ===================

    @Test
    void partida_ganadorYFase() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.setJugadorUsername("ash");
        p.setBotUsername("misty");
        p.setGanador("ash");

        assertEquals("ash", p.getGanador());
        assertEquals("ash", p.getJugadorUsername());
        assertEquals("misty", p.getBotUsername());
        assertNotNull(p.getId());
    }

    @Test
    void partida_turnLogs() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.getTurnLogs().add("TURN_STARTED:ash");
        p.getTurnLogs().add("ATTACK_USED:ash:Thunder");

        assertEquals(2, p.getTurnLogs().size());
    }

    @Test
    void partida_monedasYQueue() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.getUltimasMonedasLanzadas().add(true);
        p.getUltimasMonedasLanzadas().add(false);
        p.getExecutionQueue().add(new com.pokemon.tcg.model.battle.command.DamageCommand(30));

        assertEquals(2, p.getUltimasMonedasLanzadas().size());
        assertEquals(1, p.getExecutionQueue().size());
    }
}
