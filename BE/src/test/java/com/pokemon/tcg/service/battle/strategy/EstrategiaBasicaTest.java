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
        c.setSupertype("Pokémon");
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

    @Test
    void ejecutarTurno_retirada_activoDormidoOParalizado_noHuye() {
        Card activoCard = cardBasico("p1", "Bulbasaur");
        activoCard.setCostoRetirada(0);
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.setHpActual(5);
        activo.agregarCondicion("Asleep");
        bot.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardBasico("b1", "Charmander"));
        suplente.setHpActual(60);
        bot.getBanca().add(suplente);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Pikachu")));
        partida.setNumeroTurno(2);

        estrategia.ejecutarTurno(partida);

        assertEquals("Bulbasaur", bot.getActivo().getCard().getNombre());
        assertFalse(partida.isYaSeRetiroEsteTurno());
    }

    @Test
    void ejecutarTurno_retirada_peligroDeMuerte_conFairyGarden_huyeGratis() {
        Card activoCard = cardBasico("p1", "Xerneas");
        activoCard.setCostoRetirada(3); // alto costo
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.setHpActual(5); // en peligro
        activo.getEnergiasUnidas().add(cardEnergia("Fairy Energy")); // tiene fairy
        bot.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardBasico("b1", "Pikachu"));
        suplente.setHpActual(60);
        bot.getBanca().add(suplente);

        Card charmanderCard = cardBasico("p2", "Charmander");
        com.pokemon.tcg.model.battle.Ataque scratch = new com.pokemon.tcg.model.battle.Ataque();
        scratch.setNombre("Scratch");
        scratch.setDanio(10);
        charmanderCard.reemplazarAtaques(java.util.List.of(scratch));
        jugador.setActivo(new CartaEnJuego(charmanderCard));
        partida.setNumeroTurno(2);
        
        Card fairyGarden = new Card();
        fairyGarden.setId("xy1-117");
        fairyGarden.setNombre("Fairy Garden");
        partida.setActiveStadium(fairyGarden);

        estrategia.ejecutarTurno(partida);

        assertEquals("Pikachu", bot.getActivo().getCard().getNombre());
        assertTrue(partida.isYaSeRetiroEsteTurno());
        assertEquals(0, bot.getPilaDescarte().size()); // Huyo gratis
    }
    
    @Test
    void ejecutarTurno_gestionarCartasEnMano_analisisPotencial() {
        // Testea lineas 204-237: Evaluar debilidad y resistencia
        CartaEnJuego activoJugador = new CartaEnJuego(cardBasico("p2", "Squirtle"));
        activoJugador.getCard().setTipo("Water");
        com.pokemon.tcg.model.CardAttribute weakToLightning = new com.pokemon.tcg.model.CardAttribute();
        weakToLightning.setType("Lightning");
        activoJugador.getCard().setDebilidades(java.util.List.of(weakToLightning));
        jugador.setActivo(activoJugador);
        
        Card charmander = cardBasico("b1", "Charmander");
        charmander.setTipo("Fire");
        com.pokemon.tcg.model.CardAttribute weakToWater = new com.pokemon.tcg.model.CardAttribute();
        weakToWater.setType("Water");
        charmander.setDebilidades(java.util.List.of(weakToWater));
        
        Card pikachu = cardBasico("b2", "Pikachu");
        pikachu.setTipo("Lightning");
        
        bot.getMano().add(charmander);
        bot.getMano().add(pikachu);
        
        partida.setNumeroTurno(2);

        estrategia.ejecutarTurno(partida);

        // Pikachu deberia ser elegido como Activo porque Squirtle es debil a Lightning (potencial +200)
        // Y Charmander es debil a Water (potencial -150)
        assertNotNull(bot.getActivo());
        assertEquals("Pikachu", bot.getActivo().getCard().getNombre());
        assertEquals(1, bot.getBanca().size());
        assertEquals("Charmander", bot.getBanca().get(0).getCard().getNombre());
    }

    @Test
    void ejecutarTurno_gestionarEnergiaBotPlanificado_asignaMejorOpcion() {
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        com.pokemon.tcg.model.battle.Ataque ataque = new com.pokemon.tcg.model.battle.Ataque();
        ataque.setNombre("Thunder Shock");
        ataque.setTiposEnergia(java.util.List.of("Lightning", "Colorless"));
        ataque.setDanio(20);
        activo.getCard().reemplazarAtaques(java.util.List.of(ataque));
        bot.setActivo(activo);
        
        Card lightning = cardEnergia("Lightning Energy");
        bot.getMano().add(lightning);
        
        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Squirtle")));
        partida.setNumeroTurno(2);
        
        estrategia.ejecutarTurno(partida);
        
        assertEquals(1, activo.getEnergiasUnidas().size());
        assertEquals("Lightning Energy", activo.getEnergiasUnidas().get(0).getNombre());
        assertTrue(bot.getMano().isEmpty());
    }

    @Test
    void ejecutarTurno_intentarAtacar_instaKill_ataca() {
        com.pokemon.tcg.service.BattleAttackService mockAttack = org.mockito.Mockito.mock(com.pokemon.tcg.service.BattleAttackService.class);
        com.pokemon.tcg.service.BattleKoService mockKo = org.mockito.Mockito.mock(com.pokemon.tcg.service.BattleKoService.class);
        estrategia = new EstrategiaBasica(mockAttack, mockKo);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Charizard"));
        activo.getCard().setTipo("Fire");
        com.pokemon.tcg.model.battle.Ataque atk = new com.pokemon.tcg.model.battle.Ataque();
        atk.setNombre("Fire Spin");
        atk.setTiposEnergia(java.util.List.of("Fire"));
        atk.setDanio(100);
        activo.getCard().reemplazarAtaques(java.util.List.of(atk));
        activo.getEnergiasUnidas().add(cardEnergia("Fire Energy"));
        bot.setActivo(activo);
        
        CartaEnJuego activoJugador = new CartaEnJuego(cardBasico("p2", "Caterpie"));
        activoJugador.setHpActual(50);
        jugador.setActivo(activoJugador);
        
        partida.setNumeroTurno(2);
        
        com.pokemon.tcg.service.BattleAttackService.AttackResolution mockRes = 
            new com.pokemon.tcg.service.BattleAttackService.AttackResolution(
                new com.pokemon.tcg.model.battle.ResultadoAtaque(100, 0),
                java.util.List.of()
            );
        
        org.mockito.Mockito.when(mockAttack.resolveAttack(
            org.mockito.ArgumentMatchers.eq(partida),
            org.mockito.ArgumentMatchers.eq(atk),
            org.mockito.ArgumentMatchers.eq(activo),
            org.mockito.ArgumentMatchers.eq(activoJugador),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(mockRes);
        
        estrategia.ejecutarTurno(partida);
        
        org.mockito.Mockito.verify(mockAttack).resolveAttack(
            org.mockito.ArgumentMatchers.eq(partida),
            org.mockito.ArgumentMatchers.eq(atk),
            org.mockito.ArgumentMatchers.eq(activo),
            org.mockito.ArgumentMatchers.eq(activoJugador),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.isNull()
        );
    }
    
    @Test
    void normalizarTipoTest() throws Exception {
        // Acceder a metodos privados a traves de reflection para probar todas las ramas
        java.lang.reflect.Method method = com.pokemon.tcg.service.battle.strategy.EstrategiaBasica.class.getDeclaredMethod("normalizarTipo", String.class);
        method.setAccessible(true);
        
        assertEquals("Grass", method.invoke(estrategia, "Grass"));
        assertEquals("Grass", method.invoke(estrategia, "planta"));
        assertEquals("Fire", method.invoke(estrategia, "fuego"));
        assertEquals("Water", method.invoke(estrategia, "agua"));
        assertEquals("Lightning", method.invoke(estrategia, "eléctrica"));
        assertEquals("Psychic", method.invoke(estrategia, "psíquica"));
        assertEquals("Fighting", method.invoke(estrategia, "lucha"));
        assertEquals("Darkness", method.invoke(estrategia, "siniestra"));
        assertEquals("Metal", method.invoke(estrategia, "acero"));
        assertEquals("Colorless", method.invoke(estrategia, "incolora"));
        assertEquals("", method.invoke(estrategia, (String) null));
    }
    
    @Test
    void esPokemonBasicoTest() throws Exception {
        java.lang.reflect.Method method = com.pokemon.tcg.service.battle.strategy.EstrategiaBasica.class.getDeclaredMethod("esPokemonBasico", Card.class);
        method.setAccessible(true);
        
        Card nulo = null;
        assertFalse((Boolean) method.invoke(estrategia, nulo));
        
        Card energia = cardEnergia("Fire Energy");
        assertFalse((Boolean) method.invoke(estrategia, energia));
        
        Card evo = cardBasico("e1", "Charmeleon");
        evo.setEvolvesFrom("Charmander");
        assertFalse((Boolean) method.invoke(estrategia, evo));
        
        Card stage = cardBasico("s1", "Charizard");
        stage.setSubtypes(java.util.List.of("Stage 2"));
        assertFalse((Boolean) method.invoke(estrategia, stage));
        
        Card basico = cardBasico("b1", "Charmander");
        assertTrue((Boolean) method.invoke(estrategia, basico));

        Card trainer = new Card();
        trainer.setId("t1");
        trainer.setNombre("Great Ball");
        trainer.setSupertype("Trainer");
        assertFalse((Boolean) method.invoke(estrategia, trainer));
    }
}
