package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Habilidad;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.service.BattleEngineService;
import java.util.Optional;

public class ComandoUsarHabilidad implements ComandoTurno {

    private final String sourcePokemonId;
    private final String abilityName;
    private final String targetPokemonId;
    private final String extraParams;
    private final TableroJugador jugador;
    private final TableroJugador oponente;
    private final BattleEngineService battleEngine;

    public ComandoUsarHabilidad(
            String sourcePokemonId,
            String abilityName,
            String targetPokemonId,
            String extraParams,
            TableroJugador jugador,
            TableroJugador oponente,
            BattleEngineService battleEngine
    ) {
        this.sourcePokemonId = sourcePokemonId;
        this.abilityName = abilityName;
        this.targetPokemonId = targetPokemonId;
        this.extraParams = extraParams;
        this.jugador = jugador;
        this.oponente = oponente;
        this.battleEngine = battleEngine;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        CartaEnJuego source = encontrarEnTablero(jugador, sourcePokemonId);
        if (source == null) return false;
        
        // Verificar que tenga la habilidad
        boolean tieneHab = source.getCard().getHabilidades().stream()
                .anyMatch(h -> h.getNombre().equalsIgnoreCase(abilityName));
        if (!tieneHab) return false;

        // Si es una habilidad de un solo uso por turno, verificar que no se haya usado
        if (esUnaVezPorTurno(abilityName)) {
            if (source.getHabilidadesUsadasEsteTurno().contains(abilityName)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void ejecutar(Partida partida) {
        CartaEnJuego source = encontrarEnTablero(jugador, sourcePokemonId);
        if (source == null) {
            throw new IllegalArgumentException("No tenés ese Pokémon en tu tablero.");
        }

        Habilidad habilidad = source.getCard().getHabilidades().stream()
                .filter(h -> h.getNombre().equalsIgnoreCase(abilityName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Habilidad no encontrada en este Pokémon: " + abilityName));

        if (esUnaVezPorTurno(abilityName) && source.getHabilidadesUsadasEsteTurno().contains(abilityName)) {
            throw new IllegalStateException("Ya usaste la habilidad " + abilityName + " de este Pokémon en este turno.");
        }

        // Ejecutar efectos específicos
        switch (abilityName.toLowerCase().trim()) {
            case "water shuriken":
                ejecutarWaterShuriken(partida, source);
                break;
            case "mystical fire":
                ejecutarMysticalFire(partida, source);
                break;
            case "fairy transfer":
                ejecutarFairyTransfer(partida, source);
                break;
            case "drive off":
                ejecutarDriveOff(partida, source);
                break;
            case "stance change":
                ejecutarStanceChange(partida, source);
                break;
            case "upside-down evolution":
                ejecutarUpsideDownEvolution(partida, source);
                break;
            default:
                throw new IllegalArgumentException("Habilidad activa no implementada: " + abilityName);
        }
    }

    private boolean esUnaVezPorTurno(String name) {
        String lower = name.toLowerCase().trim();
        return lower.equals("water shuriken") 
            || lower.equals("mystical fire")
            || lower.equals("drive off")
            || lower.equals("stance change")
            || lower.equals("upside-down evolution");
    }

    private void ejecutarWaterShuriken(Partida partida, CartaEnJuego source) {
        // Discard a Water Energy from hand
        Card waterEnergy = jugador.getMano().stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()) && "Water".equalsIgnoreCase(c.getTipo()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No tenés Energía Agua en la mano para descartar."));

        // Resolver Target Pokémon (debe ser del oponente)
        if (targetPokemonId == null || targetPokemonId.trim().isEmpty()) {
            throw new IllegalArgumentException("Debés seleccionar un Pokémon objetivo.");
        }
        CartaEnJuego target = encontrarEnTablero(oponente, targetPokemonId);
        if (target == null) {
            throw new IllegalArgumentException("El Pokémon objetivo no es un Pokémon válido de tu oponente.");
        }

        // Aplicar descarte
        jugador.getMano().remove(waterEnergy);
        jugador.getPilaDescarte().add(waterEnergy);

        // Aplicar daño
        int hpAntes = target.getHpActual();
        target.setHpActual(Math.max(0, hpAntes - 30));

        System.out.println("[ABILITY] Water Shuriken descarta una energía Agua e inflige 30 de daño a " + target.getCard().getNombre());
        
        // Registrar uso
        source.registrarUsoHabilidad(abilityName);

        // Verificar KO
        if (target.getHpActual() <= 0) {
            battleEngine.resolverKO(partida, source, target);
        }
    }

    private void ejecutarMysticalFire(Partida partida, CartaEnJuego source) {
        int manoSize = jugador.getMano().size();
        if (manoSize >= 6) {
            throw new IllegalStateException("Ya tenés 6 o más cartas en la mano.");
        }
        int aRobar = 6 - manoSize;
        battleEngine.robarCartas(jugador, aRobar);
        System.out.println("[ABILITY] Mystical Fire hace que el jugador robe " + aRobar + " cartas.");
        
        source.registrarUsoHabilidad(abilityName);
    }

    private void ejecutarFairyTransfer(Partida partida, CartaEnJuego source) {
        // extraParams format: "energyCardId,originPokemonId"
        if (extraParams == null || !extraParams.contains(",")) {
            throw new IllegalArgumentException("Parámetros inválidos para Fairy Transfer. Se requiere 'energyCardId,originPokemonId'.");
        }
        String[] parts = extraParams.split(",");
        String energyCardId = parts[0].trim();
        String originPokemonId = parts[1].trim();

        CartaEnJuego origin = encontrarEnTablero(jugador, originPokemonId);
        if (origin == null) {
            throw new IllegalArgumentException("El Pokémon de origen no está en tu tablero.");
        }

        if (targetPokemonId == null || targetPokemonId.trim().isEmpty()) {
            throw new IllegalArgumentException("Debés especificar un Pokémon de destino.");
        }
        CartaEnJuego target = encontrarEnTablero(jugador, targetPokemonId);
        if (target == null) {
            throw new IllegalArgumentException("El Pokémon de destino no está en tu tablero.");
        }

        Card energyCard = origin.getEnergiasUnidas().stream()
                .filter(c -> c.getId().equals(energyCardId) && "Fairy".equalsIgnoreCase(c.getTipo()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El Pokémon de origen no tiene la energía Hada especificada."));

        origin.getEnergiasUnidas().remove(energyCard);
        target.getEnergiasUnidas().add(energyCard);

        System.out.println("[ABILITY] Fairy Transfer mueve Energía Hada de " + origin.getCard().getNombre() + " a " + target.getCard().getNombre());
    }

    private void ejecutarDriveOff(Partida partida, CartaEnJuego source) {
        if (oponente.getBanca().isEmpty()) {
            throw new IllegalStateException("El oponente no tiene Pokémon en la banca.");
        }

        CartaEnJuego activeOponente = oponente.getActivo();
        if (activeOponente == null) {
            throw new IllegalStateException("El oponente no tiene un Pokémon activo.");
        }

        // Seleccionar benched target
        CartaEnJuego benchTarget = null;
        if (targetPokemonId != null && !targetPokemonId.trim().isEmpty()) {
            benchTarget = oponente.getBanca().stream()
                    .filter(c -> c.getCard().getId().equals(targetPokemonId))
                    .findFirst()
                    .orElse(null);
        }

        if (benchTarget == null) {
            // Fallback al primer pokemon de la banca
            benchTarget = oponente.getBanca().get(0);
        }

        // Intercambiar
        oponente.getBanca().remove(benchTarget);
        oponente.getBanca().add(activeOponente);
        oponente.setActivo(benchTarget);

        System.out.println("[ABILITY] Drive Off obliga al oponente a retirar a " + activeOponente.getCard().getNombre() + " y subir a " + benchTarget.getCard().getNombre());
        source.registrarUsoHabilidad(abilityName);
    }

    private void ejecutarStanceChange(Partida partida, CartaEnJuego source) {
        // Encontrar Aegislash en la mano
        Card aegislashMano = jugador.getMano().stream()
                .filter(c -> c.getNombre() != null && c.getNombre().equalsIgnoreCase("Aegislash") && !c.getId().equals(source.getCard().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No tenés otra carta de Aegislash en la mano para cambiar."));

        // Intercambiar la carta conservando contadores de daño y energías
        Card anterior = source.getCard();
        source.setCard(aegislashMano);

        // Remover de la mano y añadir el anterior a la mano
        jugador.getMano().remove(aegislashMano);
        jugador.getMano().add(anterior);

        System.out.println("[ABILITY] Stance Change intercambia Aegislash de mesa con Aegislash en mano.");
        source.registrarUsoHabilidad(abilityName);
    }

    private void ejecutarUpsideDownEvolution(Partida partida, CartaEnJuego source) {
        if (!source.getCondicionesEspeciales().contains("Confused")) {
            throw new IllegalStateException("Inkay debe estar Confundido para usar esta habilidad.");
        }

        // Buscar evolución en el mazo
        Card evolution = jugador.getMazo().stream()
                .filter(c -> c.getEvolvesFrom() != null && c.getEvolvesFrom().equalsIgnoreCase("Inkay"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró ninguna evolución de Inkay en tu mazo."));

        // Realizar evolución
        jugador.getMazo().remove(evolution);
        source.setCard(evolution);
        // Curar confusión
        source.getCondicionesEspeciales().remove("Confused");
        // Mezclar mazo
        java.util.Collections.shuffle(jugador.getMazo());

        System.out.println("[ABILITY] Upside-Down Evolution evoluciona a Inkay desde el mazo.");
        source.registrarUsoHabilidad(abilityName);
    }

    private CartaEnJuego encontrarEnTablero(TableroJugador tablero, String id) {
        if (tablero.getActivo() != null && tablero.getActivo().getCard().getId().equals(id)) {
            return tablero.getActivo();
        }
        return tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getNombre() { return "UsarHabilidad"; }
}
