package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ComandoEvolucionar implements ComandoTurno {

    private final Card cartaEvolucion;
    private final CartaEnJuego objetivo;
    private final TableroJugador tablero;

    public ComandoEvolucionar(Card cartaEvolucion, CartaEnJuego objetivo, TableroJugador tablero) {
        this.cartaEvolucion = cartaEvolucion;
        this.objetivo = objetivo;
        this.tablero = tablero;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        if (cartaEvolucion == null || objetivo == null) return false;
        return tablero.getMano().contains(cartaEvolucion);
    }

    @Override
    public void ejecutar(Partida partida) {
        if (cartaEvolucion == null) throw new IllegalArgumentException("La carta de evolución no está en tu mano.");
        if (objetivo == null) throw new IllegalArgumentException("El Pokémon objetivo no está en tu tablero.");

        String evolvesFrom = cartaEvolucion.getEvolvesFrom();
        String nombreObjetivo = objetivo.getCard().getNombre();

        if (evolvesFrom == null || !evolvesFrom.equalsIgnoreCase(nombreObjetivo)) {
            throw new IllegalStateException("¡Evolución inválida! " + cartaEvolucion.getNombre()
                    + " evoluciona de " + evolvesFrom + ", no de " + nombreObjetivo + ".");
        }

        System.out.println("✨ ¡Evolución inminente! " + nombreObjetivo + " → " + cartaEvolucion.getNombre());

        int hpMaximoAnterior = Integer.parseInt(objetivo.getCard().getHp());
        int danioAcumulado = hpMaximoAnterior - objetivo.getHpActual();

        objetivo.setCard(cartaEvolucion);
        int nuevoHpMaximo = Integer.parseInt(cartaEvolucion.getHp());
        objetivo.setHpActual(Math.max(0, nuevoHpMaximo - danioAcumulado));
        objetivo.limpiarCondiciones();

        tablero.getMano().remove(cartaEvolucion);

        System.out.println("✅ Evolución completada. HP actual: " + objetivo.getHpActual() + "/" + nuevoHpMaximo);
    }

    @Override
    public String getNombre() { return "Evolucionar[" + cartaEvolucion.getNombre() + "]"; }
}
