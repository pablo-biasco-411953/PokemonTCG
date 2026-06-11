package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import org.junit.jupiter.api.Test;

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

    @Test
    void reconocePokemonAcentuadoComoBasicoYPreservaCopias() {
        MazoRepository mazoRepo = mock(MazoRepository.class);
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        CardRepository cardRepo = mock(CardRepository.class);
        MazoBackupService backupService = mock(MazoBackupService.class);
        MazoService service = new MazoService(mazoRepo, jugadorRepo, cardRepo, backupService);

        Jugador jugador = new Jugador("ash");
        Card basico = new Card();
        basico.setId("xy1-3");
        basico.setNombre("Weedle");
        basico.setSupertype("Pokémon");
        basico.setSubtypes(List.of("Basic"));

        List<String> ids = Collections.nCopies(60, basico.getId());
        when(jugadorRepo.findByUsername("ash")).thenReturn(jugador);
        when(cardRepo.findById(basico.getId())).thenReturn(Optional.of(basico));
        when(mazoRepo.save(any(Mazo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mazo guardado = service.guardarMazo("Bosque", "ash", ids);

        assertEquals(60, guardado.getCartas().size());
        assertSame(basico, guardado.getCartas().get(0));
        verify(backupService).backupAll();
    }

    @Test
    void actualizarMazoNoColapsaIdsRepetidos() {
        MazoRepository mazoRepo = mock(MazoRepository.class);
        JugadorRepository jugadorRepo = mock(JugadorRepository.class);
        CardRepository cardRepo = mock(CardRepository.class);
        MazoBackupService backupService = mock(MazoBackupService.class);
        MazoService service = new MazoService(mazoRepo, jugadorRepo, cardRepo, backupService);

        Card basico = new Card();
        basico.setId("xy1-3");
        basico.setSupertype("Pokemon");
        basico.setSubtypes(List.of("Basic"));
        Mazo existente = new Mazo("Viejo", new Jugador("ash"));

        when(mazoRepo.findById(1L)).thenReturn(Optional.of(existente));
        when(cardRepo.findById(basico.getId())).thenReturn(Optional.of(basico));
        when(mazoRepo.save(any(Mazo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mazo actualizado = service.actualizarMazo(
                1L,
                "Nuevo",
                Collections.nCopies(60, basico.getId())
        );

        assertEquals(60, actualizado.getCartas().size());
        assertEquals("Nuevo", actualizado.getNombre());
    }
}
