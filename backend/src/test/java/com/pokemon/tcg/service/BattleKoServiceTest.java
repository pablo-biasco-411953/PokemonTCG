package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("Pablo");

        service.resolverKO(partida, atacante, defensor);

        assertEquals(2, jugador.getMano().size());
        assertEquals(1, jugador.getPremios().size());
        assertEquals("PRIZE_TAKEN:Pablo:2", partida.getTurnLogs().getLast());
    }

    private Card card(String id, String nombre, String hp) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        return card;
    }
}
