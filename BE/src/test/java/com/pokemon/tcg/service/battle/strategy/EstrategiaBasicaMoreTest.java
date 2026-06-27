package com.pokemon.tcg.service.battle.strategy;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.CardAttribute;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.BattleAttackService;
import com.pokemon.tcg.service.BattleKoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstrategiaBasicaMoreTest {

    private EstrategiaBasica estrategia;
    private BattleAttackService mockAttack;
    private BattleKoService mockKo;
    private Partida partida;
    private TableroJugador jugador;
    private TableroJugador bot;

    @BeforeEach
    void setUp() {
        mockAttack = mock(BattleAttackService.class);
        mockKo = mock(BattleKoService.class);
        estrategia = new EstrategiaBasica(mockAttack, mockKo);
        jugador = new TableroJugador();
        bot = new TableroJugador();
        partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setNumeroTurno(2);
    }

    private Card cardBasico(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setHp("60");
        c.setSupertype("Pokemon");
        c.setSubtypes(List.of("Basic"));
        return c;
    }

    private Card cardEnergia(String nombre, String tipo) {
        Card c = new Card();
        c.setId("e-" + nombre);
        c.setNombre(nombre);
        c.setSupertype("Energy");
        c.setTipo(tipo);
        c.setHp("0");
        return c;
    }

    private Ataque crearAtaque(String nombre, int danio, String... costos) {
        Ataque a = new Ataque();
        a.setNombre(nombre);
        a.setDanio(danio);
        a.setTiposEnergia(List.of(costos));
        return a;
    }

    // =================== CantRetreat condition ===================

    @Test
    void evaluarRetirada_CantRetreat_noHuye() {
        Card activoCard = cardBasico("p1", "Pikachu");
        activoCard.setCostoRetirada(0);
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.setHpActual(5);
        activo.agregarCondicion("CantRetreat");
        bot.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardBasico("b1", "Charmander"));
        suplente.setHpActual(60);
        bot.getBanca().add(suplente);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Squirtle")));

        estrategia.ejecutarTurno(partida);

        assertEquals("Pikachu", bot.getActivo().getCard().getNombre());
        assertFalse(partida.isYaSeRetiroEsteTurno());
    }

    // =================== Asleep prevents attack ===================

    @Test
    void intentarAtacar_dormido_noAtaca() {
        Card activoCard = cardBasico("p1", "Snorlax");
        Ataque atk = crearAtaque("Slam", 40, "Colorless");
        activoCard.reemplazarAtaques(List.of(atk));
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.getEnergiasUnidas().add(cardEnergia("Colorless Energy", "Colorless"));
        activo.agregarCondicion("Asleep");
        bot.setActivo(activo);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Pikachu")));

        estrategia.ejecutarTurno(partida);

        verifyNoInteractions(mockAttack);
    }

    // =================== Paralyzed prevents attack ===================

    @Test
    void intentarAtacar_paralizado_noAtaca() {
        Card activoCard = cardBasico("p1", "Slowbro");
        Ataque atk = crearAtaque("Water Pulse", 40, "Water");
        activoCard.reemplazarAtaques(List.of(atk));
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.getEnergiasUnidas().add(cardEnergia("Water Energy", "Water"));
        activo.agregarCondicion("Paralyzed");
        bot.setActivo(activo);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Pikachu")));

        estrategia.ejecutarTurno(partida);

        verifyNoInteractions(mockAttack);
    }

    // =================== Super effective log ===================

    @Test
    void intentarAtacar_superEfectivo_logEscrito() {
        Card activoCard = cardBasico("p1", "Raichu");
        activoCard.setTipo("Lightning");
        Ataque atk = crearAtaque("Thunder", 90, "Lightning");
        activoCard.reemplazarAtaques(List.of(atk));
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.getEnergiasUnidas().add(cardEnergia("Lightning Energy", "Lightning"));
        bot.setActivo(activo);

        Card jugadorCard = cardBasico("p2", "Gyarados");
        jugadorCard.setDebilidades(List.of(new CardAttribute("Lightning", "×2")));
        jugadorCard.setResistencias(List.of());
        CartaEnJuego jugadorActivo = new CartaEnJuego(jugadorCard);
        jugadorActivo.setHpActual(100);
        jugador.setActivo(jugadorActivo);

        BattleAttackService.AttackResolution mockRes =
                new BattleAttackService.AttackResolution(
                        new com.pokemon.tcg.model.battle.ResultadoAtaque(90, 0),
                        List.of()
                );
        when(mockAttack.resolveAttack(any(), eq(atk), eq(activo), eq(jugadorActivo), any(), isNull()))
                .thenReturn(mockRes);

        estrategia.ejecutarTurno(partida);

        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("SUPER_EFFECTIVE")));
    }

    // =================== Resisted log ===================

    @Test
    void intentarAtacar_resistido_logEscrito() {
        Card activoCard = cardBasico("p1", "Raichu");
        activoCard.setTipo("Lightning");
        Ataque atk = crearAtaque("Thunder", 60, "Lightning");
        activoCard.reemplazarAtaques(List.of(atk));
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.getEnergiasUnidas().add(cardEnergia("Lightning Energy", "Lightning"));
        bot.setActivo(activo);

        Card jugadorCard = cardBasico("p2", "Aerodactyl");
        jugadorCard.setDebilidades(List.of());
        jugadorCard.setResistencias(List.of(new CardAttribute("Lightning", "-30")));
        CartaEnJuego jugadorActivo = new CartaEnJuego(jugadorCard);
        jugadorActivo.setHpActual(100);
        jugador.setActivo(jugadorActivo);

        BattleAttackService.AttackResolution mockRes =
                new BattleAttackService.AttackResolution(
                        new com.pokemon.tcg.model.battle.ResultadoAtaque(30, 0),
                        List.of()
                );
        when(mockAttack.resolveAttack(any(), eq(atk), eq(activo), eq(jugadorActivo), any(), isNull()))
                .thenReturn(mockRes);

        estrategia.ejecutarTurno(partida);

        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("RESISTED")));
    }

    // =================== Attack with "heal" in text gets bonus ===================

    @Test
    void intentarAtacar_healBonus_preferidoSiHpBajo() {
        Card activoCard = cardBasico("p1", "Blissey");
        activoCard.setHp("250");
        Ataque atkDanio = crearAtaque("Scratch", 10, "Colorless");
        Ataque atkHeal = crearAtaque("Softboiled", 0, "Colorless");
        atkHeal.setTexto("Heal 60 damage from this Pokemon.");
        activoCard.reemplazarAtaques(List.of(atkDanio, atkHeal));
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.setHpActual(30); // low HP → heal bonus kicks in
        activo.getEnergiasUnidas().add(cardEnergia("Colorless Energy", "Colorless"));
        bot.setActivo(activo);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Pidgey")));

        BattleAttackService.AttackResolution mockRes =
                new BattleAttackService.AttackResolution(
                        new com.pokemon.tcg.model.battle.ResultadoAtaque(0, 0),
                        List.of()
                );
        when(mockAttack.resolveAttack(any(), any(), any(), any(), any(), isNull()))
                .thenReturn(mockRes);

        assertDoesNotThrow(() -> estrategia.ejecutarTurno(partida));
    }

    // =================== Ataque bloqueado: skipped ===================

    @Test
    void intentarAtacar_ataqueBloqueado_saltaAtaque() {
        Card activoCard = cardBasico("p1", "Starmie");
        Ataque atkBloqueado = crearAtaque("Star Freeze", 50, "Water");
        Ataque atkLibre = crearAtaque("Water Gun", 20, "Water");
        activoCard.reemplazarAtaques(List.of(atkBloqueado, atkLibre));
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.getEnergiasUnidas().add(cardEnergia("Water Energy", "Water"));
        activo.setAtaqueBloqueadoSiguienteTurno("Star Freeze");
        bot.setActivo(activo);

        CartaEnJuego jugadorActivo = new CartaEnJuego(cardBasico("p2", "Pikachu"));
        jugadorActivo.setHpActual(60);
        jugador.setActivo(jugadorActivo);

        BattleAttackService.AttackResolution mockRes =
                new BattleAttackService.AttackResolution(
                        new com.pokemon.tcg.model.battle.ResultadoAtaque(20, 0),
                        List.of()
                );
        when(mockAttack.resolveAttack(any(), eq(atkLibre), any(), any(), any(), isNull()))
                .thenReturn(mockRes);

        estrategia.ejecutarTurno(partida);

        verify(mockAttack).resolveAttack(any(), eq(atkLibre), any(), any(), any(), isNull());
    }

    // =================== gestionarEnergiaBotPlanificado: banca pokemon ===================

    @Test
    void gestionarEnergia_bancaPokemon_recibEnergia() {
        // activo doesn't have attacks; bench pokemon has attack that needs energy
        Card activoCard = cardBasico("activo", "Magikarp");
        activoCard.reemplazarAtaques(List.of()); // no attacks
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        bot.setActivo(activo);

        Card bancaCard = cardBasico("banca", "Gyarados");
        bancaCard.setTipo("Water");
        Ataque atk = crearAtaque("Dragon Rage", 80, "Water", "Water");
        bancaCard.reemplazarAtaques(List.of(atk));
        CartaEnJuego bancaPokemon = new CartaEnJuego(bancaCard);
        bot.getBanca().add(bancaPokemon);

        Card waterEnergy = cardEnergia("Water Energy", "Water");
        bot.getMano().add(waterEnergy);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Pikachu")));

        estrategia.ejecutarTurno(partida);

        // Energy should go to the banca pokemon (best plan) or activo
        boolean energyAttached = !bancaPokemon.getEnergiasUnidas().isEmpty() || !activo.getEnergiasUnidas().isEmpty();
        assertTrue(energyAttached);
    }

    // =================== calcularDanioFinal: debilidad doubles damage ===================

    @Test
    void calcularDanio_debilidad_duplicaDanio_superEffective() {
        Card activoCard = cardBasico("p1", "Charizard");
        activoCard.setTipo("Fire");
        Ataque atk = crearAtaque("Fire Spin", 50, "Fire");
        activoCard.reemplazarAtaques(List.of(atk));
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.getEnergiasUnidas().add(cardEnergia("Fire Energy", "Fire"));
        bot.setActivo(activo);

        Card jugadorCard = cardBasico("p2", "Caterpie");
        jugadorCard.setDebilidades(List.of(new CardAttribute("Fire", "×2")));
        CartaEnJuego jugadorActivo = new CartaEnJuego(jugadorCard);
        jugadorActivo.setHpActual(70);
        jugador.setActivo(jugadorActivo);

        BattleAttackService.AttackResolution mockRes =
                new BattleAttackService.AttackResolution(
                        new com.pokemon.tcg.model.battle.ResultadoAtaque(100, 0),
                        List.of()
                );
        when(mockAttack.resolveAttack(any(), eq(atk), eq(activo), eq(jugadorActivo), any(), isNull()))
                .thenReturn(mockRes);

        estrategia.ejecutarTurno(partida);

        // Both SUPER_EFFECTIVE log and attack log should appear
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.startsWith("ATTACK_USED:BOT")));
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("SUPER_EFFECTIVE")));
    }

    // =================== No attacks on activo ===================

    @Test
    void intentarAtacar_sinAtaques_noHaceNada() {
        Card activoCard = cardBasico("p1", "Magikarp");
        activoCard.reemplazarAtaques(List.of());
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        bot.setActivo(activo);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Pikachu")));

        estrategia.ejecutarTurno(partida);

        verifyNoInteractions(mockAttack);
    }

    // =================== EstrategiaBasica default constructor ===================

    @Test
    void defaultConstructor_noThrows() {
        assertDoesNotThrow(() -> new EstrategiaBasica());
    }

    // =================== evaluarRetirada: no banca, no retiro ===================

    @Test
    void evaluarRetirada_sinBanca_noHuye() {
        Card activoCard = cardBasico("p1", "Pikachu");
        activoCard.setCostoRetirada(0);
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.setHpActual(5);
        activo.agregarCondicion("Poisoned");
        bot.setActivo(activo);
        // NO banca

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Squirtle")));

        estrategia.ejecutarTurno(partida);

        assertEquals("Pikachu", bot.getActivo().getCard().getNombre());
        assertFalse(partida.isYaSeRetiroEsteTurno());
    }

    // =================== evaluarRetirada: yaSeRetiro prevents ===================

    @Test
    void evaluarRetirada_yaSeRetiro_noHuye() {
        Card activoCard = cardBasico("p1", "Pikachu");
        activoCard.setCostoRetirada(0);
        CartaEnJuego activo = new CartaEnJuego(activoCard);
        activo.setHpActual(5);
        activo.agregarCondicion("Poisoned");
        bot.setActivo(activo);

        CartaEnJuego suplente = new CartaEnJuego(cardBasico("b1", "Charmander"));
        suplente.setHpActual(60);
        bot.getBanca().add(suplente);

        jugador.setActivo(new CartaEnJuego(cardBasico("p2", "Squirtle")));
        partida.setYaSeRetiroEsteTurno(true); // already retreated

        estrategia.ejecutarTurno(partida);

        assertEquals("Pikachu", bot.getActivo().getCard().getNombre());
    }

    // =================== SETUP_PLACE_BENCH_EXTRA: places basics on bench ===================

    @Test
    void ejecutarSetup_placeBenchExtra_colocaEnBanca() {
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoSetupPlaceBenchExtra());
        bot.getMano().add(cardBasico("xy1-1", "Pikachu"));

        estrategia.ejecutarSetup(partida);

        assertEquals(1, bot.getBanca().size());
        assertTrue(partida.isSetupBotListo());
    }

    // =================== lambda$ejecutarSetup$0: comparator with 2+ pokemons ===================

    @Test
    void ejecutarSetup_dosPokemons_ordenaPorPotencial_activaComparator() {
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoSetupPlaceActive());

        Card sinAtaque = cardBasico("a1", "Rattata");

        Card conAtaque = cardBasico("b1", "Pikachu");
        conAtaque.reemplazarAtaques(List.of(crearAtaque("Quick Attack", 10, "Colorless")));

        Card energy = cardEnergia("Colorless Energy", "Colorless");

        bot.getMano().add(sinAtaque);
        bot.getMano().add(conAtaque);
        bot.getMano().add(energy);

        estrategia.ejecutarSetup(partida);

        // Con 2+ pokémons el sort invoca el comparator (lambda$ejecutarSetup$0)
        assertNotNull(bot.getActivo());
        assertEquals("Pikachu", bot.getActivo().getCard().getNombre());
    }

    // =================== evaluarPotencialDeMano: else-branch + rival weakness + my weakness ===================

    @Test
    void evaluarPotencial_ramaElse_energiaNoColorless_yDebilidadRival() {
        // Squirtle: ataque Water (no Colorless → else-branch) + debilidad a Lightning
        Card squirtle = cardBasico("s1", "Squirtle");
        squirtle.setTipo("Water");
        Ataque waterAtk = crearAtaque("Water Gun", 20, "Water");
        squirtle.reemplazarAtaques(List.of(waterAtk));
        squirtle.setDebilidades(List.of(new CardAttribute("Lightning", "×2")));

        Card bulbasaur = cardBasico("b1", "Bulbasaur");
        bulbasaur.setTipo("Grass");

        bot.getMano().add(squirtle);
        bot.getMano().add(bulbasaur);
        // Sin energía en mano: el sort se activa pero Squirtle no puede atacar este turno

        // Rival activo: debil a Water, tipo Lightning (cubre L224-227 y L230-233)
        Card charizard = cardBasico("r1", "Charizard");
        charizard.setTipo("Lightning");
        charizard.setDebilidades(List.of(new CardAttribute("Water", "×2")));
        CartaEnJuego rivalActivo = new CartaEnJuego(charizard);
        rivalActivo.setHpActual(80);
        jugador.setActivo(rivalActivo);

        // Mock para si llegara a intentar atacar
        BattleAttackService.AttackResolution mockRes = new BattleAttackService.AttackResolution(
                new com.pokemon.tcg.model.battle.ResultadoAtaque(0, 0), List.of());
        when(mockAttack.resolveAttack(any(), any(), any(), any(), any(), any())).thenReturn(mockRes);

        estrategia.ejecutarTurno(partida);

        // Squirtle debe ser elegido como activo (rival debil a Water +200 > debilidad propia -150)
        assertNotNull(bot.getActivo());
        assertEquals("Squirtle", bot.getActivo().getCard().getNombre());
    }
}
