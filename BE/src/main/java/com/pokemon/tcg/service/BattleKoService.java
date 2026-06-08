package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.state.EstadoFinPartida;
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

        String idADescartar = defensor.getCard().getId();

        if (tableroVictima.getActivo() != null
                && tableroVictima.getActivo().getCard().getId().equals(idADescartar)) {
            tableroVictima.setActivo(null);
        }

        tableroVictima.getBanca().removeIf(c -> c == null
                || c.getCard() == null
                || c.getCard().getId().equals(idADescartar));
        tableroVictima.getPilaDescarte().add(defensor.getCard());

        if (!tableroGanador.getPremios().isEmpty()) {
            tableroGanador.getMano().add(tableroGanador.getPremios().remove(0));
        }

        boolean sinPremios = tableroGanador.getPremios().isEmpty();
        boolean sinPokemon = tableroVictima.getActivo() == null
                && tableroVictima.getBanca().isEmpty();
        if (sinPremios || sinPokemon) {
            partida.transicionarA(new EstadoFinPartida());
            partida.setGanador(tableroGanador == partida.getJugador()
                    ? partida.getJugadorUsername()
                    : (partida.getBotUsername() != null ? partida.getBotUsername() : "BOT"));
            partida.setRazonFinPartida(sinPremios
                    ? "El ganador tomo todos sus premios."
                    : "El rival se quedo sin Pokemon en juego.");
            return;
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
        if (victima == null || ganador == null) {
            return null;
        }
        return new TableroVictimaYAtacante(victima, ganador);
    }

    private TableroJugador encontrarTableroPorCarta(Partida partida, CartaEnJuego carta) {
        // Ubica una carta comparando su id contra activo y banca.
        if (carta == null || carta.getCard() == null) {
            return null;
        }

        if (contieneCarta(partida.getJugador(), carta.getCard().getId())) {
            return partida.getJugador();
        }
        if (contieneCarta(partida.getBot(), carta.getCard().getId())) {
            return partida.getBot();
        }
        return null;
    }

    private boolean contieneCarta(TableroJugador tablero, String cardId) {
        if (tablero.getActivo() != null
                && tablero.getActivo().getCard() != null
                && tablero.getActivo().getCard().getId().equals(cardId)) {
            return true;
        }

        return tablero.getBanca().stream()
                .filter(c -> c != null && c.getCard() != null)
                .anyMatch(c -> c.getCard().getId().equals(cardId));
    }

    private CartaEnJuego elegirMejorReemplazoBot(TableroJugador tableroBot, CartaEnJuego activoRival) {
        // Cuando el bot pierde su activo, prioriza el mejor suplente disponible.
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

    private record TableroVictimaYAtacante(TableroJugador victima, TableroJugador ganador) {}
}
