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
import static org.mockito.Mockito.mock;

class BattleEngineServicePasarTurnoTest {

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

    private Partida crearPartidaSPEnTurnoJugador() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);
        return partida;
    }

    private Partida crearPartidaMPEnTurnoJugador() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);
        return partida;
    }

    // =================== pasarTurno ===================

    @Test
    void pasarTurno_singlePlayer_cambiaTurnoABot() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertEquals(Partida.Turno.BOT, partida.getTurnoActual());
        assertEquals(3, partida.getNumeroTurno());
    }

    @Test
    void pasarTurno_resetearFlagsDelTurno() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        partida.setYaSeRetiroEsteTurno(true);
        partida.setYaSeUnioEnergiaEsteTurno(true);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertFalse(partida.isYaSeRetiroEsteTurno());
        assertFalse(partida.isYaSeUnioEnergiaEsteTurno());
    }

    @Test
    void pasarTurno_limpiaParalyzedDelActivo() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.agregarCondicion("Paralyzed");
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertFalse(partida.getJugador().getActivo().getCondicionesEspeciales().contains("Paralyzed"));
    }

    @Test
    void pasarTurno_multiPlayer_cambiaABotYRobaCarta() {
        Partida partida = crearPartidaMPEnTurnoJugador();
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        partida.getBot().getMazo().add(cardBasico("xy1-1", "Squirtle"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertEquals(Partida.Turno.BOT, partida.getTurnoActual());
        assertEquals(1, partida.getBot().getMano().size()); // drew one card
    }

    @Test
    void pasarTurno_bancaLlena_sinActivo_lanzaExcepcion() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        partida.getJugador().getBanca().add(new CartaEnJuego(cardBasico("b1", "Bulbasaur")));
        // no active
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.pasarTurno(partida.getId(), "ash"));
    }

    @Test
    void pasarTurno_activoConInvulnerableOponente_seDesactiva() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("p2", "Squirtle"));
        botActivo.setInvulnerable(true);
        partida.getBot().setActivo(botActivo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertFalse(partida.getBot().getActivo().isInvulnerable());
    }

    @Test
    void pasarTurno_activoPoisonado_reduceFP() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(60);
        activo.agregarCondicion("Poisoned");
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.pasarTurno(partida.getId(), "ash");

        assertTrue(partida.getJugador().getActivo().getHpActual() < 60 ||
                partida.getFaseActual() == Partida.Fase.FIN_PARTIDA);
    }

    // =================== jugarTrainer (basic cases) ===================

    @Test
    void jugarTrainer_cartaNoEnMano_lanzaExcepcion() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarTrainer(partida.getId(), "xy1-115", "ash"));
    }

    @Test
    void jugarTrainer_noEsTrainer_lanzaExcepcion() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        Card pokemon = cardBasico("xy1-1", "Pikachu");
        partida.getJugador().getMano().add(pokemon);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarTrainer(partida.getId(), "xy1-1", "ash"));
    }

    // =================== subirAActivoDesdeBanca ===================

    @Test
    void subirAActivoDesdeBanca_conBanca_subeActivo() {
        Partida partida = crearPartidaSPEnTurnoJugador();
        CartaEnJuego bancado = new CartaEnJuego(cardBasico("b1", "Bulbasaur"));
        partida.getJugador().getBanca().add(bancado);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.subirAActivoDesdeBanca(partida.getId(), "b1", "ash");

        assertNotNull(partida.getJugador().getActivo());
        assertEquals("Bulbasaur", partida.getJugador().getActivo().getCard().getNombre());
    }
}
