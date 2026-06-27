package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.command.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BattleAttackServiceTest {

    private final BattleAttackService service = new BattleAttackService();

    @Test
    void resolveAttackAplicaDanioBaseYRegistraHistorialVacioSinMonedas() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        Ataque ataque = attack("Golpe", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(20, defensor.getHpActual());
        assertEquals(30, resolution.resultado().danioFinal());
        assertTrue(resolution.historialMonedas().isEmpty());
    }

    @Test
    void resolveAttackAplicaCuracionYRobaCartaCuandoElTextoLoIndica() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        atacante.setHpActual(30);
        partida.getJugador().getMazo().add(card("draw-1", "Carta Robo", "0"));

        Ataque ataque = attack("Recuperar", 20, "Heal 20 damage from this Pokémon. Draw a card.");

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> {}, null);

        assertEquals(50, atacante.getHpActual());
        assertEquals(1, partida.getJugador().getMano().size());
        assertTrue(partida.getTurnLogs().stream().anyMatch(log -> log.startsWith("CARDS_DRAWN:")));
    }

    @Test
    void resolveAttackConBusquedaDejaUnaDecisionGuiadaParaElJugador() {
        Partida partida = partidaBasica();
        partida.setJugadorUsername("Pablo");
        Card grass = card("grass-1", "Chespin", "60");
        grass.setSupertype("Pokemon");
        grass.setTipo("Grass");
        partida.getJugador().getMazo().add(grass);

        Ataque ataque = attack(
                "Pheromotion",
                0,
                "Search your deck for a Grass Pokemon, reveal it, and put it into your hand. Shuffle your deck afterward."
        );

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {}, null);

        assertEquals(Partida.Fase.ESPERANDO_INTERACCION, partida.getFaseActual());
        assertEquals("SEARCH_DECK", partida.getPendingAction().getType());
        assertEquals("grass-1", partida.getPendingAction().getOptions().getFirst().getId());
    }

    @Test
    void resolveAttackAplicaVenenoYBloqueoDeRetiradaSinAzar() {
        Partida partida = partidaBasica();
        Ataque ataque = attack(
                "Trampa Toxica",
                10,
                "The Defending Pokémon is now Poisoned. The Defending Pokémon can't retreat during your opponent's next turn."
        );

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {}, null);

        assertTrue(partida.getBot().getActivo().getCondicionesEspeciales().contains("Poisoned"));
        assertTrue(partida.getBot().getActivo().getCondicionesEspeciales().contains("CantRetreat"));
    }

    @Test
    void resolveAttackPuedeDebilitarAlAtacantePorRetroceso() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        atacante.setHpActual(20);
        Ataque ataque = attack("Choque", 10, "This Pokémon does 20 damage to itself.");
        AtomicBoolean koInvocado = new AtomicBoolean(false);

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> koInvocado.set(true), null);

        assertEquals(0, atacante.getHpActual());
        assertTrue(koInvocado.get());
    }

    @Test
    void comandoSetNoPuedeAtacarSiguienteTurnoPoneFlagCorrecto() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        BattleCommand cmd = new SetNoPuedeAtacarSiguienteTurnoCommand(Target.SELF);
        
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertTrue(atacante.isNoPuedeAtacarSiguienteTurno());
        assertFalse(atacante.isNoPuedeAtacarYaConsumido());
    }

    @Test
    void comandoSetCannotAttackDefendingPonePuedeAtacarEnFalse() {
        Partida partida = partidaBasica();
        CartaEnJuego defensor = partida.getBot().getActivo();
        BattleCommand cmd = new SetCannotAttackDefendingCommand();

        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertFalse(defensor.isPuedeAtacar());
    }

    @Test
    void comandoBlockAttackNextTurnPoneFlagCorrecto() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        BattleCommand cmd = new BlockAttackNextTurnCommand("King's Shield", Target.SELF);

        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertEquals("King's Shield", atacante.getAtaqueBloqueadoSiguienteTurno());
        assertFalse(atacante.isAtaqueBloqueadoYaConsumido());
    }

    @Test
    void comandoConditionalDamageMultiplierBenchedPokemonAplicaDanioCorrecto() {
        Partida partida = partidaBasica();
        partida.getJugador().getBanca().add(new CartaEnJuego(card("b1", "Bulbasaur", "50")));
        partida.getJugador().getBanca().add(new CartaEnJuego(card("b2", "Squirtle", "50")));

        CartaEnJuego defensor = partida.getBot().getActivo();
        defensor.setHpActual(80);

        BattleCommand cmd = new ConditionalDamageMultiplierCommand(0, 20, "BENCHED_POKEMON", null);
        cmd.execute(partida, partida.getJugador(), partida.getBot());
        if (!partida.getExecutionQueue().isEmpty()) {
            partida.getExecutionQueue().poll().execute(partida, partida.getJugador(), partida.getBot());
        }

        assertEquals(40, defensor.getHpActual());
    }

    @Test
    void comandoSelfBenchDamageRestaHpACadaPokemonDeTuBanca() {
        Partida partida = partidaBasica();
        CartaEnJuego b1 = new CartaEnJuego(card("b1", "Bulbasaur", "50"));
        CartaEnJuego b2 = new CartaEnJuego(card("b2", "Squirtle", "60"));
        partida.getJugador().getBanca().add(b1);
        partida.getJugador().getBanca().add(b2);

        BattleCommand cmd = new SelfBenchDamageCommand(10);
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertEquals(40, b1.getHpActual());
        assertEquals(50, b2.getHpActual());
    }

    @Test
    void comandoDamageOpponentBenchedAplicaDanioCorrectoALaBancaEnemiga() {
        Partida partida = partidaBasica();
        CartaEnJuego b1 = new CartaEnJuego(card("b1-1", "Bulbasaur", "50"));
        CartaEnJuego b2 = new CartaEnJuego(card("b2-1", "Squirtle", "60"));
        partida.getBot().getBanca().add(b1);
        partida.getBot().getBanca().add(b2);

        BattleCommand cmd = new DamageOpponentBenchedCommand(20, 2);
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        // Para el jugador, se crea una acción pendiente
        assertEquals("CHOOSE_OPPONENT_BENCH_TO_DAMAGE", partida.getPendingAction().getType());
        assertEquals(2, partida.getPendingAction().getMinSelections());
        
        // Ahora probamos el bot atacando al jugador
        partida.setPendingAction(null);
        partida.getJugador().getBanca().add(b1);
        partida.getJugador().getBanca().add(b2);
        
        cmd.execute(partida, partida.getBot(), partida.getJugador());
        
        // El bot hace daño directamente a ambos porque count = 2
        assertEquals(30, b1.getHpActual());
        assertEquals(40, b2.getHpActual());
    }

    @Test
    void comandoShuffleRandomHandToDeckShufflesCardIntoOpponentsDeck() {
        Partida partida = partidaBasica();
        Card handCard = card("hc1", "Trainer's Mail", "0");
        partida.getBot().getMano().add(handCard);
        int initialDeckSize = partida.getBot().getMazo().size();

        BattleCommand cmd = new ShuffleRandomHandToDeckCommand();
        cmd.execute(partida, partida.getJugador(), partida.getBot());

        assertTrue(partida.getBot().getMano().isEmpty());
        assertEquals(initialDeckSize + 1, partida.getBot().getMazo().size());
        assertTrue(partida.getBot().getMazo().contains(handCard));
        assertTrue(partida.getTurnLogs().stream().anyMatch(log -> log.startsWith("ASTONISH_REVEALED:")));
    }

    @Test
    void resolveAttackConEnergyGlideActivaDecisionDeCambioAlAcoplar() {
        Partida partida = partidaBasica();
        partida.setJugadorUsername("Pablo");

        Card lightning = card("lightning-1", "Lightning Energy", "0");
        lightning.setSupertype("Energy");
        lightning.setTipo("Lightning");
        partida.getJugador().getMazo().add(lightning);

        CartaEnJuego suplente = new CartaEnJuego(card("suplente-1", "Bulbasaur", "50"));
        partida.getJugador().getBanca().add(suplente);

        Ataque ataque = attack(
                "Energy Glide",
                10,
                "Search your deck for a Lightning Energy card and attach it to this Pokémon. Shuffle your deck afterward. If you attached Energy in this way, switch this Pokémon with 1 of your Benched Pokémon."
        );

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {}, null);

        assertEquals(Partida.Fase.ESPERANDO_INTERACCION, partida.getFaseActual());
        assertEquals("SEARCH_DECK", partida.getPendingAction().getType());
        assertEquals("ATTACH_ACTIVE_AND_SWITCH", partida.getPendingAction().getDestination());
    }

    private Partida partidaBasica() {
        TableroJugador jugador = new TableroJugador();
        jugador.setActivo(new CartaEnJuego(card("p1", "Pikachu", "60")));

        TableroJugador bot = new TableroJugador();
        bot.setActivo(new CartaEnJuego(card("p2", "Charmander", "50")));

        return new Partida(jugador, bot);
    }

    private Card card(String id, String nombre, String hp) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        return card;
    }

    private Ataque attack(String nombre, int danio, String texto) {
        Ataque ataque = new Ataque();
        ataque.setNombre(nombre);
        ataque.setDanio(danio);
        ataque.setTexto(texto);
        ataque.setTiposEnergia(List.of());
        return ataque;
    }

    @Test
    void resolveAttackConLeafMunchAplicaMasDanioSiElDefensorEsTipoGrass() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        defensor.getCard().setTipo("Grass");

        Ataque ataque = attack(
                "Leaf Munch",
                10,
                "If your opponent's Active Pokémon is a Grass Pokémon, this attack does 20 more damage."
        );

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(30, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackConLeafMunchNoAplicaMasDanioSiElDefensorNoEsTipoGrass() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        defensor.getCard().setTipo("Fire");

        Ataque ataque = attack(
                "Leaf Munch",
                10,
                "If your opponent's Active Pokémon is a Grass Pokémon, this attack does 20 more damage."
        );

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(10, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackConTormentBloqueaAtaqueElegido() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        
        // Give defensor two attacks
        Ataque att1 = attack("Scratch", 10, "");
        Ataque att2 = attack("Ember", 30, "");
        defensor.getCard().reemplazarAtaques(List.of(att1, att2));

        Ataque torment = attack(
                "Torment",
                20,
                "Choose 1 of your opponent's Active Pokemon's attacks. That Pokemon can't use that attack during your opponent's next turn."
        );
        atacante.getCard().reemplazarAtaques(List.of(torment));

        assertEquals("CHOOSE_OPPONENT_ATTACK", torment.getInteractionType());
        assertNotNull(torment.getInteractionPrompt());

        service.resolveAttack(partida, torment, atacante, defensor, (p, a, d) -> {}, "Ember");

        assertEquals("Ember", defensor.getAtaqueBloqueadoSiguienteTurno());
        assertTrue(defensor.isAtaqueBloqueadoYaConsumido());
    }

    @Test
    void resolveAttackConTormentConComillasCurvasIdentificaInteraccionCorrectamente() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Ataque torment = attack(
                "Torment",
                20,
                "Choose 1 of your opponent’s Active Pokémon’s attacks. That Pokémon can’t use that attack during your opponent’s next turn."
        );
        atacante.getCard().reemplazarAtaques(List.of(torment));

        assertEquals("CHOOSE_OPPONENT_ATTACK", torment.getInteractionType());
        assertNotNull(torment.getInteractionPrompt());
    }

    @Test
    void resolveAttackConChargeDashAplicaRecoilYExtraDanio() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Ataque attack = attack(
                "Charge Dash",
                70,
                "You may do 20 more damage. If you do, this Pokemon does 20 damage to itself."
        );
        atacante.getCard().reemplazarAtaques(List.of(attack));

        assertEquals("YES_NO_PROMPT", attack.getInteractionType());
        assertNotNull(attack.getInteractionPrompt());

        // choice = yes
        BattleAttackService.AttackResolution resYes =
                service.resolveAttack(partida, attack, atacante, defensor, (p, a, d) -> {}, "yes");

        assertEquals(90, resYes.resultado().danioFinal());
        assertEquals(40, atacante.getHpActual()); // 60 - 20 = 40

        // Reset and choice = no
        atacante.setHpActual(60);
        defensor.setHpActual(50);
        BattleAttackService.AttackResolution resNo =
                service.resolveAttack(partida, attack, atacante, defensor, (p, a, d) -> {}, "no");

        assertEquals(70, resNo.resultado().danioFinal());
        assertEquals(60, atacante.getHpActual()); // No recoil
    }

    @Test
    void resolveAttackConMagmaMantleSumaDanioSiEsEnergiaFuego() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        // Setup top of deck with Fire Energy
        Card fireEnergy = new Card();
        fireEnergy.setId("e1");
        fireEnergy.setNombre("Fire Energy");
        fireEnergy.setSupertype("Energy");
        partida.getJugador().getMazo().add(0, fireEnergy);

        Ataque attack = attack(
                "Magma Mantle",
                30,
                "You may discard the top card of your deck. If that card is a Fire Energy card, this attack does 50 more damage."
        );
        atacante.getCard().reemplazarAtaques(List.of(attack));

        assertEquals("YES_NO_PROMPT", attack.getInteractionType());
        assertNotNull(attack.getInteractionPrompt());

        // choice = yes
        BattleAttackService.AttackResolution resYes =
                service.resolveAttack(partida, attack, atacante, defensor, (p, a, d) -> {}, "yes");

        assertEquals(80, resYes.resultado().danioFinal());
        assertTrue(partida.getJugador().getPilaDescarte().contains(fireEnergy));

        // Reset and choice = no
        partida.getJugador().getPilaDescarte().clear();
        partida.getJugador().getMazo().add(0, fireEnergy);
        defensor.setHpActual(50);
        BattleAttackService.AttackResolution resNo =
                service.resolveAttack(partida, attack, atacante, defensor, (p, a, d) -> {}, "no");

        assertEquals(30, resNo.resultado().danioFinal());
        assertFalse(partida.getJugador().getPilaDescarte().contains(fireEnergy));
    }

    @Test
    void resolveAttackConFlamethrowerDescartaEnergiaFuego() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Card fireEnergy = new Card();
        fireEnergy.setId("e1");
        fireEnergy.setNombre("Fire Energy");
        fireEnergy.setSupertype("Energy");
        atacante.getEnergiasUnidas().add(fireEnergy);

        Ataque attack = attack(
                "Flamethrower",
                90,
                "Discard a Fire Energy attached to this Pokemon."
        );

        service.resolveAttack(partida, attack, atacante, defensor, (p, a, d) -> {}, null);

        assertTrue(atacante.getEnergiasUnidas().isEmpty());
        assertTrue(partida.getJugador().getPilaDescarte().contains(fireEnergy));
    }

    @Test
    void resolveAttackConHardenPrevieneDanioMenorOIgual() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Ataque harden = attack(
                "Harden",
                0,
                "During your opponent's next turn, if this Pokemon would be damaged by an attack, prevent that attack's damage done to this Pokemon if that damage is 60 or less."
        );
        atacante.getCard().reemplazarAtaques(List.of(harden));

        service.resolveAttack(partida, harden, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(60, atacante.getPreventDamageThreshold());
        assertFalse(atacante.isPreventDamageThresholdYaConsumido());

        Ataque weakAttack = attack("Scratch", 30, "");
        BattleAttackService.AttackResolution res1 =
                service.resolveAttack(partida, weakAttack, defensor, atacante, (p, a, d) -> {}, null);

        assertEquals(0, res1.resultado().danioFinal());
        assertEquals(60, atacante.getHpActual());

        Ataque strongAttack = attack("Heavy Kick", 80, "");
        BattleAttackService.AttackResolution res2 =
                service.resolveAttack(partida, strongAttack, defensor, atacante, (p, a, d) -> {}, null);

        assertEquals(80, res2.resultado().danioFinal());
    }

    @Test
    void resolveAttackConDevastatingWindShufflesHandAndDrawsCards() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Card card1 = card("c1", "Card 1", "0");
        Card card2 = card("c2", "Card 2", "0");
        Card card3 = card("c3", "Card 3", "0");
        partida.getBot().getMano().addAll(List.of(card1, card2, card3));

        for (int i = 0; i < 5; i++) {
            partida.getBot().getMazo().add(card("d" + i, "DeckCard " + i, "0"));
        }

        Ataque devastatingWind = attack(
                "Devastating Wind",
                0,
                "Your opponent shuffles his or her hand into his or her deck and draws 4 cards."
        );

        service.resolveAttack(partida, devastatingWind, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(4, partida.getBot().getMano().size());
        assertEquals(4, partida.getBot().getMazo().size());
    }

    @Test
    void resolveAttackConFlareBlitzDescartaTodasLasEnergiasFuego() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Card fire1 = card("f1", "Fire Energy", "0");
        fire1.setSupertype("Energy");
        fire1.setTipo("Fire");

        Card fire2 = card("f2", "Fire Energy", "0");
        fire2.setSupertype("Energy");
        fire2.setTipo("Fire");

        Card colorless = card("c1", "Double Colorless Energy", "0");
        colorless.setSupertype("Energy");
        colorless.setTipo("Colorless");

        atacante.getEnergiasUnidas().addAll(List.of(fire1, fire2, colorless));

        Ataque flareBlitz = attack(
                "Flare Blitz",
                100,
                "Discard all Fire Energy attached to this Pokemon."
        );

        service.resolveAttack(partida, flareBlitz, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(1, atacante.getEnergiasUnidas().size());
        assertSame(colorless, atacante.getEnergiasUnidas().get(0));
        assertTrue(partida.getJugador().getPilaDescarte().contains(fire1));
        assertTrue(partida.getJugador().getPilaDescarte().contains(fire2));
    }

    @Test
    void resolveAttackStarmieRecoverDiscardsAndHealsAll() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        atacante.setHpActual(10); // Damaged
        Card fire = card("f-1", "Fire Energy", "0");
        fire.setSupertype("Energy");
        fire.setTipo("Fire");
        atacante.getEnergiasUnidas().add(fire);

        Ataque recover = attack(
                "Recover",
                0,
                "Discard an Energy attached to this Pokemon and heal all damage from it."
        );

        service.resolveAttack(partida, recover, atacante, defensor, (p, a, d) -> {}, null);

        int maxHp = Integer.parseInt(atacante.getCard().getHp());
        assertEquals(maxHp, atacante.getHpActual()); // Fully healed
        assertTrue(atacante.getEnergiasUnidas().isEmpty()); // Energy discarded
        assertTrue(partida.getJugador().getPilaDescarte().contains(fire));
    }

    @Test
    void resolveAttackStarmieCoreSplashConditionalDamage() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Ataque coreSplash = attack(
                "Core Splash",
                60,
                "If this Pokemon has any Psychic Energy attached to it, this attack does 30 more damage."
        );

        // Without psychic energy
        var res1 = service.resolveAttack(partida, coreSplash, atacante, defensor, (p, a, d) -> {}, null);
        assertEquals(60, res1.resultado().danioFinal());

        // With psychic energy
        Card psychic = card("psy-1", "Psychic Energy", "0");
        psychic.setSupertype("Energy");
        psychic.setTipo("Psychic");
        atacante.getEnergiasUnidas().add(psychic);

        var res2 = service.resolveAttack(partida, coreSplash, atacante, defensor, (p, a, d) -> {}, null);
        assertEquals(90, res2.resultado().danioFinal()); // 60 + 30
    }

    @Test
    void resolveAttackLaprasSeafaringCoinFlipsAndAttaches() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        // Setup benched Pokemon
        CartaEnJuego benched = new CartaEnJuego(card("b-1", "Pikachu", "60"));
        partida.getJugador().getBanca().add(benched);

        // Water energies in discard pile
        Card water1 = card("w-1", "Water Energy", "0");
        water1.setSupertype("Energy");
        water1.setTipo("Water");
        Card water2 = card("w-2", "Water Energy", "0");
        water2.setSupertype("Energy");
        water2.setTipo("Water");
        partida.getJugador().getPilaDescarte().addAll(List.of(water1, water2));

        Ataque seafaring = attack(
                "Seafaring",
                0,
                "Flip 3 coins. For each heads, attach a Water Energy card from your discard pile to your Benched Pokemon in any way you like."
        );

        // Since coin flips are random, let's execute and check the behavior based on how many heads were rolled
        service.resolveAttack(partida, seafaring, atacante, defensor, (p, a, d) -> {}, null);

        int heads = (int) partida.getUltimasMonedasLanzadas().stream().filter(h -> h).count();
        int expectedSelectable = Math.min(heads, 2);
        assertEquals(0, benched.getEnergiasUnidas().size());
        assertEquals(2, partida.getJugador().getPilaDescarte().size());
        if (expectedSelectable > 0) {
            assertNotNull(partida.getPendingAction());
            assertEquals("ATTACH_DISCARD_ENERGY_TO_BENCH", partida.getPendingAction().getType());
            assertEquals(expectedSelectable, partida.getPendingAction().getMaxSelections());
            assertTrue(partida.getPendingAction().isEndsTurn());
        } else {
            assertNull(partida.getPendingAction());
        }
    }

    @Test
    void resolveAttackLaprasHydroPumpScalesWithWaterEnergy() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Ataque hydroPump = attack(
                "Hydro Pump",
                10,
                "This attack does 20 more damage for each Water Energy attached to this Pokemon."
        );

        // 0 water energy
        var res1 = service.resolveAttack(partida, hydroPump, atacante, defensor, (p, a, d) -> {}, null);
        assertEquals(10, res1.resultado().danioFinal());

        // 2 water energy
        Card water1 = card("w-1", "Water Energy", "0");
        water1.setSupertype("Energy");
        water1.setTipo("Water");
        Card water2 = card("w-2", "Water Energy", "0");
        water2.setSupertype("Energy");
        water2.setTipo("Water");
        atacante.getEnergiasUnidas().addAll(List.of(water1, water2));

        var res2 = service.resolveAttack(partida, hydroPump, atacante, defensor, (p, a, d) -> {}, null);
        assertEquals(50, res2.resultado().danioFinal()); // 10 + 20 * 2
    }

    @Test
    void resolveAttackCorsolaRefreshHealsAndClearsStatus() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        atacante.setHpActual(20); // Damaged
        atacante.getCondicionesEspeciales().addAll(List.of("Poisoned", "Asleep"));

        Ataque refresh = attack(


                "Refresh",
                0,
                "Heal 30 damage and remove all Special Conditions from this Pokemon."
        );

        service.resolveAttack(partida, refresh, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(50, atacante.getHpActual()); // 20 + 30
        assertTrue(atacante.getCondicionesEspeciales().isEmpty()); // Cleared
    }

    @Test
    void resolveAttackCorsolaSpinyRushMultiCoins() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Ataque spinyRush = attack(
                "Spiny Rush",
                20,
                "Flip a coin until you get tails. This attack does 20 more damage for each heads."
        );

        var res = service.resolveAttack(partida, spinyRush, atacante, defensor, (p, a, d) -> {}, null);

        int heads = (int) partida.getUltimasMonedasLanzadas().stream().filter(h -> h).count();
        assertEquals(20 + 20 * heads, res.resultado().danioFinal());
    }

    @Test
    void resolveAttackRaichuThunderboltDiscardsAllAttachedEnergy() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Card l1 = card("l-1", "Lightning Energy", "0");
        l1.setSupertype("Energy");
        l1.setTipo("Lightning");
        Card c1 = card("c-1", "Double Colorless Energy", "0");
        c1.setSupertype("Energy");
        c1.setTipo("Colorless");
        atacante.getEnergiasUnidas().addAll(List.of(l1, c1));

        Ataque thunderbolt = attack(
                "Thunderbolt",
                100,
                "Discard all Energy attached to this Pokemon."
        );

        var res = service.resolveAttack(partida, thunderbolt, atacante, defensor, (p, a, d) -> {}, null);
        assertEquals(100, res.resultado().danioFinal());
        assertTrue(atacante.getEnergiasUnidas().isEmpty());
    }

    @Test
    void resolveAttackFroakieBounceHeadsSwitchesActive() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        // Setup benched Pokemon
        CartaEnJuego benched = new CartaEnJuego(card("b-1", "Pikachu", "60"));
        partida.getJugador().getBanca().add(benched);

        Ataque bounce = attack(
                "Bounce",
                10,
                "Flip a coin. If heads, switch this Pokemon with 1 of your Benched Pokemon."
        );

        boolean heads = false;
        for (int i = 0; i < 20; i++) {
            partida.getUltimasMonedasLanzadas().clear();
            partida.setPendingAction(null);
            service.resolveAttack(partida, bounce, atacante, defensor, (p, a, d) -> {}, null);
            if (partida.getUltimasMonedasLanzadas().get(0)) {
                heads = true;
                break;
            }
        }
        assertTrue(heads, "Should have got heads eventually in 20 attempts");
        assertNotNull(partida.getPendingAction());
        assertEquals("SWITCH_ACTIVE", partida.getPendingAction().getType());
        assertEquals(1, partida.getPendingAction().getMinSelections());
        assertEquals(1, partida.getPendingAction().getMaxSelections());
        assertTrue(partida.getPendingAction().isEndsTurn());
    }

    @Test
    void resolveAttackFroakieBounceTailsDoesNotSwitch() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        CartaEnJuego benched = new CartaEnJuego(card("b-1", "Pikachu", "60"));
        partida.getJugador().getBanca().add(benched);

        Ataque bounce = attack(
                "Bounce",
                10,
                "Flip a coin. If heads, switch this Pokemon with 1 of your Benched Pokemon."
        );

        boolean tails = false;
        for (int i = 0; i < 20; i++) {
            partida.getUltimasMonedasLanzadas().clear();
            partida.setPendingAction(null);
            service.resolveAttack(partida, bounce, atacante, defensor, (p, a, d) -> {}, null);
            if (!partida.getUltimasMonedasLanzadas().get(0)) {
                tails = true;
                break;
            }
        }
        assertTrue(tails, "Should have got tails eventually in 20 attempts");
        assertNull(partida.getPendingAction());
    }

    @Test
    void resolveAttackElectrodeEerieImpulseHeadsPromptsDiscardOpponentEnergy() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        // Give defensor an energy so they have a candidate
        defensor.getEnergiasUnidas().add(card("e-1", "Fire Energy", "Energy"));

        Ataque eerieImpulse = attack(
                "Eerie Impulse",
                0,
                "Flip a coin. If heads, discard an Energy attached to 1 of your opponent's Pokemon."
        );

        boolean heads = false;
        for (int i = 0; i < 20; i++) {
            partida.getUltimasMonedasLanzadas().clear();
            partida.setPendingAction(null);
            service.resolveAttack(partida, eerieImpulse, atacante, defensor, (p, a, d) -> {}, null);
            if (partida.getUltimasMonedasLanzadas().get(0)) {
                heads = true;
                break;
            }
        }
        assertTrue(heads, "Should have got heads eventually in 20 attempts");
        assertNotNull(partida.getPendingAction());
        assertEquals("DISCARD_OPPONENT_ENERGY", partida.getPendingAction().getType());
        assertEquals(1, partida.getPendingAction().getMinSelections());
        assertEquals(1, partida.getPendingAction().getMaxSelections());
        assertEquals(1, partida.getPendingAction().getOptions().size());
        assertEquals(defensor.getCard().getId(), partida.getPendingAction().getOptions().get(0).getId());
    }

    @Test
    void resolveAttackElectrodeEerieImpulseTailsDoesNotDiscard() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        defensor.getEnergiasUnidas().add(card("e-1", "Fire Energy", "Energy"));

        Ataque eerieImpulse = attack(
                "Eerie Impulse",
                0,
                "Flip a coin. If heads, discard an Energy attached to 1 of your opponent's Pokemon."
        );

        boolean tails = false;
        for (int i = 0; i < 20; i++) {
            partida.getUltimasMonedasLanzadas().clear();
            partida.setPendingAction(null);
            service.resolveAttack(partida, eerieImpulse, atacante, defensor, (p, a, d) -> {}, null);
            if (!partida.getUltimasMonedasLanzadas().get(0)) {
                tails = true;
                break;
            }
        }
        assertTrue(tails, "Should have got tails eventually in 20 attempts");
        assertNull(partida.getPendingAction());
    }

    @Test
    void resolveAttackEmolgaEXElectronCrushYesDiscardsAndDealsExtraDamage() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        atacante.getEnergiasUnidas().add(card("e-1", "Fire Energy", "Energy"));

        Ataque electronCrush = attack(
                "Electron Crush",
                60,
                "You may discard an Energy attached to this Pokemon. If you do, this attack does 30 more damage."
        );

        BattleAttackService.AttackResolution res = service.resolveAttack(partida, electronCrush, atacante, defensor, (p, a, d) -> {}, "yes");
        assertEquals(90, res.resultado().danioFinal());
        assertEquals(0, atacante.getEnergiasUnidas().size());
        assertEquals(1, partida.getJugador().getPilaDescarte().size());
    }

    @Test
    void resolveAttackEmolgaEXElectronCrushNoDoesNotDiscard() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        atacante.getEnergiasUnidas().add(card("e-1", "Fire Energy", "Energy"));

        Ataque electronCrush = attack(
                "Electron Crush",
                60,
                "You may discard an Energy attached to this Pokemon. If you do, this attack does 30 more damage."
        );

        BattleAttackService.AttackResolution res = service.resolveAttack(partida, electronCrush, atacante, defensor, (p, a, d) -> {}, "no");
        assertEquals(60, res.resultado().danioFinal());
        assertEquals(1, atacante.getEnergiasUnidas().size());
        assertEquals(0, partida.getJugador().getPilaDescarte().size());
    }

    @Test
    void resolveAttackGrumpigTrickyStepsYesPromptsMoveEnergy() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        defensor.getEnergiasUnidas().add(card("e-1", "Fire Energy", "Energy"));
        CartaEnJuego benched = new CartaEnJuego(card("b-1", "Bulbasaur", "50"));
        partida.getBot().getBanca().add(benched);

        Ataque trickySteps = attack(
                "Tricky Steps",
                30,
                "You may move an Energy attached to your opponent's Active Pokemon to 1 of your opponent's Benched Pokemon."
        );

        service.resolveAttack(partida, trickySteps, atacante, defensor, (p, a, d) -> {}, "yes");
        assertNotNull(partida.getPendingAction());
        assertEquals("MOVE_ENERGY_TO_OPPONENT_BENCH", partida.getPendingAction().getType());
        assertEquals(1, partida.getPendingAction().getMinSelections());
        assertEquals(1, partida.getPendingAction().getMaxSelections());
    }

    @Test
    void resolveAttackWhirlipedeContinuousTumbleDealsDamagePerHeads() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        Ataque continuousTumble = attack(
                "Continuous Tumble",
                0,
                "Flip a coin until you get tails. This attack does 30 damage times the number of heads."
        );

        BattleAttackService.AttackResolution res = service.resolveAttack(partida, continuousTumble, atacante, defensor, (p, a, d) -> {}, null);
        List<Boolean> coins = res.historialMonedas();
        assertTrue(coins.size() >= 1);
        assertFalse(coins.get(coins.size() - 1)); // Ends with tails

        long headsCount = coins.stream().filter(c -> c).count();
        assertEquals(headsCount * 30, res.resultado().danioFinal());
    }

    // =================== Daño por Tipo y Modificadores ===================

    @Test
    void resolveAttackAplicaMuscleBandDanioMasVeinte() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        
        Card muscleBand = new Card();
        muscleBand.setId("xy1-121");
        muscleBand.setNombre("Muscle Band");
        muscleBand.setSupertype("Trainer");
        muscleBand.setSubtypes(List.of("Item", "Tool"));
        atacante.setAttachedTools(new java.util.ArrayList<>(List.of(muscleBand)));

        Ataque ataque = attack("Golpe", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(50, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackAplicaDebilidadDanioDuplicado() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        atacante.getCard().setTipo("Fire");
        CartaEnJuego defensor = partida.getBot().getActivo();
        
        com.pokemon.tcg.model.CardAttribute debilidad = new com.pokemon.tcg.model.CardAttribute();
        debilidad.setType("Fire");
        defensor.getCard().setDebilidades(new java.util.ArrayList<>(List.of(debilidad)));

        Ataque ataque = attack("Ember", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(60, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackAplicaResistenciaDanioReducido() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        atacante.getCard().setTipo("Water");
        CartaEnJuego defensor = partida.getBot().getActivo();
        
        com.pokemon.tcg.model.CardAttribute resistencia = new com.pokemon.tcg.model.CardAttribute();
        resistencia.setType("Water");
        defensor.getCard().setResistencias(new java.util.ArrayList<>(List.of(resistencia)));

        Ataque ataque = attack("Water Splash", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(10, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackAplicaHardCharmDanioReducido() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        
        Card hardCharm = new Card();
        hardCharm.setId("xy1-119");
        hardCharm.setNombre("Hard Charm");
        hardCharm.setSupertype("Trainer");
        hardCharm.setSubtypes(List.of("Item", "Tool"));
        defensor.setAttachedTools(new java.util.ArrayList<>(List.of(hardCharm)));

        Ataque ataque = attack("Golpe", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(10, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackShadowCircleConEnergiaDarknessIgnoraDebilidad() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        atacante.getCard().setTipo("Fire");
        
        CartaEnJuego defensor = partida.getBot().getActivo();
        com.pokemon.tcg.model.CardAttribute debilidad = new com.pokemon.tcg.model.CardAttribute();
        debilidad.setType("Fire");
        defensor.getCard().setDebilidades(new java.util.ArrayList<>(List.of(debilidad)));

        Card darkEnergy = new Card();
        darkEnergy.setId("d-1");
        darkEnergy.setNombre("Darkness Energy");
        darkEnergy.setSupertype("Energy");
        darkEnergy.setTipo("Darkness");
        defensor.getEnergiasUnidas().add(darkEnergy);

        Card shadowCircle = new Card();
        shadowCircle.setId("xy1-126");
        shadowCircle.setNombre("Shadow Circle");
        partida.setActiveStadium(shadowCircle);

        Ataque ataque = attack("Ember", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(30, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackConTailspinPiledriverNoAplicaExtraDanioSiDefensorNoTieneDanio() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        defensor.setHpActual(50); // HP is full (Charmander has 50 HP)

        Ataque tailspin = attack(
                "Tailspin Piledriver",
                80,
                "If your opponent's Active Pokémon already has any damage counters on it, this attack does 40 more damage."
        );

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, tailspin, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(80, resolution.resultado().danioFinal());
    }

    @Test
    void resolveAttackConTailspinPiledriverAplicaExtraDanioSiDefensorTieneDanio() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        defensor.setHpActual(30); // Damaged (Charmander has 50 HP max, so it has 2 damage counters)

        Ataque tailspin = attack(
                "Tailspin Piledriver",
                80,
                "If your opponent's Active Pokémon already has any damage counters on it, this attack does 40 more damage."
        );

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, tailspin, atacante, defensor, (p, a, d) -> {}, null);

        assertEquals(120, resolution.resultado().danioFinal()); // 80 + 40 = 120
    }

    @Test
    void resolveAttackConMentalTrashEjecutaYDescartaCartasPorCadaCruz() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        // Agregamos 4 cartas a la mano del bot
        partida.getBot().getMano().add(card("c1", "Carta 1", "0"));
        partida.getBot().getMano().add(card("c2", "Carta 2", "0"));
        partida.getBot().getMano().add(card("c3", "Carta 3", "0"));
        partida.getBot().getMano().add(card("c4", "Carta 4", "0"));

        Ataque mentalTrash = attack(
                "Mental Trash",
                0,
                "Your opponent flips 4 coins. For each tails, he or she discards a card from his or her hand."
        );

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, mentalTrash, atacante, defensor, (p, a, d) -> {}, null);

        // Verificamos que se lanzaron 4 monedas
        assertEquals(4, resolution.historialMonedas().size());

        // Contamos cuántas salieron cruz (false)
        long cruces = resolution.historialMonedas().stream().filter(b -> !b).count();

        // El número de cartas descartadas debe ser igual a la cantidad de cruces
        assertEquals(cruces, partida.getBot().getPilaDescarte().size());
        assertEquals(4 - cruces, partida.getBot().getMano().size());
    }
}
