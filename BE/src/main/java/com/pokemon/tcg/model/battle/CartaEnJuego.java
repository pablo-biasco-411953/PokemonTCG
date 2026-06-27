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
    private Card card; // referencia a la carta original
    private int hpActual;
    private List<Card> energiasUnidas = new ArrayList<>();
    private List<Card> attachedTools = new ArrayList<>();
    private boolean puedeAtacar = true;
    private int reduccionDanioRecibido = 0;
    private int aumentoDanioCausado = 0;
    private boolean invulnerable = false;
    private int preventDamageThreshold = 0;
    private boolean preventDamageThresholdYaConsumido = false;
    private boolean bocaAbajo = false;
    private boolean debeLanzarMonedaSiAtaca = false;
    private boolean noPuedeAtacarSiguienteTurno = false;
    private boolean noPuedeAtacarYaConsumido = false;
    private String ataqueBloqueadoSiguienteTurno = null;
    private int danioExtraSiguienteTurno = 0;
    private String ataquePotenciadoSiguienteTurno = null;
    private boolean ataqueBloqueadoYaConsumido = false;
    private int reduccionDanioCausadoSiguienteTurno = 0;
    private int ultimoTurnoEvolucionado = -1;

    public CartaEnJuego(Card card) {
        this.card = card;
        try {
            this.hpActual = Integer.parseInt(card.getHp());
        } catch (NumberFormatException e) {
            this.hpActual = 0;
        }
    }

    public void setInvulnerable(boolean value) { this.invulnerable = value; }
    public boolean isInvulnerable() { return invulnerable; }
    
    public void setBocaAbajo(boolean value) { this.bocaAbajo = value; }
    public boolean isBocaAbajo() { return bocaAbajo; }
    public void setDebeLanzarMonedaSiAtaca(boolean value) { this.debeLanzarMonedaSiAtaca = value; }
    public boolean isDebeLanzarMonedaSiAtaca() { return debeLanzarMonedaSiAtaca; }

    public boolean isNoPuedeAtacarSiguienteTurno() { return noPuedeAtacarSiguienteTurno; }
    public void setNoPuedeAtacarSiguienteTurno(boolean value) { this.noPuedeAtacarSiguienteTurno = value; }
    public boolean isNoPuedeAtacarYaConsumido() { return noPuedeAtacarYaConsumido; }
    public void setNoPuedeAtacarYaConsumido(boolean value) { this.noPuedeAtacarYaConsumido = value; }

    public String getAtaqueBloqueadoSiguienteTurno() { return ataqueBloqueadoSiguienteTurno; }
    public void setAtaqueBloqueadoSiguienteTurno(String value) { this.ataqueBloqueadoSiguienteTurno = value; }

    public int getDanioExtraSiguienteTurno() { return danioExtraSiguienteTurno; }
    public void setDanioExtraSiguienteTurno(int danioExtraSiguienteTurno) { this.danioExtraSiguienteTurno = danioExtraSiguienteTurno; }

    public String getAtaquePotenciadoSiguienteTurno() { return ataquePotenciadoSiguienteTurno; }
    public void setAtaquePotenciadoSiguienteTurno(String ataquePotenciadoSiguienteTurno) { this.ataquePotenciadoSiguienteTurno = ataquePotenciadoSiguienteTurno; }

    public boolean isAtaqueBloqueadoYaConsumido() { return ataqueBloqueadoYaConsumido; }
    public void setAtaqueBloqueadoYaConsumido(boolean value) { this.ataqueBloqueadoYaConsumido = value; }

    public int getReduccionDanioCausadoSiguienteTurno() { return reduccionDanioCausadoSiguienteTurno; }
    public void setReduccionDanioCausadoSiguienteTurno(int value) { this.reduccionDanioCausadoSiguienteTurno = value; }

    public int getPreventDamageThreshold() { return preventDamageThreshold; }
    public void setPreventDamageThreshold(int value) { this.preventDamageThreshold = value; }
    public boolean isPreventDamageThresholdYaConsumido() { return preventDamageThresholdYaConsumido; }
    public void setPreventDamageThresholdYaConsumido(boolean value) { this.preventDamageThresholdYaConsumido = value; }

    // getters y setters
    public int getReduccionDanioRecibido() { return reduccionDanioRecibido; }
    public void setReduccionDanioRecibido(int value) { this.reduccionDanioRecibido = value; }
    public int getAumentoDanioCausado() { return aumentoDanioCausado; }
    public void setAumentoDanioCausado(int value) { this.aumentoDanioCausado = value; }

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
        this.puedeAtacar = true;
        this.invulnerable = false;
        this.preventDamageThreshold = 0;
        this.preventDamageThresholdYaConsumido = false;
        this.debeLanzarMonedaSiAtaca = false;
        this.noPuedeAtacarSiguienteTurno = false;
        this.noPuedeAtacarYaConsumido = false;
        this.reduccionDanioCausadoSiguienteTurno = 0;
        this.ataqueBloqueadoSiguienteTurno = null;
        this.ataqueBloqueadoYaConsumido = false;
    }

    private int turnoEntrada = 0;

    public int getTurnoEntrada() { return turnoEntrada; }
    public void setTurnoEntrada(int turnoEntrada) { this.turnoEntrada = turnoEntrada; }

    public void setCard(Card card) {
        this.card = card;
    }

    public List<Card> getAttachedTools() { return attachedTools; }
    public void setAttachedTools(List<Card> attachedTools) { this.attachedTools = attachedTools; }

    public int getUltimoTurnoEvolucionado() { return ultimoTurnoEvolucionado; }
    public void setUltimoTurnoEvolucionado(int ultimoTurnoEvolucionado) { this.ultimoTurnoEvolucionado = ultimoTurnoEvolucionado; }
}
