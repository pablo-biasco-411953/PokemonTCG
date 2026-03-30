package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class MazoService {
    private final MazoRepository mazoRepo;
    private final JugadorRepository jugadorRepo;
    private final CardRepository cardRepo;

    public MazoService(MazoRepository mazoRepo, JugadorRepository jugadorRepo, CardRepository cardRepo) {
        this.mazoRepo = mazoRepo;
        this.jugadorRepo = jugadorRepo;
        this.cardRepo = cardRepo;
    }

    /**
     * Guarda un nuevo mazo para el jugador.
     * Valida que tenga exactamente 60 cartas antes de guardar.
     */
    public Mazo guardarMazo(String nombre, String username, List<String> cartaIds) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        if (cartaIds == null || cartaIds.size() != 60) {
            throw new IllegalArgumentException("Un mazo debe contener exactamente 60 cartas. Se proporcionaron: " +
                    (cartaIds == null ? 0 : cartaIds.size()));
        }

        // Verificar que todas las cartas existen
        List<Card> cartas = new ArrayList<>();
        for (String id : cartaIds) {
            Card card = cardRepo.findById(id).orElse(null);
            if (card == null) {
                throw new IllegalArgumentException("Carta no encontrada en la BD: " + id);
            }
            cartas.add(card);
        }

        // Crear y guardar el mazo
        Mazo mazo = new Mazo(nombre, jugador);
        mazo.setCartas(cartas);
        return mazoRepo.save(mazo);
    }

    /**
     * Actualiza un mazo existente.
     */
    public Mazo actualizarMazo(Long id, String nombre, List<String> cartasIds) {
        // Usamos mazoRepo que es el nombre definido arriba
        Mazo mazo = mazoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mazo no encontrado con ID: " + id));

        mazo.setNombre(nombre);

        // Buscamos las cartas usando cardRepo
        List<Card> nuevasCartas = cardRepo.findAllById(cartasIds);

        // Opcional: PodrÃƒÂ­as validar que nuevasCartas.size() == 60 acÃƒÂ¡ tambiÃƒÂ©n
        if (nuevasCartas.size() != 60) {
            // PodrÃƒÂ­as lanzar error o simplemente dejar que guarde si confÃƒÂ­as en el front
        }

        mazo.setCartas(nuevasCartas);

        return mazoRepo.save(mazo);
    }

    /**
     * Lista todos los mazos del jugador.
     */
    public List<Mazo> listarMazos(String username) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }
        return mazoRepo.findByJugador(jugador);
    }
}
