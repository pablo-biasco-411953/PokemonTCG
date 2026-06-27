package com.pokemon.tcg.model.battle;

import com.pokemon.tcg.model.Card;
import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa todas las zonas visibles de un jugador dentro de una partida.
 */
public class TableroJugador {
    // Mazo oculto
    private List<Card> mazo = new ArrayList<>();
    // Mano privada (revelada solo al jugador)
    private List<Card> mano = new ArrayList<>();
    // Premios (6, ocultos)
    private List<Card> premios = new ArrayList<>();
    private CartaEnJuego activo;
    private List<CartaEnJuego> banca = new ArrayList<>();
    // Pila de descarte
    private List<Card> pilaDescarte = new ArrayList<>();

    public TableroJugador() {}

    // getters y setters
    public List<Card> getMazo() { return mazo; }
    public void setMazo(List<Card> mazo) { this.mazo = mazo; }
    public List<Card> getMano() { return mano; }
    public void setMano(List<Card> mano) { this.mano = mano; }
    public List<Card> getPremios() { return premios; }
    public void setPremios(List<Card> premios) { this.premios = premios; }
    public CartaEnJuego getActivo() { return activo; }
    public void setActivo(CartaEnJuego activo) { this.activo = activo; }
    public List<CartaEnJuego> getBanca() { return banca; }
    public void setBanca(List<CartaEnJuego> banca) { this.banca = banca; }
    private int turnosJugados = 0;
    private List<Card> mazoOriginal = new ArrayList<>();

    public List<Card> getMazoOriginal() { return mazoOriginal; }
    public void setMazoOriginal(List<Card> mazoOriginal) { this.mazoOriginal = mazoOriginal; }

    public int getTurnosJugados() { return turnosJugados; }
    public void setTurnosJugados(int turnosJugados) { this.turnosJugados = turnosJugados; }

    public List<Card> getPilaDescarte() { return pilaDescarte; }
    public void setPilaDescarte(List<Card> pilaDescarte) { this.pilaDescarte = pilaDescarte; }

    private boolean supporterBlockedNextTurn = false;
    public boolean isSupporterBlockedNextTurn() { return supporterBlockedNextTurn; }
    public void setSupporterBlockedNextTurn(boolean value) { this.supporterBlockedNextTurn = value; }
}
