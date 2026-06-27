package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BattleEngineServiceResolverExtendedTest {

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
        c.setSubtypes(List.of("Basic"));
        c.setHp("60");
        return c;
    }

    private Card cardEnergia(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Energy");
        return c;
    }

    private Partida crearPartidaConPendiente(String tipo, int minSel, int maxSel) {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoEsperandoInteraccion());

        PendingBattleAction pending = new PendingBattleAction();
        pending.setActor("ash");
        pending.setType(tipo);
        pending.setMinSelections(minSel);
        pending.setMaxSelections(maxSel);
        partida.setPendingAction(pending);

        return partida;
    }

    // =================== DISCARD_TO_TOP_DECK ===================

    @Test
    void resolver_discardToTopDeck_mueveCartaAlTopDelMazo() {
        Partida partida = crearPartidaConPendiente("DISCARD_TO_TOP_DECK", 0, 2);
        Card pikachu = cardBasico("p1", "Pikachu");
        Card bulba = cardBasico("p2", "Bulbasaur");
        partida.getJugador().getPilaDescarte().add(pikachu);
        partida.getJugador().getPilaDescarte().add(bulba);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(
                new PendingBattleAction.Option("p1", "Pikachu", null),
                new PendingBattleAction.Option("p2", "Bulbasaur", null)
        ));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals(1, partida.getJugador().getMazo().size());
        assertEquals("p1", partida.getJugador().getMazo().get(0).getId());
        assertEquals(1, partida.getJugador().getPilaDescarte().size()); // bulba still in discard
    }

    // =================== REORDER_TOP_DECK ===================

    @Test
    void resolver_reorderTopDeck_reordenaCartas() {
        Partida partida = crearPartidaConPendiente("REORDER_TOP_DECK", 0, 3);
        Card c0 = cardBasico("c0", "Pikachu");
        Card c1 = cardBasico("c1", "Charmander");
        Card c2 = cardBasico("c2", "Squirtle");
        partida.getJugador().getMazo().add(c0);
        partida.getJugador().getMazo().add(c1);
        partida.getJugador().getMazo().add(c2);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(
                new PendingBattleAction.Option("0", "Pikachu", null),
                new PendingBattleAction.Option("1", "Charmander", null),
                new PendingBattleAction.Option("2", "Squirtle", null)
        ));
        service.partidasEnCurso.put(partida.getId(), partida);

        // Reorder: want Squirtle(2) on top, then Pikachu(0), then Charmander(1)
        service.resolverAccionPendiente(partida.getId(), "ash", List.of("2", "0", "1"));

        assertEquals("c2", partida.getJugador().getMazo().get(0).getId()); // Squirtle on top
        assertEquals("c0", partida.getJugador().getMazo().get(1).getId());
        assertEquals("c1", partida.getJugador().getMazo().get(2).getId());
    }

    // =================== MOVE_ENERGY_TO_OPPONENT_BENCH ===================

    @Test
    void resolver_moveEnergyToOpponentBench_mueveEnergiaDelActivoRival() {
        Partida partida = crearPartidaConPendiente("MOVE_ENERGY_TO_OPPONENT_BENCH", 1, 1);

        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b0", "Charizard"));
        Card fire = cardEnergia("e1", "Fire Energy");
        botActivo.getEnergiasUnidas().add(fire);
        partida.getBot().setActivo(botActivo);

        CartaEnJuego botBanca = new CartaEnJuego(cardBasico("b1", "Charmander"));
        partida.getBot().getBanca().add(botBanca);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("b1", "Charmander", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("b1"));

        assertTrue(partida.getBot().getActivo().getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getBot().getBanca().get(0).getEnergiasUnidas().size());
    }

    @Test
    void resolver_moveEnergyToOpponentBench_sinEnergiaEnActivo_lanzaExcepcion() {
        Partida partida = crearPartidaConPendiente("MOVE_ENERGY_TO_OPPONENT_BENCH", 1, 1);
        partida.getBot().setActivo(new CartaEnJuego(cardBasico("b0", "Squirtle"))); // no energy
        CartaEnJuego botBanca = new CartaEnJuego(cardBasico("b1", "Charmander"));
        partida.getBot().getBanca().add(botBanca);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("b1", "Charmander", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "ash", List.of("b1")));
    }

    // =================== CHOOSE_OPPONENT_BENCH_TO_DAMAGE ===================

    @Test
    void resolver_chooseOpponentBenchToDamage_danaAlBancado() {
        Partida partida = crearPartidaConPendiente("CHOOSE_OPPONENT_BENCH_TO_DAMAGE", 0, 1);
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p0", "Pikachu")));

        CartaEnJuego botBanca = new CartaEnJuego(cardBasico("b1", "Charmander"));
        botBanca.setHpActual(60);
        partida.getBot().getBanca().add(botBanca);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setAmount(30);
        pending.setOptions(List.of(new PendingBattleAction.Option("b1", "Charmander", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("b1"));

        assertEquals(30, partida.getBot().getBanca().get(0).getHpActual());
    }

    // =================== DISCARD_POTION_ENERGY ===================

    @Test
    void resolver_discardPotionEnergy_descartaEnergiaDelPokemon() {
        Partida partida = crearPartidaConPendiente("DISCARD_POTION_ENERGY", 1, 1);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        Card e1 = cardEnergia("e1", "Lightning Energy");
        Card e2 = cardEnergia("e2", "Fire Energy");
        activo.getEnergiasUnidas().add(e1);
        activo.getEnergiasUnidas().add(e2);
        partida.getJugador().setActivo(activo);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setDestination("POTION_ENERGY_TARGET:p1");
        pending.setOptions(List.of(
                new PendingBattleAction.Option("e1", "Lightning Energy", null),
                new PendingBattleAction.Option("e2", "Fire Energy", null)
        ));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e1"));

        assertEquals(1, partida.getJugador().getActivo().getEnergiasUnidas().size());
        assertEquals("e2", partida.getJugador().getActivo().getEnergiasUnidas().get(0).getId());
        assertEquals(1, partida.getJugador().getPilaDescarte().size());
    }

    // =================== SEARCH_DECK → ATTACH_ACTIVE ===================

    @Test
    void resolver_searchDeckAttachActive_adjuntaEnergiaAlActivo() {
        Partida partida = crearPartidaConPendiente("SEARCH_DECK", 0, 1);
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        partida.getJugador().setActivo(activo);

        Card energia = cardEnergia("e1", "Lightning Energy");
        partida.getJugador().getMazo().add(energia);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setDestination("ATTACH_ACTIVE");
        pending.setOptions(List.of(new PendingBattleAction.Option("e1", "Lightning Energy", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e1"));

        assertEquals(1, partida.getJugador().getActivo().getEnergiasUnidas().size());
        assertTrue(partida.getJugador().getMazo().isEmpty());
        assertTrue(partida.getJugador().getMano().isEmpty()); // went to active, not hand
    }

    // =================== SEARCH_DECK → ATTACH_ACTIVE_AND_SWITCH con banca ===================

    @Test
    void resolver_searchDeckAttachActiveAndSwitch_conBanca_setPendingSwitchAction() {
        Partida partida = crearPartidaConPendiente("SEARCH_DECK", 0, 1);
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        partida.getJugador().setActivo(activo);
        CartaEnJuego banca = new CartaEnJuego(cardBasico("b1", "Bulbasaur"));
        partida.getJugador().getBanca().add(banca);

        Card energia = cardEnergia("e1", "Lightning Energy");
        partida.getJugador().getMazo().add(energia);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setDestination("ATTACH_ACTIVE_AND_SWITCH");
        pending.setOptions(List.of(new PendingBattleAction.Option("e1", "Lightning Energy", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e1"));

        // Should set a new SWITCH_ACTIVE pending action
        assertNotNull(partida.getPendingAction());
        assertEquals("SWITCH_ACTIVE", partida.getPendingAction().getType());
    }

    // =================== SEARCH_DECK → vacío (0 cartas seleccionadas) ===================

    @Test
    void resolver_searchDeckSinSeleccion_noAgregaCartas() {
        Partida partida = crearPartidaConPendiente("SEARCH_DECK", 0, 1);
        partida.getJugador().getMazo().add(cardEnergia("e1", "Lightning Energy"));

        PendingBattleAction pending = partida.getPendingAction();
        pending.setDestination("HAND");
        pending.setOptions(List.of(new PendingBattleAction.Option("e1", "Lightning Energy", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of());

        assertTrue(partida.getJugador().getMano().isEmpty());
        assertEquals(1, partida.getJugador().getMazo().size()); // shuffled but stays
    }

    // =================== endsTurn flag ===================

    @Test
    void resolver_conEndsTurnTrue_pasaTurnoAlFinalizar() {
        Partida partida = crearPartidaConPendiente("DISCARD_TO_TOP_DECK", 0, 1);
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        Card pikachu = cardBasico("p2", "Bulbasaur");
        partida.getJugador().getPilaDescarte().add(pikachu);
        partida.getJugador().getMazo().add(cardBasico("m1", "Geodude"));

        PendingBattleAction pending = partida.getPendingAction();
        pending.setEndsTurn(true);
        pending.setOptions(List.of(new PendingBattleAction.Option("p2", "Bulbasaur", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p2"));

        // Turn should have been passed (turnoActual switches to BOT)
        assertEquals(Partida.Turno.BOT, partida.getTurnoActual());
    }
}
