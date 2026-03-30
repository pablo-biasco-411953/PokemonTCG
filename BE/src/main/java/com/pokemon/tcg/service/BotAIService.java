package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class BotAIService {

    private final Random random = new Random();

    public void ejecutarTurno(Partida partida) {
        TableroJugador tableroBot = partida.getBot();

        // Ã°Å¸Å¡Â¨ REGLA DE ORO: Si no hay activo, subir uno de la banca primero
        if (tableroBot.getActivo() == null && !tableroBot.getBanca().isEmpty()) {
            CartaEnJuego mejorOpcion = tableroBot.getBanca().stream()
                    .sorted((c1, c2) -> {
                        // Prioridad 1: El que tenga mÃƒÂ¡s energÃƒÂ­a ya unida
                        int e1 = c1.getEnergiasUnidas().size();
                        int e2 = c2.getEnergiasUnidas().size();
                        if (e1 != e2) return Integer.compare(e2, e1);
                        // Prioridad 2: El que tenga mÃƒÂ¡s vida
                        return Integer.compare(c2.getHpActual(), c1.getHpActual());
                    })
                    .findFirst().get();
            tableroBot.getBanca().remove(mejorOpcion);
            tableroBot.setActivo(mejorOpcion);
            System.out.println("Ã°Å¸Â¤â€“ [BOT] Subió a " + mejorOpcion.getCard().getNombre() + " porque es el más apto.");
        }
        // Luego sigue su lÃƒÂ³gica normal...
        gestionarCartasEnMano(tableroBot);
        gestionarEnergiaBot(tableroBot);
        intentarAtacar(tableroBot, partida);
    }

    private void gestionarCartasEnMano(TableroJugador tablero) {
        // Copia para evitar ConcurrentModificationException al remover durante iteraciÃƒÂ³n
        List<Card> manoCopia = new ArrayList<>(tablero.getMano());

        for (Card carta : manoCopia) {
            if (!esPokemonBasico(carta)) continue;

            if (tablero.getActivo() == null) {
                tablero.setActivo(new CartaEnJuego(carta));
                tablero.getMano().remove(carta);
                System.out.println("Ã°Å¸Â¤â€“ [BOT] Activo: " + carta.getNombre());
            } else if (tablero.getBanca().size() < 5) {
                tablero.getBanca().add(new CartaEnJuego(carta));
                tablero.getMano().remove(carta);
                System.out.println("Ã°Å¸Â¤â€“ [BOT] Banca: " + carta.getNombre());
            }
        }
    }



    private boolean puedePagarAtaque(CartaEnJuego pokemon, Ataque ataque) {
        List<Card> energias = new ArrayList<>(pokemon.getEnergiasUnidas());
        List<String> costo = new ArrayList<>(ataque.getCosto());

        // 1. Validar energías específicas (Water, Fire, etc.)
        for (int i = costo.size() - 1; i >= 0; i--) {
            String tipoReq = costo.get(i);
            if (!tipoReq.equalsIgnoreCase("Colorless")) {
                // Buscamos si el bot tiene ese tipo
                Card tieneTipo = energias.stream()
                        .filter(e -> e.getNombre().toLowerCase().contains(tipoReq.toLowerCase()))
                        .findFirst().orElse(null);

                if (tieneTipo != null) {
                    energias.remove(tieneTipo);
                    costo.remove(i);
                } else {
                    return false; // ❌ Le falta una específica, no puede atacar
                }
            }
        }

        // 2. Validar Incoloras (Cualquiera que sobre)
        return energias.size() >= costo.size();
    }



    private boolean pokemonNecesitaEsteTipo(CartaEnJuego pokemon, Card energia) {
        if (pokemon.getCard().getAtaques() == null) return false;

        // 🚩 Usamos el normalizador acá también
        String tipoEnergia = normalizarTipo(energia.getNombre());

        return pokemon.getCard().getAtaques().stream()
                .anyMatch(ataque -> {
                    List<String> costo = ataque.getCosto();
                    return costo != null && costo.stream()
                            .anyMatch(tipoReq -> normalizarTipo(tipoReq).equals(tipoEnergia) ||
                                    tipoReq.equalsIgnoreCase("Colorless"));
                });
    }

    private void gestionarEnergiaBot(TableroJugador tablero) {
        // 1. Obtenemos las energías que el bot tiene en la mano
        List<Card> energiasEnMano = tablero.getMano().stream()
                .filter(this::esEnergia)
                .collect(java.util.stream.Collectors.toList());

        if (energiasEnMano.isEmpty()) return;

        CartaEnJuego activo = tablero.getActivo();

        // 🛡️ REGLA DE ORO: Si el activo existe, verificamos si necesita energía
        if (activo != null) {

            // 🚩 PASO A: ¿Ya puede atacar con alguno de sus ataques?
            boolean yaPuedeAtacar = activo.getCard().getAtaques().stream()
                    .anyMatch(ataque -> puedePagarCosto(activo, ataque)); // Usamos tu función de validación

            if (yaPuedeAtacar) {
                System.out.println("🤖 [BOT] " + activo.getCard().getNombre() + " ya está listo para atacar. Guardando energía...");
                // Opcional: Podrías llamar a una función para cargar a la banca acá
                return; // 🛑 FRENAMOS ACÁ, NO LE DAMOS MÁS ENERGÍA AL ACTIVO
            }

            // 🚩 PASO B: Si NO puede atacar, buscamos una energía útil para COMPLETAR el costo
            Card energiaUtil = energiasEnMano.stream()
                    .filter(e -> pokemonNecesitaEsteTipo(activo, e))
                    .findFirst()
                    .orElse(null);

            if (energiaUtil != null) {
                activo.getEnergiasUnidas().add(energiaUtil);
                tablero.getMano().remove(energiaUtil);
                System.out.println("🤖 [BOT] Unión estratégica: " + energiaUtil.getNombre() + " a " + activo.getCard().getNombre() + " para cargar ataque.");
                return; // Solo una unión por turno, regla oficial
            }
        }

        // Si no encontró energía útil o el activo ya estaba cargado, la energía se queda en la mano.
    }


    private boolean puedePagarCosto(CartaEnJuego pokemon, Ataque ataque) {
        if (pokemon.getEnergiasUnidas() == null || ataque.getCosto() == null) return false;

        List<Card> misEnergias = new ArrayList<>(pokemon.getEnergiasUnidas());
        List<String> costoReq = new ArrayList<>(ataque.getCosto());

        for (int i = costoReq.size() - 1; i >= 0; i--) {
            String tipoBuscado = normalizarTipo(costoReq.get(i));
            if (!tipoBuscado.equals("Colorless")) {
                Card match = misEnergias.stream()
                        .filter(e -> normalizarTipo(e.getNombre() + " " + e.getTipo()).contains(tipoBuscado))
                        .findFirst().orElse(null);

                if (match != null) {
                    misEnergias.remove(match);
                    costoReq.remove(i);
                } else {
                    return false;
                }
            }
        }
        return misEnergias.size() >= costoReq.size();
    }

    // 🚩 EL TRADUCTOR UNIVERSAL (Agregalo al BotAIService)
    private String normalizarTipo(String texto) {
        if (texto == null) return "";
        String t = texto.toLowerCase();
        if (t.contains("grass") || t.contains("planta")) return "Grass";
        if (t.contains("fire") || t.contains("fuego")) return "Fire";
        if (t.contains("water") || t.contains("agua")) return "Water";
        if (t.contains("lightning") || t.contains("eléctrica") || t.contains("electrica")) return "Lightning";
        if (t.contains("psychic") || t.contains("psíquica") || t.contains("psiquica")) return "Psychic";
        if (t.contains("fighting") || t.contains("lucha")) return "Fighting";
        if (t.contains("darkness") || t.contains("siniestra") || t.contains("oscuridad")) return "Darkness";
        if (t.contains("metal") || t.contains("acero")) return "Metal";
        if (t.contains("colorless") || t.contains("incolora")) return "Colorless";
        return texto;
    }

    private void intentarAtacar(TableroJugador tableroBot, Partida partida) {
        CartaEnJuego activoBot = tableroBot.getActivo();
        CartaEnJuego activoJugador = partida.getJugador().getActivo();

        if (activoBot == null || activoJugador == null) return;

        List<Ataque> ataques = activoBot.getCard().getAtaques();
        if (ataques == null || ataques.isEmpty()) return;

        // Elegimos el primer ataque por ahora
        Ataque ataqueElegido = ataques.get(0);

        // 1. Validar energía
        if (!puedePagarCosto(activoBot, ataqueElegido)) {
            System.out.println("🤖 [BOT] No tiene energía para " + ataqueElegido.getNombre());
            return;
        }

        // 2. 🚩 CÁLCULO DE DAÑO (Acá estaba el error)
        // Llamamos a la función que creamos y guardamos el resultado en 'danioFinal'
        int danioFinal = calcularDanioFinal(activoBot, activoJugador, ataqueElegido);

        // 3. Aplicar el daño al jugador usando la variable correcta
        int nuevaHp = activoJugador.getHpActual() - danioFinal;
        activoJugador.setHpActual(Math.max(0, nuevaHp));

        System.out.println("🤖 [BOT] Atacó con " + ataqueElegido.getNombre() +
                " haciendo " + danioFinal + " de daño.");

        if (activoJugador.getHpActual() <= 0) {
            resolverKO(partida, activoBot, activoJugador);
        }
    }
    private void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        TableroJugador tableroVictima = partida.getJugador();
        TableroJugador tableroBot     = partida.getBot();

        System.out.println("[BOT] K.O.! " + defensor.getCard().getNombre() + " derrotado.");

        // Mover al descarte
        tableroVictima.getPilaDescarte().add(defensor.getCard());
        tableroVictima.setActivo(null);

        // El bot toma un premio
        if (!tableroBot.getPremios().isEmpty()) {
            tableroBot.getMano().add(tableroBot.getPremios().remove(0));
            System.out.println("[BOT] Tomó un premio. Premios restantes: " + tableroBot.getPremios().size());
        }

        // Fin de partida
        boolean botSinPremios      = tableroBot.getPremios().isEmpty();
        boolean jugadorSinPokemon  = tableroVictima.getActivo() == null
                && tableroVictima.getBanca().isEmpty();

        if (botSinPremios || jugadorSinPokemon) {
            partida.setFaseActual(Partida.Fase.FIN_PARTIDA);
            System.out.println(" [BOT] ¡Partida terminada! Gana el bot.");
        }
    }

    private int calcularDanioFinal(CartaEnJuego atacante, CartaEnJuego defensor, Ataque ataque) {
        int resultado = ataque.getDanio(); // Daño base del JSON
        String tipoAtacante = atacante.getCard().getTipo();

        // Validar Debilidad (x2)
        if (defensor.getCard().getDebilidades() != null) {
            boolean esDebil = defensor.getCard().getDebilidades().stream()
                    .anyMatch(w -> w.get("tipo").equalsIgnoreCase(tipoAtacante));
            if (esDebil) {
                System.out.println("💥 ¡Debilidad! Daño x2");
                resultado *= 2;
            }
        }

        // Validar Resistencia (-20)
        if (defensor.getCard().getResistencias() != null) {
            boolean esResistente = defensor.getCard().getResistencias().stream()
                    .anyMatch(r -> r.get("tipo").equalsIgnoreCase(tipoAtacante));
            if (esResistente) {
                System.out.println("🛡️ ¡Resistencia! Daño -20");
                resultado = Math.max(0, resultado - 20);
            }
        }

        return resultado;
    }

    private boolean esPokemonBasico(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        return !tipo.contains("energy") && !tipo.contains("energía") && !tipo.contains("stage");
    }

    private boolean esEnergia(Card carta) {
        if (carta.getTipo() == null) return false;
        String tipo = carta.getTipo().toLowerCase();
        return tipo.contains("energy") || tipo.contains("energía");
    }
}
