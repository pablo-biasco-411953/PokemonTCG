package com.pokemon.tcg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MazoBackupServiceTest {

    @TempDir
    Path tempDir;

    private MazoRepository mazoRepo;
    private JugadorRepository jugadorRepo;
    private CardRepository cardRepo;
    private MazoBackupService service;

    @BeforeEach
    void setUp() {
        mazoRepo = mock(MazoRepository.class);
        jugadorRepo = mock(JugadorRepository.class);
        cardRepo = mock(CardRepository.class);
        String backupPath = tempDir.resolve("mazos-backup.json").toString();
        service = new MazoBackupService(mazoRepo, jugadorRepo, cardRepo, new ObjectMapper(), backupPath);
    }

    private Card card(String id) {
        Card c = new Card();
        c.setId(id);
        return c;
    }

    private Mazo mazoCompleto(String nombre, String username) {
        Jugador j = new Jugador(username);
        Mazo m = new Mazo(nombre, j);
        List<Card> cartas = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cartas.add(card("xy1-" + (i + 1)));
        }
        m.setCartas(cartas);
        return m;
    }

    @Test
    void backupAll_sinMazos_noLanzaExcepcion() {
        when(mazoRepo.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> service.backupAll());
    }

    @Test
    void backupAll_conMazosValidos_creaArchivoBackup() {
        Mazo m = mazoCompleto("Bosque", "ash");
        when(mazoRepo.findAll()).thenReturn(List.of(m));

        service.backupAll();

        assertTrue(tempDir.resolve("mazos-backup.json").toFile().exists());
    }

    @Test
    void backupAll_mazoSinJugador_seIgnora() {
        Mazo m = new Mazo("Bosque", null);
        m.setCartas(Collections.nCopies(60, card("xy1-1")));
        when(mazoRepo.findAll()).thenReturn(List.of(m));

        assertDoesNotThrow(() -> service.backupAll());
    }

    @Test
    void backupAll_mazoSinCartas_seIgnora() {
        Jugador j = new Jugador("ash");
        Mazo m = new Mazo("Bosque", j);
        m.setCartas(null);
        when(mazoRepo.findAll()).thenReturn(List.of(m));

        assertDoesNotThrow(() -> service.backupAll());
    }

    @Test
    void backupAll_mazoConMenos60Cartas_noSeGuardaEnBackup() throws Exception {
        Jugador j = new Jugador("ash");
        Mazo m = new Mazo("Bosque", j);
        m.setCartas(Collections.nCopies(10, card("xy1-1")));
        when(mazoRepo.findAll()).thenReturn(List.of(m));

        service.backupAll();

        Path backupFile = tempDir.resolve("mazos-backup.json");
        String content = new String(java.nio.file.Files.readAllBytes(backupFile));
        assertEquals("[ ]", content.trim());
    }

    @Test
    void restoreMissingDecks_sinArchivoBackup_noLanzaExcepcion() {
        assertDoesNotThrow(() -> service.restoreMissingDecks());
    }

    @Test
    void restoreMissingDecks_restauraMazoPerdido() throws Exception {
        Mazo m = mazoCompleto("Bosque", "ash");
        when(mazoRepo.findAll()).thenReturn(List.of(m));
        service.backupAll();

        Jugador j = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(mazoRepo.findByJugador(j)).thenReturn(List.of());
        for (int i = 0; i < 60; i++) {
            Card c = card("xy1-" + (i + 1));
            when(cardRepo.findById("xy1-" + (i + 1))).thenReturn(Optional.of(c));
        }
        when(mazoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.restoreMissingDecks();

        verify(mazoRepo).save(any(Mazo.class));
    }

    @Test
    void restoreMissingDecks_mazoYaExiste_noSeDuplica() throws Exception {
        Mazo m = mazoCompleto("Bosque", "ash");
        when(mazoRepo.findAll()).thenReturn(List.of(m));
        service.backupAll();

        Jugador j = new Jugador("ash");
        when(jugadorRepo.findByUsername("ash")).thenReturn(j);
        when(mazoRepo.findByJugador(j)).thenReturn(List.of(m));

        service.restoreMissingDecks();

        verify(mazoRepo, never()).save(any());
    }

    @Test
    void restoreMissingDecks_jugadorNoExiste_seIgnora() throws Exception {
        Mazo m = mazoCompleto("Bosque", "noexiste");
        when(mazoRepo.findAll()).thenReturn(List.of(m));
        service.backupAll();

        when(jugadorRepo.findByUsername("noexiste")).thenReturn(null);

        assertDoesNotThrow(() -> service.restoreMissingDecks());
        verify(mazoRepo, never()).save(any());
    }
}
