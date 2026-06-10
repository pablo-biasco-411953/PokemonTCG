package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ComandoUnirEnergia implements ComandoTurno {

    private final CartaEnJuego objetivo;
    private final Card energia;
    private final TableroJugador tablero;

    public ComandoUnirEnergia(CartaEnJuego objetivo, Card energia, TableroJugador tablero) {
        this.objetivo = objetivo;
        this.energia = energia;
        this.tablero = tablero;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        return objetivo != null && energia != null && tablero.getMano().contains(energia);
    }

    @Override
    public void ejecutar(Partida partida) {
        if (objetivo == null) throw new IllegalArgumentException("Pokémon objetivo no encontrado.");
        if (energia == null) throw new IllegalArgumentException("Energía no encontrada en la mano.");

        objetivo.getEnergiasUnidas().add(energia);
        tablero.getMano().remove(energia);
    }

    @Override
    public String getNombre() { return "UnirEnergia[" + energia.getNombre() + "]"; }
}
