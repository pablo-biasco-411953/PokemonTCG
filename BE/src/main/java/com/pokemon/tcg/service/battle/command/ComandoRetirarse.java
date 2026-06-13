package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;

public class ComandoRetirarse implements ComandoTurno {

    private final String nuevoActivoId;
    private final TableroJugador tablero;

    public ComandoRetirarse(String nuevoActivoId, TableroJugador tablero) {
        this.nuevoActivoId = nuevoActivoId;
        this.tablero = tablero;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        if (partida.isYaSeRetiroEsteTurno()) return false;
        CartaEnJuego activo = tablero.getActivo();
        if (activo == null) return false;
        return !activo.getCondicionesEspeciales().contains("Asleep")
                && !activo.getCondicionesEspeciales().contains("Paralyzed")
                && !activo.getCondicionesEspeciales().contains("CantRetreat");
    }

    @Override
    public void ejecutar(Partida partida) {
        CartaEnJuego activoViejo = tablero.getActivo();

        if (activoViejo == null) throw new IllegalStateException("No hay un Pokémon activo para retirar.");
        if (partida.isYaSeRetiroEsteTurno()) throw new IllegalStateException("Solo podés realizar una retirada por turno.");

        if (activoViejo.getCondicionesEspeciales().contains("Asleep")
                || activoViejo.getCondicionesEspeciales().contains("Paralyzed")
                || activoViejo.getCondicionesEspeciales().contains("CantRetreat")) {
            throw new IllegalStateException("No podés retirarte por un estado alterado o efecto de ataque.");
        }

        CartaEnJuego suplente = tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(nuevoActivoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El Pokémon elegido no está en la banca."));

        int costo = activoViejo.getCard().getCostoRetirada();
        int energiaDisponible = activoViejo.getEnergiasUnidas().stream()
                .mapToInt(com.pokemon.tcg.service.battle.EnergyCostCalculator::colorlessValue)
                .sum();
        if (energiaDisponible < costo) {
            throw new IllegalStateException("Energías insuficientes. Necesitás " + costo + " para retirar.");
        }

        int energiaPagada = 0;
        while (energiaPagada < costo) {
            Card energia = activoViejo.getEnergiasUnidas().remove(0);
            energiaPagada += com.pokemon.tcg.service.battle.EnergyCostCalculator.colorlessValue(energia);
            tablero.getPilaDescarte().add(energia);
        }

        activoViejo.limpiarCondiciones();
        tablero.getBanca().remove(suplente);
        tablero.getBanca().add(activoViejo);
        tablero.setActivo(suplente);
        partida.setYaSeRetiroEsteTurno(true);

        System.out.println("🔄 Retirada: " + activoViejo.getCard().getNombre() + " a la banca. Entra " + suplente.getCard().getNombre());
    }

    @Override
    public String getNombre() { return "Retirarse"; }
}
