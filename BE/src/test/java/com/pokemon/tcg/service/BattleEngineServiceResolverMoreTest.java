package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleEngineServiceResolverMoreTest {

    @InjectMocks
    private BattleEngineService service;
    
    @Mock
    private BattleKoService battleKoService;
    
    private Partida partida;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        TableroJugador tJugador = new TableroJugador();
        TableroJugador tBot = new TableroJugador();
        
        partida = new Partida(tJugador, tBot);
        partida.setId("match-123");
        partida.setFaseActual(Partida.Fase.INICIO);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("bot");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        
        service.partidasEnCurso.put(partida.getId(), partida);
    }
    
    private Card card(String id, String name, String hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(name);
        c.setHp(hp);
        return c;
    }
    
    private CartaEnJuego active(TableroJugador board, Card c) {
        CartaEnJuego cej = new CartaEnJuego(c);
        cej.setHpActual(Integer.parseInt(c.getHp()));
        board.setActivo(cej);
        return cej;
    }
    
    private CartaEnJuego bench(TableroJugador board, Card c) {
        CartaEnJuego cej = new CartaEnJuego(c);
        cej.setHpActual(Integer.parseInt(c.getHp()));
        board.getBanca().add(cej);
        return cej;
    }
    
    private PendingBattleAction pending(String type, int amount, String... validIds) {
        PendingBattleAction p = new PendingBattleAction();
        p.setActor("ash");
        p.setType(type);
        p.setAmount(amount);
        p.setMinSelections(1);
        p.setMaxSelections(3);
        List<PendingBattleAction.Option> options = new ArrayList<>();
        for (String id : validIds) {
            options.add(new PendingBattleAction.Option(id, "Test", null));
        }
        p.setOptions(options);
        return p;
    }

    @Test
    void resolverAccion_HEAL_OWN_POKEMON_healsTargetAndCapsAtMaxHp() {
        CartaEnJuego tActive = active(partida.getJugador(), card("p1", "Pikachu", "60"));
        tActive.setHpActual(30);
        
        CartaEnJuego tBench = bench(partida.getJugador(), card("p2", "Bulbasaur", "70"));
        tBench.setHpActual(10);
        
        PendingBattleAction p = pending("HEAL_OWN_POKEMON", 30, "p1");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("p1"));
        assertEquals(60, tActive.getHpActual()); // Capped at 60
        
        PendingBattleAction p2 = pending("HEAL_OWN_POKEMON", 30, "p2");
        partida.setPendingAction(p2);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("p2"));
        assertEquals(40, tBench.getHpActual()); // 10 + 30
    }

    @Test
    void resolverAccion_CHOOSE_OPPONENT_BENCH_TO_ACTIVE() {
        active(partida.getBot(), card("p1", "Pikachu", "60"));
        bench(partida.getBot(), card("p2", "Bulbasaur", "70"));
        
        PendingBattleAction p = pending("CHOOSE_OPPONENT_BENCH_TO_ACTIVE", 0, "p2");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("p2"));
        
        assertEquals("p2", partida.getBot().getActivo().getCard().getId());
        assertEquals(1, partida.getBot().getBanca().size());
        assertEquals("p1", partida.getBot().getBanca().get(0).getCard().getId());
    }

    @Test
    void resolverAccion_CHOOSE_OPPONENT_BENCH_TO_DAMAGE() {
        active(partida.getJugador(), card("pA", "Raichu", "90"));
        CartaEnJuego oBench = bench(partida.getBot(), card("p2", "Bulbasaur", "70"));
        
        PendingBattleAction p = pending("CHOOSE_OPPONENT_BENCH_TO_DAMAGE", 30, "p2");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("p2"));
        
        assertEquals(40, oBench.getHpActual());
    }

    @Test
    void resolverAccion_DISCARD_OPPONENT_ENERGY() {
        CartaEnJuego oActive = active(partida.getBot(), card("p1", "Pikachu", "60"));
        Card energy = card("e1", "Fire Energy", "0");
        oActive.getEnergiasUnidas().add(energy);
        
        PendingBattleAction p = pending("DISCARD_OPPONENT_ENERGY", 0, "p1");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("p1"));
        
        assertTrue(oActive.getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getBot().getPilaDescarte().size());
    }

    @Test
    void resolverAccion_DISCARD_POTION_ENERGY() {
        CartaEnJuego jActive = active(partida.getJugador(), card("p1", "Pikachu", "60"));
        Card energy = card("e1", "Fire Energy", "0");
        jActive.getEnergiasUnidas().add(energy);
        
        PendingBattleAction p = pending("DISCARD_POTION_ENERGY", 0, "e1");
        p.setDestination("target:p1");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("e1"));
        
        assertTrue(jActive.getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getJugador().getPilaDescarte().size());
    }

    @Test
    void resolverAccion_DISCARD_OPPONENT_ACTIVE_ENERGY() {
        CartaEnJuego oActive = active(partida.getBot(), card("p1", "Pikachu", "60"));
        Card energy = card("e1", "Fire Energy", "0");
        oActive.getEnergiasUnidas().add(energy);
        
        PendingBattleAction p = pending("DISCARD_OPPONENT_ACTIVE_ENERGY", 0, "e1");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("e1"));
        
        assertTrue(oActive.getEnergiasUnidas().isEmpty());
        assertEquals(1, partida.getBot().getPilaDescarte().size());
    }

    @Test
    void resolverAccion_DISCARD_TO_TOP_DECK() {
        Card c = card("c1", "Pikachu", "60");
        partida.getJugador().getPilaDescarte().add(c);
        
        PendingBattleAction p = pending("DISCARD_TO_TOP_DECK", 0, "c1");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("c1"));
        
        assertTrue(partida.getJugador().getPilaDescarte().isEmpty());
        assertEquals("c1", partida.getJugador().getMazo().get(0).getId());
    }

    @Test
    void resolverAccion_SELECT_DISCARD_ITEMS_FOR_PICKUP() {
        Card c = card("c1", "Potion", "0");
        partida.getJugador().getPilaDescarte().add(c);
        
        PendingBattleAction p = pending("SELECT_DISCARD_ITEMS_FOR_PICKUP", 0, "c1");
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", List.of("c1"));
        
        assertTrue(partida.getJugador().getPilaDescarte().isEmpty());
        assertEquals(1, partida.getJugador().getMano().size());
    }

    @Test
    void resolverAccion_REORDER_TOP_DECK() {
        partida.getJugador().getMazo().add(card("m1", "A", "0"));
        partida.getJugador().getMazo().add(card("m2", "B", "0"));
        partida.getJugador().getMazo().add(card("m3", "C", "0"));
        
        PendingBattleAction p = pending("REORDER_TOP_DECK", 0, "0", "1", "2");
        p.setMinSelections(3);
        partida.setPendingAction(p);
        
        service.resolverAccionPendiente("match-123", "ash", Arrays.asList("2", "0", "1"));
        
        assertEquals("m3", partida.getJugador().getMazo().get(0).getId());
        assertEquals("m1", partida.getJugador().getMazo().get(1).getId());
        assertEquals("m2", partida.getJugador().getMazo().get(2).getId());
    }
}
