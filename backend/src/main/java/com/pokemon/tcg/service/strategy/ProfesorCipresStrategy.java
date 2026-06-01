package com.pokemon.tcg.service.strategy;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ProfesorCipresStrategy implements EfectoTrainerStrategy {
    @Override
    public void ejecutar(Partida partida, TableroJugador jugador, Object target) {
        // 1. Discard hand
        jugador.getPilaDescarte().addAll(jugador.getMano());
        jugador.getMano().clear();
        
        // 2. Draw 7 cards
        for (int i = 0; i < 7; i++) {
            if (!jugador.getMazo().isEmpty()) {
                jugador.getMano().add(jugador.getMazo().remove(0));
            }
        }
        System.out.println("🃏 [EFECTO] Profesor Ciprés ejecutado: mano descartada y se robaron 7 cartas.");
    }
}
