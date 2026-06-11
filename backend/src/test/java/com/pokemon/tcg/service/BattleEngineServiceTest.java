package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.PendingBattleAction;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoEsperandoInteraccion;
import com.pokemon.tcg.model.battle.state.EstadoTurnoNormal;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class BattleEngineServiceTest {

    @Test
    void testResolverAccionPendienteReorderTopDeck() {
        MazoRepository mazoRepo = mock(MazoRepository.class);
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        CardRepository cardRepo = mock(CardRepository.class);
        BotAIService botAIService = mock(BotAIService.class);
        BattleAttackService battleAttackService = mock(BattleAttackService.class);
        BattleKoService battleKoService = mock(BattleKoService.class);

        BattleEngineService service = new BattleEngineService(
                jugadorRepo, mazoRepo, cardRepo, botAIService, battleAttackService, battleKoService
        );

        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();

        Card c1 = new Card(); c1.setId("c1"); c1.setNombre("Card 1");
        Card c2 = new Card(); c2.setId("c2"); c2.setNombre("Card 2");
        Card c3 = new Card(); c3.setId("c3"); c3.setNombre("Card 3");

        jugador.getMazo().add(c1);
        jugador.getMazo().add(c2);
        jugador.getMazo().add(c3);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.transicionarA(new EstadoEsperandoInteraccion());

        PendingBattleAction action = new PendingBattleAction();
        action.setActor("pablo");
        action.setType("REORDER_TOP_DECK");
        action.setMinSelections(3);
        action.setMaxSelections(3);
        action.getOptions().add(new PendingBattleAction.Option("0", "Card 1", null));
        action.getOptions().add(new PendingBattleAction.Option("1", "Card 2", null));
        action.getOptions().add(new PendingBattleAction.Option("2", "Card 3", null));
        partida.setPendingAction(action);

        service.partidasEnCurso.put(partida.getId(), partida);

        // Reorder: 2, 1, 3
        List<String> chosenIds = List.of("1", "0", "2");

        Partida resultado = service.resolverAccionPendiente(partida.getId(), "pablo", chosenIds);

        // Serialize and print to verify JSON mapping
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // We set pending action to action to see how it serializes
            resultado.setPendingAction(action);
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultado);
            System.out.println("JSON_OUTPUT_START");
            System.out.println(json);
            System.out.println("JSON_OUTPUT_END");
            resultado.setPendingAction(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertNull(resultado.getPendingAction());
        assertEquals(Partida.Fase.TURNO_NORMAL, resultado.getFaseActual());

        // Check top deck order
        assertEquals("c2", resultado.getJugador().getMazo().get(0).getId());
        assertEquals("c1", resultado.getJugador().getMazo().get(1).getId());
        assertEquals("c3", resultado.getJugador().getMazo().get(2).getId());
    }

    @Test
    void ejecutarTurnoBotHacePerderAlJugadorSiNoPuedeRobar() {
        BattleEngineService service = new BattleEngineService(
                mock(JugadorRepository.class),
                mock(MazoRepository.class),
                mock(CardRepository.class),
                mock(BotAIService.class),
                mock(BattleAttackService.class),
                mock(BattleKoService.class)
        );

        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Card cartaBot = new Card();
        cartaBot.setId("bot-draw");
        cartaBot.setNombre("Bot Draw");
        bot.getMazo().add(cartaBot);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.setTurnoActual(Partida.Turno.BOT);
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.ejecutarTurnoBot(partida.getId());

        assertEquals(Partida.Fase.FIN_PARTIDA, partida.getFaseActual());
        assertEquals("BOT", partida.getGanador());
        assertEquals("El jugador no pudo robar una carta al inicio del turno", partida.getRazonFinPartida());
    }
}
