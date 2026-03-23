package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.battle.*;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class BattleService {

    private final JugadorRepository jugadorRepo;

    public BattleService(JugadorRepository jugadorRepo) {
        this.jugadorRepo = jugadorRepo;
    }

    /**
     * Inicia una nueva partida entre un jugador y un bot.
     * Este método implementa el setup inicial del juego.
     */
    public Partida startBattle(String username, Long mazoId) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        // Para este ejemplo, creamos un tablero de jugador con su colección
        TableroJugador tableroJugador = new TableroJugador();
        // Aquí iría la lógica para mezclar y preparar el mazo

        // Creamos un bot con un mazo aleatorio (esto se hará más realista en Fase 5)
        TableroJugador tableroBot = new TableroJugador();
        // Aquí iría la lógica para generar un mazo válido de 60 cartas
        // Por ahora, usamos una lista vacía como marcador de posición

        Partida partida = new Partida(tableroJugador, tableroBot);
        return partida;
    }

    /**
     * Roba cartas del mazo al jugador.
     */
    public void robarCartas(TableroJugador tablero, int cantidad) {
        if (tablero.getMazo() == null || tablero.getMazo().isEmpty()) {
            // Si el mazo está vacío, la partida termina
            return;
        }

        List<Card> cartasRobadas = new ArrayList<>();
        for (int i = 0; i < cantidad && !tablero.getMazo().isEmpty(); i++) {
            Card carta = tablero.getMazo().remove(0);
            cartasRobadas.add(carta);
        }

        // Añadir las cartas a la mano
        tablero.getMano().addAll(cartasRobadas);
    }

    /**
     * Verifica si el jugador tiene al menos un Pokémon básico en su mano.
     */
    public boolean tienePokemonBasico(TableroJugador tablero) {
        if (tablero.getMano() == null) return false;

        for (Card carta : tablero.getMano()) {
            // Aquí se debería implementar la lógica para identificar si es un Pokémon básico
            // Esta es una implementación simplificada
            if (carta.getTipo() != null && carta.getTipo().toLowerCase().contains("basic")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Realiza el Mulligan: si no tiene un Pokémon básico, revela la mano, baraja y roba 7.
     */
    public void realizarMulligan(TableroJugador tablero) {
        if (!tienePokemonBasico(tablero)) {
            // Revelar mano
            List<Card> manoRevelada = new ArrayList<>(tablero.getMano());

            // Barajar la mano en el mazo (descarte)
            tablero.getMazo().addAll(manoRevelada);
            Collections.shuffle(tablero.getMazo());

            // Robar 7 cartas
            robarCartas(tablero, 7);
        }
    }

    /**
     * Baja un Pokémon básico a la banca.
     */
    public void bajarAPrimerBanca(TableroJugador tablero, Card carta) {
        if (tablero.getBanca().size() < 5) {
            // Convertir la carta en CartaEnJuego
            CartaEnJuego cartaEnJuego = new CartaEnJuego(carta);
            tablero.getBanca().add(cartaEnJuego);

            // Remover de la mano
            tablero.getMano().remove(carta);
        }
    }
}