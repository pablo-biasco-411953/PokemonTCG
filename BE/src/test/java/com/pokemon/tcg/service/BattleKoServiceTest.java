package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoLanzamientoMoneda;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BattleKoServiceTest {

    private final BattleKoService service = new BattleKoService();

    @Test
    void resolverKoDescartaAlActivoYOtorgaPremio() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego atacante = new CartaEnJuego(card("atk", "Pikachu", "60"));
        jugador.setActivo(atacante);
        jugador.getPremios().add(card("premio-jugador", "Premio J", "0"));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego defensor = new CartaEnJuego(card("def", "Charmander", "50"));
        bot.setActivo(defensor);
        bot.getPremios().add(card("premio-bot", "Premio B", "0"));

        Partida partida = new Partida(jugador, bot);

        service.resolverKO(partida, atacante, defensor);

        assertNull(bot.getActivo());
        assertEquals(1, bot.getPilaDescarte().size());
        assertEquals("def", bot.getPilaDescarte().get(0).getId());
        assertEquals(1, jugador.getMano().size());
        assertEquals("premio-jugador", jugador.getMano().get(0).getId());
    }

    @Test
    void resolverKoEligeReemplazoDelBotCuandoTieneBanca() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego atacante = new CartaEnJuego(card("atk-2", "Squirtle", "70"));
        jugador.setActivo(atacante);
        jugador.getPremios().add(card("premio-jugador-2", "Premio J2", "0"));
        jugador.getPremios().add(card("premio-jugador-3", "Premio J3", "0"));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego defensor = new CartaEnJuego(card("def-2", "Bulbasaur", "50"));
        bot.setActivo(defensor);
        bot.getPremios().add(card("premio-b1", "Premio B1", "0"));

        CartaEnJuego bancaDebil = new CartaEnJuego(card("bench-1", "Caterpie", "40"));
        bancaDebil.setHpActual(20);

        CartaEnJuego bancaFuerte = new CartaEnJuego(card("bench-2", "Ivysaur", "90"));
        bancaFuerte.getEnergiasUnidas().add(card("ene-1", "Grass Energy", "0"));
        bancaFuerte.getEnergiasUnidas().add(card("ene-2", "Grass Energy", "0"));

        bot.getBanca().add(bancaDebil);
        bot.getBanca().add(bancaFuerte);

        Partida partida = new Partida(jugador, bot);

        service.resolverKO(partida, atacante, defensor);

        assertSame(bancaFuerte, bot.getActivo());
        assertEquals(1, bot.getBanca().size());
        assertSame(bancaDebil, bot.getBanca().get(0));
    }

    @Test
    void resolverKoDePokemonExOtorgaDosPremios() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego atacante = new CartaEnJuego(card("atk-ex", "Pikachu", "60"));
        jugador.setActivo(atacante);
        jugador.getPremios().add(card("premio-ex-1", "Premio 1", "0"));
        jugador.getPremios().add(card("premio-ex-2", "Premio 2", "0"));
        jugador.getPremios().add(card("premio-ex-3", "Premio 3", "0"));

        Card ex = card("def-ex", "Xerneas-EX", "170");
        ex.setSubtypes(List.of("Basic", "EX"));
        TableroJugador bot = new TableroJugador();
        CartaEnJuego defensor = new CartaEnJuego(ex);
        bot.setActivo(defensor);
        bot.getPremios().add(card("premio-b-ex", "Premio Bot", "0"));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("Pablo");

        service.resolverKO(partida, atacante, defensor);

        assertEquals(2, jugador.getMano().size());
        assertEquals(1, jugador.getPremios().size());
        assertEquals("PRIZE_TAKEN:Pablo:2", partida.getTurnLogs().getLast());
    }

    @Test
    void resolverKoIniciaMuerteSubitaCuandoAmbosCumplenCondicionDeVictoria() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego atacante = new CartaEnJuego(card("atk-sd", "Pikachu", "60"));
        jugador.setActivo(atacante);
        jugador.setMazoOriginal(List.of(
                card("j1", "J Basic 1", "50"),
                card("j2", "J Basic 2", "50"),
                card("j3", "J Basic 3", "50"),
                card("j4", "J Basic 4", "50"),
                card("j5", "J Basic 5", "50"),
                card("j6", "J Basic 6", "50"),
                card("j7", "J Basic 7", "50"),
                card("j8", "J Basic 8", "50")
        ));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego defensor = new CartaEnJuego(card("def-sd", "Charmander", "50"));
        bot.setActivo(defensor);
        bot.setMazoOriginal(List.of(
                card("b1", "B Basic 1", "50"),
                card("b2", "B Basic 2", "50"),
                card("b3", "B Basic 3", "50"),
                card("b4", "B Basic 4", "50"),
                card("b5", "B Basic 5", "50"),
                card("b6", "B Basic 6", "50"),
                card("b7", "B Basic 7", "50"),
                card("b8", "B Basic 8", "50")
        ));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("Pablo");
        partida.setBotUsername("BOT");

        jugador.getPremios().clear();
        bot.getPremios().clear();
        jugador.getMano().add(card("jh", "Carta mano J", "0"));
        bot.getMano().add(card("bh", "Carta mano B", "0"));
        jugador.getPilaDescarte().add(card("jd", "Desc J", "0"));
        bot.getPilaDescarte().add(card("bd", "Desc B", "0"));

        service.resolverKO(partida, atacante, defensor);
        service.resolverKO(partida, defensor, atacante);

        assertTrue(partida.isMuerteSubita());
        assertEquals(Partida.Fase.LANZAMIENTO_MONEDA, partida.getFaseActual());
        assertInstanceOf(EstadoLanzamientoMoneda.class, partida.getEstado());
        assertEquals(1, partida.getNumeroTurno());
        assertEquals(7, jugador.getMano().size());
        assertEquals(7, bot.getMano().size());
        assertEquals(0, jugador.getPremios().size());
        assertEquals(0, bot.getPremios().size());
        assertNull(jugador.getActivo());
        assertNull(bot.getActivo());
        assertTrue(jugador.getBanca().isEmpty());
        assertTrue(bot.getBanca().isEmpty());
        assertTrue(jugador.getPilaDescarte().isEmpty());
        assertTrue(bot.getPilaDescarte().isEmpty());
        assertEquals(List.of("MUERTE_SUBITA:INICIADA"), partida.getTurnLogs());
        assertFalse(partida.isCoinFlipped());
    }

    @Test
    void resolverKoProcesaSegundoKoAunqueElAtacanteYaNoEsteEnTablero() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego activoJugador = new CartaEnJuego(card("atk-double", "Tauros", "100"));
        activoJugador.setHpActual(0);
        jugador.setActivo(activoJugador);
        jugador.getPremios().add(card("premio-j1", "Premio J1", "0"));
        jugador.setMazoOriginal(List.of(
                card("j1", "J Basic 1", "50"),
                card("j2", "J Basic 2", "50"),
                card("j3", "J Basic 3", "50"),
                card("j4", "J Basic 4", "50"),
                card("j5", "J Basic 5", "50"),
                card("j6", "J Basic 6", "50"),
                card("j7", "J Basic 7", "50")
        ));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego activoBot = new CartaEnJuego(card("def-double", "Machoke", "100"));
        activoBot.setHpActual(0);
        bot.setActivo(activoBot);
        bot.getPremios().add(card("premio-b1", "Premio B1", "0"));
        bot.setMazoOriginal(List.of(
                card("b1", "B Basic 1", "50"),
                card("b2", "B Basic 2", "50"),
                card("b3", "B Basic 3", "50"),
                card("b4", "B Basic 4", "50"),
                card("b5", "B Basic 5", "50"),
                card("b6", "B Basic 6", "50"),
                card("b7", "B Basic 7", "50")
        ));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("Pablo");
        partida.setBotUsername("BOT");

        service.resolverKO(partida, activoJugador, activoBot);
        service.resolverKO(partida, activoBot, activoJugador);

        assertTrue(partida.isMuerteSubita());
        assertEquals(Partida.Fase.LANZAMIENTO_MONEDA, partida.getFaseActual());
        assertEquals(List.of("MUERTE_SUBITA:INICIADA"), partida.getTurnLogs());
    }

    private Card card(String id, String nombre, String hp) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        return card;
    }
}
