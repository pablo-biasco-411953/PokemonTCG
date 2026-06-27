package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ComandoUnirEnergia implements ComandoTurno {

    private final CartaEnJuego objetivo;
    private final Card energia;
    private final TableroJugador tablero;
    private final String selectedType;

    public ComandoUnirEnergia(CartaEnJuego objetivo, Card energia, TableroJugador tablero, String selectedType) {
        this.objetivo = objetivo;
        this.energia = energia;
        this.tablero = tablero;
        this.selectedType = selectedType;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        return !partida.isYaSeUnioEnergiaEsteTurno()
                && objetivo != null
                && energia != null
                && tablero.getMano().contains(energia);
    }

    @Override
    public void ejecutar(Partida partida) {
        if (objetivo == null) throw new IllegalArgumentException("Pokémon objetivo no encontrado.");
        if (energia == null) throw new IllegalArgumentException("Energía no encontrada en la mano.");

        if (partida.isYaSeUnioEnergiaEsteTurno()) {
            throw new IllegalStateException("Solo podes unir 1 Energia por turno.");
        }

        if ("Rainbow Energy".equals(energia.getNombre()) && selectedType != null && !selectedType.isBlank()) {
            energia.setTipo(selectedType);
            // Apply 10 damage to the Pokémon that Rainbow Energy is attached to (1 damage counter)
            objetivo.setHpActual(objetivo.getHpActual() - 10);
            if (objetivo.getHpActual() <= 0) {
                // Let the KO loop handle it later, or it stays at 0 until turn checks.
                // In Pokemon TCG, placing a damage counter from Rainbow Energy can KO the Pokemon immediately.
            }
        }

        objetivo.getEnergiasUnidas().add(energia);
        tablero.getMano().remove(energia);
        partida.setYaSeUnioEnergiaEsteTurno(true);
    }

    @Override
    public String getNombre() { return "UnirEnergia[" + energia.getNombre() + "]"; }
}
