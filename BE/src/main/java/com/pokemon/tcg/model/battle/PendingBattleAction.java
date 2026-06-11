package com.pokemon.tcg.model.battle;

import java.util.ArrayList;
import java.util.List;

public class PendingBattleAction {
    private String actor;
    private String type;
    private String prompt;
    private String destination;
    private int minSelections;
    private int maxSelections;
    private boolean endsTurn;
    private List<Option> options = new ArrayList<>();

    public boolean isEndsTurn() { return endsTurn; }
    public void setEndsTurn(boolean endsTurn) { this.endsTurn = endsTurn; }

    public static class Option {
        private String id;
        private String nombre;
        private String imagen;

        public Option() {}

        public Option(String id, String nombre, String imagen) {
            this.id = id;
            this.nombre = nombre;
            this.imagen = imagen;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getImagen() { return imagen; }
        public void setImagen(String imagen) { this.imagen = imagen; }
    }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public int getMinSelections() { return minSelections; }
    public void setMinSelections(int minSelections) { this.minSelections = minSelections; }
    public int getMaxSelections() { return maxSelections; }
    public void setMaxSelections(int maxSelections) { this.maxSelections = maxSelections; }
    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options; }
}
