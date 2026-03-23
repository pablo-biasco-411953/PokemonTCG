package com.pokemon.tcg.model.battle;

import com.pokemon.tcg.model.Card;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el estado de un jugador en la partida.
 */
public class TableroJugador {
    // Mazo oculto
    private List<Card> mazo = new ArrayList<>();
    // Mano privada (revelada solo al jugador)
    private List<Card> mano = new ArrayList<>();
    // Premios (6, ocultos)
    private List<Card> premios = new ArrayList<>();
    // Pokémon activo
    private CartaEnJuego activo;
    // Banca (máximo 5 Pokémon básicos)
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
    public List<Card> getPilaDescarte() { return pilaDescarte; }
    public void setPilaDescarte(List<Card> pilaDescarte) { this.pilaDescarte = pilaDescarte; }
}
