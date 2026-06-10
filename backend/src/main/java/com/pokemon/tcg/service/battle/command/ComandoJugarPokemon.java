package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ComandoJugarPokemon implements ComandoTurno {

    private final Card carta;
    private final TableroJugador tablero;

    public ComandoJugarPokemon(Card carta, TableroJugador tablero) {
        this.carta = carta;
        this.tablero = tablero;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        return tablero.getMano().contains(carta);
    }

    @Override
    public void ejecutar(Partida partida) {
        if (carta == null) throw new IllegalArgumentException("La carta no está en tu mano.");

        CartaEnJuego nuevoPokemon = new CartaEnJuego(carta);

        if (tablero.getActivo() == null) {
            tablero.setActivo(nuevoPokemon);
            tablero.getMano().remove(carta);
            System.out.println("✅ " + carta.getNombre() + " entró como Activo.");
        } else {
            if (tablero.getBanca().size() >= 5) {
                throw new IllegalStateException("La banca está llena (máximo 5).");
            }
            tablero.getBanca().add(nuevoPokemon);
            tablero.getMano().remove(carta);
            System.out.println("✅ " + carta.getNombre() + " se unió a la banca.");
        }
    }

    @Override
    public String getNombre() { return "JugarPokemon[" + carta.getNombre() + "]"; }
}
