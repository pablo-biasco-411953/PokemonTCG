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

class BattleEngineServiceTurnTest {

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

    private Card cardEnergia(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Energy");
        c.setHp("0");
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

    // =================== validarTurno ===================

    @Test
    void validarTurno_jugadorEnSuTurno_noLanza() {
        Partida partida = crearPartidaEnTurno();

        assertDoesNotThrow(() -> service.validarTurno(partida, "ash"));
    }

    @Test
    void validarTurno_noEsSuTurno_lanza() {
        Partida partida = crearPartidaEnTurno();
        partida.setTurnoActual(Partida.Turno.BOT);

        assertThrows(IllegalStateException.class,
                () -> service.validarTurno(partida, "ash"));
    }

    // =================== jugarPokemon ===================

    @Test
    void jugarPokemon_enSuTurno_colocaEnBanca() {
        Partida partida = crearPartidaEnTurno();
        Card bulbasaur = cardBasico("xy1-1", "Bulbasaur");
        partida.getJugador().getMano().add(bulbasaur);
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("xy1-0", "Charmander")));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarPokemon(partida.getId(), "xy1-1", "ash");

        assertEquals(1, partida.getJugador().getBanca().size());
    }

    @Test
    void jugarPokemon_cartaNoEnMano_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarPokemon(partida.getId(), "xy1-99", "ash"));
    }

    @Test
    void jugarPokemon_noEsBasico_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card evolucion = new Card();
        evolucion.setId("xy1-2");
        evolucion.setNombre("Ivysaur");
        evolucion.setSupertype("Pokemon");
        evolucion.setSubtypes(java.util.List.of("Stage 1"));
        evolucion.setEvolvesFrom("Bulbasaur");
        evolucion.setHp("90");
        partida.getJugador().getMano().add(evolucion);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarPokemon(partida.getId(), "xy1-2", "ash"));
    }

    // =================== unirEnergia ===================

    @Test
    void unirEnergia_exitoso_adjuntaEnergia() {
        Partida partida = crearPartidaEnTurno();
        CartaEnJuego activo = new CartaEnJuego(cardBasico("xy1-1", "Pikachu"));
        partida.getJugador().setActivo(activo);

        Card energia = cardEnergia("e1", "Lightning Energy");
        partida.getJugador().getMano().add(energia);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.unirEnergia(partida.getId(), "xy1-1", "e1", "ash", null);

        assertEquals(1, activo.getEnergiasUnidas().size());
        assertTrue(partida.isYaSeUnioEnergiaEsteTurno());
    }

    @Test
    void unirEnergia_pokemonNoEncontrado_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card energia = cardEnergia("e1", "Lightning Energy");
        partida.getJugador().getMano().add(energia);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.unirEnergia(partida.getId(), "xy1-99", "e1", "ash", null));
    }

    @Test
    void unirEnergia_energiaNoEncontrada_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("xy1-1", "Pikachu")));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.unirEnergia(partida.getId(), "xy1-1", "e99", "ash", null));
    }

    // =================== realizarRetirada ===================

    @Test
    void realizarRetirada_exitoso_cambiaPokemon() {
        Partida partida = crearPartidaEnTurno();

        Card activoCard = cardBasico("xy1-1", "Pikachu");
        activoCard.setCostoRetirada(0);
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        partida.getJugador().setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardBasico("xy1-2", "Squirtle"));
        partida.getJugador().getBanca().add(suplente);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.realizarRetirada(partida.getId(), "xy1-2", "ash");

        assertEquals("Squirtle", partida.getJugador().getActivo().getCard().getNombre());
    }

    @Test
    void realizarRetirada_noEsSuTurno_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        partida.setTurnoActual(Partida.Turno.BOT);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.realizarRetirada(partida.getId(), "xy1-2", "ash"));
    }

    // =================== realizarAtaque - primer turno ===================

    @Test
    void realizarAtaque_primerTurno_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        partida.setNumeroTurno(1); // first turn
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.realizarAtaque(partida.getId(), "Thunder", "ash", null));
    }
}
