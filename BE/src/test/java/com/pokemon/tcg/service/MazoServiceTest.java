package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MazoServiceTest {

    /** Crea lista de 60 IDs con 15 cartas distintas x4 copias (respeta regla de 4 cópias máx). */
    private List<String> ids60Validos(String prefijo) {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            for (int j = 0; j < 4; j++) {
                ids.add(prefijo + i);
            }
        }
        return ids;
    }

    @Test
    void reconocePokemonAcentuadoComoBasicoYPreservaCopias() {
        MazoRepository mazoRepo = mock(MazoRepository.class);
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        CardRepository cardRepo = mock(CardRepository.class);
        MazoBackupService backupService = mock(MazoBackupService.class);
        MazoService service = new MazoService(mazoRepo, jugadorRepo, cardRepo, backupService);

        Jugador jugador = new Jugador("ash");

        // Crear 15 Pokémon básicos distintos con prefijo, 4 copias cada uno = 60 cartas
        List<Card> pokemons = new ArrayList<>();
        Card primero = null;
        for (int i = 1; i <= 15; i++) {
            Card c = new Card();
            c.setId("xy1-" + i);
            c.setNombre("Pokémon" + i);
            c.setSupertype("Pokémon");
            c.setSubtypes(List.of("Basic"));
            pokemons.add(c);
            if (i == 1) primero = c;
            when(cardRepo.findById("xy1-" + i)).thenReturn(Optional.of(c));
        }
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(mazoRepo.save(any(Mazo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mazo guardado = service.guardarMazo("Bosque", "ash", ids60Validos("xy1-"));

        assertEquals(60, guardado.getCartas().size());
        assertSame(primero, guardado.getCartas().get(0));
        verify(backupService).backupAll();
    }

    @Test
    void actualizarMazoNoColapsaIdsRepetidos() {
        MazoRepository mazoRepo = mock(MazoRepository.class);
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        CardRepository cardRepo = mock(CardRepository.class);
        MazoBackupService backupService = mock(MazoBackupService.class);
        MazoService service = new MazoService(mazoRepo, jugadorRepo, cardRepo, backupService);

        // 15 Pokémon distintos x4 copias = 60 cartas, respetando límite de 4 copias
        List<String> ids = ids60Validos("xy1-");
        Mazo existente = new Mazo("Viejo", new Jugador("ash"));

        when(mazoRepo.findById(1L)).thenReturn(Optional.of(existente));
        for (int i = 1; i <= 15; i++) {
            Card c = new Card();
            c.setId("xy1-" + i);
            c.setSupertype("Pokemon");
            c.setSubtypes(List.of("Basic"));
            when(cardRepo.findById("xy1-" + i)).thenReturn(Optional.of(c));
        }
        when(mazoRepo.save(any(Mazo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mazo actualizado = service.actualizarMazo(1L, "Nuevo", ids);

        assertEquals(60, actualizado.getCartas().size());
        assertEquals("Nuevo", actualizado.getNombre());
    }
}
