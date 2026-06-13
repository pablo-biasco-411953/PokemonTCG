package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.command.*;
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
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

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

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> {}, null);

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

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {}, null);

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

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {}, null);

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

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> koInvocado.set(true), null);

        assertEquals(0, atacante.getHpActual());
        assertTrue(koInvocado.get());
    }

    @Test
    void comandoSetNoPuedeAtacarSiguienteTurnoPoneFlagCorrecto() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        BattleCommand cmd = new SetNoPuedeAtacarSiguienteTurnoCommand(Target.SELF);
        
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertTrue(atacante.isNoPuedeAtacarSiguienteTurno());
        assertFalse(atacante.isNoPuedeAtacarYaConsumido());
    }

    @Test
    void comandoSetCannotAttackDefendingPonePuedeAtacarEnFalse() {
        Partida partida = partidaBasica();
        CartaEnJuego defensor = partida.getBot().getActivo();
        BattleCommand cmd = new SetCannotAttackDefendingCommand();

        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertFalse(defensor.isPuedeAtacar());
    }

    @Test
    void comandoBlockAttackNextTurnPoneFlagCorrecto() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        BattleCommand cmd = new BlockAttackNextTurnCommand("King's Shield", Target.SELF);

        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertEquals("King's Shield", atacante.getAtaqueBloqueadoSiguienteTurno());
        assertFalse(atacante.isAtaqueBloqueadoYaConsumido());
    }

    @Test
    void comandoConditionalDamageMultiplierBenchedPokemonAplicaDanioCorrecto() {
        Partida partida = partidaBasica();
        partida.getJugador().getBanca().add(new CartaEnJuego(card("b1", "Bulbasaur", "50")));
        partida.getJugador().getBanca().add(new CartaEnJuego(card("b2", "Squirtle", "50")));

        CartaEnJuego defensor = partida.getBot().getActivo();
        defensor.setHpActual(80);

        BattleCommand cmd = new ConditionalDamageMultiplierCommand(0, 20, "BENCHED_POKEMON", null);
        cmd.execute(partida, partida.getJugador(), partida.getBot());
        if (!partida.getExecutionQueue().isEmpty()) {
            partida.getExecutionQueue().poll().execute(partida, partida.getJugador(), partida.getBot());
        }

        assertEquals(40, defensor.getHpActual());
    }

    @Test
    void comandoSelfBenchDamageRestaHpACadaPokemonDeTuBanca() {
        Partida partida = partidaBasica();
        CartaEnJuego b1 = new CartaEnJuego(card("b1", "Bulbasaur", "50"));
        CartaEnJuego b2 = new CartaEnJuego(card("b2", "Squirtle", "60"));
        partida.getJugador().getBanca().add(b1);
        partida.getJugador().getBanca().add(b2);

        BattleCommand cmd = new SelfBenchDamageCommand(10);
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertEquals(40, b1.getHpActual());
        assertEquals(50, b2.getHpActual());
    }

    @Test
    void comandoDamageOpponentBenchedAplicaDanioCorrectoALaBancaEnemiga() {
        Partida partida = partidaBasica();
        CartaEnJuego b1 = new CartaEnJuego(card("b1-1", "Bulbasaur", "50"));
        CartaEnJuego b2 = new CartaEnJuego(card("b2-1", "Squirtle", "60"));
        partida.getBot().getBanca().add(b1);
        partida.getBot().getBanca().add(b2);

        BattleCommand cmd = new DamageOpponentBenchedCommand(20, 2);
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        // Para el jugador, se crea una acción pendiente
        assertEquals("CHOOSE_OPPONENT_BENCH_TO_DAMAGE", partida.getPendingAction().getType());
        assertEquals(2, partida.getPendingAction().getMinSelections());
        
        // Ahora probamos el bot atacando al jugador
        partida.setPendingAction(null);
        partida.getJugador().getBanca().add(b1);
        partida.getJugador().getBanca().add(b2);
        
        cmd.execute(partida, partida.getBot(), partida.getJugador());
        
        // El bot hace daño directamente a ambos porque count = 2
        assertEquals(30, b1.getHpActual());
        assertEquals(40, b2.getHpActual());
    }

    @Test
    void comandoShuffleRandomHandToDeckShufflesCardIntoOpponentsDeck() {
        Partida partida = partidaBasica();
        Card handCard = card("hc1", "Trainer's Mail", "0");
        partida.getBot().getMano().add(handCard);
        int initialDeckSize = partida.getBot().getMazo().size();

        BattleCommand cmd = new ShuffleRandomHandToDeckCommand();
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertTrue(partida.getBot().getMano().isEmpty());
        assertEquals(initialDeckSize + 1, partida.getBot().getMazo().size());
        assertTrue(partida.getBot().getMazo().contains(handCard));
        assertTrue(partida.getTurnLogs().stream().anyMatch(log -> log.startsWith("ASTONISH_REVEALED:")));
    }

    @Test
    void resolveAttackConEnergyGlideActivaDecisionDeCambioAlAcoplar() {
        Partida partida = partidaBasica();
        partida.setJugadorUsername("Pablo");

        Card lightning = card("lightning-1", "Lightning Energy", "0");
        lightning.setSupertype("Energy");
        lightning.setTipo("Lightning");
        partida.getJugador().getMazo().add(lightning);

        CartaEnJuego suplente = new CartaEnJuego(card("suplente-1", "Bulbasaur", "50"));
        partida.getJugador().getBanca().add(suplente);

        Ataque ataque = attack(
                "Energy Glide",
                10,
                "Search your deck for a Lightning Energy card and attach it to this Pokémon. Shuffle your deck afterward. If you attached Energy in this way, switch this Pokémon with 1 of your Benched Pokémon."
        );

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {}, null);

        assertEquals(Partida.Fase.ESPERANDO_INTERACCION, partida.getFaseActual());
        assertEquals("SEARCH_DECK", partida.getPendingAction().getType());
        assertEquals("ATTACH_ACTIVE_AND_SWITCH", partida.getPendingAction().getDestination());
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
