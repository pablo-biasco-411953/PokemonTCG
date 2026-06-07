package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

/**
 * Patrón Command: Encapsula una acción en la batalla.
 */
public interface BattleCommand {
    /**
     * Ejecuta el comando sobre la partida actual.
     * @param partida El estado actual de la partida
     * @param atacante El tablero del jugador atacante
     * @param defensor El tablero del jugador defensor
     */
    void execute(Partida partida, TableroJugador atacante, TableroJugador defensor);
}
