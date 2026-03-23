package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

@Service
public class BotAIService {
    private final Random random = new Random();

    /**
     * El bot ejecuta su turno de juego.
     */
    public void ejecutarTurno(Partida partida) {
        // Determinar el tablero del bot (es el tableroBot)
        TableroJugador tableroBot = partida.getBot();

        // 1. Si hay espacio en la banca, bajar un Pokémon básico
        if (tableroBot.getBanca().size() < 5) {
            bajarAPrimerBanca(tableroBot);
        }

        // 2. Unir energía al Pokémon activo o a uno de la banca si es necesario
        unirEnergia(tableroBot);

        // 3. Evaluar si puede atacar y ejecutar un ataque
        realizarAccionAtaque(tableroBot, partida);
    }

    /**
     * El bot baja un Pokémon básico a la banca si hay espacio.
     */
    private void bajarAPrimerBanca(TableroJugador tablero) {
        // Buscar una carta básica en la mano que pueda ir a la banca
        for (Card carta : tablero.getMano()) {
            if (esPokemonBasico(carta)) {
                // Convertir en CartaEnJuego y bajar a la banca
                CartaEnJuego cartaEnJuego = new CartaEnJuego(carta);
                if (tablero.getBanca().size() < 5) {
                    tablero.getBanca().add(cartaEnJuego);
                    tablero.getMano().remove(carta);
                    break;
                }
            }
        }
    }

    /**
     * El bot une energía a su Pokémon activo o a uno de la banca.
     */
    private void unirEnergia(TableroJugador tablero) {
        // Verificar si hay energía en la mano
        List<Card> energiasEnMano = new ArrayList<>();
        for (Card carta : tablero.getMano()) {
            if (esEnergia(carta)) {
                energiasEnMano.add(carta);
            }
        }

        if (!energiasEnMano.isEmpty()) {
            // Unir energía al Pokémon activo o a uno de la banca
            CartaEnJuego objetivo = tablero.getActivo();
            if (objetivo == null) {
                // Si no hay activo, usar el primer Pokémon en la banca
                if (!tablero.getBanca().isEmpty()) {
                    objetivo = tablero.getBanca().get(0);
                }
            }

            if (objetivo != null && !energiasEnMano.isEmpty()) {
                // Unir una energía al objetivo
                Card energia = energiasEnMano.get(0);
                objetivo.getEnergiasUnidas().add(energia);
                tablero.getMano().remove(energia);
            }
        }
    }

    /**
     * El bot evalúa si puede atacar y realiza una acción.
     */
    private void realizarAccionAtaque(TableroJugador tablero, Partida partida) {
        // Verificar si hay un Pokémon activo
        CartaEnJuego activo = tablero.getActivo();
        if (activo == null || !activo.isPuedeAtacar()) {
            return;
        }

        // Evaluar posibles ataques y elegir uno
        // Para simplificar, elegimos un ataque aleatorio si hay uno disponible
        // En una implementación real, se debería usar lógica más compleja

        // Simulamos que el bot decide atacar con probabilidad del 30%
        if (random.nextDouble() < 0.3) {
            // Aquí iría la lógica para elegir un ataque específico
            // Por ahora, solo se indica que atacará
        }
    }

    /**
     * Verifica si una carta es un Pokémon básico.
     */
    private boolean esPokemonBasico(Card carta) {
        if (carta.getTipo() == null) return false;
        return carta.getTipo().toLowerCase().contains("basic");
    }

    /**
     * Verifica si una carta es energía.
     */
    private boolean esEnergia(Card carta) {
        if (carta.getTipo() == null) return false;
        return carta.getTipo().toLowerCase().contains("energy");
    }
}