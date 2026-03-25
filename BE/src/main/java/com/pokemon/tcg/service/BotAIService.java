package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class BotAIService {

    private final Random random = new Random();

    public void ejecutarTurno(Partida partida) {
        TableroJugador tableroBot = partida.getBot();

        // 🚨 REGLA DE ORO: Si no hay activo, subir uno de la banca primero
        if (tableroBot.getActivo() == null && !tableroBot.getBanca().isEmpty()) {
            CartaEnJuego mejorOpcion = tableroBot.getBanca().stream()
                    .sorted((c1, c2) -> {
                        // Prioridad 1: El que tenga más energía ya unida
                        int e1 = c1.getEnergiasUnidas().size();
                        int e2 = c2.getEnergiasUnidas().size();
                        if (e1 != e2) return Integer.compare(e2, e1);
                        // Prioridad 2: El que tenga más vida
                        return Integer.compare(c2.getHpActual(), c1.getHpActual());
                    })
                    .findFirst().get();
            tableroBot.getBanca().remove(mejorOpcion);
            tableroBot.setActivo(mejorOpcion);
            System.out.println("🤖 [BOT] Subió a " + mejorOpcion.getCard().getNombre() + " porque es el más apto.");
        }
        // Luego sigue su lógica normal...
        gestionarCartasEnMano(tableroBot);
        gestionarEnergiaBot(tableroBot);
        intentarAtacar(tableroBot, partida);
    }
    private void gestionarCartasEnMano(TableroJugador tablero) {
        // Copia para evitar ConcurrentModificationException al remover durante iteración
        List<Card> manoCopia = new ArrayList<>(tablero.getMano());

        for (Card carta : manoCopia) {
            if (!esPokemonBasico(carta)) continue;

            if (tablero.getActivo() == null) {
                tablero.setActivo(new CartaEnJuego(carta));
                tablero.getMano().remove(carta);
                System.out.println("🤖 [BOT] Activo: " + carta.getNombre());
            } else if (tablero.getBanca().size() < 5) {
                tablero.getBanca().add(new CartaEnJuego(carta));
                tablero.getMano().remove(carta);
                System.out.println("🤖 [BOT] Banca: " + carta.getNombre());
            }
        }
    }

    private void gestionarEnergiaBot(TableroJugador tablero) {
        Card energia = tablero.getMano().stream()
                .filter(this::esEnergia)
                .findFirst().orElse(null);

        if (energia != null) {
            CartaEnJuego activo = tablero.getActivo();
            CartaEnJuego objetivo = null;

            // ESTRATEGIA:
            // 1. Si el activo existe y no tiene mucha energía, priorizarlo para atacar rápido.
            if (activo != null && activo.getEnergiasUnidas().size() < 2) {
                objetivo = activo;
            }
            // 2. Si el activo está cargado o no existe, cargar al de la banca con más HP (el "tanque")
            else if (!tablero.getBanca().isEmpty()) {
                objetivo = tablero.getBanca().stream()
                        .max((c1, c2) -> Integer.compare(c1.getHpActual(), c2.getHpActual()))
                        .orElse(null);
            } else {
                objetivo = activo;
            }

            if (objetivo != null) {
                objetivo.getEnergiasUnidas().add(energia);
                tablero.getMano().remove(energia);
                System.out.println("🤖 [BOT] Decisión estratégica: Energía unida a " + objetivo.getCard().getNombre());
            }
        }
    }

    private void intentarAtacar(TableroJugador tableroBot, Partida partida) {
        CartaEnJuego activoBot     = tableroBot.getActivo();
        CartaEnJuego activoJugador = partida.getJugador().getActivo();

        if (activoBot == null || activoJugador == null) {
            System.out.println("🤖 [BOT] Sin Pokémon activo para atacar.");
            return;
        }

        if (activoBot.getEnergiasUnidas().isEmpty()) {
            System.out.println("🤖 [BOT] Sin energía para atacar.");
            return;
        }

        int danio = calcularDanio(activoBot);
        activoJugador.setHpActual(activoJugador.getHpActual() - danio);
        System.out.println("🤖 [BOT] Atacó con " + danio + " daño. HP jugador: " + activoJugador.getHpActual());

        if (activoJugador.getHpActual() <= 0) {
            resolverKO(partida, activoBot, activoJugador);
        }
    }

    /**
     * Manejo de K.O. consistente con BattleEngineService:
     * el Pokémon va al descarte y el ganador toma un premio.
     */
    private void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        TableroJugador tableroVictima = partida.getJugador();  // el bot siempre ataca al jugador aquí
        TableroJugador tableroBot     = partida.getBot();

        System.out.println("💀 [BOT] K.O.! " + defensor.getCard().getNombre() + " derrotado.");

        // Mover al descarte
        tableroVictima.getPilaDescarte().add(defensor.getCard());
        tableroVictima.setActivo(null);

        // El bot toma un premio
        if (!tableroBot.getPremios().isEmpty()) {
            tableroBot.getMano().add(tableroBot.getPremios().remove(0));
            System.out.println("🤖 [BOT] Tomó un premio. Premios restantes: " + tableroBot.getPremios().size());
        }

        // Fin de partida
        boolean botSinPremios      = tableroBot.getPremios().isEmpty();
        boolean jugadorSinPokemon  = tableroVictima.getActivo() == null
                && tableroVictima.getBanca().isEmpty();

        if (botSinPremios || jugadorSinPokemon) {
            partida.setFaseActual(Partida.Fase.FIN_PARTIDA);
            System.out.println("🏆 [BOT] ¡Partida terminada! Ganó el bot.");
        }
    }

    private int calcularDanio(CartaEnJuego cartaEnJuego) {
        // 🚨 SOLUCIÓN FINAL:
        // Como getAtaques() devuelve un String ("Tackle, Razor Leaf"),
        // no podemos hacer .get(0). Por ahora devolvemos un daño base.

        Card card = cartaEnJuego.getCard();
        String ataquesTxt = card.getAtaques();

        if (ataquesTxt != null && !ataquesTxt.isEmpty()) {
            System.out.println("🤖 Bot atacando con: " + ataquesTxt);
            return 30; // Daño base para testear mientras los ataques sean un String
        }

        return 20; // Fallback
    }

    private boolean esPokemonBasico(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        return !tipo.contains("energy") && !tipo.contains("energía") && !tipo.contains("stage");
    }

    private boolean esEnergia(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        return tipo.contains("energy") || tipo.contains("energía");
    }
}