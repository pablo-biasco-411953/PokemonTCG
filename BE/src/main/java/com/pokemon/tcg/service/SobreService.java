package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class SobreService {
    private final JugadorRepository jugadorRepo;
    private final CardRepository cardRepo;
    private final Random random = new Random(); // Agregamos el Random

    public SobreService(JugadorRepository jugadorRepo, CardRepository cardRepo) {
        this.jugadorRepo = jugadorRepo;
        this.cardRepo = cardRepo;
    }

    /**
     * Abre un sobre para el usuario indicado.
     * Reduce los sobres disponibles en 1, asegura entre 2 y 5 cartas de EnergÃƒÂ­a,
     * aÃƒÂ±ade todo a la colecciÃƒÂ³n del jugador y devuelve la lista de cartas obtenidas.
     */
    public List<Card> abrirSobre(String username) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        if (jugador.getSobresDisponibles() <= 0) {
            throw new IllegalStateException("No hay sobres disponibles para el jugador: " + username);
        }

        // 1. Traemos todas las cartas para separar por tipo
        // (Como tenÃƒÂ©s ~200 cartas, esto es rapidÃƒÂ­simo en memoria)
        List<Card> todasLasCartas = cardRepo.findAll();

        List<Card> energias = todasLasCartas.stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getTipo()))
                .toList();

        List<Card> pokemones = todasLasCartas.stream()
                .filter(c -> !"Energy".equalsIgnoreCase(c.getTipo()))
                .toList();

        if (energias.isEmpty() || pokemones.isEmpty()) {
            throw new IllegalStateException("La base de datos no tiene suficientes cartas cargadas.");
        }

        // 2. Calculamos la proporciÃƒÂ³n: entre 2 y 5 energÃƒÂ­as por sobre de 10
        int cantEnergias = random.nextInt(4) + 2;
        int cantPokemones = 10 - cantEnergias;

        List<Card> sobreGenerado = new ArrayList<>();

        // 3. Seleccionamos las EnergÃƒÂ­as al azar
        List<Card> energiasMezcladas = new ArrayList<>(energias);
        Collections.shuffle(energiasMezcladas);
        // Usamos Math.min por si en el JSON pusiste menos energÃƒÂ­as de las que pide el random
        sobreGenerado.addAll(energiasMezcladas.subList(0, Math.min(cantEnergias, energiasMezcladas.size())));

        // 4. Seleccionamos los PokÃƒÂ©mon al azar
        List<Card> pokemonesMezclados = new ArrayList<>(pokemones);
        Collections.shuffle(pokemonesMezclados);
        sobreGenerado.addAll(pokemonesMezclados.subList(0, Math.min(cantPokemones, pokemonesMezclados.size())));

        // 5. Mezclamos el sobre final para que al abrirlo se vea natural
        Collections.shuffle(sobreGenerado);

        // 6. AÃƒÂ±adir las cartas a la colecciÃƒÂ³n del jugador
        jugador.getColeccion().addAll(sobreGenerado);
        jugador.setSobresDisponibles(jugador.getSobresDisponibles() - 1);
        jugadorRepo.save(jugador);

        return sobreGenerado;
    }
}
