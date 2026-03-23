package com.pokemon.tcg.model.battle;

import java.util.UUID;

/**
 * Representa una partida en curso.
 */
public class Partida {
    private String id; // UUID
    private TableroJugador jugador;
    private TableroJugador bot;
    private Turno turnoActual;
    private Fase faseActual;

    public enum Turno { JUGADOR, BOT }
    public enum Fase { INICIO, LANZAMIENTO_MONEDA, TURNO_NORMAL, FIN_PARTIDA }

    public Partida(TableroJugador jugador, TableroJugador bot) {
        this.id = UUID.randomUUID().toString();
        this.jugador = jugador;
        this.bot = bot;
        this.turnoActual = Turno.JUGADOR; // por defecto
        this.faseActual = Fase.INICIO;
    }

    // getters y setters
    public String getId() { return id; }
    public TableroJugador getJugador() { return jugador; }
    public TableroJugador getBot() { return bot; }
    public Turno getTurnoActual() { return turnoActual; }
    public void setTurnoActual(Turno turnoActual) { this.turnoActual = turnoActual; }
    public Fase getFaseActual() { return faseActual; }
    public void setFaseActual(Fase faseActual) { this.faseActual = faseActual; }
}
