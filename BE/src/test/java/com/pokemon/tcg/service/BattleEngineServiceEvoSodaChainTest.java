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

class BattleEngineServiceEvoSodaChainTest {

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

    private Partida crearPartidaConPendiente(String tipo, String destination, int minSel, int maxSel) {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoEsperandoInteraccion());

        PendingBattleAction pending = new PendingBattleAction();
        pending.setActor("ash");
        pending.setType(tipo);
        pending.setDestination(destination);
        pending.setMinSelections(minSel);
        pending.setMaxSelections(maxSel);
        partida.setPendingAction(pending);

        return partida;
    }

    // =================== SELECT_POKEMON_EVOSODA → SEARCH_EVOLUTION ===================

    @Test
    void resolver_evosodaSelectPokemon_setPendingSearchEvolution() {
        Partida partida = crearPartidaConPendiente("SELECT_POKEMON_EVOSODA", null, 1, 1);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Bulbasaur"));
        partida.getJugador().setActivo(activo);

        Card ivysaur = new Card();
        ivysaur.setId("xy1-2");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setEvolvesFrom("Bulbasaur");
        ivysaur.setHp("90");
        partida.getJugador().getMazo().add(ivysaur);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("p1", "Bulbasaur", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        // Should now have SEARCH_EVOLUTION pending
        assertNotNull(partida.getPendingAction());
        assertEquals("SEARCH_EVOLUTION", partida.getPendingAction().getType());
        assertEquals(1, partida.getPendingAction().getOptions().size());
        assertEquals("xy1-2", partida.getPendingAction().getOptions().get(0).getId());
    }

    @Test
    void resolver_searchEvolution_evolucionaElPokemon() {
        Partida partida = crearPartidaConPendiente("SEARCH_EVOLUTION", "EVOLVE_TARGET:p1", 0, 1);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Bulbasaur"));
        activo.setHpActual(60);
        partida.getJugador().setActivo(activo);

        Card ivysaur = new Card();
        ivysaur.setId("xy1-2");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setEvolvesFrom("Bulbasaur");
        ivysaur.setHp("90");
        partida.getJugador().getMazo().add(ivysaur);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("xy1-2", "Ivysaur", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("xy1-2"));

        assertEquals("Ivysaur", partida.getJugador().getActivo().getCard().getNombre());
        assertTrue(partida.getJugador().getMazo().isEmpty());
    }

    @Test
    void resolver_searchEvolution_sinSeleccion_barajaElMazo() {
        Partida partida = crearPartidaConPendiente("SEARCH_EVOLUTION", "EVOLVE_TARGET:p1", 0, 1);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Bulbasaur"));
        partida.getJugador().setActivo(activo);

        Card ivysaur = new Card();
        ivysaur.setId("xy1-2");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setEvolvesFrom("Bulbasaur");
        ivysaur.setHp("90");
        partida.getJugador().getMazo().add(ivysaur);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("xy1-2", "Ivysaur", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        // Cancel selection
        service.resolverAccionPendiente(partida.getId(), "ash", List.of());

        // Pokemon should NOT have evolved
        assertEquals("Bulbasaur", partida.getJugador().getActivo().getCard().getNombre());
        assertEquals(1, partida.getJugador().getMazo().size()); // ivysaur still in deck
    }

    // =================== ATTACH_TOOL resolución real ===================

    @Test
    void resolver_attachTool_adjuntaHerramientaAlActivo() {
        Partida partida = crearPartidaConPendiente("ATTACH_TOOL", null, 1, 1);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        partida.getJugador().setActivo(activo);

        Card hardCharm = new Card();
        hardCharm.setId("xy1-119");
        hardCharm.setNombre("Hard Charm");
        hardCharm.setSupertype("Trainer");
        hardCharm.setSubtypes(List.of("Pokemon Tool"));
        partida.getJugador().getMano().add(hardCharm);

        PendingBattleAction pending = partida.getPendingAction();
        pending.setOptions(List.of(new PendingBattleAction.Option("p1", "Pikachu", null)));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.resolverAccionPendiente(partida.getId(), "ash", List.of("p1"));

        assertNotNull(partida.getJugador().getActivo().getAttachedTools());
        assertEquals(1, partida.getJugador().getActivo().getAttachedTools().size());
        assertEquals("Hard Charm", partida.getJugador().getActivo().getAttachedTools().get(0).getNombre());
        assertTrue(partida.getJugador().getMano().isEmpty());
    }
}
