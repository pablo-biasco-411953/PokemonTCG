package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.model.battle.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleEngineService {
    private final JugadorRepository jugadorRepo;
    private final MazoRepository mazoRepo;
    private final Random random = new Random();
    private final Map<String, Partida> partidasEnCurso = new ConcurrentHashMap<>();

    public BattleEngineService(JugadorRepository jugadorRepo, MazoRepository mazoRepo) {
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
    }

    /**
     * Inicia una partida entre el jugador y un bot.
     * Asigna el mazo seleccionado por el jugador.
     */
    public Partida startBattle(String username, Long mazoId) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + username);
        }

        // Obtener el mazo del jugador
        Mazo mazoSeleccionado = mazoRepo.findById(mazoId).orElse(null);
        if (mazoSeleccionado == null) {
            throw new IllegalArgumentException("Mazo no encontrado: " + mazoId);
        }

        // Verificar que el mazo pertenece al jugador
        if (!mazoSeleccionado.getJugador().equals(jugador)) {
            throw new IllegalArgumentException("El mazo no pertenece al jugador especificado");
        }

        // Crear tableros vacíos y asignar cartas del mazo seleccionado
        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        // Asignar el mazo del jugador al tablero
        List<Card> mazoJugador = mazoSeleccionado.getCartas();
        if (mazoJugador.size() != 60) {
            throw new IllegalStateException("El mazo debe tener exactamente 60 cartas. Actualmente tiene: " + mazoJugador.size());
        }
        tableroJugador.setMazo(new java.util.ArrayList<>(mazoJugador));

        // Generar un mazo aleatorio para el bot (simulación)
        List<Card> collectionCopy = jugador.getColeccion();
        if (collectionCopy.size() < 60) {
            throw new IllegalStateException("El jugador no tiene suficientes cartas para armar un mazo. Necesita al menos 60.");
        }

        java.util.Collections.shuffle(collectionCopy);
        List<Card> mazoBot = collectionCopy.subList(0, 60);
        tableroBot.setMazo(new java.util.ArrayList<>(mazoBot));

        Partida partida = new Partida(tableroJugador, tableroBot);

        // Lanzamiento de moneda para determinar quién comienza
        boolean jugadorGanaMoneda = random.nextBoolean();
        if (jugadorGanaMoneda) {
            partida.setFaseActual(Partida.Fase.LANZAMIENTO_MONEDA);
        } else {
            partida.setFaseActual(Partida.Fase.LANZAMIENTO_MONEDA);
        }

        // Guardar partida en curso
        partidasEnCurso.put(partida.getId(), partida);

        return partida;
    }

    /**
     * Realiza el lanzamiento de moneda para determinar quién comienza.
     */
    public boolean lanzarMoneda(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) {
            throw new IllegalArgumentException("Partida no encontrada: " + matchId);
        }

        boolean jugadorGana = random.nextBoolean();
        partida.setFaseActual(Partida.Fase.TURNO_NORMAL);
        return jugadorGana;
    }

    /**
     * Permite al jugador elegir si quiere ir primero o segundo después del lanzamiento de moneda.
     */
    public void elegirTurno(String matchId, boolean vaPrimero) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) {
            throw new IllegalArgumentException("Partida no encontrada: " + matchId);
        }

        if (vaPrimero) {
            partida.setTurnoActual(Partida.Turno.JUGADOR);
        } else {
            partida.setTurnoActual(Partida.Turno.BOT);
        }
    }

    /**
     * Obtiene el estado actual de una partida.
     */
    public Partida getEstadoPartida(String matchId) {
        return partidasEnCurso.get(matchId);
    }
    
    /**
     * Realiza el ataque entre dos cartas en juego.
     */
    public void realizarAtaque(CartaEnJuego atacante, CartaEnJuego defensor, Ataque ataque) {
        // Verificar que el atacante tiene suficientes energías
        if (!tieneEnergiaSuficiente(atacante, ataque)) {
            throw new IllegalArgumentException("El atacante no tiene energía suficiente para ejecutar este ataque");
        }
        
        // Calcular daño base
        int danioBase = ataque.getDanio();
        
        // Aplicar modificadores de tipo (debilidad/resistencia)
        int danioFinal = calcularDaño(atacante, defensor, danioBase);
        
        // Restar daño a la vida del defensor
        defensor.setHpActual(defensor.getHpActual() - danioFinal);
        
        // Verificar si el defensor fue eliminado (K.O.)
        if (defensor.getHpActual() <= 0) {
            // El Pokémon es eliminado y va al descarte
            // Este método se completará en Fase 5
        }
    }
    
    /**
     * Verifica si el atacante tiene suficiente energía para ejecutar el ataque.
     */
    private boolean tieneEnergiaSuficiente(CartaEnJuego atacante, Ataque ataque) {
        // Implementar lógica de verificación de energías
        // Esta es una implementación simplificada
        return true;
    }
    
    /**
     * Calcula el daño final considerando debilidades y resistencias.
     */
    private int calcularDaño(CartaEnJuego atacante, CartaEnJuego defensor, int danioBase) {
        // Esta es una implementación simplificada
        // En la implementación real, se debe comparar tipos y aplicar modificadores
        
        return danioBase;
    }
}
