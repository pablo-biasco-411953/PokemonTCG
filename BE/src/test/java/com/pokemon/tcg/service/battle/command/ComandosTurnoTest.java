package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ComandosTurnoTest {

    private Partida partida;
    private TableroJugador tableroJugador;

    @BeforeEach
    void setUp() {
        tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();
        partida = new Partida(tableroJugador, tableroBot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("BOT");
    }

    private Card cardPokemon(String id, String nombre, String hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setHp(hp);
        c.setTipo("Basic Pokemon");
        return c;
    }

    private Card cardPokemonEvolucion(String id, String nombre, String hp, String evolvesFrom) {
        Card c = cardPokemon(id, nombre, hp);
        c.setEvolvesFrom(evolvesFrom);
        return c;
    }

    private Card cardEnergia(String nombre) {
        Card c = new Card();
        c.setId("energy-" + nombre);
        c.setNombre(nombre);
        c.setSupertype("Energy");
        return c;
    }

    // =================== ComandoJugarPokemon ===================

    @Test
    void jugarPokemon_activo_null_poneLoPrimeroComoActivo() {
        Card bulbasaur = cardPokemon("xy1-1", "Bulbasaur", "60");
        tableroJugador.getMano().add(bulbasaur);

        ComandoJugarPokemon cmd = new ComandoJugarPokemon(bulbasaur, tableroJugador);
        assertTrue(cmd.puedeEjecutar(partida));
        cmd.ejecutar(partida);

        assertNotNull(tableroJugador.getActivo());
        assertEquals("Bulbasaur", tableroJugador.getActivo().getCard().getNombre());
        assertFalse(tableroJugador.getMano().contains(bulbasaur));
    }

    @Test
    void jugarPokemon_conActivo_vaALaBanca() {
        Card charmander = cardPokemon("xy1-2", "Charmander", "50");
        CartaEnJuego activoPrevio = new CartaEnJuego(cardPokemon("xy1-3", "Squirtle", "40"));
        tableroJugador.setActivo(activoPrevio);
        tableroJugador.getMano().add(charmander);

        new ComandoJugarPokemon(charmander, tableroJugador).ejecutar(partida);

        assertEquals(1, tableroJugador.getBanca().size());
        assertEquals("Charmander", tableroJugador.getBanca().get(0).getCard().getNombre());
    }

    @Test
    void jugarPokemon_cartaNoEnMano_noPuedeEjecutar() {
        Card pikachu = cardPokemon("xy1-4", "Pikachu", "60");
        ComandoJugarPokemon cmd = new ComandoJugarPokemon(pikachu, tableroJugador);
        assertFalse(cmd.puedeEjecutar(partida));
    }

    @Test
    void jugarPokemon_bancaLlena_lanzaExcepcion() {
        Card activo = cardPokemon("xy1-0", "Activo", "60");
        tableroJugador.setActivo(new CartaEnJuego(activo));

        for (int i = 0; i < 5; i++) {
            tableroJugador.getBanca().add(new CartaEnJuego(cardPokemon("xy1-" + i, "Pokemon" + i, "40")));
        }

        Card nuevo = cardPokemon("xy1-99", "Extra", "40");
        tableroJugador.getMano().add(nuevo);

        assertThrows(IllegalStateException.class,
                () -> new ComandoJugarPokemon(nuevo, tableroJugador).ejecutar(partida));
    }

    @Test
    void jugarPokemon_getNombre_contienePokemon() {
        Card card = cardPokemon("xy1-1", "Bulbasaur", "60");
        tableroJugador.getMano().add(card);
        String nombre = new ComandoJugarPokemon(card, tableroJugador).getNombre();
        assertTrue(nombre.contains("Bulbasaur"));
    }

    // =================== ComandoUnirEnergia ===================

    @Test
    void unirEnergia_exitoso_quitaDeManoYAgregaAEnergias() {
        Card energia = cardEnergia("Fire Energy");
        CartaEnJuego objetivo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.getMano().add(energia);

        ComandoUnirEnergia cmd = new ComandoUnirEnergia(objetivo, energia, tableroJugador, null);
        assertTrue(cmd.puedeEjecutar(partida));
        cmd.ejecutar(partida);

        assertEquals(1, objetivo.getEnergiasUnidas().size());
        assertFalse(tableroJugador.getMano().contains(energia));
        assertTrue(partida.isYaSeUnioEnergiaEsteTurno());
    }

    @Test
    void unirEnergia_yaSeUnioEnergia_noPuedeEjecutar() {
        partida.setYaSeUnioEnergiaEsteTurno(true);
        Card energia = cardEnergia("Water Energy");
        CartaEnJuego objetivo = new CartaEnJuego(cardPokemon("xy1-1", "Squirtle", "50"));
        tableroJugador.getMano().add(energia);

        assertFalse(new ComandoUnirEnergia(objetivo, energia, tableroJugador, null).puedeEjecutar(partida));
    }

    @Test
    void unirEnergia_energiaNula_noPuedeEjecutar() {
        CartaEnJuego objetivo = new CartaEnJuego(cardPokemon("xy1-1", "Squirtle", "50"));
        assertFalse(new ComandoUnirEnergia(objetivo, null, tableroJugador, null).puedeEjecutar(partida));
    }

    @Test
    void unirEnergia_objetivoNulo_noPuedeEjecutar() {
        Card energia = cardEnergia("Water Energy");
        tableroJugador.getMano().add(energia);
        assertFalse(new ComandoUnirEnergia(null, energia, tableroJugador, null).puedeEjecutar(partida));
    }

    @Test
    void unirEnergia_yaSeUnio_lanzaExcepcion() {
        partida.setYaSeUnioEnergiaEsteTurno(true);
        Card energia = cardEnergia("Fire Energy");
        CartaEnJuego objetivo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));

        assertThrows(IllegalStateException.class,
                () -> new ComandoUnirEnergia(objetivo, energia, tableroJugador, null).ejecutar(partida));
    }

    @Test
    void unirEnergia_getNombre() {
        Card energia = cardEnergia("Fire Energy");
        CartaEnJuego objetivo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.getMano().add(energia);
        String nombre = new ComandoUnirEnergia(objetivo, energia, tableroJugador, null).getNombre();
        assertTrue(nombre.contains("Fire Energy"));
    }

    // =================== ComandoEvolucionar ===================

    @Test
    void evolucionar_exitoso_cambiaCard() {
        Card ivysaur = cardPokemonEvolucion("xy1-2", "Ivysaur", "90", "Bulbasaur");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        bulbasaur.setTurnoEntrada(0);

        tableroJugador.setActivo(bulbasaur);
        tableroJugador.getMano().add(ivysaur);
        tableroJugador.setTurnosJugados(3);

        ComandoEvolucionar cmd = new ComandoEvolucionar(ivysaur, bulbasaur, tableroJugador);
        assertTrue(cmd.puedeEjecutar(partida));
        cmd.ejecutar(partida);

        assertEquals("Ivysaur", tableroJugador.getActivo().getCard().getNombre());
        assertFalse(tableroJugador.getMano().contains(ivysaur));
    }

    @Test
    void evolucionar_primerTurno_noPuedeEjecutar() {
        Card ivysaur = cardPokemonEvolucion("xy1-2", "Ivysaur", "90", "Bulbasaur");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.getMano().add(ivysaur);
        tableroJugador.setTurnosJugados(1);

        assertFalse(new ComandoEvolucionar(ivysaur, bulbasaur, tableroJugador).puedeEjecutar(partida));
    }

    @Test
    void evolucionar_mismoTurnoEntrada_noPuedeEjecutar() {
        Card ivysaur = cardPokemonEvolucion("xy1-2", "Ivysaur", "90", "Bulbasaur");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        bulbasaur.setTurnoEntrada(partida.getNumeroTurno());

        tableroJugador.getMano().add(ivysaur);
        tableroJugador.setTurnosJugados(3);

        assertFalse(new ComandoEvolucionar(ivysaur, bulbasaur, tableroJugador).puedeEjecutar(partida));
    }

    @Test
    void evolucionar_evolucionInvalida_lanzaExcepcion() {
        Card raichu = cardPokemonEvolucion("xy1-3", "Raichu", "90", "Pikachu");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        bulbasaur.setTurnoEntrada(0);

        tableroJugador.getMano().add(raichu);
        tableroJugador.setTurnosJugados(3);

        assertThrows(IllegalStateException.class,
                () -> new ComandoEvolucionar(raichu, bulbasaur, tableroJugador).ejecutar(partida));
    }

    @Test
    void evolucionar_getNombre() {
        Card ivysaur = cardPokemonEvolucion("xy1-2", "Ivysaur", "90", "Bulbasaur");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.getMano().add(ivysaur);
        String nombre = new ComandoEvolucionar(ivysaur, bulbasaur, tableroJugador).getNombre();
        assertTrue(nombre.contains("Ivysaur"));
    }

    // =================== ComandoRetirarse ===================

    @Test
    void retirarse_puedeEjecutar_conBanca() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        CartaEnJuego suplente = new CartaEnJuego(cardPokemon("xy1-2", "Charmander", "50"));
        tableroJugador.setActivo(activo);
        tableroJugador.getBanca().add(suplente);

        ComandoRetirarse cmd = new ComandoRetirarse(suplente.getCard().getId(), tableroJugador);
        assertTrue(cmd.puedeEjecutar(partida));
    }

    @Test
    void retirarse_yaSeRetiro_noPuedeEjecutar() {
        partida.setYaSeRetiroEsteTurno(true);
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.setActivo(activo);

        assertFalse(new ComandoRetirarse("xy1-2", tableroJugador).puedeEjecutar(partida));
    }

    @Test
    void retirarse_paralizado_noPuedeEjecutar() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        activo.agregarCondicion("Paralyzed");
        tableroJugador.setActivo(activo);

        assertFalse(new ComandoRetirarse("xy1-2", tableroJugador).puedeEjecutar(partida));
    }

    @Test
    void retirarse_dormido_noPuedeEjecutar() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        activo.agregarCondicion("Asleep");
        tableroJugador.setActivo(activo);

        assertFalse(new ComandoRetirarse("xy1-2", tableroJugador).puedeEjecutar(partida));
    }

    @Test
    void retirarse_sinActivo_noPuedeEjecutar() {
        assertFalse(new ComandoRetirarse("xy1-1", tableroJugador).puedeEjecutar(partida));
    }

    @Test
    void retirarse_getNombre() {
        assertEquals("Retirarse", new ComandoRetirarse("xy1-1", tableroJugador).getNombre());
    }
}
