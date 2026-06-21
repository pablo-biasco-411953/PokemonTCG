package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoFinPartida;
import com.pokemon.tcg.model.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
/**
 * Centraliza lo que pasa cuando un Pokémon queda fuera de combate.
 */
public class BattleKoService {

    public void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        // Descarta la carta, entrega premio y decide si la partida termina.
        TableroVictimaYAtacante tableros = identificarTableros(partida, defensor, atacante);
        if (tableros == null) {
            return;
        }

        TableroJugador tableroVictima = tableros.victima();
        TableroJugador tableroGanador = tableros.ganador();

        System.out.println("[KO] Procesando K.O. de: " + defensor.getCard().getNombre());

        if (tableroVictima.getActivo() == defensor) {
            tableroVictima.setActivo(null);
        } else {
            tableroVictima.getBanca().remove(defensor);
        }
        tableroVictima.getPilaDescarte().add(defensor.getCard());

        int premiosATomar = esPokemonEx(defensor) ? 2 : 1;
        int premiosTomados = 0;
        while (premiosTomados < premiosATomar && !tableroGanador.getPremios().isEmpty()) {
            tableroGanador.getMano().add(tableroGanador.getPremios().remove(0));
            premiosTomados++;
        }
        String ganador = tableroGanador == partida.getJugador()
                ? partida.getJugadorUsername()
                : (partida.getBotUsername() != null ? partida.getBotUsername() : "BOT");
        partida.getTurnLogs().add("KNOCK_OUT:" + limpiar(ganador) + ":" + limpiar(defensor.getCard().getNombre()));
        if (premiosTomados > 0) {
            partida.getTurnLogs().add("PRIZE_TAKEN:" + limpiar(ganador) + ":" + premiosTomados);
        }

        boolean hayKOPendiente = false;
        if (partida.getJugador().getActivo() != null && partida.getJugador().getActivo().getHpActual() <= 0) {
            hayKOPendiente = true;
        }
        if (partida.getBot().getActivo() != null && partida.getBot().getActivo().getHpActual() <= 0) {
            hayKOPendiente = true;
        }

        if (hayKOPendiente) {
            // Se resolverá el otro KO en la siguiente llamada a resolverKO, no terminamos aún.
            if (tableroVictima == partida.getBot() && tableroVictima.getActivo() == null) {
                CartaEnJuego mejor = elegirMejorReemplazoBot(tableroVictima, partida.getJugador().getActivo());
                if (mejor != null) {
                    tableroVictima.getBanca().remove(mejor);
                    tableroVictima.setActivo(mejor);
                }
            }
            return;
        }

        // Evaluar condiciones de victoria/derrota
        boolean jugadorSinPremios = partida.getJugador().getPremios().isEmpty();
        boolean botSinPremios = partida.getBot().getPremios().isEmpty();

        boolean jugadorSinPokemon = partida.getJugador().getActivo() == null && partida.getJugador().getBanca().isEmpty();
        boolean botSinPokemon = partida.getBot().getActivo() == null && partida.getBot().getBanca().isEmpty();

        boolean jugadorGana = botSinPokemon || jugadorSinPremios;
        boolean botGana = jugadorSinPokemon || botSinPremios;

        int winConditionsJugador = (botSinPokemon ? 1 : 0) + (jugadorSinPremios ? 1 : 0);
        int winConditionsBot = (jugadorSinPokemon ? 1 : 0) + (botSinPremios ? 1 : 0);

        if (winConditionsJugador > 0 || winConditionsBot > 0) {
            if (winConditionsJugador == winConditionsBot) {
                iniciarMuerteSubita(partida);
                return;
            } else if (winConditionsJugador > winConditionsBot) {
                partida.transicionarA(new EstadoFinPartida());
                partida.setGanador(partida.getJugadorUsername());
                partida.setRazonFinPartida(jugadorSinPremios
                        ? "El ganador tomó todos sus premios."
                        : "El rival se quedó sin Pokémon en juego.");
                return;
            } else {
                partida.transicionarA(new EstadoFinPartida());
                String ganadorBot = partida.getBotUsername() != null ? partida.getBotUsername() : "BOT";
                partida.setGanador(ganadorBot);
                partida.setRazonFinPartida(botSinPremios
                        ? "El ganador tomó todos sus premios."
                        : "El rival se quedó sin Pokémon en juego.");
                return;
            }
        }

        if (tableroVictima == partida.getBot() && tableroVictima.getActivo() == null) {
            CartaEnJuego mejor = elegirMejorReemplazoBot(tableroVictima, partida.getJugador().getActivo());
            if (mejor != null) {
                tableroVictima.getBanca().remove(mejor);
                tableroVictima.setActivo(mejor);
            }
        }
    }

    private TableroVictimaYAtacante identificarTableros(Partida partida, CartaEnJuego defensor, CartaEnJuego atacante) {
        // Resuelve quién perdió el Pokémon y quién recibe el premio.
        TableroJugador victima = encontrarTableroPorCarta(partida, defensor);
        TableroJugador ganador = encontrarTableroPorCarta(partida, atacante);
        if (victima != null && ganador == null) {
            ganador = victima == partida.getJugador() ? partida.getBot() : partida.getJugador();
        }
        if (victima == null || ganador == null || victima == ganador) {
            return null;
        }
        return new TableroVictimaYAtacante(victima, ganador);
    }

    private TableroJugador encontrarTableroPorCarta(Partida partida, CartaEnJuego carta) {
        if (carta == null) {
            return null;
        }

        if (contieneCarta(partida.getJugador(), carta)) {
            return partida.getJugador();
        }
        if (contieneCarta(partida.getBot(), carta)) {
            return partida.getBot();
        }
        return null;
    }

    private boolean contieneCarta(TableroJugador tablero, CartaEnJuego carta) {
        if (tablero.getActivo() == carta) {
            return true;
        }
        if (tablero.getBanca() != null) {
            for (CartaEnJuego c : tablero.getBanca()) {
                if (c == carta) {
                    return true;
                }
            }
        }
        return false;
    }

    private CartaEnJuego elegirMejorReemplazoBot(TableroJugador tableroBot, CartaEnJuego activoRival) {
        // Cuando el bot pierde su activo, prioriza el mejor suplente disponible.
        if (tableroBot.getBanca() == null || tableroBot.getBanca().isEmpty()) {
            return null;
        }
        return tableroBot.getBanca().stream()
                .max((c1, c2) -> Integer.compare(
                        calcularPuntajeEstrategico(c1, activoRival),
                        calcularPuntajeEstrategico(c2, activoRival)))
                .orElse(tableroBot.getBanca().get(0));
    }

    private int calcularPuntajeEstrategico(CartaEnJuego candidato, CartaEnJuego rival) {
        // Puntaje simple: energías, HP y matchup contra el rival.
        int puntaje = 0;
        puntaje += candidato.getEnergiasUnidas().size() * 50;
        puntaje += candidato.getHpActual();

        if (rival == null || rival.getCard() == null || rival.getCard().getTipo() == null) {
            return puntaje;
        }

        String tipoRival = rival.getCard().getTipo();
        String miTipo = candidato.getCard().getTipo();

        if (candidato.getCard().getDebilidades() != null) {
            boolean esDebil = candidato.getCard().getDebilidades().stream()
                    .anyMatch(w -> w.getType().equalsIgnoreCase(tipoRival));
            if (esDebil) {
                puntaje -= 1000;
            }
        }

        if (candidato.getCard().getResistencias() != null) {
            boolean esResistente = candidato.getCard().getResistencias().stream()
                    .anyMatch(r -> r.getType().equalsIgnoreCase(tipoRival));
            if (esResistente) {
                puntaje += 300;
            }
        }

        if (rival.getCard().getDebilidades() != null && miTipo != null) {
            boolean rivalEsDebil = rival.getCard().getDebilidades().stream()
                    .anyMatch(w -> w.getType().equalsIgnoreCase(miTipo));
            if (rivalEsDebil) {
                puntaje += 500;
            }
        }

        return puntaje;
    }

    private boolean tienePokemonBasicoEnMano(TableroJugador tablero) {
        return tablero.getMano().stream().anyMatch(this::esPokemonBasico);
    }

    private boolean esPokemonBasico(com.pokemon.tcg.model.Card c) {
        if (c == null || c.getSupertype() == null) return false;
        String supertype = java.text.Normalizer.normalize(c.getSupertype(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        if (!"pokemon".equals(supertype)) return false;
        return c.getSubtypes() != null && c.getSubtypes().stream()
                .anyMatch(s -> "Basic".equalsIgnoreCase(s));
    }

    private boolean esPokemonEx(CartaEnJuego carta) {
        return carta != null
                && carta.getCard() != null
                && carta.getCard().getSubtypes() != null
                && carta.getCard().getSubtypes().stream().anyMatch(s -> "EX".equalsIgnoreCase(s));
    }

    private String limpiar(String value) {
        return value == null ? "" : value.replace(':', '-').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private void iniciarMuerteSubita(Partida partida) {
        System.out.println("⚠️ [MUERTE SÚBITA] Se ha detectado un empate. Iniciando Muerte Súbita...");

        partida.setMuerteSubita(true);
        partida.setNumeroTurno(1);
        partida.setMulligansJugador(0);
        partida.setMulligansBot(0);
        partida.setYaSeRetiroEsteTurno(false);
        partida.setYaSeUnioEnergiaEsteTurno(false);
        partida.setCoinFlipped(false);
        partida.setCoinFlipWinner(null);
        partida.setCoinFlipResult(null);

        // En muerte súbita saltamos el darse la mano
        partida.setCoinHandshakeJugadorPower(100);
        partida.setCoinHandshakeBotPower(100);
        partida.setCoinHandshakeJugadorHolding(true);
        partida.setCoinHandshakeBotHolding(true);
        partida.setCoinHandshakeComplete(true);
        partida.setFaseActual(Partida.Fase.LANZAMIENTO_MONEDA);

        partida.setSetupJugadorListo(false);
        partida.setSetupBotListo(false);
        partida.setCartasMulliganExtraPendientesJugador(0);
        partida.setCartasMulliganExtraPendientesBot(0);
        partida.setSetupJugadorRoboExtraMulligan(false);
        partida.setSetupBotRoboExtraMulligan(false);

        // Reset boards
        resetearTableroParaMuerteSubita(partida.getJugador());
        resetearTableroParaMuerteSubita(partida.getBot());

        // Draw 7 cards
        for (int i = 0; i < 7; i++) {
            if (!partida.getJugador().getMazo().isEmpty()) {
                partida.getJugador().getMano().add(partida.getJugador().getMazo().remove(0));
            }
            if (!partida.getBot().getMazo().isEmpty()) {
                partida.getBot().getMano().add(partida.getBot().getMazo().remove(0));
            }
        }

        // Add special logs
        partida.getTurnLogs().clear();
        partida.getTurnLogs().add("MUERTE_SUBITA:INICIADA");

        // Transition back to Lanzamiento de Moneda
        partida.transicionarA(new com.pokemon.tcg.model.battle.state.EstadoLanzamientoMoneda());
    }

    private void resetearTableroParaMuerteSubita(TableroJugador tablero) {
        tablero.getMano().clear();
        tablero.getBanca().clear();
        tablero.setActivo(null);
        tablero.getPremios().clear();
        tablero.getPilaDescarte().clear();
        tablero.setTurnosJugados(0);

        // Restore deck from mazoOriginal
        List<Card> deck = new ArrayList<>(tablero.getMazoOriginal());
        java.util.Collections.shuffle(deck);
        tablero.setMazo(deck);
    }

    private record TableroVictimaYAtacante(TableroJugador victima, TableroJugador ganador) {}
}
