package com.pokemon.tcg.model.battle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pokemon.tcg.model.battle.state.*;
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
    private String ganador;
    private String razonFinPartida;
    private long jugadorLastSeenAt = System.currentTimeMillis();
    private long botLastSeenAt = System.currentTimeMillis();
    private int coinHandshakeJugadorPower = 0;
    private int coinHandshakeBotPower = 0;
    private boolean coinHandshakeJugadorHolding = false;
    private boolean coinHandshakeBotHolding = false;
    private boolean coinHandshakeComplete = false;

    private transient EstadoPartida estado;

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

    @JsonIgnore
    public EstadoPartida getEstado() {
        if (estado == null) {
            estado = switch (faseActual) {
                case INICIO -> new EstadoInicio();
                case LANZAMIENTO_MONEDA -> new EstadoLanzamientoMoneda();
                case TURNO_NORMAL -> new EstadoTurnoNormal();
                case FIN_PARTIDA -> new EstadoFinPartida();
            };
        }
        return estado;
    }

    public void transicionarA(EstadoPartida nuevoEstado) {
        this.estado = nuevoEstado;
        this.faseActual = nuevoEstado.getFase();
    }

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

    public String getGanador() { return ganador; }
    public void setGanador(String ganador) { this.ganador = ganador; }

    public String getRazonFinPartida() { return razonFinPartida; }
    public void setRazonFinPartida(String razonFinPartida) { this.razonFinPartida = razonFinPartida; }

    public long getJugadorLastSeenAt() { return jugadorLastSeenAt; }
    public void setJugadorLastSeenAt(long jugadorLastSeenAt) { this.jugadorLastSeenAt = jugadorLastSeenAt; }

    public long getBotLastSeenAt() { return botLastSeenAt; }
    public void setBotLastSeenAt(long botLastSeenAt) { this.botLastSeenAt = botLastSeenAt; }

    public int getCoinHandshakeJugadorPower() { return coinHandshakeJugadorPower; }
    public void setCoinHandshakeJugadorPower(int coinHandshakeJugadorPower) {
        this.coinHandshakeJugadorPower = Math.max(0, Math.min(100, coinHandshakeJugadorPower));
    }

    public int getCoinHandshakeBotPower() { return coinHandshakeBotPower; }
    public void setCoinHandshakeBotPower(int coinHandshakeBotPower) {
        this.coinHandshakeBotPower = Math.max(0, Math.min(100, coinHandshakeBotPower));
    }

    public boolean isCoinHandshakeJugadorHolding() { return coinHandshakeJugadorHolding; }
    public void setCoinHandshakeJugadorHolding(boolean coinHandshakeJugadorHolding) {
        this.coinHandshakeJugadorHolding = coinHandshakeJugadorHolding;
    }

    public boolean isCoinHandshakeBotHolding() { return coinHandshakeBotHolding; }
    public void setCoinHandshakeBotHolding(boolean coinHandshakeBotHolding) {
        this.coinHandshakeBotHolding = coinHandshakeBotHolding;
    }

    public boolean isCoinHandshakeComplete() { return coinHandshakeComplete; }
    public void setCoinHandshakeComplete(boolean coinHandshakeComplete) { this.coinHandshakeComplete = coinHandshakeComplete; }
}
