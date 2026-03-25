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

    public void ejecutarTurno(Partida partida) {
        TableroJugador tableroBot = partida.getBot();
        System.out.println("🤖 [BOT] Iniciando turno...");

        // 1. BAJAR POKÉMON (Primero al Activo, luego a la Banca)
        gestionarCartasEnMano(tableroBot);

        // 2. UNIR ENERGÍA
        gestionarEnergiaBot(tableroBot);

        // 3. ATACAR AL JUGADOR
        intentarAtacar(tableroBot, partida);

        System.out.println("🤖 [BOT] Turno finalizado.");
    }

    private void gestionarCartasEnMano(TableroJugador tablero) {
        // Usamos una copia de la mano para evitar errores de concurrencia al remover cartas
        List<Card> manoCopia = new ArrayList<>(tablero.getMano());

        for (Card carta : manoCopia) {
            if (esPokemonBasico(carta)) {
                // Si no tiene activo, lo pone como activo
                if (tablero.getActivo() == null) {
                    tablero.setActivo(new CartaEnJuego(carta));
                    tablero.getMano().remove(carta);
                    System.out.println("🤖 [BOT] Puso ACTIVO: " + carta.getNombre());
                }
                // Si ya tiene activo, intenta llenar la banca (max 5)
                else if (tablero.getBanca().size() < 5) {
                    tablero.getBanca().add(new CartaEnJuego(carta));
                    tablero.getMano().remove(carta);
                    System.out.println("🤖 [BOT] Puso en BANCA: " + carta.getNombre());
                }
            }
        }
    }

    private void gestionarEnergiaBot(TableroJugador tablero) {
        List<Card> manoCopia = new ArrayList<>(tablero.getMano());

        for (Card carta : manoCopia) {
            if (esEnergia(carta)) {
                // Prioridad de energía: Al Pokémon Activo
                CartaEnJuego objetivo = tablero.getActivo();

                // Si no hay activo, a la banca
                if (objetivo == null && !tablero.getBanca().isEmpty()) {
                    objetivo = tablero.getBanca().get(0);
                }

                if (objetivo != null) {
                    objetivo.getEnergiasUnidas().add(carta);
                    tablero.getMano().remove(carta);
                    System.out.println("🤖 [BOT] Unió ENERGÍA a: " + objetivo.getCard().getNombre());
                    break; // Regla TCG: Solo una energía por turno
                }
            }
        }
    }

    private void intentarAtacar(TableroJugador tableroBot, Partida partida) {
        CartaEnJuego activoBot = tableroBot.getActivo();
        CartaEnJuego activoJugador = partida.getJugador().getActivo();

        // Si el bot no tiene activo o el jugador no tiene activo, no hay pelea
        if (activoBot == null || activoJugador == null) {
            System.out.println("🤖 [BOT] No puede atacar: Falta algún Pokémon activo.");
            return;
        }

        // Si el bot tiene al menos una energía, realiza un ataque genérico
        // Esto es para que el bot haga daño mientras no termines el parser de JSON
        if (!activoBot.getEnergiasUnidas().isEmpty()) {
            int danioBase = 20;
            activoJugador.setHpActual(activoJugador.getHpActual() - danioBase);
            System.out.println("🤖 [BOT] ATACÓ con 20 de daño! Vida restante del jugador: " + activoJugador.getHpActual());

            // Validar si el jugador fue derrotado (K.O.)
            if (activoJugador.getHpActual() <= 0) {
                System.out.println("💀 [BOT] ¡Tu Pokémon fue derrotado!");
                partida.getJugador().setActivo(null); // El puesto queda vacío
                // El bot toma una carta de premio
                if (!tableroBot.getPremios().isEmpty()) {
                    tableroBot.getMano().add(tableroBot.getPremios().remove(0));
                }
            }
        }
    }

    // --- MÉTODOS DE VALIDACIÓN FLEXIBLES ---

    private boolean esPokemonBasico(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        // Aceptamos cualquier cosa que no sea energía ni evolución "Stage"
        return !tipo.contains("energy") && !tipo.contains("stage") && !tipo.contains("energía");
    }

    private boolean esEnergia(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        return tipo.contains("energy") || tipo.contains("energía");
    }
}