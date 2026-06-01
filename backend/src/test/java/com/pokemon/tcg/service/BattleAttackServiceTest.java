package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.PokemonCard;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleAttackServiceTest {

    private final BattleAttackService service = new BattleAttackService();

    @Test
    void resolveAttackAplicaDanioBaseYRegistraHistorialVacioSinMonedas() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();
        Ataque ataque = attack("Golpe", 30, "");

        BattleAttackService.AttackResolution resolution =
                service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {});

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

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> {});

        assertEquals(50, atacante.getHpActual());
        assertEquals(1, partida.getJugador().getMano().size());
    }

    @Test
    void resolveAttackAplicaVenenoYBloqueoDeRetiradaSinAzar() {
        Partida partida = partidaBasica();
        Ataque ataque = attack(
                "Trampa Toxica",
                10,
                "The Defending Pokémon is now Poisoned. The Defending Pokémon can't retreat during your opponent's next turn."
        );

        service.resolveAttack(partida, ataque, partida.getJugador().getActivo(), partida.getBot().getActivo(), (p, a, d) -> {});

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

        service.resolveAttack(partida, ataque, atacante, partida.getBot().getActivo(), (p, a, d) -> koInvocado.set(true));

        assertEquals(0, atacante.getHpActual());
        assertTrue(koInvocado.get());
    }

    private Partida partidaBasica() {
        TableroJugador jugador = new TableroJugador();
        jugador.setActivo(new CartaEnJuego(card("p1", "Pikachu", "60")));

        TableroJugador bot = new TableroJugador();
        bot.setActivo(new CartaEnJuego(card("p2", "Charmander", "50")));

        return new Partida(jugador, bot);
    }

    private Card card(String id, String nombre, String hp) {
        PokemonCard card = new PokemonCard();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        return card;
    }

    private Card energy(String id, String nombre, String tipo, String supertype) {
        com.pokemon.tcg.model.EnergyCard card = new com.pokemon.tcg.model.EnergyCard();
        card.setId(id);
        card.setNombre(nombre);
        card.setTipo(tipo);
        card.setSupertype(supertype);
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
    void resolveAttackValidaCorrectamenteCostoEnergia() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        // Ataque que requiere 1 Planta y 2 Incoloras (Grass, Colorless, Colorless)
        Ataque ataque = attack("Jungle Hammer", 90, "");
        ataque.setTiposEnergia(List.of("Grass", "Colorless", "Colorless"));

        // Caso 1: Sin energías attached -> Debe fallar
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {});
        });

        // Caso 2: Energías insuficientes (Solo 1 Planta y 1 Fuego -> 2 símbolos) -> Debe fallar
        atacante.getEnergiasUnidas().add(energy("e1", "Grass Energy", "Energy", "Energy"));
        atacante.getEnergiasUnidas().add(energy("e2", "Fire Energy", "Energy", "Energy"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {});
        });

        // Caso 3: Energías suficientes (1 Planta, 1 Fuego, 1 Lucha -> 3 símbolos) -> Debe pasar
        atacante.getEnergiasUnidas().add(energy("e3", "Fighting Energy", "Energy", "Energy"));
        // El HP inicial del defensor es 50 (Charmander). El daño es 90, por lo tanto queda en 0.
        service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {});
        assertEquals(0, defensor.getHpActual());
    }

    @Test
    void resolveAttackValidaCostoConDobleIncoloraYRainbow() {
        Partida partida = partidaBasica();
        CartaEnJuego atacante = partida.getJugador().getActivo();
        CartaEnJuego defensor = partida.getBot().getActivo();

        // Ataque que requiere 1 Planta y 2 Incoloras (Grass, Colorless, Colorless)
        Ataque ataque = attack("Jungle Hammer", 90, "");
        ataque.setTiposEnergia(List.of("Grass", "Colorless", "Colorless"));

        // Caso 1: Rainbow Energy (Grass) + Double Colorless Energy (Colorless, Colorless) -> Debe pasar
        atacante.getEnergiasUnidas().add(energy("e1", "Rainbow Energy", "Energy", "Energy"));
        atacante.getEnergiasUnidas().add(energy("e2", "Double Colorless Energy", "Energy", "Energy"));

        service.resolveAttack(partida, ataque, atacante, defensor, (p, a, d) -> {});
        assertEquals(0, defensor.getHpActual());
    }
}
