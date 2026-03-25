package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;

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

        // Evaluar posibles ataques y elegir uno basado en las energías disponibles
        try {
            // Parsear los ataques desde el JSON
            List<Ataque> ataques = parsearAtaques(activo.getCard().getAttacks());

            // Filtrar ataques que pueda ejecutar con la energía actual
            List<Ataque> ataquesEjecutables = new ArrayList<>();
            for (Ataque ataque : ataques) {
                if (tieneEnergiaSuficiente(activo, ataque)) {
                    ataquesEjecutables.add(ataque);
                }
            }

            // Elegir un ataque aleatorio de los ejecutables
            if (!ataquesEjecutables.isEmpty()) {
                Ataque ataqueSeleccionado = ataquesEjecutables.get(random.nextInt(ataquesEjecutables.size()));

                // Encontrar un objetivo válido (Pokémon del jugador)
                TableroJugador tableroJugador = partida.getJugador();
                CartaEnJuego objetivo = null;

                // Buscar un Pokémon en la mano del jugador para atacar
                if (!tableroJugador.getBanca().isEmpty()) {
                    objetivo = tableroJugador.getBanca().get(0);
                } else if (tableroJugador.getActivo() != null) {
                    objetivo = tableroJugador.getActivo();
                }

                if (objetivo != null) {
                    // Aquí se ejecutaría el ataque real
                    // Por ahora solo se muestra que se seleccionó un ataque
                    partida.setFaseActual(Partida.Fase.TURNO_NORMAL);
                }
            }
        } catch (Exception e) {
            // Si hay error al parsear, simplemente no atacar
            return;
        }
    }

    /**
     * Parsea los ataques desde el JSON.
     */
    private List<Ataque> parsearAtaques(String attacksJson) {
        // Implementación simplificada para evitar dependencias externas
        // En una implementación real, usarías ObjectMapper o similar
        return new ArrayList<>();
    }

    /**
     * Verifica si el atacante tiene suficiente energía para ejecutar el ataque.
     */
    private boolean tieneEnergiaSuficiente(CartaEnJuego atacante, Ataque ataque) {
        // Contar energías del tipo requerido
        java.util.Map<String, Integer> energiaRequerida = new java.util.HashMap<>();
        for (String tipo : ataque.getTiposEnergia()) {
            energiaRequerida.put(tipo, energiaRequerida.getOrDefault(tipo, 0) + 1);
        }

        // Contar energías disponibles en el atacante
        java.util.Map<String, Integer> energiaDisponible = new java.util.HashMap<>();
        for (Card energia : atacante.getEnergiasUnidas()) {
            String tipoEnergia = energia.getTipo();
            if (tipoEnergia != null) {
                energiaDisponible.put(tipoEnergia, energiaDisponible.getOrDefault(tipoEnergia, 0) + 1);
            }
        }

        // Verificar si se tienen suficientes energías
        for (String tipo : energiaRequerida.keySet()) {
            int necesitado = energiaRequerida.get(tipo);
            int disponible = energiaDisponible.getOrDefault(tipo, 0);
            if (disponible < necesitado) {
                return false;
            }
        }

        return true;
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