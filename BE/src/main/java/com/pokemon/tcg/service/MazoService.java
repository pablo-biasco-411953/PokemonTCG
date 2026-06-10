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
import java.text.Normalizer;

@Service
/**
 * Gestiona guardado, edición y listado de mazos del jugador.
 */
public class MazoService {
    private final MazoRepository mazoRepo;
    private final JugadorRepository jugadorRepo;
    private final CardRepository cardRepo;
    private final MazoBackupService mazoBackupService;

    public MazoService(MazoRepository mazoRepo, JugadorRepository jugadorRepo, CardRepository cardRepo, MazoBackupService mazoBackupService) {
        this.mazoRepo = mazoRepo;
        this.jugadorRepo = jugadorRepo;
        this.cardRepo = cardRepo;
        this.mazoBackupService = mazoBackupService;
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
        validarMazo(cartas);

        // Crear y guardar el mazo
        Mazo mazo = new Mazo(nombre, jugador);
        mazo.setCartas(cartas);
        Mazo guardado = mazoRepo.save(mazo);
        mazoBackupService.backupAll();
        return guardado;
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
        validarMazo(nuevasCartas);

        mazo.setCartas(nuevasCartas);

        Mazo guardado = mazoRepo.save(mazo);
        mazoBackupService.backupAll();
        return guardado;
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

    /**
     * Elimina un mazo del jugador.
     */
    public void eliminarMazo(Long id) {
        if (!mazoRepo.existsById(id)) {
            throw new IllegalArgumentException("Mazo no encontrado con ID: " + id);
        }
        mazoRepo.deleteById(id);
        mazoBackupService.backupAll();
    }

    /**
     * Inyecta una carta en un mazo para pruebas manuales.
     * Si el mazo ya tiene 60 cartas, requiere indicar cual reemplazar.
     */
    public Mazo debugInyectarCarta(Long mazoId, String cartaId, String cartaAReemplazarId) {
        Mazo mazo = mazoRepo.findById(mazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado con ID: " + mazoId));

        Card cartaNueva = cardRepo.findById(cartaId)
                .orElseThrow(() -> new IllegalArgumentException("Carta no encontrada en la BD: " + cartaId));

        List<Card> cartasActuales = mazo.getCartas() != null
                ? new ArrayList<>(mazo.getCartas())
                : new ArrayList<>();

        if (cartasActuales.size() >= 60) {
            if (cartaAReemplazarId == null || cartaAReemplazarId.isBlank()) {
                throw new IllegalArgumentException("Debes elegir una carta a reemplazar en un mazo de 60 cartas.");
            }

            boolean reemplazada = false;
            for (int i = 0; i < cartasActuales.size(); i++) {
                Card actual = cartasActuales.get(i);
                if (actual != null && cartaAReemplazarId.equals(actual.getId())) {
                    cartasActuales.set(i, cartaNueva);
                    reemplazada = true;
                    break;
                }
            }

            if (!reemplazada) {
                throw new IllegalArgumentException("La carta a reemplazar no existe dentro del mazo.");
            }
        } else {
            cartasActuales.add(cartaNueva);
        }

        mazo.setCartas(cartasActuales);
        Mazo guardado = mazoRepo.save(mazo);
        mazoBackupService.backupAll();
        return guardado;
    }

    private void validarMazo(List<Card> cartas) {
        if (cartas == null || cartas.size() != 60) {
            throw new IllegalArgumentException("Un mazo debe contener exactamente 60 cartas.");
        }
        boolean tieneBasico = cartas.stream().anyMatch(this::esPokemonBasico);
        if (!tieneBasico) {
            throw new IllegalArgumentException("El mazo debe incluir al menos 1 Pokemon Basico.");
        }
    }

    private boolean esPokemonBasico(Card card) {
        if (card == null || card.getSupertype() == null) {
            return false;
        }
        boolean esPokemon = "pokemon".equals(normalizar(card.getSupertype()));
        boolean esBasico = card.getSubtypes() != null
                && card.getSubtypes().stream().anyMatch(subtype -> "basic".equals(normalizar(subtype)));
        return esPokemon && esBasico;
    }

    private String normalizar(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
