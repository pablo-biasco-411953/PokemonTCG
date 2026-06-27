package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;
import com.pokemon.tcg.model.battle.state.EstadoTurnoNormal;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BattleEngineServiceResolverBranchTest {

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
        c.setSubtypes(java.util.List.of("Basic"));
        c.setHp("0");
        return c;
    }

    private Card cardTrainer(String id, String nombre, String subtype) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Trainer");
        if (subtype != null) c.setSubtypes(java.util.List.of(subtype));
        return c;
    }

    private Partida crearPartidaConInteraccion(String actor) {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.transicionarA(new EstadoEsperandoInteraccion());
        return partida;
    }

    private PendingBattleAction pending(String type, int min, int max, List<PendingBattleAction.Option> opts) {
        PendingBattleAction p = new PendingBattleAction();
        p.setActor("ash");
        p.setType(type);
        p.setMinSelections(min);
        p.setMaxSelections(max);
        p.setOptions(opts);
        return p;
    }

    // =================== Error cases ===================

    @Test
    void resolverAccion_sinPendiente_lanzaExcepcion() {
        Partida partida = crearPartidaConInteraccion("ash");
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "ash", List.of()));
    }

    @Test
    void resolverAccion_actorIncorrecto_lanzaExcepcion() {
        Partida partida = crearPartidaConInteraccion("ash");
        partida.setPendingAction(pending("HEAL_OWN_POKEMON", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "otro", List.of("p1")));
    }

    @Test
    void resolverAccion_cantidadInvalida_lanzaExcepcion() {
        Partida partida = crearPartidaConInteraccion("ash");
        partida.setPendingAction(pending("HEAL_OWN_POKEMON", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "ash", List.of()));
    }

    @Test
    void resolverAccion_cartaNoPermitida_lanzaExcepcion() {
        Partida partida = crearPartidaConInteraccion("ash");
        partida.setPendingAction(pending("HEAL_OWN_POKEMON", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "ash", List.of("carta-ilegal")));
    }

    // =================== CASSIUS - activo ===================

    @Test
    void resolverAccion_cassiusConActivo_barajaAlMazo() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.getEnergiasUnidas().add(cardEnergia("e1", "Grass Energy"));
        partida.getJugador().setActivo(activo);

        partida.setPendingAction(pending("SELECT_POKEMON_CASSIUS", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertNull(partida.getJugador().getActivo());
        assertTrue(partida.getJugador().getMazo().size() >= 2); // pokemon + energy
    }

    // =================== CASSIUS - banca ===================

    @Test
    void resolverAccion_cassiusConBanca_barajaAlMazo() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("activo", "Charmander"));
        partida.getJugador().setActivo(activo);

        CartaEnJuego bancado = new CartaEnJuego(cardBasico("p2", "Squirtle"));
        partida.getJugador().getBanca().add(bancado);

        partida.setPendingAction(pending("SELECT_POKEMON_CASSIUS", 1, 1,
                List.of(new PendingBattleAction.Option("p2", "Squirtle", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p2"));

        assertTrue(partida.getJugador().getBanca().isEmpty());
        assertTrue(partida.getJugador().getMazo().contains(bancado.getCard()));
    }

    // =================== ATTACH_TOOL ===================

    @Test
    void resolverAccion_attachTool_adjuntaHerramientaAlActivo() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        partida.getJugador().setActivo(activo);

        Card tool = cardTrainer("xy1-119", "Hard Charm", "Pokémon Tool");
        partida.getJugador().getMano().add(tool);

        partida.setPendingAction(pending("ATTACH_TOOL", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertFalse(activo.getAttachedTools().isEmpty());
        assertEquals("Hard Charm", activo.getAttachedTools().get(0).getNombre());
    }

    // =================== DISCARD_RECOVERY (Max Revive) ===================

    @Test
    void resolverAccion_discardRecovery_ponePokemonArriba() {
        Partida partida = crearPartidaConInteraccion("ash");
        Card pokemon = cardBasico("p1", "Pikachu");
        partida.getJugador().getPilaDescarte().add(pokemon);

        partida.setPendingAction(pending("DISCARD_RECOVERY", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertFalse(partida.getJugador().getMazo().isEmpty());
        assertEquals("p1", partida.getJugador().getMazo().get(0).getId());
        assertFalse(partida.getJugador().getPilaDescarte().contains(pokemon));
    }

    // =================== SELECT_POKEMON_SUPER_POTION - 1 energía ===================

    @Test
    void resolverAccion_superPotion1Energia_curaYDescartaEnergia() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(40);
        activo.getEnergiasUnidas().add(cardEnergia("e1", "Grass Energy"));
        partida.getJugador().setActivo(activo);

        partida.setPendingAction(pending("SELECT_POKEMON_SUPER_POTION", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals(60, activo.getHpActual());
        assertTrue(activo.getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getJugador().getPilaDescarte().size());
    }

    // =================== SELECT_POKEMON_SUPER_POTION - 2+ energías → nuevo pending ===================

    @Test
    void resolverAccion_superPotion2Energias_creaEnergyDiscardAction() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(40);
        activo.getEnergiasUnidas().add(cardEnergia("e1", "Grass Energy"));
        activo.getEnergiasUnidas().add(cardEnergia("e2", "Fire Energy"));
        partida.getJugador().setActivo(activo);

        partida.setPendingAction(pending("SELECT_POKEMON_SUPER_POTION", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals("DISCARD_POTION_ENERGY", partida.getPendingAction().getType());
    }

    // =================== DISCARD_POTION_ENERGY ===================

    @Test
    void resolverAccion_discardPotionEnergy_eliminaEnergiaEspecifica() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.getEnergiasUnidas().add(cardEnergia("e1", "Grass Energy"));
        activo.getEnergiasUnidas().add(cardEnergia("e2", "Fire Energy"));
        partida.getJugador().setActivo(activo);

        PendingBattleAction p = pending("DISCARD_POTION_ENERGY", 1, 1,
                List.of(
                        new PendingBattleAction.Option("e1", "Grass Energy", null),
                        new PendingBattleAction.Option("e2", "Fire Energy", null)
                ));
        p.setDestination("POTION_ENERGY_TARGET:p1");
        partida.setPendingAction(p);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e1"));

        assertEquals(1, activo.getEnergiasUnidas().size());
        assertEquals("e2", activo.getEnergiasUnidas().get(0).getId());
        assertEquals(1, partida.getJugador().getPilaDescarte().size());
    }

    // =================== SWITCH_ACTIVE ===================

    @Test
    void resolverAccion_switchActive_intercambiaActivoYBanca() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        CartaEnJuego bancado = new CartaEnJuego(cardBasico("p2", "Charmander"));
        partida.getJugador().setActivo(activo);
        partida.getJugador().getBanca().add(bancado);

        partida.setPendingAction(pending("SWITCH_ACTIVE", 1, 1,
                List.of(new PendingBattleAction.Option("p2", "Charmander", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p2"));

        assertEquals("Charmander", partida.getJugador().getActivo().getCard().getNombre());
        assertEquals(1, partida.getJugador().getBanca().size());
        assertEquals("Pikachu", partida.getJugador().getBanca().get(0).getCard().getNombre());
    }

    // =================== HEAL_OWN_POKEMON ===================

    @Test
    void resolverAccion_healOwnActivo_curaHPAActivo() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(30);
        partida.getJugador().setActivo(activo);

        PendingBattleAction p = pending("HEAL_OWN_POKEMON", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Pikachu", null)));
        p.setAmount(20);
        partida.setPendingAction(p);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals(50, activo.getHpActual());
    }

    @Test
    void resolverAccion_healOwnBancado_curaHPABanca() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("activo", "Bulbasaur"));
        activo.setHpActual(60);
        partida.getJugador().setActivo(activo);

        CartaEnJuego bancado = new CartaEnJuego(cardBasico("p2", "Squirtle"));
        bancado.setHpActual(20);
        partida.getJugador().getBanca().add(bancado);

        PendingBattleAction p = pending("HEAL_OWN_POKEMON", 1, 1,
                List.of(new PendingBattleAction.Option("p2", "Squirtle", null)));
        p.setAmount(30);
        partida.setPendingAction(p);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p2"));

        assertEquals(50, bancado.getHpActual());
    }

    // =================== CHOOSE_OPPONENT_BENCH_TO_ACTIVE ===================

    @Test
    void resolverAccion_chooseOpponentBenchToActive_forzaCambioRival() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego myActivo = new CartaEnJuego(cardBasico("my", "Pikachu"));
        partida.getJugador().setActivo(myActivo);

        CartaEnJuego rivalActivo = new CartaEnJuego(cardBasico("ra", "Charmander"));
        CartaEnJuego rivalBanca = new CartaEnJuego(cardBasico("rb", "Squirtle"));
        partida.getBot().setActivo(rivalActivo);
        partida.getBot().getBanca().add(rivalBanca);

        partida.setPendingAction(pending("CHOOSE_OPPONENT_BENCH_TO_ACTIVE", 1, 1,
                List.of(new PendingBattleAction.Option("rb", "Squirtle", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("rb"));

        assertEquals("Squirtle", partida.getBot().getActivo().getCard().getNombre());
        assertEquals(1, partida.getBot().getBanca().size());
        assertEquals("Charmander", partida.getBot().getBanca().get(0).getCard().getNombre());
    }

    // =================== CHOOSE_OPPONENT_BENCH_TO_DAMAGE ===================

    @Test
    void resolverAccion_chooseOpponentBenchToDamage_daniaRivalBanca() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego myActivo = new CartaEnJuego(cardBasico("my", "Pikachu"));
        partida.getJugador().setActivo(myActivo);

        CartaEnJuego rivalActivo = new CartaEnJuego(cardBasico("ra", "Charmander"));
        CartaEnJuego rivalBanca = new CartaEnJuego(cardBasico("rb", "Squirtle"));
        rivalBanca.setHpActual(60);
        partida.getBot().setActivo(rivalActivo);
        partida.getBot().getBanca().add(rivalBanca);

        PendingBattleAction p = pending("CHOOSE_OPPONENT_BENCH_TO_DAMAGE", 1, 1,
                List.of(new PendingBattleAction.Option("rb", "Squirtle", null)));
        p.setAmount(30);
        partida.setPendingAction(p);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("rb"));

        assertEquals(30, rivalBanca.getHpActual());
    }

    // =================== DISCARD_OPPONENT_ACTIVE_ENERGY ===================

    @Test
    void resolverAccion_discardOpponentActiveEnergy_eliminaEnergia() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego myActivo = new CartaEnJuego(cardBasico("my", "Pikachu"));
        partida.getJugador().setActivo(myActivo);

        CartaEnJuego rivalActivo = new CartaEnJuego(cardBasico("ra", "Charmander"));
        Card energia = cardEnergia("e1", "Fire Energy");
        rivalActivo.getEnergiasUnidas().add(energia);
        partida.getBot().setActivo(rivalActivo);

        partida.setPendingAction(pending("DISCARD_OPPONENT_ACTIVE_ENERGY", 1, 1,
                List.of(new PendingBattleAction.Option("e1", "Fire Energy", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e1"));

        assertTrue(rivalActivo.getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getBot().getPilaDescarte().size());
    }

    // =================== DISCARD_TO_TOP_DECK ===================

    @Test
    void resolverAccion_discardToTopDeck_ponePrimeraCarta() {
        Partida partida = crearPartidaConInteraccion("ash");
        Card card = cardBasico("c1", "Pikachu");
        partida.getJugador().getPilaDescarte().add(card);

        partida.setPendingAction(pending("DISCARD_TO_TOP_DECK", 1, 1,
                List.of(new PendingBattleAction.Option("c1", "Pikachu", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("c1"));

        assertEquals("c1", partida.getJugador().getMazo().get(0).getId());
        assertFalse(partida.getJugador().getPilaDescarte().contains(card));
    }

    // =================== SELECT_DISCARD_ITEMS_FOR_PICKUP ===================

    @Test
    void resolverAccion_pickupItems_mueveAMano() {
        Partida partida = crearPartidaConInteraccion("ash");
        Card item1 = cardTrainer("i1", "Potion", "Item");
        Card item2 = cardTrainer("i2", "Repel", "Item");
        partida.getJugador().getPilaDescarte().add(item1);
        partida.getJugador().getPilaDescarte().add(item2);

        partida.setPendingAction(pending("SELECT_DISCARD_ITEMS_FOR_PICKUP", 0, 2,
                List.of(
                        new PendingBattleAction.Option("i1", "Potion", null),
                        new PendingBattleAction.Option("i2", "Repel", null)
                )));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("i1", "i2"));

        assertEquals(2, partida.getJugador().getMano().size());
        assertTrue(partida.getJugador().getPilaDescarte().isEmpty());
    }

    @Test
    void resolverAccion_pickupItemsVacio_agregarCancelLog() {
        Partida partida = crearPartidaConInteraccion("ash");
        partida.setPendingAction(pending("SELECT_DISCARD_ITEMS_FOR_PICKUP", 0, 2, List.of()));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of());

        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("PICKUP_CANCELLED")));
    }

    // =================== else branch (SEARCH_DECK → hand) ===================

    @Test
    void resolverAccion_searchDeckToHand_mueveCartasAMano() {
        Partida partida = crearPartidaConInteraccion("ash");
        Card card = cardBasico("m1", "Pikachu");
        partida.getJugador().getMazo().add(card);

        PendingBattleAction p = pending("SEARCH_DECK", 0, 1,
                List.of(new PendingBattleAction.Option("m1", "Pikachu", null)));
        p.setDestination("HAND");
        partida.setPendingAction(p);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("m1"));

        assertEquals(1, partida.getJugador().getMano().size());
        assertFalse(partida.getJugador().getMazo().contains(card));
    }

    // =================== SELECT_POKEMON_EVOSODA → SEARCH_EVOLUTION ===================

    @Test
    void resolverAccion_evosoda_transicionaASearchEvolution() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Bulbasaur"));
        partida.getJugador().setActivo(activo);

        Card ivysaur = new Card();
        ivysaur.setId("evo1");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setEvolvesFrom("Bulbasaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setHp("80");
        partida.getJugador().getMazo().add(ivysaur);

        partida.setPendingAction(pending("SELECT_POKEMON_EVOSODA", 1, 1,
                List.of(new PendingBattleAction.Option("p1", "Bulbasaur", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals("SEARCH_EVOLUTION", resultado.getPendingAction().getType());
    }

    // =================== SEARCH_EVOLUTION cancelled ===================

    @Test
    void resolverAccion_searchEvolutionCancelled_barajaRival() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Bulbasaur"));
        partida.getJugador().setActivo(activo);

        PendingBattleAction p = pending("SEARCH_EVOLUTION", 0, 1, List.of());
        p.setDestination("EVOLVE_TARGET:p1");
        partida.setPendingAction(p);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of());

        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.contains("DECK_SEARCH_CANCELLED")));
    }

    // =================== DISCARD_OPPONENT_ENERGY ===================

    @Test
    void resolverAccion_discardOpponentEnergy_discardaEnergiaDePokemon() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego myActivo = new CartaEnJuego(cardBasico("my", "Pikachu"));
        partida.getJugador().setActivo(myActivo);

        CartaEnJuego rivalActivo = new CartaEnJuego(cardBasico("ra", "Charmander"));
        Card energia = cardEnergia("e1", "Fire Energy");
        rivalActivo.getEnergiasUnidas().add(energia);
        partida.getBot().setActivo(rivalActivo);

        partida.setPendingAction(pending("DISCARD_OPPONENT_ENERGY", 1, 1,
                List.of(new PendingBattleAction.Option("ra", "Charmander", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("ra"));

        assertTrue(rivalActivo.getEnergiasUnidas().isEmpty());
    }

    // =================== MOVE_ENERGY_TO_OPPONENT_BENCH ===================

    @Test
    void resolverAccion_moveEnergyToOpponentBench_mueveEnergia() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego myActivo = new CartaEnJuego(cardBasico("my", "Pikachu"));
        partida.getJugador().setActivo(myActivo);

        CartaEnJuego rivalActivo = new CartaEnJuego(cardBasico("ra", "Charmander"));
        Card energia = cardEnergia("e1", "Fire Energy");
        rivalActivo.getEnergiasUnidas().add(energia);
        CartaEnJuego rivalBanca = new CartaEnJuego(cardBasico("rb", "Squirtle"));
        partida.getBot().setActivo(rivalActivo);
        partida.getBot().getBanca().add(rivalBanca);

        partida.setPendingAction(pending("MOVE_ENERGY_TO_OPPONENT_BENCH", 1, 1,
                List.of(new PendingBattleAction.Option("rb", "Squirtle", null))));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("rb"));

        assertTrue(rivalActivo.getEnergiasUnidas().isEmpty());
        assertEquals(1, rivalBanca.getEnergiasUnidas().size());
    }

    // =================== SEARCH_DECK with ATTACH_ACTIVE ===================

    @Test
    void resolverAccion_searchDeckAttachActive_adjuntaEnergiaAlActivo() {
        Partida partida = crearPartidaConInteraccion("ash");
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        partida.getJugador().setActivo(activo);

        Card energia = cardEnergia("e1", "Lightning Energy");
        partida.getJugador().getMazo().add(energia);

        PendingBattleAction p = pending("SEARCH_DECK_ENERGY", 0, 1,
                List.of(new PendingBattleAction.Option("e1", "Lightning Energy", null)));
        p.setDestination("ATTACH_ACTIVE");
        partida.setPendingAction(p);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e1"));

        assertEquals(1, activo.getEnergiasUnidas().size());
        assertEquals("Lightning Energy", activo.getEnergiasUnidas().get(0).getNombre());
    }
}
