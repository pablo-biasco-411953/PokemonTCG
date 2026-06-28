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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;

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
    }

    @Test
    void testGodModeDebugActions() {
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
        jugador.setActivo(new com.pokemon.tcg.model.battle.CartaEnJuego(card("p1", "Pikachu", "60")));
        TableroJugador bot = new TableroJugador();
        bot.setActivo(new com.pokemon.tcg.model.battle.CartaEnJuego(card("p2", "Charmander", "50")));

        Partida partida = new Partida(jugador, bot);
        service.partidasEnCurso.put(partida.getId(), partida);

        Card waterCard = card("xy1-34", "Starmie", "90");
        org.mockito.Mockito.when(cardRepo.findById("xy1-34")).thenReturn(java.util.Optional.of(waterCard));

        com.pokemon.tcg.model.Jugador adminUser = new com.pokemon.tcg.model.Jugador();
        adminUser.setUsername("admin");
        adminUser.setAdmin(true);
        org.mockito.Mockito.when(jugadorRepo.findByUsername("admin")).thenReturn(adminUser);

        // Test debugRobarCarta
        Partida res1 = service.debugRobarCarta(partida.getId(), "xy1-34", "admin");
        assertEquals(1, res1.getJugador().getMano().size());
        assertEquals("Starmie", res1.getJugador().getMano().get(0).getNombre());

        // Test debugForzarEstado
        Partida res2 = service.debugForzarEstado(partida.getId(), "JUGADOR", "Poisoned", "admin");
        org.junit.jupiter.api.Assertions.assertTrue(res2.getJugador().getActivo().getCondicionesEspeciales().contains("Poisoned"));

        // Test debugSetHp
        Partida res3 = service.debugSetHp(partida.getId(), "BOT", 10, "admin");
        assertEquals(10, res3.getBot().getActivo().getHpActual());
    }

    private Card card(String id, String nombre, String hp) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        return card;
    }

    @Test
    void testResolverAccionPendienteDiscardOpponentEnergy() {
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

        Card electro = card("xy1-45", "Electrode", "90");
        Card energyCard = card("e-1", "Fire Energy", "Energy");

        com.pokemon.tcg.model.battle.CartaEnJuego activeOpponent = new com.pokemon.tcg.model.battle.CartaEnJuego(card("p2", "Charmander", "50"));
        activeOpponent.getEnergiasUnidas().add(energyCard);
        bot.setActivo(activeOpponent);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.transicionarA(new EstadoEsperandoInteraccion());

        PendingBattleAction action = new PendingBattleAction();
        action.setActor("pablo");
        action.setType("DISCARD_OPPONENT_ENERGY");
        action.setMinSelections(1);
        action.setMaxSelections(1);
        action.getOptions().add(new PendingBattleAction.Option("p2", "Charmander", null));
        partida.setPendingAction(action);

        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.resolverAccionPendiente(partida.getId(), "pablo", List.of("p2"));

        assertNull(resultado.getPendingAction());
        assertEquals(0, resultado.getBot().getActivo().getEnergiasUnidas().size());
        assertEquals(1, resultado.getBot().getPilaDescarte().size());
        assertEquals("Fire Energy", resultado.getBot().getPilaDescarte().get(0).getNombre());
    }

    @Test
    void testResolverAccionPendienteMoveEnergyToOpponentBench() {
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

        Card energyCard = card("e-1", "Fire Energy", "Energy");

        com.pokemon.tcg.model.battle.CartaEnJuego activeOpponent = new com.pokemon.tcg.model.battle.CartaEnJuego(card("p2", "Charmander", "50"));
        activeOpponent.getEnergiasUnidas().add(energyCard);
        bot.setActivo(activeOpponent);

        com.pokemon.tcg.model.battle.CartaEnJuego benchedOpponent = new com.pokemon.tcg.model.battle.CartaEnJuego(card("b-1", "Bulbasaur", "50"));
        bot.getBanca().add(benchedOpponent);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.transicionarA(new EstadoEsperandoInteraccion());

        PendingBattleAction action = new PendingBattleAction();
        action.setActor("pablo");
        action.setType("MOVE_ENERGY_TO_OPPONENT_BENCH");
        action.setMinSelections(1);
        action.setMaxSelections(1);
        action.setEndsTurn(true);
        action.getOptions().add(new PendingBattleAction.Option("b-1", "Bulbasaur", null));
        partida.setPendingAction(action);

        service.partidasEnCurso.put(partida.getId(), partida);

        Partida resultado = service.resolverAccionPendiente(partida.getId(), "pablo", List.of("b-1"));

        assertNull(resultado.getPendingAction());
        assertEquals(0, resultado.getBot().getActivo().getEnergiasUnidas().size());
        assertEquals(1, resultado.getBot().getBanca().get(0).getEnergiasUnidas().size());
        assertEquals("Fire Energy", resultado.getBot().getBanca().get(0).getEnergiasUnidas().get(0).getNombre());
    }
    @Test
    void testUnirEnergia() {
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

        Card pika = card("p1", "Pikachu", "60");
        pika.setSupertype("Pokemon");
        pika.setTipo("Lightning");
        jugador.setActivo(new com.pokemon.tcg.model.battle.CartaEnJuego(pika));

        Card energy = card("e1", "Lightning Energy", "Energy");
        energy.setSupertype("Energy");
        energy.setTipo("Lightning");
        jugador.getMano().add(energy);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        org.mockito.Mockito.when(cardRepo.findById("e1")).thenReturn(java.util.Optional.of(energy));

        service.unirEnergia(partida.getId(), "p1", "e1", "pablo", null);

        assertEquals(1, jugador.getActivo().getEnergiasUnidas().size());
        org.junit.jupiter.api.Assertions.assertTrue(partida.isYaSeUnioEnergiaEsteTurno());
        org.junit.jupiter.api.Assertions.assertTrue(jugador.getMano().isEmpty());
    }

    @Test
    void testRealizarRetirada() {
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

        Card pika = card("p1", "Pikachu", "60");
        pika.setSupertype("Pokemon");
        pika.setTipo("Lightning");
        pika.setCostoRetirada(1);
        
        com.pokemon.tcg.model.battle.CartaEnJuego activo = new com.pokemon.tcg.model.battle.CartaEnJuego(pika);
        Card energy = card("e1", "Lightning Energy", "Energy");
        energy.setSupertype("Energy");
        energy.setTipo("Lightning");
        activo.getEnergiasUnidas().add(energy);
        jugador.setActivo(activo);

        Card squirtle = card("p2", "Squirtle", "60");
        squirtle.setSupertype("Pokemon");
        squirtle.setTipo("Water");
        jugador.getBanca().add(new com.pokemon.tcg.model.battle.CartaEnJuego(squirtle));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        service.realizarRetirada(partida.getId(), "p2", "pablo");

        assertEquals("p2", jugador.getActivo().getCard().getId());
        org.junit.jupiter.api.Assertions.assertTrue(partida.isYaSeRetiroEsteTurno());
        assertEquals(1, jugador.getPilaDescarte().size()); // Energy discarded
    }

    @Test
    void testEvolucionarPokemon() {
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

        Card bulba = card("p1", "Bulbasaur", "60");
        bulba.setSupertype("Pokemon");
        bulba.setSubtypes(List.of("Basic"));
        com.pokemon.tcg.model.battle.CartaEnJuego activo = new com.pokemon.tcg.model.battle.CartaEnJuego(bulba);
        activo.setTurnoEntrada(2);
        jugador.setActivo(activo);

        Card ivy = card("p2", "Ivysaur", "90");
        ivy.setSupertype("Pokemon");
        ivy.setSubtypes(List.of("Stage 1"));
        ivy.setEvolvesFrom("Bulbasaur");
        jugador.getMano().add(ivy);
        jugador.setTurnosJugados(2);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.setNumeroTurno(3);
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        org.mockito.Mockito.when(cardRepo.findById("p2")).thenReturn(java.util.Optional.of(ivy));

        service.evolucionarPokemon(partida.getId(), "p2", "p1", "pablo");

        assertEquals("p2", jugador.getActivo().getCard().getId());
        assertEquals("90", jugador.getActivo().getCard().getHp());
    }

    @Test
    void testJugarTrainer() {
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

        Card trainer = card("t1", "Potion", "0");
        trainer.setSupertype("Trainer");
        trainer.setSubtypes(List.of("Item"));
        jugador.getMano().add(trainer);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        org.mockito.Mockito.when(cardRepo.findById("t1")).thenReturn(java.util.Optional.of(trainer));

        // Potion heal target isn't tested deeply here, just the playing mechanism
        // Potion requires pending action usually, but we test the initial call
        service.jugarTrainer(partida.getId(), "t1", "pablo");
        
        org.junit.jupiter.api.Assertions.assertTrue(jugador.getMano().isEmpty());
    }

    @Test
    void testRealizarAtaque() {
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
        
        Card pika = card("p1", "Pikachu", "60");
        com.pokemon.tcg.model.battle.Ataque attack = new com.pokemon.tcg.model.battle.Ataque();
        attack.setNombre("Thunder Jolt");
        pika.reemplazarAtaques(List.of(attack));
        jugador.setActivo(new com.pokemon.tcg.model.battle.CartaEnJuego(pika));
        bot.setActivo(new com.pokemon.tcg.model.battle.CartaEnJuego(card("p2", "Squirtle", "60")));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("pablo");
        partida.setNumeroTurno(2);
        partida.transicionarA(new EstadoTurnoNormal());
        service.partidasEnCurso.put(partida.getId(), partida);

        org.mockito.Mockito.when(battleAttackService.resolveAttack(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), 
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(new com.pokemon.tcg.service.BattleAttackService.AttackResolution(
            new com.pokemon.tcg.model.battle.ResultadoAtaque(10, 0), java.util.List.of()
        ));

        service.realizarAtaque(partida.getId(), "Thunder Jolt", "pablo", null);

        org.mockito.Mockito.verify(battleAttackService, org.mockito.Mockito.times(1))
            .resolveAttack(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull());
    }
    @Test
    void testStartBattleJugadorNoEncontrado() {
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        BattleEngineService service = new BattleEngineService(
                jugadorRepo, mock(MazoRepository.class), mock(CardRepository.class),
                mock(BotAIService.class), mock(BattleAttackService.class), mock(BattleKoService.class)
        );
        org.mockito.Mockito.when(jugadorRepo.findByUsername("no_existe")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> {
            service.startBattle("no_existe", 1L);
        });
    }

    @Test
    void testStartBattleMazoNoEncontrado() {
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        MazoRepository mazoRepo = mock(MazoRepository.class);
        BattleEngineService service = new BattleEngineService(
                jugadorRepo, mazoRepo, mock(CardRepository.class),
                mock(BotAIService.class), mock(BattleAttackService.class), mock(BattleKoService.class)
        );
        org.mockito.Mockito.when(jugadorRepo.findByUsername("pablo")).thenReturn(new Jugador());
        org.mockito.Mockito.when(mazoRepo.findById(99L)).thenReturn(java.util.Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            service.startBattle("pablo", 99L);
        });
    }

    @Test
    void testStartBattleMazoIncompleto() {
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        MazoRepository mazoRepo = mock(MazoRepository.class);
        BattleEngineService service = new BattleEngineService(
                jugadorRepo, mazoRepo, mock(CardRepository.class),
                mock(BotAIService.class), mock(BattleAttackService.class), mock(BattleKoService.class)
        );
        org.mockito.Mockito.when(jugadorRepo.findByUsername("pablo")).thenReturn(new Jugador());
        Mazo mazo = new Mazo();
        mazo.setCartas(new ArrayList<>());
        org.mockito.Mockito.when(mazoRepo.findById(1L)).thenReturn(java.util.Optional.of(mazo));
        assertThrows(IllegalStateException.class, () -> {
            service.startBattle("pablo", 1L);
        });
    }
    @Test
    void testLanzarMoneda_CallerInvalido() {
        BattleEngineService service = new BattleEngineService(
                mock(JugadorRepository.class), mock(MazoRepository.class), mock(CardRepository.class),
                mock(BotAIService.class), mock(BattleAttackService.class), mock(BattleKoService.class)
        );
        Partida partida = new Partida(new TableroJugador(), new TableroJugador());
        partida.setJugadorUsername("pablo");
        partida.setBotUsername("bot");
        partida.setCoinFlipCallerUsername("pablo");
        service.partidasEnCurso.put(partida.getId(), partida);
        assertThrows(IllegalStateException.class, () -> {
            service.lanzarMoneda(partida.getId(), "bot_o_alguien", "CARA");
        });
    }

    @Test
    void testLanzarMoneda_YaLanzada() {
        BattleEngineService service = new BattleEngineService(
                mock(JugadorRepository.class), mock(MazoRepository.class), mock(CardRepository.class),
                mock(BotAIService.class), mock(BattleAttackService.class), mock(BattleKoService.class)
        );
        Partida partida = new Partida(new TableroJugador(), new TableroJugador());
        partida.setCoinFlipped(true);
        service.partidasEnCurso.put(partida.getId(), partida);
        
        Partida res = service.lanzarMoneda(partida.getId(), "pablo", "CARA");
        assertEquals(partida, res);
    }
}
