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
        // Generamos un ID por defecto para que nunca empiece en null
        this.id = UUID.randomUUID().toString();
        this.jugador = jugador;
        this.bot = bot;
        this.turnoActual = Turno.JUGADOR;
        this.faseActual = Fase.INICIO;
    }

    // --- GETTERS Y SETTERS ---

    // Este es el mÃ©todo que te pedÃ­a el compilador:
    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public TableroJugador getJugador() { return jugador; }
    public void setJugador(TableroJugador jugador) { this.jugador = jugador; }

    public TableroJugador getBot() { return bot; }
    public void setBot(TableroJugador bot) { this.bot = bot; }

    public Turno getTurnoActual() { return turnoActual; }
    public void setTurnoActual(Turno turnoActual) { this.turnoActual = turnoActual; }

    public Fase getFaseActual() { return faseActual; }
    public void setFaseActual(Fase faseActual) { this.faseActual = faseActual; }
}
