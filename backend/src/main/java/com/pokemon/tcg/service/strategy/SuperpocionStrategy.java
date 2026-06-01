package com.pokemon.tcg.service.strategy;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class SuperpocionStrategy implements EfectoTrainerStrategy {
    @Override
    public void ejecutar(Partida partida, TableroJugador jugador, Object target) {
        if (!(target instanceof CartaEnJuego pokemon)) {
            throw new IllegalArgumentException("El objetivo de la Superpoción debe ser un Pokémon en juego.");
        }

        // Heal up to 60 HP
        int hpMaximo;
        try {
            hpMaximo = Integer.parseInt(pokemon.getCard().getHp());
        } catch (NumberFormatException e) {
            hpMaximo = 0;
        }
        
        int nuevoHp = Math.min(hpMaximo, pokemon.getHpActual() + 60);
        int curado = nuevoHp - pokemon.getHpActual();
        pokemon.setHpActual(nuevoHp);

        // Discard 1 energy attached to this Pokémon
        if (!pokemon.getEnergiasUnidas().isEmpty()) {
            Card energiaDescartada = pokemon.getEnergiasUnidas().remove(0);
            jugador.getPilaDescarte().add(energiaDescartada);
            System.out.println("💖 [EFECTO] Superpoción curó " + curado + " HP a " + pokemon.getCard().getNombre() + " y descartó la energía: " + energiaDescartada.getNombre());
        } else {
            System.out.println("💖 [EFECTO] Superpoción curó " + curado + " HP a " + pokemon.getCard().getNombre() + " (no tenía energías unidas).");
        }
    }
}
