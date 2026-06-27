package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
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
    @Transactional
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
        eagerlyLoadMazo(guardado);
        mazoBackupService.backupAll();
        return guardado;
    }

    /**
     * Actualiza un mazo existente.
     */
    @Transactional
    public Mazo actualizarMazo(Long id, String nombre, List<String> cartasIds) {
        // Usamos mazoRepo que es el nombre definido arriba
        Mazo mazo = mazoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mazo no encontrado con ID: " + id));

        mazo.setNombre(nombre);

        if (cartasIds == null || cartasIds.size() != 60) {
            throw new IllegalArgumentException("Un mazo debe contener exactamente 60 cartas.");
        }

        List<Card> nuevasCartas = new ArrayList<>();
        for (String cartaId : cartasIds) {
            Card card = cardRepo.findById(cartaId)
                    .orElseThrow(() -> new IllegalArgumentException("Carta no encontrada en la BD: " + cartaId));
            nuevasCartas.add(card);
        }
        validarMazo(nuevasCartas);

        mazo.setCartas(nuevasCartas);

        Mazo guardado = mazoRepo.save(mazo);
        eagerlyLoadMazo(guardado);
        mazoBackupService.backupAll();
        return guardado;
    }

    /**
     * Lista todos los mazos del jugador.
     */
    @Transactional(readOnly = true)
    public List<Mazo> listarMazos(String username) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }
        List<Mazo> mazos = mazoRepo.findByJugador(jugador);
        if (mazos != null) {
            for (Mazo mazo : mazos) {
                eagerlyLoadMazo(mazo);
            }
        }
        return mazos;
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

    @Transactional
    public Mazo debugInyectarCarta(Long mazoId, String cartaId, String cartaAReemplazarId) {
        Mazo mazo = mazoRepo.findById(mazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado con ID: " + mazoId));

        if (mazo.getJugador() == null || !mazo.getJugador().isAdmin()) {
            throw new SecurityException("Solo los administradores pueden usar God Mode.");
        }

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
        eagerlyLoadMazo(guardado);
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

        // Validar límite de 4 copias para cartas no-energía-básica (incluye Energías Especiales)
        Map<String, Long> conteo = cartas.stream()
                .collect(java.util.stream.Collectors.groupingBy(Card::getId, java.util.stream.Collectors.counting()));
        for (Map.Entry<String, Long> entry : conteo.entrySet()) {
            // Buscar la carta para determinar si es energía básica
            cartas.stream()
                    .filter(c -> c.getId().equals(entry.getKey()))
                    .findFirst()
                    .ifPresent(card -> {
                        boolean esEnergiaBasica = "energy".equals(normalizar(card.getSupertype()))
                                && card.getSubtypes() != null
                                && card.getSubtypes().stream().anyMatch(s -> "basic".equals(normalizar(s)));
                        if (!esEnergiaBasica && entry.getValue() > 4) {
                            throw new IllegalArgumentException(
                                    "No podés incluir más de 4 copias de \"" + card.getNombre() + "\" en un mazo.");
                        }
                    });
        }
    }
    private void eagerlyLoadMazo(Mazo mazo) {
        if (mazo != null && mazo.getCartas() != null) {
            mazo.getCartas().size();
            for (Card card : mazo.getCartas()) {
                if (card.getSubtypes() != null) card.getSubtypes().size();
                if (card.getReglas() != null) card.getReglas().size();
                if (card.getAtaques() != null) card.getAtaques().size();
                if (card.getDebilidades() != null) card.getDebilidades().size();
                if (card.getResistencias() != null) card.getResistencias().size();
            }
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
