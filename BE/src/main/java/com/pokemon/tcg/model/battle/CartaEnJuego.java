package com.pokemon.tcg.model.battle;

import com.pokemon.tcg.model.Card;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Estado en mesa de una carta: HP actual, energías y condiciones.
 */
public class CartaEnJuego {
    private Set<String> condicionesEspeciales = new HashSet<>();
    private  Card card; // referencia a la carta original
    private int hpActual;
    private List<Card> energiasUnidas = new ArrayList<>();
    private boolean puedeAtacar = true;
    private int reduccionDanioRecibido = 0;
    private int aumentoDanioCausado = 0;
    private boolean invulnerable = false;
    private boolean bocaAbajo = false;

    public void setInvulnerable(boolean value) { this.invulnerable = value; }
    public boolean isInvulnerable() { return invulnerable; }
    
    public void setBocaAbajo(boolean value) { this.bocaAbajo = value; }
    public boolean isBocaAbajo() { return bocaAbajo; }
    
    public CartaEnJuego(Card card) {
        // El HP inicial se toma de la carta base al entrar en juego.
        this.card = card;
        try {
            this.hpActual = Integer.parseInt(card.getHp());
        } catch (NumberFormatException e) {
            this.hpActual = 0;
        }
    }

    // getters y setters
    public Card getCard() { return card; }
    public int getHpActual() { return hpActual; }
    public void setHpActual(int hpActual) { this.hpActual = hpActual; }
    public List<Card> getEnergiasUnidas() { return energiasUnidas; }
    public boolean isPuedeAtacar() { return puedeAtacar; }
    public void setPuedeAtacar(boolean puedeAtacar) { this.puedeAtacar = puedeAtacar; }
    public Set<String> getCondicionesEspeciales() {
        return condicionesEspeciales;
    }

    public void agregarCondicion(String condicion) {
        this.condicionesEspeciales.add(condicion);
    }

    public void limpiarCondiciones() {
        this.condicionesEspeciales.clear();
    }

    public void setCard(Card card) {
        this.card = card;
    }
}
