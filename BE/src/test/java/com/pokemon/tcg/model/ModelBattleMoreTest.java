package com.pokemon.tcg.model;

import com.pokemon.tcg.model.battle.AttackTranslation;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelBattleMoreTest {

    // =================== AttackTranslation ===================

    @Test
    void attackTranslation_allGettersSetters() {
        AttackTranslation at = new AttackTranslation();
        at.setId(5L);
        at.setAtaqueId(10L);
        at.setLang("es");
        at.setNombre("Trueno");
        at.setTexto("Inflige 90 de daño");

        assertEquals(5L, at.getId());
        assertEquals(10L, at.getAtaqueId());
        assertEquals("es", at.getLang());
        assertEquals("Trueno", at.getNombre());
        assertEquals("Inflige 90 de daño", at.getTexto());
    }

    @Test
    void attackTranslation_4argConstructor() {
        AttackTranslation at = new AttackTranslation(1L, "pt", "Raio", "Aplica 60 de dano");
        assertEquals(1L, at.getAtaqueId());
        assertEquals("pt", at.getLang());
        assertEquals("Raio", at.getNombre());
        assertEquals("Aplica 60 de dano", at.getTexto());
    }

    // =================== PendingBattleAction.Option ===================

    @Test
    void option_7argConstructor() {
        PendingBattleAction.Option opt = new PendingBattleAction.Option(
                "xy1-1", "Venusaur-EX", "img.png", 120, 180, "1", "xy1"
        );

        assertEquals("xy1-1", opt.getId());
        assertEquals("Venusaur-EX", opt.getNombre());
        assertEquals("img.png", opt.getImagen());
        assertEquals(120, opt.getHpActual());
        assertEquals(180, opt.getMaxHp());
        assertEquals("1", opt.getNumero());
        assertEquals("xy1", opt.getSet());
    }

    @Test
    void option_defaultConstructorAndSetters() {
        PendingBattleAction.Option opt = new PendingBattleAction.Option();
        opt.setId("xy1-2");
        opt.setNombre("Charizard-EX");
        opt.setImagen("charizard.png");
        opt.setHpActual(150);
        opt.setMaxHp(180);
        opt.setNumero("11");
        opt.setSet("xy1");

        assertEquals("xy1-2", opt.getId());
        assertEquals("Charizard-EX", opt.getNombre());
        assertEquals("charizard.png", opt.getImagen());
        assertEquals(150, opt.getHpActual());
        assertEquals(180, opt.getMaxHp());
        assertEquals("11", opt.getNumero());
        assertEquals("xy1", opt.getSet());
    }

    // =================== PendingBattleAction: remaining fields ===================

    @Test
    void pendingBattleAction_endsTurnAndPrompt() {
        PendingBattleAction p = new PendingBattleAction();
        p.setEndsTurn(true);
        p.setPrompt("Seleccioná un Pokémon");

        assertTrue(p.isEndsTurn());
        assertEquals("Seleccioná un Pokémon", p.getPrompt());
    }

    // =================== Partida: remaining setters for coverage ===================

    @Test
    void partida_handshakeFields() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.setCoinHandshakeJugadorPower(75);
        p.setCoinHandshakeBotPower(80);
        p.setCoinHandshakeJugadorHolding(true);
        p.setCoinHandshakeBotHolding(false);
        p.setCoinHandshakeComplete(true);

        assertEquals(75, p.getCoinHandshakeJugadorPower());
        assertEquals(80, p.getCoinHandshakeBotPower());
        assertTrue(p.isCoinHandshakeJugadorHolding());
        assertFalse(p.isCoinHandshakeBotHolding());
        assertTrue(p.isCoinHandshakeComplete());
    }

    @Test
    void partida_mulliganFields() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.setMulligansJugador(2);
        p.setMulligansBot(1);
        p.setCartasMulliganExtraPendientesJugador(3);
        p.setCartasMulliganExtraPendientesBot(2);
        p.setSetupJugadorRoboExtraMulligan(true);
        p.setSetupBotRoboExtraMulligan(true);

        assertEquals(2, p.getMulligansJugador());
        assertEquals(1, p.getMulligansBot());
        assertEquals(3, p.getCartasMulliganExtraPendientesJugador());
        assertEquals(2, p.getCartasMulliganExtraPendientesBot());
        assertTrue(p.isSetupJugadorRoboExtraMulligan());
        assertTrue(p.isSetupBotRoboExtraMulligan());
    }

    @Test
    void partida_setupAndCoinFields() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        p.setSetupJugadorListo(true);
        p.setSetupBotListo(true);
        p.setCoinFlipped(true);
        p.setCoinFlipWinner("ash");
        p.setCoinFlipResult("HEADS");
        p.setRazonFinPartida("Jugador tomó todos sus premios");
        p.setYaSeRetiroEsteTurno(true);
        p.setYaSeUnioEnergiaEsteTurno(true);
        p.setNumeroTurno(5);
        p.setMuerteSubita(true);

        assertTrue(p.isSetupJugadorListo());
        assertTrue(p.isSetupBotListo());
        assertTrue(p.isCoinFlipped());
        assertEquals("ash", p.getCoinFlipWinner());
        assertEquals("HEADS", p.getCoinFlipResult());
        assertEquals("Jugador tomó todos sus premios", p.getRazonFinPartida());
        assertTrue(p.isYaSeRetiroEsteTurno());
        assertTrue(p.isYaSeUnioEnergiaEsteTurno());
        assertEquals(5, p.getNumeroTurno());
        assertTrue(p.isMuerteSubita());
    }

    @Test
    void partida_pendingActionAndTurnoCampo() {
        Partida p = new Partida(new TableroJugador(), new TableroJugador());
        PendingBattleAction pending = new PendingBattleAction();
        pending.setType("HEAL_OWN_POKEMON");
        p.setPendingAction(pending);
        p.setTurnoActual(Partida.Turno.BOT);

        assertNotNull(p.getPendingAction());
        assertEquals("HEAL_OWN_POKEMON", p.getPendingAction().getType());
        assertEquals(Partida.Turno.BOT, p.getTurnoActual());
    }

    @Test
    void cartaEnJuego_puedeAtacar() {
        Card card = new Card();
        card.setId("p1");
        card.setNombre("Pikachu");
        card.setHp("60");
        CartaEnJuego c = new CartaEnJuego(card);

        assertTrue(c.isPuedeAtacar()); // default true
        c.setPuedeAtacar(false);
        assertFalse(c.isPuedeAtacar());
    }
}
