package com.pokemon.tcg.service.battle.chain;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.BattleAttackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.mockito.ArgumentMatchers.any;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChainHandlersTest {

    private BattleAttackService.KoResolver koResolver;
    private Partida partida;
    private CartaEnJuego atacante;
    private CartaEnJuego defensor;

    @BeforeEach
    void setUp() {
        koResolver = mock(BattleAttackService.KoResolver.class);

        Card cardAtacante = new Card();
        cardAtacante.setId("xy1-1");
        cardAtacante.setNombre("Bulbasaur");
        cardAtacante.setHp("60");

        Card cardDefensor = new Card();
        cardDefensor.setId("xy1-2");
        cardDefensor.setNombre("Charmander");
        cardDefensor.setHp("50");

        atacante = new CartaEnJuego(cardAtacante);
        defensor = new CartaEnJuego(cardDefensor);

        TableroJugador tableroJugador = new TableroJugador();
        tableroJugador.setActivo(atacante);
        tableroJugador.setBanca(new ArrayList<>());
        tableroJugador.setMano(new ArrayList<>());
        tableroJugador.setMazo(new ArrayList<>());
        tableroJugador.setPilaDescarte(new ArrayList<>());
        tableroJugador.setPremios(new ArrayList<>());

        TableroJugador tableroBot = new TableroJugador();
        tableroBot.setActivo(defensor);
        tableroBot.setBanca(new ArrayList<>());
        tableroBot.setMano(new ArrayList<>());
        tableroBot.setMazo(new ArrayList<>());
        tableroBot.setPilaDescarte(new ArrayList<>());
        tableroBot.setPremios(new ArrayList<>());

        partida = new Partida(tableroJugador, tableroBot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("BOT");
    }

    private ContextoAtaque ctx(String textoAtaque, int danio, Random random) {
        Ataque ataque = new Ataque();
        ataque.setNombre("Test");
        ataque.setDanio(danio);
        ataque.setTexto(textoAtaque);
        return new ContextoAtaque(partida, ataque, atacante, defensor, koResolver, random);
    }

    private Random siempre(boolean cara) {
        Random r = mock(Random.class);
        when(r.nextBoolean()).thenReturn(cara);
        return r;
    }

    // =================== EfectoVeneno ===================

    @Test
    void veneno_textoContiene_envenenaDef() {
        ContextoAtaque c = ctx("the defending pokemon is now poisoned", 30, new Random());
        new EfectoVeneno().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Poisoned"));
    }

    @Test
    void veneno_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoVeneno().procesar(c);
        assertFalse(defensor.getCondicionesEspeciales().contains("Poisoned"));
    }

    // =================== EfectoQuemadura ===================

    @Test
    void quemadura_textoContiene_quemaDef() {
        ContextoAtaque c = ctx("the defending pokemon is now burned", 20, new Random());
        new EfectoQuemadura().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Burned"));
    }

    @Test
    void quemadura_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 20 damage", 20, new Random());
        new EfectoQuemadura().procesar(c);
        assertFalse(defensor.getCondicionesEspeciales().contains("Burned"));
    }

    // =================== EfectoParalisis ===================

    @Test
    void paralisis_textoSinMoneda_paraliza() {
        ContextoAtaque c = ctx("the defending is now paralyzed", 30, new Random());
        new EfectoParalisis().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Paralyzed"));
    }

    @Test
    void paralisis_conMonedaCara_paraliza() {
        ContextoAtaque c = ctx("flip a coin. if heads, the defending is now paralyzed", 30, siempre(true));
        new EfectoParalisis().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Paralyzed"));
    }

    @Test
    void paralisis_conMonedaCruz_noParaliza() {
        ContextoAtaque c = ctx("flip a coin. if heads, the defending is now paralyzed", 30, siempre(false));
        new EfectoParalisis().procesar(c);
        assertFalse(defensor.getCondicionesEspeciales().contains("Paralyzed"));
    }

    @Test
    void paralisis_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 40 damage", 40, new Random());
        new EfectoParalisis().procesar(c);
        assertFalse(defensor.getCondicionesEspeciales().contains("Paralyzed"));
    }

    // =================== EfectoSueno ===================

    @Test
    void sueno_textoSinMoneda_duerme() {
        ContextoAtaque c = ctx("the defending is now asleep", 20, new Random());
        new EfectoSueno().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Asleep"));
    }

    @Test
    void sueno_conMonedaCara_duerme() {
        ContextoAtaque c = ctx("flip a coin. if heads, the defending is now asleep", 20, siempre(true));
        new EfectoSueno().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Asleep"));
    }

    @Test
    void sueno_conMonedaCruz_noDuerme() {
        ContextoAtaque c = ctx("flip a coin. if heads, the defending is now asleep", 20, siempre(false));
        new EfectoSueno().procesar(c);
        assertFalse(defensor.getCondicionesEspeciales().contains("Asleep"));
    }

    // =================== EfectoConfusion ===================

    @Test
    void confusion_textoSinMoneda_confunde() {
        ContextoAtaque c = ctx("the defending is now confused", 30, new Random());
        new EfectoConfusion().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Confused"));
    }

    @Test
    void confusion_conMonedaCara_confunde() {
        ContextoAtaque c = ctx("flip a coin. if heads, the defending is now confused", 30, siempre(true));
        new EfectoConfusion().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("Confused"));
    }

    @Test
    void confusion_conMonedaCruz_noConfunde() {
        ContextoAtaque c = ctx("flip a coin. if heads, the defending is now confused", 30, siempre(false));
        new EfectoConfusion().procesar(c);
        assertFalse(defensor.getCondicionesEspeciales().contains("Confused"));
    }

    // =================== EfectoInmunidad ===================

    @Test
    void inmunidad_textoSinMoneda_activaEscudo() {
        ContextoAtaque c = ctx("prevent all effects of attacks, including damage done to this pokemon", 0, new Random());
        new EfectoInmunidad().procesar(c);
        assertTrue(atacante.isInvulnerable());
    }

    @Test
    void inmunidad_conMonedaCara_activaEscudo() {
        ContextoAtaque c = ctx("flip a coin. if heads, prevent all effects of attacks, including damage done to this", 0, siempre(true));
        new EfectoInmunidad().procesar(c);
        assertTrue(atacante.isInvulnerable());
    }

    @Test
    void inmunidad_conMonedaCruz_noActivaEscudo() {
        ContextoAtaque c = ctx("flip a coin. if heads, prevent all effects of attacks, including damage done to this", 0, siempre(false));
        new EfectoInmunidad().procesar(c);
        assertFalse(atacante.isInvulnerable());
    }

    @Test
    void inmunidad_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoInmunidad().procesar(c);
        assertFalse(atacante.isInvulnerable());
    }

    // =================== EfectoMonedaFalla ===================

    @Test
    void monedaFalla_cara_noAnulaAtaque() {
        ContextoAtaque c = ctx("tails, this attack does nothing", 30, siempre(true));
        new EfectoMonedaFalla().procesar(c);
        assertFalse(c.ataqueAnulado);
        assertEquals(30, c.danioFinal);
    }

    @Test
    void monedaFalla_cruz_anulaAtaque() {
        ContextoAtaque c = ctx("tails, this attack does nothing", 30, siempre(false));
        new EfectoMonedaFalla().procesar(c);
        assertTrue(c.ataqueAnulado);
        assertEquals(0, c.danioFinal);
    }

    @Test
    void monedaFalla_thatAttack_anulaConCruz() {
        ContextoAtaque c = ctx("tails, that attack does nothing", 20, siempre(false));
        new EfectoMonedaFalla().procesar(c);
        assertTrue(c.ataqueAnulado);
    }

    @Test
    void monedaFalla_ataqueYaAnulado_noSeProcesa() {
        ContextoAtaque c = ctx("tails, this attack does nothing", 30, siempre(false));
        c.ataqueAnulado = true;
        new EfectoMonedaFalla().procesar(c);
        assertEquals(30, c.danioFinal);
    }

    // =================== EfectoMonedaExtraDanio ===================

    @Test
    void monedaExtraDanio_cara_duplicaDanio() {
        ContextoAtaque c = ctx("if heads, this attack does 20 more damage", 20, siempre(true));
        new EfectoMonedaExtraDanio().procesar(c);
        assertEquals(40, c.danioFinal);
    }

    @Test
    void monedaExtraDanio_cruz_noCambiaDanio() {
        ContextoAtaque c = ctx("if heads, this attack does 20 more damage", 20, siempre(false));
        new EfectoMonedaExtraDanio().procesar(c);
        assertEquals(20, c.danioFinal);
    }

    @Test
    void monedaExtraDanio_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoMonedaExtraDanio().procesar(c);
        assertEquals(30, c.danioFinal);
    }

    // =================== EfectoDanioPropio ===================

    @Test
    void danioPropio_textoContiene_reducirHpAtacante() {
        atacante.setHpActual(60);
        ContextoAtaque c = ctx("put 1 damage counter on this pokemon damage to itself", 20, new Random());
        new EfectoDanioPropio().procesar(c);
        assertTrue(atacante.getHpActual() < 60);
    }

    @Test
    void danioPropio_textoNoContiene_noHaceNada() {
        atacante.setHpActual(60);
        ContextoAtaque c = ctx("deal 30 damage to the defending pokemon", 30, new Random());
        new EfectoDanioPropio().procesar(c);
        assertEquals(60, atacante.getHpActual());
    }

    @Test
    void danioPropio_knockOut_llamaKoResolver() {
        atacante.setHpActual(5);
        ContextoAtaque c = ctx("this pokemon does 10 damage to itself", 20, new Random());
        new EfectoDanioPropio().procesar(c);
        assertEquals(0, atacante.getHpActual());
        verify(koResolver).resolve(any(), any(), any());
    }

    // =================== EfectoRobarCartas ===================

    @Test
    void robarCartas_draw1_agregaAMano() {
        Card cartaMazo = new Card();
        cartaMazo.setId("xy1-10");
        partida.getJugador().getMazo().add(cartaMazo);

        ContextoAtaque c = ctx("draw 1 card", 0, new Random());
        new EfectoRobarCartas().procesar(c);

        assertEquals(1, partida.getJugador().getMano().size());
        assertEquals(0, partida.getJugador().getMazo().size());
    }

    @Test
    void robarCartas_draw2_agregaDosAMano() {
        for (int i = 0; i < 3; i++) {
            Card carta = new Card();
            carta.setId("xy1-" + i);
            partida.getJugador().getMazo().add(carta);
        }

        ContextoAtaque c = ctx("draw 2 cards", 0, new Random());
        new EfectoRobarCartas().procesar(c);

        assertEquals(2, partida.getJugador().getMano().size());
    }

    @Test
    void robarCartas_mazoVacio_noFalla() {
        ContextoAtaque c = ctx("draw a card", 0, new Random());
        assertDoesNotThrow(() -> new EfectoRobarCartas().procesar(c));
    }

    // =================== EfectoCuracion ===================

    @Test
    void curacion_cura20Hp() {
        atacante.setHpActual(30);
        ContextoAtaque c = ctx("heal 20 damage from this pokemon", 0, new Random());
        new EfectoCuracion().procesar(c);
        assertEquals(50, atacante.getHpActual());
    }

    @Test
    void curacion_noPasaMaxHp() {
        atacante.setHpActual(55);
        ContextoAtaque c = ctx("heal 20 damage from this pokemon", 0, new Random());
        new EfectoCuracion().procesar(c);
        assertEquals(60, atacante.getHpActual());
    }

    @Test
    void curacion_textoNoContiene_noHaceNada() {
        atacante.setHpActual(30);
        ContextoAtaque c = ctx("deal 20 damage", 20, new Random());
        new EfectoCuracion().procesar(c);
        assertEquals(30, atacante.getHpActual());
    }

    // =================== ManejadorEfecto chaining ===================

    @Test
    void chain_cadenaDeEfectos_ambosSeEjecutan() {
        ContextoAtaque c = ctx("the defending is now poisoned. is now burned.", 20, new Random());
        EfectoVeneno veneno = new EfectoVeneno();
        EfectoQuemadura quemadura = new EfectoQuemadura();
        veneno.encadenar(quemadura);

        veneno.procesar(c);

        assertTrue(defensor.getCondicionesEspeciales().contains("Poisoned"));
        assertTrue(defensor.getCondicionesEspeciales().contains("Burned"));
    }

    @Test
    void chain_ataqueAnulado_siguientesNoEjecutan() {
        ContextoAtaque c = ctx("tails, this attack does nothing. the defending is now poisoned.", 20, siempre(false));
        c.ataqueAnulado = true;

        new EfectoVeneno().procesar(c);

        assertFalse(defensor.getCondicionesEspeciales().contains("Poisoned"));
    }

    // =================== EfectoAtrapar ===================

    @Test
    void atrapar_textoContiene_cantRetreat() {
        ContextoAtaque c = ctx("can't retreat during your opponent's next turn", 20, new Random());
        new EfectoAtrapar().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("CantRetreat"));
    }

    @Test
    void atrapar_cannotRetreat_activa() {
        ContextoAtaque c = ctx("cannot retreat this turn", 10, new Random());
        new EfectoAtrapar().procesar(c);
        assertTrue(defensor.getCondicionesEspeciales().contains("CantRetreat"));
    }

    @Test
    void atrapar_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoAtrapar().procesar(c);
        assertFalse(defensor.getCondicionesEspeciales().contains("CantRetreat"));
    }

    // =================== EfectoContadoresDanio ===================

    @Test
    void contadoresDanio_conContadores_escalaDanio() {
        atacante.setHpActual(30); // 60 max - 30 actual = 30 faltante = 3 contadores
        ContextoAtaque c = ctx("this attack does 10 more damage for each damage counter on this pokemon", 10, new Random());
        new EfectoContadoresDanio().procesar(c);
        assertEquals(40, c.danioFinal); // 10 + 3*10
    }

    @Test
    void contadoresDanio_sinContadores_noCambiaDanio() {
        atacante.setHpActual(60); // hp lleno = 0 contadores
        ContextoAtaque c = ctx("damage counter on this pokemon", 10, new Random());
        new EfectoContadoresDanio().procesar(c);
        assertEquals(10, c.danioFinal);
    }

    @Test
    void contadoresDanio_textoNoContiene_noHaceNada() {
        atacante.setHpActual(30);
        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoContadoresDanio().procesar(c);
        assertEquals(30, c.danioFinal);
    }

    // =================== EfectoDanioBanca ===================

    @Test
    void danioBanca_conPokemonEnBanca_reducirHp() {
        Card cardBanca = new Card();
        cardBanca.setId("xy1-5");
        cardBanca.setNombre("Squirtle");
        cardBanca.setHp("40");
        CartaEnJuego bancaPokemon = new CartaEnJuego(cardBanca);

        // atacante is player's active, defensor is bot's active → rival's bench = bot's bench
        partida.getBot().getBanca().add(bancaPokemon);
        ContextoAtaque c = ctx("does 10 damage to 1 of your opponent's benched pokemon", 30, new Random());
        new EfectoDanioBanca().procesar(c);
        assertTrue(bancaPokemon.getHpActual() < 40);
    }

    @Test
    void danioBanca_bancaVacia_noFalla() {
        ContextoAtaque c = ctx("does 10 damage to 1 of your opponent's benched pokemon", 20, new Random());
        assertDoesNotThrow(() -> new EfectoDanioBanca().procesar(c));
    }

    @Test
    void danioBanca_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        assertDoesNotThrow(() -> new EfectoDanioBanca().procesar(c));
    }

    // =================== EfectoDescartarEnergiaPropia ===================

    @Test
    void descartarEnergiaPropia_conEnergia_descartaUna() {
        Card energia = new Card();
        energia.setId("energy-1");
        energia.setNombre("Fire Energy");
        atacante.getEnergiasUnidas().add(energia);

        ContextoAtaque c = ctx("discard an energy card attached to this pokemon", 20, new Random());
        new EfectoDescartarEnergiaPropia().procesar(c);

        assertTrue(atacante.getEnergiasUnidas().isEmpty());
    }

    @Test
    void descartarEnergiaPropia_sinEnergia_noFalla() {
        ContextoAtaque c = ctx("discard an energy card attached to this pokemon", 20, new Random());
        assertDoesNotThrow(() -> new EfectoDescartarEnergiaPropia().procesar(c));
    }

    @Test
    void descartarEnergiaPropia_textoNoContiene_noHaceNada() {
        Card energia = new Card();
        energia.setId("energy-1");
        atacante.getEnergiasUnidas().add(energia);

        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoDescartarEnergiaPropia().procesar(c);

        assertEquals(1, atacante.getEnergiasUnidas().size());
    }

    // =================== EfectoDescartarEnergiaRival ===================

    @Test
    void descartarEnergiaRival_conEnergia_descartaDelDefensor() {
        Card energia = new Card();
        energia.setId("energy-2");
        energia.setNombre("Water Energy");
        defensor.getEnergiasUnidas().add(energia);

        ContextoAtaque c = ctx("discard an energy from the defending pokemon", 20, new Random());
        new EfectoDescartarEnergiaRival().procesar(c);

        assertTrue(defensor.getEnergiasUnidas().isEmpty());
    }

    @Test
    void descartarEnergiaRival_textoNoContiene_noHaceNada() {
        Card energia = new Card();
        energia.setId("energy-2");
        defensor.getEnergiasUnidas().add(energia);

        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoDescartarEnergiaRival().procesar(c);

        assertEquals(1, defensor.getEnergiasUnidas().size());
    }

    // =================== EfectoEscalaPorEnergias ===================

    @Test
    void escalaPorEnergias_conEnergias_aumentaDanio() {
        Card e1 = new Card(); e1.setId("e1");
        Card e2 = new Card(); e2.setId("e2");
        atacante.getEnergiasUnidas().add(e1);
        atacante.getEnergiasUnidas().add(e2);

        ContextoAtaque c = ctx("does 10 more damage for each energy attached to this pokemon", 20, new Random());
        new EfectoEscalaPorEnergias().procesar(c);

        assertEquals(40, c.danioFinal); // 20 + 2*10
    }

    @Test
    void escalaPorEnergias_sinEnergias_noCambiaDanio() {
        ContextoAtaque c = ctx("does 10 more damage for each energy attached to this pokemon", 20, new Random());
        new EfectoEscalaPorEnergias().procesar(c);
        assertEquals(20, c.danioFinal);
    }

    @Test
    void escalaPorEnergias_textoNoContiene_noHaceNada() {
        Card e1 = new Card(); e1.setId("e1");
        atacante.getEnergiasUnidas().add(e1);

        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoEscalaPorEnergias().procesar(c);
        assertEquals(30, c.danioFinal);
    }

    // =================== EfectoMultiMoneda ===================

    @Test
    void multiMoneda_2monedas2caras_dasMasimo() {
        Random allHeads = mock(Random.class);
        when(allHeads.nextBoolean()).thenReturn(true);

        ContextoAtaque c = ctx("flip 2 coins. does 20 damage times the number of heads", 20, allHeads);
        new EfectoMultiMoneda().procesar(c);

        assertEquals(40, c.danioFinal); // 20 * 2 caras
    }

    @Test
    void multiMoneda_todasCruz_danio0() {
        Random allTails = mock(Random.class);
        when(allTails.nextBoolean()).thenReturn(false);

        ContextoAtaque c = ctx("flip 2 coins. does 20 damage times the number of heads", 20, allTails);
        new EfectoMultiMoneda().procesar(c);

        assertEquals(0, c.danioFinal); // 20 * 0 caras
    }

    @Test
    void multiMoneda_textoNoContiene_noHaceNada() {
        ContextoAtaque c = ctx("deal 30 damage", 30, new Random());
        new EfectoMultiMoneda().procesar(c);
        assertEquals(30, c.danioFinal);
    }

    // =================== CadenaAtaqueFactory ===================

    @Test
    void factory_buildCadenaPreDanio_retornaNoNull() {
        ManejadorEfecto cadena = CadenaAtaqueFactory.buildCadenaPreDanio();
        assertNotNull(cadena);
    }

    @Test
    void factory_buildCadenaEfectosSecundarios_retornaNoNull() {
        ManejadorEfecto cadena = CadenaAtaqueFactory.buildCadenaEfectosSecundarios();
        assertNotNull(cadena);
    }

    @Test
    void factory_preDanio_procesa_sinExcepcion() {
        ManejadorEfecto cadena = CadenaAtaqueFactory.buildCadenaPreDanio();
        ContextoAtaque c = ctx("deal 20 damage", 20, new Random());
        assertDoesNotThrow(() -> cadena.procesar(c));
    }

    @Test
    void factory_efectosSecundarios_procesa_sinExcepcion() {
        ManejadorEfecto cadena = CadenaAtaqueFactory.buildCadenaEfectosSecundarios();
        ContextoAtaque c = ctx("deal 20 damage", 20, new Random());
        assertDoesNotThrow(() -> cadena.procesar(c));
    }
}
