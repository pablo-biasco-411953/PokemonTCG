package com.pokemon.tcg.model.battle.command;

import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Ataque;

import java.util.List;

public class TormentBlockAttackCommand implements BattleCommand {
    private final String chosenAttack;

    public TormentBlockAttackCommand(String chosenAttack) {
        this.chosenAttack = chosenAttack;
    }

    @Override
    public void execute(Partida partida, TableroJugador atacante, TableroJugador defensor) {
        CartaEnJuego activeDefensor = defensor.getActivo();
        if (activeDefensor == null) return;

        List<Ataque> attacks = activeDefensor.getCard().getAtaques();
        if (attacks == null || attacks.isEmpty()) return;

        String attackToBlock = null;
        if (chosenAttack != null && !chosenAttack.trim().isEmpty()) {
            boolean exists = attacks.stream().anyMatch(a -> a.getNombre().equalsIgnoreCase(chosenAttack.trim()));
            if (exists) {
                attackToBlock = chosenAttack.trim();
            }
        }

        // Fallback to first attack if invalid or not specified
        if (attackToBlock == null) {
            attackToBlock = attacks.get(0).getNombre();
        }

        activeDefensor.setAtaqueBloqueadoSiguienteTurno(attackToBlock);
        activeDefensor.setAtaqueBloqueadoYaConsumido(true);
        
        String actor = (atacante == partida.getJugador()) ? "JUGADOR" : "BOT";
        partida.getTurnLogs().add("ATTACK_BLOCKED:" + actor + ":" + attackToBlock);
        System.out.println("🔒 Torment blocked attack: " + attackToBlock + " on defender " + activeDefensor.getCard().getNombre());
    }
}
