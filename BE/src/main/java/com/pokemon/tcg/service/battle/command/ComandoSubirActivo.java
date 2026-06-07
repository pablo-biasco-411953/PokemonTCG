package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ComandoSubirActivo implements ComandoTurno {

    private final String cartaIdEnBanca;
    private final TableroJugador tablero;

    public ComandoSubirActivo(String cartaIdEnBanca, TableroJugador tablero) {
        this.cartaIdEnBanca = cartaIdEnBanca;
        this.tablero = tablero;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        return tablero.getActivo() == null && !tablero.getBanca().isEmpty();
    }

    @Override
    public void ejecutar(Partida partida) {
        if (tablero.getActivo() != null) {
            throw new IllegalStateException("Ya tenés un Pokémon activo. Debés retirarlo primero.");
        }

        CartaEnJuego elegido = tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(cartaIdEnBanca))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pokémon no encontrado en la banca."));

        tablero.getBanca().remove(elegido);
        tablero.setActivo(elegido);
        System.out.println("🚀 " + elegido.getCard().getNombre() + " ahora es tu Pokémon activo.");
    }

    @Override
    public String getNombre() { return "SubirActivo"; }
}
