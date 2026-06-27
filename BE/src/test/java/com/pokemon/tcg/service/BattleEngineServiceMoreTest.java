package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.*;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BattleEngineServiceMoreTest {

    private BattleEngineService service;

    @BeforeEach
    void setUp() {
        service = new BattleEngineService(
                mock(JugadorRepository.class),
                mock(MazoRepository.class),
                mock(CardRepository.class),
                mock(BotAIService.class),
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

    private Partida crearPartidaSinglePlayer() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        // no botUsername => single player mode
        return partida;
    }

    private Partida crearPartidaMultiPlayer() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty");
        return partida;
    }

    // =================== lanzarMoneda ===================

    @Test
    void lanzarMoneda_primeraVez_estableceCoinFlipped() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.lanzarMoneda(partida.getId(), "ash", "CARA");

        assertTrue(resultado.isCoinFlipped());
        assertNotNull(resultado.getCoinFlipResult());
        assertNotNull(resultado.getCoinFlipWinner());
    }

    @Test
    void lanzarMoneda_resultadoEsCARAoCRUZ() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.lanzarMoneda(partida.getId(), "ash", "CARA");

        assertTrue(resultado.getCoinFlipResult().equals("CARA") || resultado.getCoinFlipResult().equals("CRUZ"));
    }

    @Test
    void lanzarMoneda_yaLanzada_noReLanza() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        partida.setCoinFlipped(true);
        partida.setCoinFlipResult("CARA");
        partida.setCoinFlipWinner("ash");
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.lanzarMoneda(partida.getId(), "ash", "CRUZ");

        assertEquals("CARA", resultado.getCoinFlipResult());
        assertEquals("ash", resultado.getCoinFlipWinner());
    }

    @Test
    void lanzarMoneda_callerNoAutorizado_lanzaExcepcion() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        partida.setCoinFlipCallerUsername("ash");
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.lanzarMoneda(partida.getId(), "misty", "CARA"));
    }

    @Test
    void lanzarMoneda_callerWins_ganadoresCallerUsername() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        partida.setCoinFlipCallerUsername("ash");
        service.partidasEnCurso.put(partida.getId(), partida);

        // Run until caller wins
        boolean callerWon = false;
        for (int i = 0; i < 50; i++) {
            partida.setCoinFlipped(false);
            partida.setCoinFlipWinner(null);
            partida.setCoinFlipResult(null);
            Partida r = service.lanzarMoneda(partida.getId(), "ash", "CARA");
            if ("ash".equals(r.getCoinFlipWinner())) {
                callerWon = true;
                break;
            }
        }
        assertTrue(callerWon, "Caller should win at least once in 50 tries");
    }

    // =================== actualizarLoading ===================

    @Test
    void actualizarLoading_jugador_actualizaPorcentaje() {
        Partida partida = crearPartidaMultiPlayer();
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.actualizarLoading(partida.getId(), "ash", 75);

        assertEquals(75, resultado.getJugadorLoadingPercentage());
    }

    @Test
    void actualizarLoading_bot_actualizaPorcentajeBot() {
        Partida partida = crearPartidaMultiPlayer();
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.actualizarLoading(partida.getId(), "misty", 50);

        assertEquals(50, resultado.getBotLoadingPercentage());
    }

    @Test
    void actualizarLoading_singlePlayer_botAutoCompleta() {
        Partida partida = crearPartidaSinglePlayer();
        service.partidasEnCurso.put(partida.getId(), partida);

        // null username triggers single-player auto-complete branch (botUsername is null)
        Partida resultado = service.actualizarLoading(partida.getId(), null, 60);

        assertEquals(60, resultado.getJugadorLoadingPercentage());
        assertEquals(100, resultado.getBotLoadingPercentage());
    }

    // =================== actualizarHandshakeMoneda ===================

    @Test
    void actualizarHandshake_jugadorAlcanza100_botAutoCompleta() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.actualizarHandshakeMoneda(partida.getId(), "ash", true, 100);

        assertEquals(100, resultado.getCoinHandshakeJugadorPower());
        assertEquals(100, resultado.getCoinHandshakeBotPower());
        assertTrue(resultado.isCoinHandshakeComplete());
    }

    @Test
    void actualizarHandshake_potenciaClamped() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.actualizarHandshakeMoneda(partida.getId(), "ash", true, 200);

        assertEquals(100, resultado.getCoinHandshakeJugadorPower());
    }

    @Test
    void actualizarHandshake_monedaYaLanzada_noActualiza() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        partida.setCoinFlipped(true);
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.actualizarHandshakeMoneda(partida.getId(), "ash", true, 100);

        assertEquals(0, resultado.getCoinHandshakeJugadorPower());
    }

    @Test
    void actualizarHandshake_faseIncorrecta_noActualiza() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.actualizarHandshakeMoneda(partida.getId(), "ash", true, 100);

        assertEquals(0, resultado.getCoinHandshakeJugadorPower());
    }

    // =================== elegirTurno ===================

    @Test
    void elegirTurno_singlePlayer_vaPrimero_setJugador() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.elegirTurno(partida.getId(), true, "ash");

        assertEquals(Partida.Turno.JUGADOR, partida.getTurnoActual());
        assertEquals(Partida.Fase.SETUP_INITIAL_DRAW, partida.getFaseActual());
    }

    @Test
    void elegirTurno_singlePlayer_noVaPrimero_setBot() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.elegirTurno(partida.getId(), false, "ash");

        assertEquals(Partida.Turno.BOT, partida.getTurnoActual());
        assertEquals(Partida.Fase.SETUP_INITIAL_DRAW, partida.getFaseActual());
    }

    @Test
    void elegirTurno_multiPlayer_ganadorElige_vaPrimero() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        partida.setCoinFlipWinner("ash");
        service.partidasEnCurso.put(partida.getId(), partida);

        service.elegirTurno(partida.getId(), true, "ash");

        assertEquals(Partida.Turno.JUGADOR, partida.getTurnoActual());
        assertEquals(Partida.Fase.SETUP_INITIAL_DRAW, partida.getFaseActual());
    }

    @Test
    void elegirTurno_multiPlayer_perdedorIntenta_noActualiza() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoLanzamientoMoneda());
        partida.setCoinFlipWinner("ash");
        partida.setTurnoActual(Partida.Turno.BOT); // set explicit initial value
        service.partidasEnCurso.put(partida.getId(), partida);

        service.elegirTurno(partida.getId(), true, "misty");

        assertEquals(Partida.Turno.BOT, partida.getTurnoActual()); // loser's call should not change it
    }

    @Test
    void elegirTurno_faseYaEsTurnoNormal_noHaceNada() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.elegirTurno(partida.getId(), false, "ash");

        assertEquals(Partida.Turno.JUGADOR, partida.getTurnoActual());
    }

    // =================== evaluarSetupInitialDraw ===================

    @Test
    void evaluarSetupInitialDraw_singlePlayer_conBasico_transicionaAPlaceActive() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupInitialDraw());
        partida.getJugador().getMano().add(cardBasico("xy1-1", "Bulbasaur"));
        partida.getBot().getMano().add(cardBasico("xy1-2", "Charmander"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.evaluarSetupInitialDraw(partida.getId(), "ash");

        assertEquals(Partida.Fase.SETUP_PLACE_ACTIVE, partida.getFaseActual());
    }

    @Test
    void evaluarSetupInitialDraw_singlePlayer_sinBasico_transicionaAMulliganReveal() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupInitialDraw());
        // No basic in jugador mano
        service.partidasEnCurso.put(partida.getId(), partida);

        service.evaluarSetupInitialDraw(partida.getId(), "ash");

        assertEquals(Partida.Fase.SETUP_MULLIGAN_REVEAL, partida.getFaseActual());
    }

    @Test
    void evaluarSetupInitialDraw_faseIncorrecta_noActualiza() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.evaluarSetupInitialDraw(partida.getId(), "ash");

        assertEquals(Partida.Fase.TURNO_NORMAL, partida.getFaseActual());
    }

    // =================== ejecutarMulligan ===================

    @Test
    void ejecutarMulligan_singlePlayer_sinBasico_rehacesMano() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupMulliganReveal());

        Card energyCard = new Card();
        energyCard.setId("e1");
        energyCard.setNombre("Energy");
        energyCard.setSupertype("Energy");
        partida.getJugador().getMano().add(energyCard);

        for (int i = 0; i < 10; i++) {
            partida.getJugador().getMazo().add(cardBasico("xy1-" + i, "Pokemon" + i));
        }
        partida.getBot().getMano().add(cardBasico("xy1-bot", "Squirtle"));

        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarMulligan(partida.getId(), "ash");

        assertEquals(1, partida.getMulligansJugador());
    }

    @Test
    void ejecutarMulligan_singlePlayer_conBasico_noRehace() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupMulliganReveal());
        partida.getJugador().getMano().add(cardBasico("xy1-1", "Bulbasaur"));
        partida.getBot().getMano().add(cardBasico("xy1-2", "Charmander"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarMulligan(partida.getId(), "ash");

        assertEquals(0, partida.getMulligansJugador());
        assertEquals(Partida.Fase.SETUP_PLACE_ACTIVE, partida.getFaseActual());
    }

    @Test
    void ejecutarMulligan_faseIncorrecta_noHaceNada() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarMulligan(partida.getId(), "ash");

        assertEquals(Partida.Fase.TURNO_NORMAL, partida.getFaseActual());
    }

    // =================== colocarActivoSetup ===================

    @Test
    void colocarActivoSetup_singlePlayer_colocaCartaComoActivo() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceActive());
        Card bulbasaur = cardBasico("xy1-1", "Bulbasaur");
        partida.getJugador().getMano().add(bulbasaur);
        partida.getBot().getMano().add(cardBasico("xy1-2", "Charmander"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarActivoSetup(partida.getId(), "ash", "xy1-1");

        assertNotNull(partida.getJugador().getActivo());
        assertEquals("Bulbasaur", partida.getJugador().getActivo().getCard().getNombre());
        assertTrue(partida.getJugador().getActivo().isBocaAbajo());
    }

    @Test
    void colocarActivoSetup_cartaNoEnMano_lanzaExcepcion() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceActive());
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.colocarActivoSetup(partida.getId(), "ash", "xy1-999"));
    }

    @Test
    void colocarActivoSetup_noEsBasico_lanzaExcepcion() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceActive());

        Card evolucion = new Card();
        evolucion.setId("xy1-evo");
        evolucion.setNombre("Ivysaur");
        evolucion.setTipo("Stage 1");
        evolucion.setHp("90");
        evolucion.setEvolvesFrom("Bulbasaur");
        partida.getJugador().getMano().add(evolucion);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.colocarActivoSetup(partida.getId(), "ash", "xy1-evo"));
    }

    @Test
    void colocarActivoSetup_yaColocado_noReplaza() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceActive());

        CartaEnJuego activoPrevio = new CartaEnJuego(cardBasico("xy1-0", "Squirtle"));
        partida.getJugador().setActivo(activoPrevio);

        Card nuevo = cardBasico("xy1-1", "Bulbasaur");
        partida.getJugador().getMano().add(nuevo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarActivoSetup(partida.getId(), "ash", "xy1-1");

        assertEquals("Squirtle", partida.getJugador().getActivo().getCard().getNombre());
    }

    @Test
    void colocarActivoSetup_faseIncorrecta_noHaceNada() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoTurnoNormal());
        partida.getJugador().getMano().add(cardBasico("xy1-1", "Bulbasaur"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarActivoSetup(partida.getId(), "ash", "xy1-1");

        assertNull(partida.getJugador().getActivo());
    }

    @Test
    void colocarActivoSetup_ambosListos_transicionaAPlaceBench() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceActive());
        partida.getJugador().getMano().add(cardBasico("xy1-1", "Bulbasaur"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarActivoSetup(partida.getId(), "ash", "xy1-1");

        assertEquals(Partida.Fase.SETUP_PLACE_BENCH, partida.getFaseActual());
    }

    // =================== colocarBancaSetup ===================

    @Test
    void colocarBancaSetup_agrega_cartaABanca() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceBench());
        partida.getJugador().getMano().add(cardBasico("xy1-2", "Charmander"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarBancaSetup(partida.getId(), "ash", "xy1-2");

        assertEquals(1, partida.getJugador().getBanca().size());
        assertEquals("Charmander", partida.getJugador().getBanca().get(0).getCard().getNombre());
    }

    @Test
    void colocarBancaSetup_bancaLlena_noAgrega() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceBench());
        for (int i = 0; i < 5; i++) {
            partida.getJugador().getBanca().add(new CartaEnJuego(cardBasico("xy1-" + i, "Pokemon" + i)));
        }
        partida.getJugador().getMano().add(cardBasico("xy1-99", "Extra"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarBancaSetup(partida.getId(), "ash", "xy1-99");

        assertEquals(5, partida.getJugador().getBanca().size());
    }

    @Test
    void colocarBancaSetup_noEsBasico_lanzaExcepcion() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceBench());

        Card trainer = new Card();
        trainer.setId("t1");
        trainer.setNombre("Potion");
        trainer.setSupertype("Trainer");
        partida.getJugador().getMano().add(trainer);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.colocarBancaSetup(partida.getId(), "ash", "t1"));
    }

    // =================== confirmarBancaSetup ===================

    @Test
    void confirmarBancaSetup_singlePlayer_transicionaAPrizePlacement() {
        Partida partida = crearPartidaSinglePlayer();
        partida.transicionarA(new EstadoSetupPlaceBench());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.confirmarBancaSetup(partida.getId(), "ash");

        assertEquals(Partida.Fase.SETUP_PRIZE_PLACEMENT, partida.getFaseActual());
    }

    @Test
    void confirmarBancaSetup_multiPlayer_soloJugador_noTransiciona() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoSetupPlaceBench());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.confirmarBancaSetup(partida.getId(), "ash");

        assertEquals(Partida.Fase.SETUP_PLACE_BENCH, partida.getFaseActual());
    }

    @Test
    void confirmarBancaSetup_multiPlayer_ambosListos_transiciona() {
        Partida partida = crearPartidaMultiPlayer();
        partida.transicionarA(new EstadoSetupPlaceBench());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.confirmarBancaSetup(partida.getId(), "ash");
        service.confirmarBancaSetup(partida.getId(), "misty");

        assertEquals(Partida.Fase.SETUP_PRIZE_PLACEMENT, partida.getFaseActual());
    }
}
