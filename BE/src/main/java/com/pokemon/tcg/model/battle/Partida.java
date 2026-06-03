package com.pokemon.tcg.model.battle;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Representa la partida completa que viaja entre backend y frontend.
 */
public class Partida {
    private String id; // UUID
    private TableroJugador jugador;
    private TableroJugador bot;
    private Turno turnoActual;
    private Fase faseActual;
    private boolean yaSeRetiroEsteTurno = false;
    private int mulligansJugador = 0;
    private int mulligansBot = 0;

    private String jugadorUsername;
    private String botUsername;
    private boolean coinFlipped = false;
    private String coinFlipWinner;
    private String coinFlipResult;
    private String coinFlipCallerUsername;

    // 🚩 ACÁ GUARDAMOS LA "VERDAD" DE LAS MONEDAS
    private List<Boolean> ultimasMonedasLanzadas = new ArrayList<>();

    public enum Turno { JUGADOR, BOT }
    public enum Fase { INICIO, LANZAMIENTO_MONEDA, TURNO_NORMAL, FIN_PARTIDA }

    public Partida(TableroJugador jugador, TableroJugador bot) {
        // Cada partida vive en memoria y se identifica por UUID.
        this.id = UUID.randomUUID().toString();
        this.jugador = jugador;
        this.bot = bot;
        this.turnoActual = Turno.JUGADOR;
        this.faseActual = Fase.INICIO;
    }

    // --- GETTERS Y SETTERS ---

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }

    public TableroJugador getJugador() { return jugador; }
    public void setJugador(TableroJugador jugador) { this.jugador = jugador; }

    public TableroJugador getBot() { return bot; }
    public void setBot(TableroJugador bot) { this.bot = bot; }

    public Turno getTurnoActual() { return turnoActual; }
    public void setTurnoActual(Turno turnoActual) { this.turnoActual = turnoActual; }

    public Fase getFaseActual() { return faseActual; }
    public void setFaseActual(Fase faseActual) { this.faseActual = faseActual; }

    public boolean isYaSeRetiroEsteTurno() { return yaSeRetiroEsteTurno; }
    public void setYaSeRetiroEsteTurno(boolean yaSeRetiroEsteTurno) {
        this.yaSeRetiroEsteTurno = yaSeRetiroEsteTurno;
    }

    public int getMulligansJugador() { return mulligansJugador; }
    public void setMulligansJugador(int mulligansJugador) { this.mulligansJugador = mulligansJugador; }

    public int getMulligansBot() { return mulligansBot; }
    public void setMulligansBot(int mulligansBot) { this.mulligansBot = mulligansBot; }

    // 🚩 GETTER Y SETTER DE LAS MONEDAS PARA QUE VIAJEN A ANGULAR
    public List<Boolean> getUltimasMonedasLanzadas() {
        return ultimasMonedasLanzadas;
    }

    public void setUltimasMonedasLanzadas(List<Boolean> ultimasMonedasLanzadas) {
        this.ultimasMonedasLanzadas = ultimasMonedasLanzadas;
    }

    public String getJugadorUsername() { return jugadorUsername; }
    public void setJugadorUsername(String jugadorUsername) { this.jugadorUsername = jugadorUsername; }

    public String getBotUsername() { return botUsername; }
    public void setBotUsername(String botUsername) { this.botUsername = botUsername; }

    public boolean isCoinFlipped() { return coinFlipped; }
    public void setCoinFlipped(boolean coinFlipped) { this.coinFlipped = coinFlipped; }

    public String getCoinFlipWinner() { return coinFlipWinner; }
    public void setCoinFlipWinner(String coinFlipWinner) { this.coinFlipWinner = coinFlipWinner; }

    public String getCoinFlipResult() { return coinFlipResult; }
    public void setCoinFlipResult(String coinFlipResult) { this.coinFlipResult = coinFlipResult; }

    public String getCoinFlipCallerUsername() { return coinFlipCallerUsername; }
    public void setCoinFlipCallerUsername(String coinFlipCallerUsername) { this.coinFlipCallerUsername = coinFlipCallerUsername; }
}
