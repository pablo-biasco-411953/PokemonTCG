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

class BattleEngineServiceResolverTest {

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

    // =================== Error paths ===================

    @Test
    void resolverAccionPendiente_sinPendiente_lanzaExcepcion() {
        Partida partida = new Partida(new TableroJugador(), new TableroJugador());
        partida.setJugadorUsername("ash");
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "ash", List.of()));
    }

    @Test
    void resolverAccionPendiente_actorIncorrecto_lanzaExcepcion() {
        Partida partida = crearPartidaConPendiente("SEARCH_DECK", 0, 1);
        partida.getPendingAction().setOptions(List.of());
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalStateException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "misty", List.of()));
    }

    @Test
    void resolverAccionPendiente_seleccionInvalida_lanzaExcepcion() {
        Partida partida = crearPartidaConPendiente("SEARCH_DECK", 1, 2);
        partida.getPendingAction().setOptions(List.of());
        service.partidasEnCurso.put(partida.getId(), partida);

        // 0 selections but min is 1
        assertThrows(IllegalArgumentException.class,
                () -> service.resolverAccionPendiente(partida.getId(), "ash", List.of()));
    }

    // =================== SELECT_POKEMON_CASSIUS ===================

    @Test
    void resolverAccionPendiente_cassius_activoPokemon_barajaEnMazo() {
        Partida partida = crearPartidaConPendiente("SELECT_POKEMON_CASSIUS", 1, 1);
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        partida.getJugador().setActivo(activo);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("p1", "Pikachu", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertNull(partida.getJugador().getActivo());
        assertEquals(1, partida.getJugador().getMazo().size());
    }

    // =================== DISCARD_RECOVERY (Max Revive) ===================

    @Test
    void resolverAccionPendiente_discardRecovery_moveCartaAlTopDelMazo() {
        Partida partida = crearPartidaConPendiente("DISCARD_RECOVERY", 1, 1);
        Card pikachu = cardBasico("p1", "Pikachu");
        partida.getJugador().getPilaDescarte().add(pikachu);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("p1", "Pikachu", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals(1, partida.getJugador().getMazo().size());
        assertEquals("p1", partida.getJugador().getMazo().get(0).getId());
        assertTrue(partida.getJugador().getPilaDescarte().isEmpty());
    }

    // =================== SELECT_POKEMON_SUPER_POTION ===================

    @Test
    void resolverAccionPendiente_superPotion_curaYDescartaEnergia() {
        Partida partida = crearPartidaConPendiente("SELECT_POKEMON_SUPER_POTION", 1, 1);
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(30);
        Card energia = cardEnergia("e1", "Lightning Energy");
        activo.getEnergiasUnidas().add(energia);
        partida.getJugador().setActivo(activo);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("p1", "Pikachu", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals(60, partida.getJugador().getActivo().getHpActual()); // capped at max
        assertTrue(partida.getJugador().getActivo().getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getJugador().getPilaDescarte().size());
    }

    // =================== DISCARD_OPPONENT_ACTIVE_ENERGY ===================

    @Test
    void resolverAccionPendiente_discardOpponentEnergy_descartaEnergiaDelRival() {
        Partida partida = crearPartidaConPendiente("DISCARD_OPPONENT_ACTIVE_ENERGY", 1, 1);
        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b1", "Squirtle"));
        Card botEnergia = cardEnergia("e2", "Water Energy");
        botActivo.getEnergiasUnidas().add(botEnergia);
        partida.getBot().setActivo(botActivo);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("e2", "Water Energy", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e2"));

        assertTrue(partida.getBot().getActivo().getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getBot().getPilaDescarte().size());
    }

    // =================== SWITCH_ACTIVE ===================

    @Test
    void resolverAccionPendiente_switchActive_cambiaActivo() {
        Partida partida = crearPartidaConPendiente("SWITCH_ACTIVE", 1, 1);
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        partida.getJugador().setActivo(activo);
        CartaEnJuego suplente = new CartaEnJuego(cardBasico("b1", "Bulbasaur"));
        partida.getJugador().getBanca().add(suplente);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("b1", "Bulbasaur", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("b1"));

        assertEquals("Bulbasaur", partida.getJugador().getActivo().getCard().getNombre());
        assertEquals(1, partida.getJugador().getBanca().size());
        assertEquals("Pikachu", partida.getJugador().getBanca().get(0).getCard().getNombre());
    }

    // =================== SEARCH_DECK -> HAND ===================

    @Test
    void resolverAccionPendiente_searchDeck_addToHand() {
        Partida partida = crearPartidaConPendiente("SEARCH_DECK", 0, 1);
        Card energy = cardEnergia("e1", "Fire Energy");
        partida.getJugador().getMazo().add(energy);
        partida.getJugador().getMazo().add(cardBasico("p1", "Bulbasaur"));

        PendingBattleAction pending = partida.getPendingAction();
        pending.setDestination("HAND");
        pending.setOptions(List.of(new PendingBattleAction.Option("e1", "Fire Energy", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("e1"));

        assertEquals(1, partida.getJugador().getMano().size());
        assertEquals("e1", partida.getJugador().getMano().get(0).getId());
        assertEquals(1, partida.getJugador().getMazo().size()); // only bulbasaur left
    }

    // =================== CHOOSE_OPPONENT_BENCH_TO_ACTIVE ===================

    @Test
    void resolverAccionPendiente_chooseOpponentBenchToActive_cambiaBotActivo() {
        Partida partida = crearPartidaConPendiente("CHOOSE_OPPONENT_BENCH_TO_ACTIVE", 1, 1);
        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b0", "Squirtle"));
        partida.getBot().setActivo(botActivo);
        CartaEnJuego botBanca = new CartaEnJuego(cardBasico("b1", "Charmander"));
        partida.getBot().getBanca().add(botBanca);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("b1", "Charmander", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("b1"));

        assertEquals("Charmander", partida.getBot().getActivo().getCard().getNombre());
    }

    // =================== DISCARD_OPPONENT_ENERGY ===================

    @Test
    void resolverAccionPendiente_discardOpponentEnergyFromAny_descartaEnergiaDelActivo() {
        Partida partida = crearPartidaConPendiente("DISCARD_OPPONENT_ENERGY", 1, 1);
        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b0", "Squirtle"));
        Card agua = cardEnergia("e1", "Water Energy");
        botActivo.getEnergiasUnidas().add(agua);
        partida.getBot().setActivo(botActivo);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("b0", "Squirtle", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("b0"));

        assertTrue(partida.getBot().getActivo().getEnergiasUnidas().isEmpty());
        assertFalse(partida.getBot().getPilaDescarte().isEmpty());
    }

    // =================== HEAL_OWN_POKEMON ===================

    @Test
    void resolverAccionPendiente_healOwnPokemon_curaActivo() {
        Partida partida = crearPartidaConPendiente("HEAL_OWN_POKEMON", 1, 1);
        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(30);
        partida.getJugador().setActivo(activo);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setAmount(30);
        pending.setOptions(List.of(new PendingBattleAction.Option("p1", "Pikachu", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertEquals(60, partida.getJugador().getActivo().getHpActual());
    }
}
