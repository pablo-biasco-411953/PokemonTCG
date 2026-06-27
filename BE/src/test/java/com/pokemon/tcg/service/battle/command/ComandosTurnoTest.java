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

    // =================== ComandoRetirarse Ejecucion ===================

    @Test
    void retirarse_ejecutar_exitoso() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        activo.getCard().setCostoRetirada(1);
        Card energy = cardEnergia("Fire Energy");
        activo.getEnergiasUnidas().add(energy);
        tableroJugador.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardPokemon("xy1-2", "Charmander", "50"));
        tableroJugador.getBanca().add(suplente);

        ComandoRetirarse cmd = new ComandoRetirarse(suplente.getCard().getId(), tableroJugador);
        cmd.ejecutar(partida);

        assertNull(tableroJugador.getActivo() == activo ? activo : null);
        assertEquals(suplente, tableroJugador.getActivo());
        assertTrue(tableroJugador.getBanca().contains(activo));
        assertTrue(tableroJugador.getPilaDescarte().contains(energy));
        assertTrue(partida.isYaSeRetiroEsteTurno());
    }

    @Test
    void retirarse_ejecutar_sinActivo_lanzaExcepcion() {
        ComandoRetirarse cmd = new ComandoRetirarse("xy1-2", tableroJugador);
        assertThrows(IllegalStateException.class, () -> cmd.ejecutar(partida));
    }

    @Test
    void retirarse_ejecutar_yaRetirado_lanzaExcepcion() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.setActivo(activo);
        partida.setYaSeRetiroEsteTurno(true);

        ComandoRetirarse cmd = new ComandoRetirarse("xy1-2", tableroJugador);
        assertThrows(IllegalStateException.class, () -> cmd.ejecutar(partida));
    }

    @Test
    void retirarse_ejecutar_condicionesEspeciales_lanzaExcepcion() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.setActivo(activo);
        
        activo.agregarCondicion("Asleep");
        ComandoRetirarse cmd = new ComandoRetirarse("xy1-2", tableroJugador);
        assertThrows(IllegalStateException.class, () -> cmd.ejecutar(partida));

        activo.limpiarCondiciones();
        activo.agregarCondicion("Paralyzed");
        assertThrows(IllegalStateException.class, () -> cmd.ejecutar(partida));

        activo.limpiarCondiciones();
        activo.agregarCondicion("CantRetreat");
        assertThrows(IllegalStateException.class, () -> cmd.ejecutar(partida));
    }

    @Test
    void retirarse_ejecutar_suplenteNoEnBanca_lanzaExcepcion() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.setActivo(activo);

        ComandoRetirarse cmd = new ComandoRetirarse("xy1-2", tableroJugador);
        assertThrows(IllegalArgumentException.class, () -> cmd.ejecutar(partida));
    }

    @Test
    void retirarse_ejecutar_fairyGardenYFairyEnergy_costoRetiradaCero() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        activo.getCard().setCostoRetirada(2);
        Card fairyEnergy = cardEnergia("Fairy Energy");
        fairyEnergy.setTipo("Fairy");
        activo.getEnergiasUnidas().add(fairyEnergy);
        tableroJugador.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardPokemon("xy1-2", "Charmander", "50"));
        tableroJugador.getBanca().add(suplente);

        Card fairyGarden = new Card();
        fairyGarden.setId("xy1-117");
        fairyGarden.setNombre("Fairy Garden");
        partida.setActiveStadium(fairyGarden);

        ComandoRetirarse cmd = new ComandoRetirarse(suplente.getCard().getId(), tableroJugador);
        cmd.ejecutar(partida);

        assertEquals(suplente, tableroJugador.getActivo());
        assertEquals(1, activo.getEnergiasUnidas().size());
        assertFalse(tableroJugador.getPilaDescarte().contains(fairyEnergy));
    }

    @Test
    void retirarse_ejecutar_energiasInsuficientes_lanzaExcepcion() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        activo.getCard().setCostoRetirada(2);
        tableroJugador.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardPokemon("xy1-2", "Charmander", "50"));
        tableroJugador.getBanca().add(suplente);

        ComandoRetirarse cmd = new ComandoRetirarse(suplente.getCard().getId(), tableroJugador);
        assertThrows(IllegalStateException.class, () -> cmd.ejecutar(partida));
    }

    @Test
    void retirarse_ejecutar_pagaConRainbowEnergy_reseteaTipo() {
        CartaEnJuego activo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        activo.getCard().setCostoRetirada(1);
        Card rainbow = new Card();
        rainbow.setId("rainbow-1");
        rainbow.setNombre("Rainbow Energy");
        rainbow.setSupertype("Energy");
        rainbow.setTipo("Grass");
        activo.getEnergiasUnidas().add(rainbow);
        tableroJugador.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardPokemon("xy1-2", "Charmander", "50"));
        tableroJugador.getBanca().add(suplente);

        ComandoRetirarse cmd = new ComandoRetirarse(suplente.getCard().getId(), tableroJugador);
        cmd.ejecutar(partida);

        assertEquals("", rainbow.getTipo());
        assertTrue(tableroJugador.getPilaDescarte().contains(rainbow));
    }

    // =================== ComandoUnirEnergia Adicionales ===================

    @Test
    void unirEnergia_ejecutar_nulos_lanzaExcepcion() {
        Card energia = cardEnergia("Fire Energy");
        CartaEnJuego objetivo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));

        assertThrows(IllegalArgumentException.class, 
                () -> new ComandoUnirEnergia(null, energia, tableroJugador, null).ejecutar(partida));

        assertThrows(IllegalArgumentException.class, 
                () -> new ComandoUnirEnergia(objetivo, null, tableroJugador, null).ejecutar(partida));
    }

    @Test
    void unirEnergia_ejecutar_rainbowEnergy_aplicaDanioYAsignaTipo() {
        Card rainbow = new Card();
        rainbow.setId("rainbow-1");
        rainbow.setNombre("Rainbow Energy");
        rainbow.setSupertype("Energy");
        tableroJugador.getMano().add(rainbow);

        CartaEnJuego objetivo = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        objetivo.setHpActual(60);

        ComandoUnirEnergia cmd = new ComandoUnirEnergia(objetivo, rainbow, tableroJugador, "Fire");
        cmd.ejecutar(partida);

        assertEquals("Fire", rainbow.getTipo());
        assertEquals(50, objetivo.getHpActual());
        assertTrue(objetivo.getEnergiasUnidas().contains(rainbow));
        assertFalse(tableroJugador.getMano().contains(rainbow));
    }

    // =================== ComandoEvolucionar Adicionales ===================

    @Test
    void evolucionar_ejecutar_nulos_lanzaExcepcion() {
        Card ivysaur = cardPokemonEvolucion("xy1-2", "Ivysaur", "90", "Bulbasaur");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));

        assertThrows(IllegalArgumentException.class,
                () -> new ComandoEvolucionar(null, bulbasaur, tableroJugador).ejecutar(partida));

        assertThrows(IllegalArgumentException.class,
                () -> new ComandoEvolucionar(ivysaur, null, tableroJugador).ejecutar(partida));
    }

    @Test
    void evolucionar_ejecutar_primerTurno_lanzaExcepcion() {
        Card ivysaur = cardPokemonEvolucion("xy1-2", "Ivysaur", "90", "Bulbasaur");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        tableroJugador.setTurnosJugados(1);

        assertThrows(IllegalStateException.class,
                () -> new ComandoEvolucionar(ivysaur, bulbasaur, tableroJugador).ejecutar(partida));
    }

    @Test
    void evolucionar_ejecutar_mismoTurnoEntrada_lanzaExcepcion() {
        Card ivysaur = cardPokemonEvolucion("xy1-2", "Ivysaur", "90", "Bulbasaur");
        CartaEnJuego bulbasaur = new CartaEnJuego(cardPokemon("xy1-1", "Bulbasaur", "60"));
        bulbasaur.setTurnoEntrada(partida.getNumeroTurno());
        tableroJugador.setTurnosJugados(2);

        assertThrows(IllegalStateException.class,
                () -> new ComandoEvolucionar(ivysaur, bulbasaur, tableroJugador).ejecutar(partida));
    }
}

