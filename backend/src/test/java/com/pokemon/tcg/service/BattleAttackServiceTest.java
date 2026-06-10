package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleAttackServiceTest {

    private final BattleAttackService service = new BattleAttackService();

    @Test
    void resolveAttackAplicaDanioBaseYRegistraHistorialVacioSinMonedas() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        Ataque ataque = attack("Golpe", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {});

        assertEquals(20, defensor.getHpActual());
        assertEquals(30, resolution.resultado().danioFinal());
        assertTrue(resolution.historialMonedas().isEmpty());
    }

    @Test
    void resolveAttackAplicaCuracionYRobaCartaCuandoElTextoLoIndica() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        atacante.setHpActual(30);
        partida.getJugador().getMazo().add(card("draw-1", "Carta Robo", "0"));

        Ataque ataque = attack("Recuperar", 20, "Heal 20 damage from this Pokémon. Draw a card.");

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> {});

        assertEquals(50, atacante.getHpActual());
        assertEquals(1, partida.getJugador().getMano().size());
        assertTrue(partida.getTurnLogs().stream().anyMatch(log -> log.startsWith("CARDS_DRAWN:")));
    }

    @Test
    void resolveAttackConBusquedaDejaUnaDecisionGuiadaParaElJugador() {
        Partida partida = partidaBasica();
        partida.setJugadorUsername("Pablo");
        Card grass = card("grass-1", "Chespin", "60");
        grass.setSupertype("Pokemon");
        grass.setTipo("Grass");
        partida.getJugador().getMazo().add(grass);

        Ataque ataque = attack(
                "Pheromotion",
                0,
                "Search your deck for a Grass Pokemon, reveal it, and put it into your hand. Shuffle your deck afterward."
        );

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {});

        assertEquals(Partida.Fase.ESPERANDO_INTERACCION, partida.getFaseActual());
        assertEquals("SEARCH_DECK", partida.getPendingAction().getType());
        assertEquals("grass-1", partida.getPendingAction().getOptions().getFirst().getId());
    }

    @Test
    void resolveAttackAplicaVenenoYBloqueoDeRetiradaSinAzar() {
        Partida partida = partidaBasica();
        Ataque ataque = attack(
                "Trampa Toxica",
                10,
                "The Defending Pokémon is now Poisoned. The Defending Pokémon can't retreat during your opponent's next turn."
        );

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {});

        assertTrue(partida.getBot().getActivo().getCondicionesEspeciales().contains("Poisoned"));
        assertTrue(partida.getBot().getActivo().getCondicionesEspeciales().contains("CantRetreat"));
    }

    @Test
    void resolveAttackPuedeDebilitarAlAtacantePorRetroceso() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        atacante.setHpActual(20);
        Ataque ataque = attack("Choque", 10, "This Pokémon does 20 damage to itself.");
        AtomicBoolean koInvocado = new AtomicBoolean(false);

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> koInvocado.set(true));

        assertEquals(0, atacante.getHpActual());
        assertTrue(koInvocado.get());
    }

    private Partida partidaBasica() {
        TableroJugador jugador = new TableroJugador();
        jugador.setActivo(new CartaEnJuego(card("p1", "Pikachu", "60")));

        TableroJugador bot = new TableroJugador();
        bot.setActivo(new CartaEnJuego(card("p2", "Charmander", "50")));

        return new Partida(jugador, bot);
    }

    private Card card(String id, String nombre, String hp) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        return card;
    }

    private Ataque attack(String nombre, int danio, String texto) {
        Ataque ataque = new Ataque();
        ataque.setNombre(nombre);
        ataque.setDanio(danio);
        ataque.setTexto(texto);
        ataque.setTiposEnergia(List.of());
        return ataque;
    }
}
