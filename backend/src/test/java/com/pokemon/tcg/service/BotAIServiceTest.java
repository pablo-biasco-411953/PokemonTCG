package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.PokemonCard;
import com.pokemon.tcg.model.EnergyCard;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotAIServiceTest {

    private final BotAIService service = new BotAIService();

    @Test
    void ejecutarTurnoPromueveElMejorPokemonDeLaBancaSiNoTieneActivo() {
        TableroJugador jugador = new TableroJugador();
        jugador.setActivo(new CartaEnJuego(basicPokemon("enemy", "Squirtle", "70", "Water", attack("Bubble", 10, List.of("Water"), ""))));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego opcionDebil = new CartaEnJuego(basicPokemon("bench-1", "Caterpie", "50", "Grass"));
        opcionDebil.getEnergiasUnidas().add(energy("g-1", "Grass Energy"));

        CartaEnJuego mejorOpcion = new CartaEnJuego(basicPokemon("bench-2", "Charmander", "60", "Fire"));
        mejorOpcion.getEnergiasUnidas().add(energy("f-1", "Fire Energy"));
        mejorOpcion.getEnergiasUnidas().add(energy("f-2", "Fire Energy"));

        bot.getBanca().add(opcionDebil);
        bot.getBanca().add(mejorOpcion);

        service.ejecutarTurno(new Partida(jugador, bot));

        assertSame(mejorOpcion, bot.getActivo());
        assertEquals(1, bot.getBanca().size());
        assertSame(opcionDebil, bot.getBanca().get(0));
    }

    @Test
    void ejecutarTurnoBajaUnBasicoDeLaManoComoActivo() {
        TableroJugador jugador = new TableroJugador();
        jugador.setActivo(new CartaEnJuego(basicPokemon("enemy-2", "Pikachu", "60", "Lightning", attack("Zap", 10, List.of("Lightning"), ""))));

        TableroJugador bot = new TableroJugador();
        Card basico = basicPokemon("basic-1", "Eevee", "50", "Colorless", attack("Tackle", 10, List.of("Colorless"), ""));
        bot.getMano().add(basico);
        bot.getMano().add(energy("c-1", "Colorless Energy"));

        service.ejecutarTurno(new Partida(jugador, bot));

        assertTrue(bot.getActivo() != null);
        assertEquals("basic-1", bot.getActivo().getCard().getId());
        assertTrue(bot.getMano().stream().noneMatch(card -> "basic-1".equals(card.getId())));
    }

    @Test
    void ejecutarTurnoUneEnergiaUtilAlActivo() {
        TableroJugador jugador = new TableroJugador();
        jugador.setActivo(new CartaEnJuego(basicPokemon("enemy-3", "Bulbasaur", "70", "Grass", attack("Vine Whip", 10, List.of("Grass"), ""))));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego activo = new CartaEnJuego(basicPokemon("bot-1", "Charmander", "60", "Fire", attack("Ember", 30, List.of("Fire"), "")));
        bot.setActivo(activo);
        bot.getMano().add(energy("fire-1", "Fire Energy"));

        service.ejecutarTurno(new Partida(jugador, bot));

        assertEquals(1, activo.getEnergiasUnidas().size());
        assertEquals("fire-1", activo.getEnergiasUnidas().get(0).getId());
        assertTrue(bot.getMano().isEmpty());
    }

    @Test
    void ejecutarTurnoHaceRetiradaEstrategicaCuandoElActivoVaAMorir() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego rivalActivo = new CartaEnJuego(basicPokemon("enemy-4", "Blastoise", "120", "Water", attack("Hydro Pump", 60, List.of("Water"), "")));
        jugador.setActivo(rivalActivo);

        TableroJugador bot = new TableroJugador();
        CartaEnJuego activoEnPeligro = new CartaEnJuego(basicPokemon("bot-2", "Growlithe", "70", "Fire", attack("Bite", 10, List.of("Colorless"), "")));
        activoEnPeligro.setHpActual(40);
        activoEnPeligro.getEnergiasUnidas().add(energy("ret-1", "Fire Energy"));
        ((PokemonCard) activoEnPeligro.getCard()).setCostoRetirada(1);
        bot.setActivo(activoEnPeligro);

        CartaEnJuego suplente = new CartaEnJuego(basicPokemon("bench-3", "Ponyta", "80", "Fire", attack("Kick", 20, List.of("Fire"), "")));
        bot.getBanca().add(suplente);

        Partida partida = new Partida(jugador, bot);
        service.ejecutarTurno(partida);

        assertSame(suplente, bot.getActivo());
        assertTrue(bot.getBanca().contains(activoEnPeligro));
        assertEquals(1, bot.getPilaDescarte().size());
        assertTrue(partida.isYaSeRetiroEsteTurno());
    }

    private Card basicPokemon(String id, String nombre, String hp, String tipo) {
        return basicPokemon(id, nombre, hp, tipo, null);
    }

    private Card basicPokemon(String id, String nombre, String hp, String tipo, Ataque ataque) {
        PokemonCard card = new PokemonCard();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        card.setTipo(tipo);
        card.setSupertype("Pokemon");
        card.setSubtypes(List.of("Basic"));
        if (ataque != null) {
            card.cargarAtaquesDesdeJson(List.of(Map.of(
                    "nombre", ataque.getNombre(),
                    "dano", ataque.getDanio(),
                    "costo", ataque.getCosto(),
                    "texto", ataque.getTexto()
            )));
        }
        return card;
    }

    private Card energy(String id, String nombre) {
        EnergyCard card = new EnergyCard();
        card.setId(id);
        card.setNombre(nombre);
        card.setSupertype("Energy");
        card.setTipo("Energy");
        return card;
    }

    private Ataque attack(String nombre, int danio, List<String> costo, String texto) {
        Ataque ataque = new Ataque();
        ataque.setNombre(nombre);
        ataque.setDanio(danio);
        ataque.setTiposEnergia(costo);
        ataque.setTexto(texto);
        return ataque;
    }
}
