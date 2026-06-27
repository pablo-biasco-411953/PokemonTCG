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
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BattleEngineServiceStatusTest {

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

    private Card cardBasico(String id, String nombre, int hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Pokemon");
        c.setSubtypes(java.util.List.of("Basic"));
        c.setHp(String.valueOf(hp));
        return c;
    }

    private Partida crearPartidaEnTurno() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);
        return partida;
    }

    // =================== Burned status (via procesarEstado) ===================

    @RepeatedTest(10)
    void pasarTurno_activoBurnado_reducePVo20oLimpia() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 60));
        activo.setHpActual(60);
        activo.agregarCondicion("Burned");
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        // Either took damage OR condition was removed (random coin)
        boolean damageTaken = partida.getJugador().getActivo() != null &&
                partida.getJugador().getActivo().getHpActual() < 60;
        boolean conditionRemoved = partida.getJugador().getActivo() != null &&
                !partida.getJugador().getActivo().getCondicionesEspeciales().contains("Burned");
        assertTrue(damageTaken || conditionRemoved || partida.getFaseActual() == Partida.Fase.FIN_PARTIDA);
    }

    @Test
    void pasarTurno_activoBurnadoConPocoHP_puedeTerminarPartida() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 20));
        activo.setHpActual(10);
        activo.agregarCondicion("Burned");
        partida.getJugador().setActivo(activo);

        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b1", "Bot", 60));
        botActivo.setHpActual(60);
        partida.getBot().setActivo(botActivo);
        partida.getBot().getPremios().add(cardBasico("px", "Premio", 0));

        service.partidasEnCurso.put(partida.getId(), partida);

        assertDoesNotThrow(() -> service.pasarTurno(partida.getId(), "ash"));
    }

    // =================== Asleep status (via procesarEstado) ===================

    @RepeatedTest(10)
    void pasarTurno_activoDormido_despertaOSigue() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Snorlax", 160));
        activo.setHpActual(160);
        activo.agregarCondicion("Asleep");
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        // Must have logged either wake-up or stay asleep
        boolean hasWakeLog = partida.getTurnLogs().stream().anyMatch(l -> l.contains("AWAKE_FLIP_HEADS"));
        boolean hasAsleepLog = partida.getTurnLogs().stream().anyMatch(l -> l.contains("AWAKE_FLIP_TAILS"));
        assertTrue(hasWakeLog || hasAsleepLog);
    }

    // =================== noPuedeAtacar logic ===================

    @Test
    void pasarTurno_noPuedeAtacarNoConsumido_setConsumidoYDesactiva() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 60));
        activo.setHpActual(60);
        activo.setNoPuedeAtacarSiguienteTurno(true);
        activo.setNoPuedeAtacarYaConsumido(false);
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertTrue(activo.isNoPuedeAtacarYaConsumido());
        assertFalse(activo.isPuedeAtacar());
    }

    @Test
    void pasarTurno_noPuedeAtacarYaConsumido_resetea() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 60));
        activo.setHpActual(60);
        activo.setNoPuedeAtacarSiguienteTurno(true);
        activo.setNoPuedeAtacarYaConsumido(true);
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertFalse(activo.isNoPuedeAtacarSiguienteTurno());
        assertFalse(activo.isNoPuedeAtacarYaConsumido());
        assertTrue(activo.isPuedeAtacar());
    }

    // =================== ataqueBloqueado logic ===================

    @Test
    void pasarTurno_ataqueBloqueadoNoConsumido_marcaComoConsumido() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 60));
        activo.setHpActual(60);
        activo.setAtaqueBloqueadoSiguienteTurno("Tackle");
        activo.setAtaqueBloqueadoYaConsumido(false);
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertTrue(activo.isAtaqueBloqueadoYaConsumido());
        assertEquals("Tackle", activo.getAtaqueBloqueadoSiguienteTurno());
    }

    @Test
    void pasarTurno_ataqueBloqueadoYaConsumido_limpiaBloqueo() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 60));
        activo.setHpActual(60);
        activo.setAtaqueBloqueadoSiguienteTurno("Tackle");
        activo.setAtaqueBloqueadoYaConsumido(true);
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertNull(activo.getAtaqueBloqueadoSiguienteTurno());
        assertFalse(activo.isAtaqueBloqueadoYaConsumido());
    }

    // =================== multiPlayer: BOT → JUGADOR transition ===================

    @Test
    void pasarTurno_multiPlayer_botTurno_cambiaAJugadorYRobaCarta() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty");
        partida.setTurnoActual(Partida.Turno.BOT);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);

        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b1", "Charmander", 50));
        botActivo.setHpActual(50);
        partida.getBot().setActivo(botActivo);

        partida.getJugador().getMazo().add(cardBasico("j1", "Pikachu", 60));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "misty");

        assertEquals(Partida.Turno.JUGADOR, partida.getTurnoActual());
        assertEquals(1, partida.getJugador().getMano().size());
    }

    // =================== ejecutarSetupBot ===================

    @Test
    void ejecutarSetupBot_multiPlayer_noHaceNada() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty");
        service.partidasEnCurso.put(partida.getId(), partida);

        assertDoesNotThrow(() -> service.ejecutarSetupBot(partida.getId()));
    }

    @Test
    void ejecutarSetupBot_matchNoExiste_noLanza() {
        assertDoesNotThrow(() -> service.ejecutarSetupBot("no-existe"));
    }

    @Test
    void ejecutarSetupBot_singlePlayerConPlaceActive_haceLlamadaAlBot() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoSetupPlaceActive());

        partida.setSetupJugadorListo(true);
        partida.setSetupBotListo(false);

        service.partidasEnCurso.put(partida.getId(), partida);

        assertDoesNotThrow(() -> service.ejecutarSetupBot(partida.getId()));
    }

    // =================== limpieza de habilidades usadas en banca ===================

    @Test
    void pasarTurno_limpiaHabilidadesUsadasEnBanca() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 60));
        activo.setHpActual(60);
        partida.getJugador().setActivo(activo);

        CartaEnJuego bancado = new CartaEnJuego(cardBasico("b1", "Bulbasaur", 70));
        bancado.registrarUsoHabilidad("Overgrow");
        partida.getJugador().getBanca().add(bancado);

        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertFalse(bancado.getHabilidadesUsadasEsteTurno().contains("Overgrow"));
    }

    // =================== debeLanzarMonedaSiAtaca reset ===================

    @Test
    void pasarTurno_resetDebeLanzarMoneda() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu", 60));
        activo.setHpActual(60);
        activo.setDebeLanzarMonedaSiAtaca(true);
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertFalse(activo.isDebeLanzarMonedaSiAtaca());
    }

    // =================== robarCartas utility ===================

    @Test
    void robarCartas_conMazoLleno_agregaAMano() {
        TableroJugador tablero = new TableroJugador();
        tablero.getMazo().add(cardBasico("c1", "Card1", 60));
        tablero.getMazo().add(cardBasico("c2", "Card2", 60));
        tablero.getMazo().add(cardBasico("c3", "Card3", 60));

        service.robarCartas(tablero, 2);

        assertEquals(2, tablero.getMano().size());
        assertEquals(1, tablero.getMazo().size());
    }

    @Test
    void robarCartas_conMazoVacio_noFalla() {
        TableroJugador tablero = new TableroJugador();
        assertDoesNotThrow(() -> service.robarCartas(tablero, 5));
        assertTrue(tablero.getMano().isEmpty());
    }

    @Test
    void robarCartas_cantidadMayorAlMazo_robaTodos() {
        TableroJugador tablero = new TableroJugador();
        tablero.getMazo().add(cardBasico("c1", "Card1", 60));
        tablero.getMazo().add(cardBasico("c2", "Card2", 60));

        service.robarCartas(tablero, 10);

        assertEquals(2, tablero.getMano().size());
        assertTrue(tablero.getMazo().isEmpty());
    }
}
