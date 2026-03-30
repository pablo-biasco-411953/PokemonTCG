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

        // Ã°Å¸Å¡Â¨ REGLA DE ORO: Si no hay activo, subir uno de la banca primero
        if (tableroBot.getActivo() == null && !tableroBot.getBanca().isEmpty()) {
            CartaEnJuego mejorOpcion = tableroBot.getBanca().stream()
                    .sorted((c1, c2) -> {
                        // Prioridad 1: El que tenga mÃƒÂ¡s energÃƒÂ­a ya unida
                        int e1 = c1.getEnergiasUnidas().size();
                        int e2 = c2.getEnergiasUnidas().size();
                        if (e1 != e2) return Integer.compare(e2, e1);
                        // Prioridad 2: El que tenga mÃƒÂ¡s vida
                        return Integer.compare(c2.getHpActual(), c1.getHpActual());
                    })
                    .findFirst().get();
            tableroBot.getBanca().remove(mejorOpcion);
            tableroBot.setActivo(mejorOpcion);
            System.out.println("Ã°Å¸Â¤â€“ [BOT] SubiÃƒÂ³ a " + mejorOpcion.getCard().getNombre() + " porque es el mÃƒÂ¡s apto.");
        }
        // Luego sigue su lÃƒÂ³gica normal...
        gestionarCartasEnMano(tableroBot);
        gestionarEnergiaBot(tableroBot);
        intentarAtacar(tableroBot, partida);
    }

    private void gestionarCartasEnMano(TableroJugador tablero) {
        // Copia para evitar ConcurrentModificationException al remover durante iteraciÃƒÂ³n
        List<Card> manoCopia = new ArrayList<>(tablero.getMano());

        for (Card carta : manoCopia) {
            if (!esPokemonBasico(carta)) continue;

            if (tablero.getActivo() == null) {
                tablero.setActivo(new CartaEnJuego(carta));
                tablero.getMano().remove(carta);
                System.out.println("Ã°Å¸Â¤â€“ [BOT] Activo: " + carta.getNombre());
            } else if (tablero.getBanca().size() < 5) {
                tablero.getBanca().add(new CartaEnJuego(carta));
                tablero.getMano().remove(carta);
                System.out.println("Ã°Å¸Â¤â€“ [BOT] Banca: " + carta.getNombre());
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
            // 1. Si el activo existe y no tiene mucha energÃƒÂ­a, priorizarlo para atacar rÃƒÂ¡pido.
            if (activo != null && activo.getEnergiasUnidas().size() < 2) {
                objetivo = activo;
            }
            // 2. Si el activo estÃƒÂ¡ cargado o no existe, cargar al de la banca con mÃƒÂ¡s HP (el "tanque")
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
                System.out.println("Ã°Å¸Â¤â€“ [BOT] DecisiÃƒÂ³n estratÃƒÂ©gica: EnergÃƒÂ­a unida a " + objetivo.getCard().getNombre());
            }
        }
    }

    private void intentarAtacar(TableroJugador tableroBot, Partida partida) {
        CartaEnJuego activoBot = tableroBot.getActivo();
        CartaEnJuego activoJugador = partida.getJugador().getActivo();

        if (activoBot == null || activoJugador == null) return;

        // 🚩 Si no tiene energía, que ni lo intente y pase el turno
        if (activoBot.getEnergiasUnidas().isEmpty()) {
            System.out.println("🤖 [BOT] Esperando energía para atacar...");
            return;
        }

        // Buscamos el daño real
        int danio = calcularDanio(activoBot);

        // Solo si el daño es mayor a 0 ejecutamos la lógica
        if (danio > 0) {
            int nuevaHp = activoJugador.getHpActual() - danio;
            activoJugador.setHpActual(Math.max(0, nuevaHp));
            System.out.println("🤖 [BOT] Atacó con " + danio + " daño.");

            if (activoJugador.getHpActual() <= 0) {
                resolverKO(partida, activoBot, activoJugador);
            }
        }
    }

    private void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        TableroJugador tableroVictima = partida.getJugador();
        TableroJugador tableroBot     = partida.getBot();

        System.out.println("Ã°Å¸â€™â‚¬ [BOT] K.O.! " + defensor.getCard().getNombre() + " derrotado.");

        // Mover al descarte
        tableroVictima.getPilaDescarte().add(defensor.getCard());
        tableroVictima.setActivo(null);

        // El bot toma un premio
        if (!tableroBot.getPremios().isEmpty()) {
            tableroBot.getMano().add(tableroBot.getPremios().remove(0));
            System.out.println("Ã°Å¸Â¤â€“ [BOT] TomÃƒÂ³ un premio. Premios restantes: " + tableroBot.getPremios().size());
        }

        // Fin de partida
        boolean botSinPremios      = tableroBot.getPremios().isEmpty();
        boolean jugadorSinPokemon  = tableroVictima.getActivo() == null
                && tableroVictima.getBanca().isEmpty();

        if (botSinPremios || jugadorSinPokemon) {
            partida.setFaseActual(Partida.Fase.FIN_PARTIDA);
            System.out.println("Ã°Å¸Ââ€  [BOT] Ã‚Â¡Partida terminada! GanÃƒÂ³ el bot.");
        }
    }

    private int calcularDanio(CartaEnJuego cartaEnJuego) {
        Card card = cartaEnJuego.getCard();

        // Ã¢Å“â€¦ AHORA SÃƒÂ: Traemos la lista real de objetos Ataque
        List<Ataque> ataques = card.getAtaques();

        if (ataques != null && !ataques.isEmpty()) {
            // Hacemos que el bot elija el primer ataque que tenga la carta
            Ataque ataqueElegido = ataques.get(0);

            System.out.println("Ã°Å¸Â¤â€“ Bot atacando con: " + ataqueElegido.getNombre());

            // Si el ataque hace 0 de daÃƒÂ±o (ej: "Growl" que solo baja stats),
            // le ponemos un mÃƒÂ­nimo de 10 para que el bot no se quede pegando por 0 eternamente.
            return ataqueElegido.getDanio() > 0 ? ataqueElegido.getDanio() : 10;
        }

        return 20; // Fallback por si la carta vino sin ataques cargados
    }

    private boolean esPokemonBasico(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        return !tipo.contains("energy") && !tipo.contains("energÃƒÂ­a") && !tipo.contains("stage");
    }

    private boolean esEnergia(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        return tipo.contains("energy") || tipo.contains("energÃƒÂ­a");
    }
}
