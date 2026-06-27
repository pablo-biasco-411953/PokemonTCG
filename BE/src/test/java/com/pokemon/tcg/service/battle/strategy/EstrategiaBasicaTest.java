package com.pokemon.tcg.service.battle.strategy;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.*;
import com.pokemon.tcg.service.BattleAttackService;
import com.pokemon.tcg.service.BattleKoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EstrategiaBasicaTest {

    private EstrategiaBasica estrategia;
    private Partida partida;
    private TableroJugador jugador;
    private TableroJugador bot;

    @BeforeEach
    void setUp() {
        estrategia = new EstrategiaBasica(mock(BattleAttackService.class), mock(BattleKoService.class));
        jugador = new TableroJugador();
        bot = new TableroJugador();
        partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
    }

    private Card cardBasico(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setHp("60");
        return c;
    }

    private Card cardEnergia(String nombre) {
        Card c = new Card();
        c.setId("e-" + nombre);
        c.setNombre(nombre);
        c.setSupertype("Energy");
        c.setHp("0");
        return c;
    }

    // =================== ejecutarSetup ===================

    @Test
    void ejecutarSetup_placeActive_conBasico_colocaActivo() {
        partida.transicionarA(new EstadoSetupPlaceActive());
        bot.getMano().add(cardBasico("xy1-1", "Bulbasaur"));

        estrategia.ejecutarSetup(partida);

        assertNotNull(bot.getActivo());
        assertEquals("Bulbasaur", bot.getActivo().getCard().getNombre());
        assertTrue(bot.getActivo().isBocaAbajo());
        assertTrue(bot.getMano().isEmpty());
    }

    @Test
    void ejecutarSetup_placeActive_sinBasico_noColocaActivo() {
        partida.transicionarA(new EstadoSetupPlaceActive());
        bot.getMano().add(cardEnergia("Fire Energy"));

        estrategia.ejecutarSetup(partida);

        assertNull(bot.getActivo());
    }

    @Test
    void ejecutarSetup_placeBench_colocaBasicosEnBanca() {
        partida.transicionarA(new EstadoSetupPlaceBench());
        bot.getMano().add(cardBasico("xy1-1", "Charmander"));
        bot.getMano().add(cardBasico("xy1-2", "Squirtle"));

        estrategia.ejecutarSetup(partida);

        assertEquals(2, bot.getBanca().size());
        assertTrue(bot.getMano().isEmpty());
    }

    @Test
    void ejecutarSetup_placeBench_maxCincoBanca() {
        partida.transicionarA(new EstadoSetupPlaceBench());
        for (int i = 0; i < 5; i++) {
            bot.getBanca().add(new CartaEnJuego(cardBasico("b" + i, "Pokemon" + i)));
        }
        bot.getMano().add(cardBasico("xy1-1", "Extra"));

        estrategia.ejecutarSetup(partida);

        assertEquals(5, bot.getBanca().size());
    }

    @Test
    void ejecutarSetup_prizePlacement_marcaBotListo() {
        partida.transicionarA(new EstadoSetupPrizePlacement());

        estrategia.ejecutarSetup(partida);

        assertTrue(partida.isSetupBotListo());
    }

    @Test
    void ejecutarSetup_faseOtra_noHaceNada() {
        partida.transicionarA(new EstadoTurnoNormal());

        estrategia.ejecutarSetup(partida);

        assertFalse(partida.isSetupBotListo());
    }

    // =================== ejecutarTurno ===================

    @Test
    void ejecutarTurno_sinActivo_conBanca_subeElMejor() {
        CartaEnJuego b1 = new CartaEnJuego(cardBasico("b1", "Bulbasaur"));
        b1.setHpActual(60);
        CartaEnJuego b2 = new CartaEnJuego(cardBasico("b2", "Charmander"));
        b2.setHpActual(50);
        bot.getBanca().add(b1);
        bot.getBanca().add(b2);

        jugador.setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        partida.setNumeroTurno(2);

        estrategia.ejecutarTurno(partida);

        assertNotNull(bot.getActivo());
        assertEquals("Bulbasaur", bot.getActivo().getCard().getNombre());
        assertEquals(1, bot.getBanca().size());
    }

    @Test
    void ejecutarTurno_conActivoYSinBasicoEnMano_noModificaMano() {
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        bot.setActivo(activo);
        bot.getMano().add(cardEnergia("Lightning Energy"));
        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Squirtle")));
        partida.setNumeroTurno(2);

        int manoAntes = bot.getMano().size();

        estrategia.ejecutarTurno(partida);

        assertEquals(manoAntes, bot.getMano().size()); // no basic to play
    }

    @Test
    void ejecutarTurno_primerTurno_noAtaca() {
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        bot.setActivo(activo);
        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Squirtle")));
        partida.setNumeroTurno(1);

        assertDoesNotThrow(() -> estrategia.ejecutarTurno(partida));
        // jugador activo hp should be unchanged since turn 1 = no attack
        assertEquals(60, jugador.getActivo().getHpActual());
    }

    @Test
    void ejecutarTurno_retirada_activoPoisonado_conSuplente() {
        Card activoCard = cardBasico("p1", "Bulbasaur");
        activoCard.setCostoRetirada(0);
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.setHpActual(5);
        activo.agregarCondicion("Poisoned");
        bot.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardBasico("b1", "Charmander"));
        suplente.setHpActual(60);
        bot.getBanca().add(suplente);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Pikachu")));
        partida.setNumeroTurno(2);

        estrategia.ejecutarTurno(partida);

        assertEquals("Charmander", bot.getActivo().getCard().getNombre());
        assertTrue(partida.isYaSeRetiroEsteTurno());
    }
}
