package com.pokemon.tcg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class MazoBackupService {
    private final MazoRepository mazoRepo;
    private final JugadorRepository jugadorRepo;
    private final CardRepository cardRepo;
    private final ObjectMapper objectMapper;
    private final Path backupPath;

    public MazoBackupService(
            MazoRepository mazoRepo,
            JugadorRepository jugadorRepo,
            CardRepository cardRepo,
            ObjectMapper objectMapper,
            @Value("${app.mazos.backup-path:data/mazos-backup.json}") String backupPath
    ) {
        this.mazoRepo = mazoRepo;
        this.jugadorRepo = jugadorRepo;
        this.cardRepo = cardRepo;
        this.objectMapper = objectMapper;
        this.backupPath = Path.of(backupPath);
    }

    @Transactional(readOnly = true)
    public void backupAll() {
        try {
            List<MazoBackupEntry> entries = new ArrayList<>();
            for (Mazo mazo : mazoRepo.findAll()) {
                if (mazo.getJugador() == null || mazo.getCartas() == null) continue;
                MazoBackupEntry entry = new MazoBackupEntry();
                entry.nombre = mazo.getNombre();
                entry.username = mazo.getJugador().getUsername();
                entry.cartas = mazo.getCartas().stream()
                        .filter(card -> card != null && card.getId() != null)
                        .map(Card::getId)
                        .toList();
                if (entry.username != null && entry.cartas.size() == 60) {
                    entries.add(entry);
                }
            }
            Files.createDirectories(backupPath.toAbsolutePath().getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(backupPath.toFile(), entries);
        } catch (Exception e) {
            System.out.println("[MazoBackup] No se pudo escribir backup de mazos: " + e.getMessage());
        }
    }

    @Transactional
    public void restoreMissingDecks() {
        if (!Files.exists(backupPath)) return;
        try {
            MazoBackupEntry[] entries = objectMapper.readValue(backupPath.toFile(), MazoBackupEntry[].class);
            for (MazoBackupEntry entry : entries) {
                if (entry == null || entry.username == null || entry.cartas == null || entry.cartas.size() != 60) continue;
                Jugador jugador = jugadorRepo.findByUsername(entry.username);
                if (jugador == null) continue;
                boolean alreadyExists = mazoRepo.findByJugador(jugador).stream()
                        .anyMatch(mazo -> entry.nombre != null && entry.nombre.equals(mazo.getNombre()));
                if (alreadyExists) continue;

                List<Card> cartas = new ArrayList<>();
                for (String cardId : entry.cartas) {
                    cardRepo.findById(cardId).ifPresent(cartas::add);
                }
                if (cartas.size() != 60) continue;

                Mazo mazo = new Mazo(entry.nombre == null || entry.nombre.isBlank() ? "Mazo restaurado" : entry.nombre, jugador);
                mazo.setCartas(cartas);
                mazoRepo.save(mazo);
                System.out.println("[MazoBackup] Mazo restaurado para " + entry.username + ": " + mazo.getNombre());
            }
        } catch (Exception e) {
            System.out.println("[MazoBackup] No se pudo restaurar backup de mazos: " + e.getMessage());
        }
    }

    public static class MazoBackupEntry {
        public String username;
        public String nombre;
        public List<String> cartas = new ArrayList<>();
    }
}
