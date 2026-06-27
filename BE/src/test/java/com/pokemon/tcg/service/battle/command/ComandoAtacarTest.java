package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.BattleAttackService;
import com.pokemon.tcg.service.BattleKoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ComandoAtacarTest {

    private TableroJugador tableroAtacante;
    private TableroJugador tableroDefensor;
    private BattleAttackService attackService;
    private BattleKoService koService;
    private Partida partida;

    @BeforeEach
    void setUp() {
        tableroAtacante = new TableroJugador();
        tableroDefensor = new TableroJugador();
        attackService = mock(BattleAttackService.class);
        koService = mock(BattleKoService.class);

        partida = new Partida(tableroAtacante, tableroDefensor);
        partida.setJugadorUsername("ash");
        partida.setBotUsername("BOT");
    }

    private Card crearCardPokemon(String id, String nombre, String hp) {
        Card card = new Card();
        card.setId(id);
        card.setNombre(nombre);
        card.setHp(hp);
        card.setTipo("Basic Pokemon");
        return card;
    }

    private Ataque crearAtaque(String nombre, int danio, List<String> costo) {
        Ataque ataque = new Ataque();
        ataque.setNombre(nombre);
        ataque.setDanio(danio);
        ataque.setTiposEnergia(costo);
        return ataque;
    }

    @Test
    void puedeEjecutar_conActivoNull_retornaFalse() {
        ComandoAtacar comando = new ComandoAtacar("Ataque", tableroAtacante, tableroDefensor, attackService, koService, null);

        // Caso 1: Atacante null, Defensor null
        assertFalse(comando.puedeEjecutar(partida));

        // Caso 2: Atacante con activo, Defensor null
        tableroAtacante.setActivo(new CartaEnJuego(crearCardPokemon("1", "Pikachu", "60")));
        assertFalse(comando.puedeEjecutar(partida));

        // Caso 3: Atacante null, Defensor con activo
        tableroAtacante.setActivo(null);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));
        assertFalse(comando.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_conCondicionesEspecialesBloqueantes_retornaFalse() {
        ComandoAtacar comando = new ComandoAtacar("Ataque", tableroAtacante, tableroDefensor, attackService, koService, null);

        CartaEnJuego activoAtacante = new CartaEnJuego(crearCardPokemon("1", "Pikachu", "60"));
        tableroAtacante.setActivo(activoAtacante);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        // Caso 1: Dormido (Asleep)
        activoAtacante.agregarCondicion("Asleep");
        assertFalse(comando.puedeEjecutar(partida));

        // Caso 2: Paralizado (Paralyzed)
        activoAtacante.getCondicionesEspeciales().clear();
        activoAtacante.agregarCondicion("Paralyzed");
        assertFalse(comando.puedeEjecutar(partida));
    }

    @Test
    void puedeEjecutar_normal_retornaTrue() {
        ComandoAtacar comando = new ComandoAtacar("Ataque", tableroAtacante, tableroDefensor, attackService, koService, null);

        CartaEnJuego activoAtacante = new CartaEnJuego(crearCardPokemon("1", "Pikachu", "60"));
        tableroAtacante.setActivo(activoAtacante);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        assertTrue(comando.puedeEjecutar(partida));

        // Caso con condicion no bloqueante (e.g. Poisoned)
        activoAtacante.agregarCondicion("Poisoned");
        assertTrue(comando.puedeEjecutar(partida));
    }

    @Test
    void ejecutar_defensorNull_lanzaIllegalStateException() {
        ComandoAtacar comando = new ComandoAtacar("Ataque", tableroAtacante, tableroDefensor, attackService, koService, null);

        tableroAtacante.setActivo(new CartaEnJuego(crearCardPokemon("1", "Pikachu", "60")));
        tableroDefensor.setActivo(null);

        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_defensorInvulnerable_retornaSinAtacar() {
        ComandoAtacar comando = new ComandoAtacar("Ataque", tableroAtacante, tableroDefensor, attackService, koService, null);

        tableroAtacante.setActivo(new CartaEnJuego(crearCardPokemon("1", "Pikachu", "60")));
        CartaEnJuego defensor = new CartaEnJuego(crearCardPokemon("2", "Charmander", "50"));
        defensor.setInvulnerable(true);
        tableroDefensor.setActivo(defensor);

        partida.setUltimasMonedasLanzadas(List.of(true));

        comando.ejecutar(partida);

        assertTrue(partida.getUltimasMonedasLanzadas().isEmpty());
        verify(attackService, never()).resolveAttack(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ejecutar_atacanteNull_lanzaIllegalStateException() {
        ComandoAtacar comando = new ComandoAtacar("Ataque", tableroAtacante, tableroDefensor, attackService, koService, null);

        tableroAtacante.setActivo(null);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_atacanteDormidoOParalizado_lanzaIllegalStateException() {
        ComandoAtacar comando = new ComandoAtacar("Ataque", tableroAtacante, tableroDefensor, attackService, koService, null);

        CartaEnJuego atacante = new CartaEnJuego(crearCardPokemon("1", "Pikachu", "60"));
        tableroAtacante.setActivo(atacante);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        // Caso 1: Dormido
        atacante.agregarCondicion("Asleep");
        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));

        // Caso 2: Paralizado
        atacante.getCondicionesEspeciales().clear();
        atacante.agregarCondicion("Paralyzed");
        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_ataqueNoEncontrado_lanzaIllegalStateException() {
        ComandoAtacar comando = new ComandoAtacar("Impactrueno", tableroAtacante, tableroDefensor, attackService, koService, null);

        CartaEnJuego atacante = new CartaEnJuego(crearCardPokemon("1", "Pikachu", "60"));
        tableroAtacante.setActivo(atacante);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_ataqueBloqueadoSiguienteTurno_lanzaIllegalStateException() {
        ComandoAtacar comando = new ComandoAtacar("Impactrueno", tableroAtacante, tableroDefensor, attackService, koService, null);

        Card card = crearCardPokemon("1", "Pikachu", "60");
        Ataque ataque = crearAtaque("Impactrueno", 30, Collections.emptyList());
        card.reemplazarAtaques(List.of(ataque));

        CartaEnJuego atacante = new CartaEnJuego(card);
        atacante.setAtaqueBloqueadoSiguienteTurno("Impactrueno");

        tableroAtacante.setActivo(atacante);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_energiasInsuficientes_lanzaIllegalStateException() {
        ComandoAtacar comando = new ComandoAtacar("Impactrueno", tableroAtacante, tableroDefensor, attackService, koService, null);

        Card card = crearCardPokemon("1", "Pikachu", "60");
        Ataque ataque = crearAtaque("Impactrueno", 30, List.of("Lightning"));
        card.reemplazarAtaques(List.of(ataque));

        CartaEnJuego atacante = new CartaEnJuego(card);
        // Sin energías unidas

        tableroAtacante.setActivo(atacante);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        assertThrows(IllegalStateException.class, () -> comando.ejecutar(partida));
    }

    @Test
    void ejecutar_lanzaMonedaSiAtaca_monedaFalla() {
        // Necesitamos interceptar las monedas en partida
        Partida spyPartida = spy(partida);

        // Simulamos lista de monedas que devuelve false (tails) al evaluar pudoAtacar
        List<Boolean> monedasFake = new ArrayList<>() {
            @Override
            public boolean isEmpty() {
                return false;
            }
            @Override
            public Boolean get(int index) {
                return false;
            }
        };
        doReturn(monedasFake).when(spyPartida).getUltimasMonedasLanzadas();

        ComandoAtacar comando = new ComandoAtacar("Impactrueno", tableroAtacante, tableroDefensor, attackService, koService, null);

        Card card = crearCardPokemon("1", "Pikachu", "60");
        Ataque ataque = crearAtaque("Impactrueno", 30, Collections.emptyList());
        card.reemplazarAtaques(List.of(ataque));

        CartaEnJuego atacante = new CartaEnJuego(card);
        atacante.setDebeLanzarMonedaSiAtaca(true);

        tableroAtacante.setActivo(atacante);
        tableroDefensor.setActivo(new CartaEnJuego(crearCardPokemon("2", "Charmander", "50")));

        // Ejecutar
        comando.ejecutar(spyPartida);

        assertFalse(atacante.isDebeLanzarMonedaSiAtaca()); // Se limpió el flag
        verify(attackService).registrarEventoMoneda(eq(spyPartida), anyString(), eq("Impactrueno"));
        // Nunca debió llamarse a resolveAttack
        verify(attackService, never()).resolveAttack(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ejecutar_lanzaMonedaSiAtaca_monedaExito() {
        Partida spyPartida = spy(partida);

        // Simulamos lista de monedas que devuelve true (heads)
        List<Boolean> monedasFake = new ArrayList<>() {
            @Override
            public boolean isEmpty() {
                return false;
            }
            @Override
            public Boolean get(int index) {
                return true;
            }
        };
        doReturn(monedasFake).when(spyPartida).getUltimasMonedasLanzadas();

        ComandoAtacar comando = new ComandoAtacar("Impactrueno", tableroAtacante, tableroDefensor, attackService, koService, null);

        Card card = crearCardPokemon("1", "Pikachu", "60");
        Ataque ataque = crearAtaque("Impactrueno", 30, Collections.emptyList());
        card.reemplazarAtaques(List.of(ataque));

        CartaEnJuego atacante = new CartaEnJuego(card);
        atacante.setDebeLanzarMonedaSiAtaca(true);

        tableroAtacante.setActivo(atacante);
        CartaEnJuego defensor = new CartaEnJuego(crearCardPokemon("2", "Charmander", "50"));
        tableroDefensor.setActivo(defensor);

        BattleAttackService.AttackResolution mockResolution = mock(BattleAttackService.AttackResolution.class);
        when(mockResolution.historialMonedas()).thenReturn(List.of(true));
        when(attackService.resolveAttack(eq(spyPartida), eq(ataque), eq(atacante), eq(defensor), any(), any()))
                .thenReturn(mockResolution);

        // Ejecutar
        comando.ejecutar(spyPartida);

        assertFalse(atacante.isDebeLanzarMonedaSiAtaca());
        verify(attackService, times(2)).registrarEventoMoneda(eq(spyPartida), anyString(), eq("Impactrueno"));
        verify(attackService).resolveAttack(eq(spyPartida), eq(ataque), eq(atacante), eq(defensor), any(), any());
    }

    @Test
    void ejecutar_flujoNormalExitoso() {
        ComandoAtacar comando = new ComandoAtacar("Impactrueno", tableroAtacante, tableroDefensor, attackService, koService, "extra-param");

        Card card = crearCardPokemon("1", "Pikachu", "60");
        Ataque ataque = crearAtaque("Impactrueno", 30, Collections.emptyList());
        card.reemplazarAtaques(List.of(ataque));

        CartaEnJuego atacante = new CartaEnJuego(card);
        tableroAtacante.setActivo(atacante);

        CartaEnJuego defensor = new CartaEnJuego(crearCardPokemon("2", "Charmander", "50"));
        tableroDefensor.setActivo(defensor);

        BattleAttackService.AttackResolution mockResolution = mock(BattleAttackService.AttackResolution.class);
        when(mockResolution.historialMonedas()).thenReturn(Collections.emptyList());
        when(attackService.resolveAttack(eq(partida), eq(ataque), eq(atacante), eq(defensor), any(), eq("extra-param")))
                .thenReturn(mockResolution);

        // Ejecutar
        comando.ejecutar(partida);

        verify(attackService).resolveAttack(eq(partida), eq(ataque), eq(atacante), eq(defensor), any(), eq("extra-param"));
        verify(attackService, never()).registrarEventoMoneda(any(), any(), any());
    }

    @Test
    void getNombre_retornaFormatoEsperado() {
        ComandoAtacar comando = new ComandoAtacar("Impactrueno", tableroAtacante, tableroDefensor, attackService, koService, null);
        assertEquals("Atacar[Impactrueno]", comando.getNombre());
    }
}
