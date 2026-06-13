package com.pokemon.tcg;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.battle.command.ComandoRetirarse;
import com.pokemon.tcg.service.battle.EnergyCostCalculator;
import java.util.ArrayList;

public class TestRetreat {
    public static void main(String[] args) {
        try {
            Card pokemon = new Card();
            pokemon.setId("p1");
            pokemon.setNombre("Pikachu");
            pokemon.setCostoRetirada(1);

            Card bench = new Card();
            bench.setId("p2");
            bench.setNombre("Squirtle");

            Card dce = new Card();
            dce.setId("e1");
            dce.setNombre("Double Colorless Energy");
            dce.setTipo("Energy");

            CartaEnJuego activo = new CartaEnJuego(pokemon);
            activo.getEnergiasUnidas().add(dce);

            CartaEnJuego benched = new CartaEnJuego(bench);

            TableroJugador tablero = new TableroJugador();
            tablero.setActivo(activo);
            tablero.getBanca().add(benched);

            Partida partida = new Partida(tablero, new TableroJugador());

            ComandoRetirarse cmd = new ComandoRetirarse("p2", tablero);
            cmd.ejecutar(partida);
            
            System.out.println("Activo ahora: " + tablero.getActivo().getCard().getNombre());
            System.out.println("Pila de descarte size: " + tablero.getPilaDescarte().size());
            System.out.println("Energias en activo original: " + activo.getEnergiasUnidas().size());
            System.out.println("SUCCESS");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
