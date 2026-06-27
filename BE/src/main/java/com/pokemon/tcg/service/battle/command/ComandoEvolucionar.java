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
        if (tablero.getTurnosJugados() <= 1) return false;
        if (objetivo.getTurnoEntrada() == partida.getNumeroTurno()) return false;
        if (objetivo.getUltimoTurnoEvolucionado() == partida.getNumeroTurno()) return false;
        return tablero.getMano().contains(cartaEvolucion);
    }

    @Override
    public void ejecutar(Partida partida) {
        if (cartaEvolucion == null) throw new IllegalArgumentException("La carta de evolución no está en tu mano.");
        if (objetivo == null) throw new IllegalArgumentException("El Pokémon objetivo no está en tu tablero.");

        if (tablero.getTurnosJugados() <= 1) {
            throw new IllegalStateException("No podés evolucionar Pokémon en el primer turno de la partida.");
        }
        if (objetivo.getTurnoEntrada() == partida.getNumeroTurno()) {
            throw new IllegalStateException("No podés evolucionar un Pokémon en el mismo turno en el que entra en juego.");
        }
        if (objetivo.getUltimoTurnoEvolucionado() == partida.getNumeroTurno()) {
            throw new IllegalStateException("No podés evolucionar el mismo Pokémon más de una vez en el mismo turno.");
        }

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
        objetivo.setUltimoTurnoEvolucionado(partida.getNumeroTurno());

        tablero.getMano().remove(cartaEvolucion);

        System.out.println("✅ Evolución completada. HP actual: " + objetivo.getHpActual() + "/" + nuevoHpMaximo);
    }

    @Override
    public String getNombre() { return "Evolucionar[" + cartaEvolucion.getNombre() + "]"; }
}
