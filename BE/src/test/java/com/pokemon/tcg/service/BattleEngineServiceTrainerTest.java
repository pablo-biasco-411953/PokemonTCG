package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoTurnoNormal;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BattleEngineServiceTrainerTest {

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

    private Card cardTrainer(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Trainer");
        return c;
    }

    private Card cardEnergiaBasica(String id, String nombre) {
        Card c = new Card();
        c.setId(id);
        c.setNombre(nombre);
        c.setSupertype("Energy");
        c.setSubtypes(java.util.List.of("Basic"));
        return c;
    }

    private Partida crearPartidaEnTurno() {
        TableroJugador jugador = new TableroJugador();
        TableroJugador bot = new TableroJugador();
        Partida partida = new Partida(jugador, bot);
        partida.setJugadorUsername("ash");
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.transicionarA(new EstadoTurnoNormal());
        partida.setNumeroTurno(2);
        return partida;
    }

    // =================== Professor Sycamore (xy1-122) ===================

    @Test
    void jugarTrainer_professorSycamore_descartaManoYRoba7() {
        Partida partida = crearPartidaEnTurno();
        Card sycamore = cardTrainer("xy1-122", "Professor Sycamore");
        partida.getJugador().getMano().add(sycamore);
        partida.getJugador().getMano().add(cardBasico("xy1-1", "Pikachu"));
        partida.getJugador().getMano().add(cardBasico("xy1-2", "Charmander"));
        for (int i = 0; i < 10; i++) {
            partida.getJugador().getMazo().add(cardBasico("m" + i, "Pokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-122", "ash");

        assertEquals(7, partida.getJugador().getMano().size());
        // sycamore + 2 hand cards = 3 in discard (trainer itself is discarded after use)
        assertEquals(3, partida.getJugador().getPilaDescarte().size());
    }

    // =================== Shauna (xy1-127) ===================

    @Test
    void jugarTrainer_shauna_barajaYRoba5() {
        Partida partida = crearPartidaEnTurno();
        Card shauna = cardTrainer("xy1-127", "Shauna");
        partida.getJugador().getMano().add(shauna);
        partida.getJugador().getMano().add(cardBasico("xy1-1", "Pikachu"));
        for (int i = 0; i < 10; i++) {
            partida.getJugador().getMazo().add(cardBasico("m" + i, "Pokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-127", "ash");

        assertEquals(5, partida.getJugador().getMano().size());
        // only the trainer card itself goes to discard (pikachu returns to deck)
        assertEquals(1, partida.getJugador().getPilaDescarte().size());
    }

    // =================== Red Card (xy1-124) ===================

    @Test
    void jugarTrainer_redCard_oponenteBarajayRoba4() {
        Partida partida = crearPartidaEnTurno();
        Card redCard = cardTrainer("xy1-124", "Red Card");
        partida.getJugador().getMano().add(redCard);
        // opponent has 3 cards in hand
        partida.getBot().getMano().add(cardBasico("b1", "Squirtle"));
        partida.getBot().getMano().add(cardBasico("b2", "Bulbasaur"));
        partida.getBot().getMano().add(cardBasico("b3", "Geodude"));
        for (int i = 0; i < 10; i++) {
            partida.getBot().getMazo().add(cardBasico("bm" + i, "BotPokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-124", "ash");

        assertEquals(4, partida.getBot().getMano().size());
        assertTrue(partida.getBot().getPilaDescarte().isEmpty());
    }

    // =================== Roller Skates (xy1-125) ===================

    @Test
    void jugarTrainer_rollerSkates_noLanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card rollerSkates = cardTrainer("xy1-125", "Roller Skates");
        partida.getJugador().getMano().add(rollerSkates);
        for (int i = 0; i < 5; i++) {
            partida.getJugador().getMazo().add(cardBasico("m" + i, "Pokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        assertDoesNotThrow(() -> service.jugarTrainer(partida.getId(), "xy1-125", "ash"));
    }

    // =================== Great Ball (xy1-118) ===================

    @Test
    void jugarTrainer_greatBall_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card greatBall = cardTrainer("xy1-118", "Great Ball");
        partida.getJugador().getMano().add(greatBall);
        for (int i = 0; i < 7; i++) {
            partida.getJugador().getMazo().add(cardBasico("m" + i, "Pokemon" + i));
        }
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-118", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("SEARCH_DECK", partida.getPendingAction().getType());
    }

    // =================== Cassius (xy1-115) ===================

    @Test
    void jugarTrainer_cassius_conActivo_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card cassius = cardTrainer("xy1-115", "Cassius");
        partida.getJugador().getMano().add(cassius);
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-115", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("SELECT_POKEMON_CASSIUS", partida.getPendingAction().getType());
    }

    @Test
    void jugarTrainer_cassius_sinPokemon_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card cassius = cardTrainer("xy1-115", "Cassius");
        partida.getJugador().getMano().add(cassius);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarTrainer(partida.getId(), "xy1-115", "ash"));
    }

    // =================== Professor's Letter (xy1-123) ===================

    @Test
    void jugarTrainer_professorsLetter_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card letter = cardTrainer("xy1-123", "Professor's Letter");
        partida.getJugador().getMano().add(letter);
        partida.getJugador().getMazo().add(cardEnergiaBasica("e1", "Lightning Energy"));
        partida.getJugador().getMazo().add(cardEnergiaBasica("e2", "Fire Energy"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-123", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("SEARCH_DECK", partida.getPendingAction().getType());
        assertEquals(2, partida.getPendingAction().getOptions().size());
    }

    // =================== Super Potion (xy1-128) ===================

    @Test
    void jugarTrainer_superPotion_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card superPotion = cardTrainer("xy1-128", "Super Potion");
        partida.getJugador().getMano().add(superPotion);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(40); // damaged
        Card energia = cardEnergiaBasica("e1", "Lightning Energy");
        activo.getEnergiasUnidas().add(energia);
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-128", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("SELECT_POKEMON_SUPER_POTION", partida.getPendingAction().getType());
    }

    @Test
    void jugarTrainer_superPotion_sinDamageOSinEnergia_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card superPotion = cardTrainer("xy1-128", "Super Potion");
        partida.getJugador().getMano().add(superPotion);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Pikachu"));
        activo.setHpActual(60); // full HP
        partida.getJugador().setActivo(activo);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarTrainer(partida.getId(), "xy1-128", "ash"));
    }

    // =================== Team Flare Grunt (xy1-129) ===================

    @Test
    void jugarTrainer_teamFlareGrunt_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card flareGrunt = cardTrainer("xy1-129", "Team Flare Grunt");
        partida.getJugador().getMano().add(flareGrunt);

        CartaEnJuego botActivo = new CartaEnJuego(cardBasico("b1", "Squirtle"));
        Card energia = cardEnergiaBasica("e1", "Water Energy");
        botActivo.getEnergiasUnidas().add(energia);
        partida.getBot().setActivo(botActivo);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-129", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("DISCARD_OPPONENT_ACTIVE_ENERGY", partida.getPendingAction().getType());
    }

    @Test
    void jugarTrainer_teamFlareGrunt_sinEnergiaOponente_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card flareGrunt = cardTrainer("xy1-129", "Team Flare Grunt");
        partida.getJugador().getMano().add(flareGrunt);
        partida.getBot().setActivo(new CartaEnJuego(cardBasico("b1", "Squirtle")));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarTrainer(partida.getId(), "xy1-129", "ash"));
    }

    // =================== Hard Charm / Muscle Band (xy1-119, xy1-121) ===================

    @Test
    void jugarTrainer_hardCharm_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card hardCharm = cardTrainer("xy1-119", "Hard Charm");
        partida.getJugador().getMano().add(hardCharm);
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Pikachu")));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-119", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("ATTACH_TOOL", partida.getPendingAction().getType());
    }

    // =================== Evosoda (xy1-116) ===================

    @Test
    void jugarTrainer_evosoda_conEvolucionEnMazo_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card evosoda = cardTrainer("xy1-116", "Evosoda");
        partida.getJugador().getMano().add(evosoda);

        CartaEnJuego activo = new CartaEnJuego(cardBasico("p1", "Bulbasaur"));
        partida.getJugador().setActivo(activo);

        Card ivysaur = new Card();
        ivysaur.setId("xy1-2");
        ivysaur.setNombre("Ivysaur");
        ivysaur.setSupertype("Pokemon");
        ivysaur.setEvolvesFrom("Bulbasaur");
        ivysaur.setHp("90");
        partida.getJugador().getMazo().add(ivysaur);
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-116", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("SELECT_POKEMON_EVOSODA", partida.getPendingAction().getType());
    }

    @Test
    void jugarTrainer_evosoda_sinEvolucionEnMazo_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card evosoda = cardTrainer("xy1-116", "Evosoda");
        partida.getJugador().getMano().add(evosoda);
        partida.getJugador().setActivo(new CartaEnJuego(cardBasico("p1", "Bulbasaur")));
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarTrainer(partida.getId(), "xy1-116", "ash"));
    }

    // =================== Max Revive (xy1-120) ===================

    @Test
    void jugarTrainer_maxRevive_conPokemonEnDescarte_setsPendingAction() {
        Partida partida = crearPartidaEnTurno();
        Card maxRevive = cardTrainer("xy1-120", "Max Revive");
        partida.getJugador().getMano().add(maxRevive);
        partida.getJugador().getPilaDescarte().add(cardBasico("p1", "Pikachu"));
        service.partidasEnCurso.put(partida.getId(), partida);

        service.jugarTrainer(partida.getId(), "xy1-120", "ash");

        assertNotNull(partida.getPendingAction());
        assertEquals("DISCARD_RECOVERY", partida.getPendingAction().getType());
    }

    @Test
    void jugarTrainer_maxRevive_sinPokemonEnDescarte_lanzaExcepcion() {
        Partida partida = crearPartidaEnTurno();
        Card maxRevive = cardTrainer("xy1-120", "Max Revive");
        partida.getJugador().getMano().add(maxRevive);
        service.partidasEnCurso.put(partida.getId(), partida);

        assertThrows(IllegalArgumentException.class,
                () -> service.jugarTrainer(partida.getId(), "xy1-120", "ash"));
    }
}
