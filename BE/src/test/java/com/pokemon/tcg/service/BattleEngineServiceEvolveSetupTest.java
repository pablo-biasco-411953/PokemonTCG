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

class BattleEngineServiceEvolveSetupTest {

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

    // =================== evolucionarPokemon ===================

    @Test
    void evolucionarPokemon_activoValido_evoluciona() {
        Partida partida = crearPartidaEnTurno();
        partida.getJugador().setTurnosJugados(2); // must be > 1 to allow evolution

        Card bulbasaur = cardBasico("xy1-1", "Bulbasaur");
        CartaEnJuego activo = new CartaEnJuego(bulbasaur);
        activo.setHpActual(60);
        activo.setTurnoEntrada(1); // entered on turn 1, current is turn 2 → allowed
        partida.getJugador().setActivo(activo);

        Card ivysaur = new Card();
        ivysaur.setId("xy1-2");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setSubtypes(java.util.List.of("Stage 1"));
        ivysaur.setEvolvesFrom("Bulbasaur");
        ivysaur.setHp("90");
        partida.getJugador().getMano().add(ivysaur);

        service.partidasEnCurso.put(partida.getId(), partida);

        service.evolucionarPokemon(partida.getId(), "xy1-2", "xy1-1", "ash");

        assertEquals("Ivysaur", partida.getJugador().getActivo().getCard().getNombre());
        assertTrue(partida.getJugador().getMano().isEmpty());
    }

    @Test
    void evolucionarPokemon_cartaNoEnMano_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("xy1-1", "Bulbasaur")));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.evolucionarPokemon(partida.getId(), "xy1-99", "xy1-1", "ash"));
    }

    @Test
    void evolucionarPokemon_pokemonNoEnTablero_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card ivysaur = new Card();
        ivysaur.setId("xy1-2");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setEvolvesFrom("Bulbasaur");
        ivysaur.setHp("90");
        partida.getJugador().getMano().add(ivysaur);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.evolucionarPokemon(partida.getId(), "xy1-2", "xy1-99", "ash"));
    }

    @Test
    void evolucionarPokemon_noEsEvolucionDelBasico_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        partida.getJugador().setTurnosJugados(2);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("xy1-1", "Pikachu"));
        activo.setTurnoEntrada(1);
        partida.getJugador().setActivo(activo);

        Card ivysaur = new Card();
        ivysaur.setId("xy1-2");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setEvolvesFrom("Bulbasaur"); // doesn't evolve from Pikachu
        ivysaur.setHp("90");
        partida.getJugador().getMano().add(ivysaur);

        service.partidasEnCurso.put(partida.getId(), partida);

        // ComandoEvolucionar throws IllegalStateException for invalid evolution
        assertThrows(IllegalStateException.class,
                () -> service.evolucionarPokemon(partida.getId(), "xy1-2", "xy1-1", "ash"));
    }

    // =================== ejecutarSetupBot (single player only) ===================

    @Test
    void ejecutarSetupBot_matchInexistente_noHaceNada() {
        assertDoesNotThrow(() -> service.ejecutarSetupBot("no-existe"));
    }

    @Test
    void ejecutarSetupBot_multiPlayer_noHaceNada() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty"); // multiplayer
        partida.transicionarA(new EstadoSetupPlaceActive());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarSetupBot(partida.getId());

        // For multiplayer, nothing should change - still in initial phase
        assertEquals(Partida.Fase.SETUP_PLACE_ACTIVE, partida.getFaseActual());
    }

    @Test
    void ejecutarSetupBot_placeActiveFase_transicionaABench() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.transicionarA(new EstadoSetupPlaceActive());
        partida.setSetupJugadorListo(true);

        // After botAI ejecutarSetup, bot marks itself as ready
        partida.setSetupBotListo(true);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarSetupBot(partida.getId());

        assertEquals(Partida.Fase.SETUP_PLACE_BENCH, partida.getFaseActual());
    }

    @Test
    void ejecutarSetupBot_placeBenchFase_transicionaAPrizePlacement() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.transicionarA(new EstadoSetupPlaceBench());
        partida.setSetupJugadorListo(true);
        partida.setSetupBotListo(true);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarSetupBot(partida.getId());

        assertEquals(Partida.Fase.SETUP_PRIZE_PLACEMENT, partida.getFaseActual());
    }

    // =================== getTableroDeJugador and getTableroOponente ===================

    @Test
    void getTableroDeJugador_jugadorUsername_retornaTableroJugador() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        TableroJugador resultado = service.getTableroDeJugador(partida, "ash");

        assertSame(jugador, resultado);
    }

    @Test
    void getTableroDeJugador_botUsername_retornaTableroBot() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty");

        TableroJugador resultado = service.getTableroDeJugador(partida, "misty");

        assertSame(bot, resultado);
    }

    @Test
    void getTableroOponente_jugadorUsername_retornaTableroBot() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        TableroJugador resultado = service.getTableroOponente(partida, "ash");

        assertSame(bot, resultado);
    }
}
