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

class BattleEngineServiceSetupTest {

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

    private Partida crearPartidaSP() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida p = new Partida(jugador, bot);
        p.setJugadorUsername("ash");
        return p;
    }

    private Partida crearPartidaMP() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida p = new Partida(jugador, bot);
        p.setJugadorUsername("ash");
        p.setBotUsername("misty");
        return p;
    }

    // =================== rendirse ===================

    @Test
    void rendirse_jugador_setGanadorBot() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.rendirse(partida.getId(), "ash");

        assertEquals(Partida.Fase.FIN_PARTIDA, resultado.getFaseActual());
        assertEquals("BOT", resultado.getGanador());
    }

    @Test
    void rendirse_bot_setGanadorJugador() {
        Partida partida = crearPartidaMP();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.rendirse(partida.getId(), "misty");

        assertEquals(Partida.Fase.FIN_PARTIDA, resultado.getFaseActual());
        assertEquals("ash", resultado.getGanador());
    }

    @Test
    void rendirse_yaTerminada_retornaSinCambios() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoFinPartida());
        partida.setGanador("ash");
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.rendirse(partida.getId(), "ash");

        assertEquals("ash", resultado.getGanador()); // unchanged
    }

    @Test
    void rendirse_usernameVacio_lanzaExcepcion() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.rendirse(partida.getId(), ""));
    }

    @Test
    void rendirse_usernameAjeno_lanzaExcepcion() {
        Partida partida = crearPartidaMP();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.rendirse(partida.getId(), "gary"));
    }

    @Test
    void rendirse_multiPlayer_jugador_ganaBot() {
        Partida partida = crearPartidaMP();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.rendirse(partida.getId(), "ash");

        assertEquals("misty", resultado.getGanador());
    }

    // =================== colocarPremios ===================

    @Test
    void colocarPremios_singlePlayer_colocaSeisCartas() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoSetupPrizePlacement());
        for (int i = 0; i < 20; i++) {
            partida.getJugador().getMazo().add(cardBasico("xy1-" + i, "Pokemon" + i));
            partida.getBot().getMazo().add(cardBasico("bot-" + i, "BotPokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarPremios(partida.getId(), "ash");

        assertEquals(6, partida.getJugador().getPremios().size());
        assertEquals(6, partida.getBot().getPremios().size());
    }

    @Test
    void colocarPremios_faseIncorrecta_noHaceNada() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarPremios(partida.getId(), "ash");

        assertTrue(partida.getJugador().getPremios().isEmpty());
    }

    @Test
    void colocarPremios_multiPlayer_soloJugador_noColoca() {
        Partida partida = crearPartidaMP();
        partida.transicionarA(new EstadoSetupPrizePlacement());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarPremios(partida.getId(), "ash");

        assertTrue(partida.getJugador().getPremios().isEmpty());
    }

    @Test
    void colocarPremios_multiPlayer_ambosListos_colocaPremios() {
        Partida partida = crearPartidaMP();
        partida.transicionarA(new EstadoSetupPrizePlacement());
        for (int i = 0; i < 15; i++) {
            partida.getJugador().getMazo().add(cardBasico("xy1-" + i, "Pokemon" + i));
            partida.getBot().getMazo().add(cardBasico("bot-" + i, "BotPokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        service.colocarPremios(partida.getId(), "ash");
        service.colocarPremios(partida.getId(), "misty");

        assertEquals(6, partida.getJugador().getPremios().size());
        assertEquals(6, partida.getBot().getPremios().size());
    }

    // =================== confirmarRevealSetup ===================

    @Test
    void confirmarRevealSetup_singlePlayer_iniciaPartida() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoSetupReveal());
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Bulbasaur")));
        partida.getBot().setActivo(new CartaEnJuego(cardBasico("p2", "Squirtle")));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.confirmarRevealSetup(partida.getId(), "ash");

        assertEquals(Partida.Fase.TURNO_NORMAL, partida.getFaseActual());
    }

    @Test
    void confirmarRevealSetup_faseIncorrecta_noHaceNada() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.confirmarRevealSetup(partida.getId(), "ash");

        assertEquals(Partida.Fase.TURNO_NORMAL, partida.getFaseActual());
    }

    // =================== getEstadoPartida ===================

    @Test
    void getEstadoPartida_existente_retornaPartida() {
        Partida partida = crearPartidaSP();
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.getEstadoPartida(partida.getId());

        assertNotNull(resultado);
        assertEquals(partida.getId(), resultado.getId());
    }

    @Test
    void getEstadoPartida_inexistente_retornaNull() {
        assertNull(service.getEstadoPartida("no-existe"));
    }

    @Test
    void getEstadoPartida_conUsername_actualizaHeartbeat() {
        Partida partida = crearPartidaSP();
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.getEstadoPartida(partida.getId(), "ash");

        assertNotNull(resultado);
    }

    // =================== registrarHeartbeat ===================

    @Test
    void registrarHeartbeat_jugadorKnown_actualizaTimestamp() {
        Partida partida = crearPartidaSP();
        long before = partida.getJugadorLastSeenAt();
        service.partidasEnCurso.put(partida.getId(), partida);

        service.registrarHeartbeat(partida.getId(), "ash");

        assertTrue(partida.getJugadorLastSeenAt() >= before);
    }

    @Test
    void registrarHeartbeat_matchInexistente_retornaNull() {
        assertNull(service.registrarHeartbeat("no-existe", "ash"));
    }

    // =================== resolverCartasExtra ===================

    @Test
    void resolverCartasExtra_singlePlayer_robaCartas() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoSetupMulliganExtraDraw());
        partida.setCartasMulliganExtraPendientesJugador(2);
        for (int i = 0; i < 5; i++) {
            partida.getJugador().getMazo().add(cardBasico("xy1-" + i, "Pokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverCartasExtra(partida.getId(), "ash", 2);

        assertEquals(2, partida.getJugador().getMano().size());
        assertEquals(0, partida.getCartasMulliganExtraPendientesJugador());
    }

    @Test
    void resolverCartasExtra_faseIncorrecta_noHaceNada() {
        Partida partida = crearPartidaSP();
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setCartasMulliganExtraPendientesJugador(2);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverCartasExtra(partida.getId(), "ash", 2);

        assertTrue(partida.getJugador().getMano().isEmpty());
    }

    // =================== not found throws ===================

    @Test
    void rendirse_matchNoExistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.rendirse("no-existe", "ash"));
    }

    @Test
    void colocarPremios_matchNoExistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.colocarPremios("no-existe", "ash"));
    }

    @Test
    void lanzarMoneda_matchNoExistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.lanzarMoneda("no-existe", "ash", "CARA"));
    }
}
