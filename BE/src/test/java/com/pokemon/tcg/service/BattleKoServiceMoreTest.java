package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.CardAttribute;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleKoServiceMoreTest {

    private final BattleKoService service = new BattleKoService();

    private Card card(String id, String nombre, int hp) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setHp(String.valueOf(hp));
        c.setSupertype("Pokemon");
        c.setSubtypes(List.of("Basic"));
        return c;
    }

    private Card cardConTipo(String id, String nombre, int hp, String tipo) {
        Card c = card(id, nombre, hp);
        c.setTipo(tipo);
        return c;
    }

    // =================== Bot wins by taking all prizes ===================

    @Test
    void resolverKO_botGanaPorTomarTodosLosPremios() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego jugadorActivo = new CartaEnJuego(card("j-atk", "Pikachu", 60));
        jugadorActivo.setHpActual(60);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j1", "Premio J1", 0));
        jugador.getBanca().add(new CartaEnJuego(card("j-bench", "Squirtle", 70)));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Charmander", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b1", "Premio B1", 0));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty");

        // Bot attacks and KOs jugador's activo; bot takes its last prize
        service.resolverKO(partida, botActivo, jugadorActivo);

        assertEquals(Partida.Fase.FIN_PARTIDA, partida.getFaseActual());
        assertEquals("misty", partida.getGanador());
        assertTrue(partida.getRazonFinPartida().contains("tomó todos sus premios"));
    }

    // =================== Bot wins without botUsername ===================

    @Test
    void resolverKO_botGana_sinBotUsername_usaNombreDefault() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego jugadorActivo = new CartaEnJuego(card("j-atk", "Pikachu", 60));
        jugadorActivo.setHpActual(60);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j1", "Premio J1", 0));
        jugador.getBanca().add(new CartaEnJuego(card("j-bench", "Squirtle", 70)));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Charmander", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b1", "Premio B1", 0));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        // No botUsername set → defaults to "BOT"

        service.resolverKO(partida, botActivo, jugadorActivo);

        assertEquals(Partida.Fase.FIN_PARTIDA, partida.getFaseActual());
        assertEquals("BOT", partida.getGanador());
    }

    // =================== atacante null → ganador inferred from victim ===================

    @Test
    void resolverKO_atacanteNull_ganadorInferido_victimaBotBord() {
        TableroJugador jugador = new TableroJugador();
        // jugador needs an active so jugadorSinPokemon = false (prevents muerte súbita / bot win)
        CartaEnJuego jugadorActivo = new CartaEnJuego(card("j-atk", "Pikachu", 60));
        jugadorActivo.setHpActual(60);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j", "Premio J", 0));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego defensor = new CartaEnJuego(card("def", "Charmander", 50));
        defensor.setHpActual(0);
        bot.setActivo(defensor);
        bot.getPremios().add(card("p-b", "Premio B", 0));
        bot.getBanca().add(new CartaEnJuego(card("bench", "Squirtle", 60)));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        // atacante = null → ganador inferred as jugador (opposite of bot's tablero)
        service.resolverKO(partida, null, defensor);

        // jugador takes last prize → wins the game
        assertEquals(1, jugador.getMano().size());
        assertEquals(Partida.Fase.FIN_PARTIDA, partida.getFaseActual());
        assertEquals("ash", partida.getGanador());
    }

    // =================== hayKOPendiente: bot needs auto-replacement ===================

    @Test
    void resolverKO_hayKOPendiente_botNecesitaReemplazo() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego jugadorActivo = new CartaEnJuego(card("j-atk", "Pikachu", 60));
        jugadorActivo.setHpActual(0); // jugador's activo is also at 0 → hayKOPendiente = true
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j1", "Premio J1", 0));
        jugador.getPremios().add(card("p-j2", "Premio J2", 0));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Charmander", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b1", "Premio B1", 0));
        CartaEnJuego bench = new CartaEnJuego(card("bench", "Bulbasaur", 80));
        bench.setHpActual(80);
        bot.getBanca().add(bench);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        // KO bot's active; jugador's active also has hpActual = 0 → hayKOPendiente
        service.resolverKO(partida, jugadorActivo, botActivo);

        // Bot should auto-replace with bench card
        assertSame(bench, bot.getActivo());
        assertTrue(bot.getBanca().isEmpty());
        // Game should NOT be over yet (hayKOPendiente → returns early)
        assertNotEquals(Partida.Fase.FIN_PARTIDA, partida.getFaseActual());
    }

    // =================== calcularPuntajeEstrategico: debilidad penalizes ===================

    @Test
    void resolverKO_eligeReemplazo_ignoraDebilAlRival() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego jugadorActivo = new CartaEnJuego(cardConTipo("j-atk", "Charizard", 150, "Fire"));
        jugadorActivo.setHpActual(150);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j", "Premio J", 0));
        jugador.getPremios().add(card("p-j2", "Premio J2", 0));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Bot", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b", "Premio B", 0));

        // candidateA has weakness to Fire → puntaje -= 1000
        Card cardA = cardConTipo("bench-A", "Caterpie", 40, "Grass");
        cardA.setDebilidades(List.of(new CardAttribute("Fire", "×2")));
        CartaEnJuego candidateA = new CartaEnJuego(cardA);
        candidateA.setHpActual(40);

        // candidateB has no weakness → puntaje = just hpActual = 80
        Card cardB = cardConTipo("bench-B", "Squirtle", 80, "Water");
        CartaEnJuego candidateB = new CartaEnJuego(cardB);
        candidateB.setHpActual(80);

        bot.getBanca().add(candidateA);
        bot.getBanca().add(candidateB);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        service.resolverKO(partida, jugadorActivo, botActivo);

        // Should pick candidateB (no weakness), not candidateA (weakness to Fire)
        assertSame(candidateB, bot.getActivo());
    }

    // =================== calcularPuntajeEstrategico: resistencia gives bonus ===================

    @Test
    void resolverKO_eligeReemplazo_prefiereMasResistente() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego jugadorActivo = new CartaEnJuego(cardConTipo("j-atk", "Jolteon", 90, "Lightning"));
        jugadorActivo.setHpActual(90);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j", "Premio J", 0));
        jugador.getPremios().add(card("p-j2", "Premio J2", 0));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Bot", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b", "Premio B", 0));

        // candidateA: no resistance, hp = 60
        Card cardA = cardConTipo("bench-A", "Pikachu", 60, "Lightning");
        CartaEnJuego candidateA = new CartaEnJuego(cardA);
        candidateA.setHpActual(60);

        // candidateB: resistance to Lightning (rival's type) → puntaje += 300, hp = 50
        Card cardB = cardConTipo("bench-B", "Drowzee", 50, "Psychic");
        cardB.setResistencias(List.of(new CardAttribute("Lightning", "-30")));
        CartaEnJuego candidateB = new CartaEnJuego(cardB);
        candidateB.setHpActual(50);

        bot.getBanca().add(candidateA);
        bot.getBanca().add(candidateB);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        service.resolverKO(partida, jugadorActivo, botActivo);

        // candidateB: 50 + 300 = 350 > candidateA: 60 → choose B
        assertSame(candidateB, bot.getActivo());
    }

    // =================== calcularPuntajeEstrategico: rival weak to my type ===================

    @Test
    void resolverKO_eligeReemplazo_rivalDebilAMiTipo() {
        TableroJugador jugador = new TableroJugador();
        Card jugadorCard = cardConTipo("j-atk", "Charizard", 150, "Fire");
        jugadorCard.setDebilidades(List.of(new CardAttribute("Water", "×2")));
        CartaEnJuego jugadorActivo = new CartaEnJuego(jugadorCard);
        jugadorActivo.setHpActual(150);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j", "Premio J", 0));
        jugador.getPremios().add(card("p-j2", "Premio J2", 0));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Bot", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b", "Premio B", 0));

        // candidateA: Water type → rival (Charizard) is weak to Water → +500
        Card cardA = cardConTipo("bench-A", "Squirtle", 40, "Water");
        CartaEnJuego candidateA = new CartaEnJuego(cardA);
        candidateA.setHpActual(40);

        // candidateB: Grass type → no bonus
        Card cardB = cardConTipo("bench-B", "Bulbasaur", 60, "Grass");
        CartaEnJuego candidateB = new CartaEnJuego(cardB);
        candidateB.setHpActual(60);

        bot.getBanca().add(candidateA);
        bot.getBanca().add(candidateB);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        service.resolverKO(partida, jugadorActivo, botActivo);

        // candidateA: 40 + 500 = 540 vs candidateB: 60 → choose A
        assertSame(candidateA, bot.getActivo());
    }

    // =================== calcularPuntajeEstrategico: rival has null tipo ===================

    @Test
    void resolverKO_eligeReemplazo_rivalSinTipo_soloHP() {
        TableroJugador jugador = new TableroJugador();
        Card jugadorCard = card("j-atk", "Unknown", 100);
        // No tipo set → calcularPuntajeEstrategico returns early after hp
        CartaEnJuego jugadorActivo = new CartaEnJuego(jugadorCard);
        jugadorActivo.setHpActual(100);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j", "Premio J", 0));
        jugador.getPremios().add(card("p-j2", "Premio J2", 0));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Bot", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b", "Premio B", 0));

        // candidateA: hp = 30 + 2 energies = 30 + 100 = 130
        CartaEnJuego candidateA = new CartaEnJuego(card("bench-A", "Rattata", 30));
        candidateA.setHpActual(30);
        candidateA.getEnergiasUnidas().add(card("e1", "Energy", 0));
        candidateA.getEnergiasUnidas().add(card("e2", "Energy", 0));

        // candidateB: hp = 120, no energies = 120
        CartaEnJuego candidateB = new CartaEnJuego(card("bench-B", "Snorlax", 120));
        candidateB.setHpActual(120);

        bot.getBanca().add(candidateA);
        bot.getBanca().add(candidateB);

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        service.resolverKO(partida, jugadorActivo, botActivo);

        // candidateA: 30 + 100 = 130 vs candidateB: 120 → choose A
        assertSame(candidateA, bot.getActivo());
    }

    // =================== Bot KO in banca while rival bot has no bench ===================

    @Test
    void resolverKO_botSinBancaNiReemplazo_jugadorGana() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego jugadorActivo = new CartaEnJuego(card("j-atk", "Pikachu", 60));
        jugadorActivo.setHpActual(60);
        jugador.setActivo(jugadorActivo);
        jugador.getPremios().add(card("p-j1", "Premio J1", 0));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego botActivo = new CartaEnJuego(card("b-atk", "Charmander", 50));
        botActivo.setHpActual(50);
        bot.setActivo(botActivo);
        bot.getPremios().add(card("p-b1", "Premio B1", 0));
        // bot has NO bench

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("misty");

        service.resolverKO(partida, jugadorActivo, botActivo);

        // Bot has no pokemon left → jugador wins
        assertEquals(Partida.Fase.FIN_PARTIDA, partida.getFaseActual());
        assertEquals("ash", partida.getGanador());
    }

    // =================== KO banca card with no log prize (0 prizes available) ===================

    @Test
    void resolverKO_sinPremiosDisponibles_noPrizeTakenLog() {
        TableroJugador jugador = new TableroJugador();
        CartaEnJuego atacante = new CartaEnJuego(card("atk", "Pikachu", 60));
        jugador.setActivo(atacante);
        // jugador has NO prizes
        jugador.getBanca().add(new CartaEnJuego(card("j-bench", "Squirtle", 60)));

        TableroJugador bot = new TableroJugador();
        CartaEnJuego defensor = new CartaEnJuego(card("def", "Charmander", 50));
        bot.setActivo(defensor);
        bot.getPremios().add(card("p-b", "Premio B", 0));
        bot.getBanca().add(new CartaEnJuego(card("b-bench", "Bulbasaur", 70)));

        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");

        service.resolverKO(partida, atacante, defensor);

        // No prize taken → PRIZE_TAKEN should NOT appear in logs
        boolean hasPrizeTakenLog = partida.getTurnLogs().stream().anyMatch(l -> l.startsWith("PRIZE_TAKEN"));
        assertFalse(hasPrizeTakenLog);
        // KNOCK_OUT should still appear
        assertTrue(partida.getTurnLogs().stream().anyMatch(l -> l.startsWith("KNOCK_OUT")));
    }
}
