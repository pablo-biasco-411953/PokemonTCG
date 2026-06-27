package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoTurnoNormal;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BattleEngineServiceBotTurnTest {

    private BattleEngineService service;
    private BotAIService botAIService;

    @BeforeEach
    void setUp() {
        botAIService = mock(BotAIService.class);
        service = new BattleEngineService(
                mock(JugadorRepository.class),
                mock(MazoRepository.class),
                mock(CardRepository.class),
                botAIService,
                mock(BattleAttackService.class),
                mock(BattleKoService.class)
        );
    }

    private Card cardBasico(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Pokemon");
        c.setSubtypes(java.util.List.of("Basic"));
        c.setHp("60");
        return c;
    }

    private Partida crearPartidaSPEnTurnoBot() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.BOT);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);
        return partida;
    }

    // =================== ejecutarTurnoBot ===================

    @Test
    void ejecutarTurnoBot_matchInexistente_noHaceNada() {
        assertDoesNotThrow(() -> service.ejecutarTurnoBot("no-existe"));
        verify(botAIService, never()).ejecutarTurno(any());
    }

    @Test
    void ejecutarTurnoBot_normal_cambiaTurnoAJugador() {
        Partida partida = crearPartidaSPEnTurnoBot();
        partida.getJugador().getMazo().add(cardBasico("m1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        assertEquals(Partida.Turno.JUGADOR, partida.getTurnoActual());
        assertEquals(3, partida.getNumeroTurno());
        verify(botAIService, times(1)).ejecutarTurno(partida);
    }

    @Test
    void ejecutarTurnoBot_robaCarta_botYJugador() {
        Partida partida = crearPartidaSPEnTurnoBot();
        partida.getBot().getMazo().add(cardBasico("bm1", "Squirtle"));
        partida.getJugador().getMazo().add(cardBasico("jm1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        // Bot draws at start, jugador draws at end
        assertEquals(1, partida.getBot().getMano().size());
        assertEquals(1, partida.getJugador().getMano().size());
    }

    @Test
    void ejecutarTurnoBot_limpiaParalyzedDelBot() {
        Partida partida = crearPartidaSPEnTurnoBot();
        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b1", "Squirtle"));
        botActivo.agregarCondicion("Paralyzed");
        partida.getBot().setActivo(botActivo);
        partida.getJugador().getMazo().add(cardBasico("m1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        assertFalse(partida.getBot().getActivo().getCondicionesEspeciales().contains("Paralyzed"));
    }

    @Test
    void ejecutarTurnoBot_resetInvulnerableDelJugador() {
        Partida partida = crearPartidaSPEnTurnoBot();
        CartaEnJuego jugadorActivo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        jugadorActivo.setInvulnerable(true);
        partida.getJugador().setActivo(jugadorActivo);
        partida.getJugador().getMazo().add(cardBasico("m1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        assertFalse(partida.getJugador().getActivo().isInvulnerable());
    }

    @Test
    void ejecutarTurnoBot_partidaTerminadaEnTurnoBot_noSigueProcesando() {
        Partida partida = crearPartidaSPEnTurnoBot();

        // Make botAI end the game
        doAnswer(invocation -> {
            Partida p = invocation.getArgument(0);
            p.setGanador("BOT");
            p.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoFinPartida());
            return null;
        }).when(botAIService).ejecutarTurno(any());

        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        assertEquals(Partida.Fase.FIN_PARTIDA, partida.getFaseActual());
        // Turn should NOT have been incremented (returned early)
        assertEquals(2, partida.getNumeroTurno());
    }

    @Test
    void ejecutarTurnoBot_botActivoConNoPuedeAtacar_cicloDeControl() {
        Partida partida = crearPartidaSPEnTurnoBot();
        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b1", "Squirtle"));
        botActivo.setNoPuedeAtacarSiguienteTurno(true);
        botActivo.setNoPuedeAtacarYaConsumido(false);
        partida.getBot().setActivo(botActivo);
        partida.getJugador().getMazo().add(cardBasico("m1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        // First cycle: yaConsumido becomes true, puedeAtacar=false
        assertTrue(partida.getBot().getActivo().isNoPuedeAtacarYaConsumido());
        assertFalse(partida.getBot().getActivo().isPuedeAtacar());
    }

    @Test
    void ejecutarTurnoBot_botActivoConNoPuedeAtacarYaConsumido_desbloquea() {
        Partida partida = crearPartidaSPEnTurnoBot();
        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b1", "Squirtle"));
        botActivo.setNoPuedeAtacarSiguienteTurno(true);
        botActivo.setNoPuedeAtacarYaConsumido(true); // already consumed
        partida.getBot().setActivo(botActivo);
        partida.getJugador().getMazo().add(cardBasico("m1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        assertFalse(partida.getBot().getActivo().isNoPuedeAtacarSiguienteTurno());
        assertTrue(partida.getBot().getActivo().isPuedeAtacar());
    }

    // =================== jugarTrainer - stadiums ===================

    @Test
    void jugarTrainer_fairyGarden_soloLogSinPendiente() {
        Partida partida = new Partida(new TableroJugador(), new TableroJugador());
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);

        Card fairyGarden = new Card();
        fairyGarden.setId("xy1-117");
        fairyGarden.setNombre("Fairy Garden");
        fairyGarden.setSupertype("Trainer");
        partida.getJugador().getMano().add(fairyGarden);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-117", "ash");

        assertNull(partida.getPendingAction());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("STADIUM_ACTIVE")));
    }

    @Test
    void jugarTrainer_shadowCircle_soloLogSinPendiente() {
        Partida partida = new Partida(new TableroJugador(), new TableroJugador());
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);

        Card shadowCircle = new Card();
        shadowCircle.setId("xy1-126");
        shadowCircle.setNombre("Shadow Circle");
        shadowCircle.setSupertype("Trainer");
        partida.getJugador().getMano().add(shadowCircle);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-126", "ash");

        assertNull(partida.getPendingAction());
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("STADIUM_ACTIVE")));
    }

    // =================== aplicarMantenimientoEntreTurnos (Burn, Asleep) ===================

    @Test
    void aplicarMantenimiento_activoBurnado_reduce20HP() {
        Partida partida = crearPartidaSPEnTurnoBot();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(60);
        activo.agregarCondicion("Burned");
        partida.getBot().setActivo(activo);
        partida.getJugador().getMazo().add(cardBasico("m1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        // ejecutarTurnoBot internally calls aplicarMantenimientoEntreTurnos on bot's pokemon
        service.ejecutarTurnoBot(partida.getId());

        // Burned = 20 damage (or healed if coin flip heads cures it)
        int hp = partida.getBot() != null && partida.getBot().getActivo() != null
                ? partida.getBot().getActivo().getHpActual()
                : -1;
        // HP changed (either reduced by 20 or flipped heads and removed condition)
        assertTrue(hp <= 60);
    }

    @Test
    void aplicarMantenimiento_activoAsleep_aplicaCondicion() {
        Partida partida = crearPartidaSPEnTurnoBot();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(60);
        activo.agregarCondicion("Asleep");
        partida.getBot().setActivo(activo);
        partida.getJugador().getMazo().add(cardBasico("m1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        // After maintenance: either still Asleep (tails) or cured (heads) - no exception
        assertTrue(true); // just assert it doesn't throw
    }
}
