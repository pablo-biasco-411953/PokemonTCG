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

        // 🚨 REGLA DE ORO: Si no hay activo, subir uno de la banca primero
        if (tableroBot.getActivo() == null && !tableroBot.getBanca().isEmpty()) {
            CartaEnJuego mejorOpcion = tableroBot.getBanca().stream()
                    .sorted((c1, c2) -> {
                        int e1 = c1.getEnergiasUnidas().size();
                        int e2 = c2.getEnergiasUnidas().size();
                        if (e1 != e2) return Integer.compare(e2, e1);
                        return Integer.compare(c2.getHpActual(), c1.getHpActual());
                    })
                    .findFirst().get();
            tableroBot.getBanca().remove(mejorOpcion);
            tableroBot.setActivo(mejorOpcion);
            System.out.println("🤖 [BOT] Subió a " + mejorOpcion.getCard().getNombre() + " como nuevo activo.");
        }

        gestionarCartasEnMano(tableroBot, partida);
        // 🚩 NUEVO: Analizar si conviene huir antes de gastar la energía del turno
        evaluarRetiradaEstrategica(tableroBot, partida.getJugador(), partida);

        gestionarEnergiaBot(tableroBot);
        intentarAtacar(tableroBot, partida);
    }

    private void evaluarRetiradaEstrategica(TableroJugador bot, TableroJugador jugador, Partida partida) {
        CartaEnJuego miActivo = bot.getActivo();
        CartaEnJuego suActivo = jugador.getActivo();

        if (miActivo == null || suActivo == null || bot.getBanca().isEmpty() || partida.isYaSeRetiroEsteTurno()) {
            return;
        }

        // 🚨 REGLA DE ORO: Si el bot está incapacitado o atrapado, NO PUEDE HUIR.
        if (miActivo.getCondicionesEspeciales().contains("Asleep") ||
                miActivo.getCondicionesEspeciales().contains("Paralyzed") ||
                miActivo.getCondicionesEspeciales().contains("CantRetreat")) {
            System.out.println("🤖 [BOT] Quería huir, pero está incapacitado o atrapado por un efecto.");
            return; // Corta acá
        }

        int danioInminente = calcularAmenazaMaxima(suActivo, miActivo);
        boolean peligroDeMuerte = miActivo.getHpActual() <= danioInminente;

        boolean estancado = miActivo.getCard().getAtaques().stream()
                .mapToInt(a -> a.getCosto() != null ? a.getCosto().size() : 0)
                .min().orElse(0) > (miActivo.getEnergiasUnidas().size() + 1);

        // 🧠 NUEVA ESTRATEGIA: El bot detecta que se está muriendo lentamente por estados
        boolean muriendoPorEstados = miActivo.getCondicionesEspeciales().contains("Poisoned") ||
                miActivo.getCondicionesEspeciales().contains("Burned");

        // Si corre peligro, está estancado, o se está pudriendo por veneno... ¡HUYE!
        if (peligroDeMuerte || estancado || muriendoPorEstados) {
            int costoRetirada = miActivo.getCard().getCostoRetirada();

            if (miActivo.getEnergiasUnidas().size() >= costoRetirada) {
                CartaEnJuego mejorSuplente = bot.getBanca().stream()
                        .max(java.util.Comparator.comparingInt(CartaEnJuego::getHpActual))
                        .orElse(null);

                if (mejorSuplente != null && mejorSuplente.getHpActual() > miActivo.getHpActual()) {
                    ejecutarRetirada(bot, mejorSuplente, costoRetirada, partida);
                }
            }
        }
    }

    private void ejecutarRetirada(TableroJugador bot, CartaEnJuego suplente, int costo, Partida partida) {
        CartaEnJuego activoHuyendo = bot.getActivo();

        System.out.println("🛡️ [BOT] ¡Retirada táctica! " + activoHuyendo.getCard().getNombre() + " estaba en peligro o estancado y huye a la banca.");

        // Paga el costo mandando energías al descarte
        for (int i = 0; i < costo; i++) {
            bot.getPilaDescarte().add(activoHuyendo.getEnergiasUnidas().remove(0));
        }

        // SWAP de cartas
        bot.getBanca().remove(suplente);
        bot.getBanca().add(activoHuyendo);
        bot.setActivo(suplente);

        // Bloquea múltiples retiradas
        partida.setYaSeRetiroEsteTurno(true);
        activoHuyendo.limpiarCondiciones();
    }

    private int calcularAmenazaMaxima(CartaEnJuego atacanteRival, CartaEnJuego miDefensor) {
        if (atacanteRival.getCard().getAtaques() == null) return 0;

        // Modo Paranoico: El bot asume que el jugador TIENE la energía para su mejor ataque
        return atacanteRival.getCard().getAtaques().stream()
                .mapToInt(ataque -> calcularDanioFinal(atacanteRival, miDefensor, ataque))
                .max()
                .orElse(0);
    }

    private void gestionarCartasEnMano(TableroJugador tablero, Partida partida) {
        List<Card> mano = new ArrayList<>(tablero.getMano());

        // Separar qué tenemos en la mano
        List<Card> energiasEnMano = mano.stream().filter(this::esEnergia).collect(java.util.stream.Collectors.toList());
        List<Card> pokemonesBasicos = mano.stream().filter(this::esPokemonBasico).collect(java.util.stream.Collectors.toList());

        if (pokemonesBasicos.isEmpty()) return;

        CartaEnJuego activoRival = partida.getJugador().getActivo();

        // 🧠 MAGIA IA: Ordenamos los Pokémon de la mano según su "Potencial a futuro"
        pokemonesBasicos.sort((p1, p2) -> {
            int potencial1 = evaluarPotencialDeMano(p1, energiasEnMano, activoRival);
            int potencial2 = evaluarPotencialDeMano(p2, energiasEnMano, activoRival);
            return Integer.compare(potencial2, potencial1); // Mayor a menor
        });

        // Ahora bajamos los Pokémon, pero priorizando a los que sacaron mejor nota
        for (Card cartaEstrella : pokemonesBasicos) {
            if (tablero.getActivo() == null) {
                tablero.setActivo(new CartaEnJuego(cartaEstrella));
                tablero.getMano().remove(cartaEstrella);
                System.out.println("🤖 [BOT] Bajó como ACTIVO a " + cartaEstrella.getNombre() + " por su alto potencial.");
            } else if (tablero.getBanca().size() < 5) {
                tablero.getBanca().add(new CartaEnJuego(cartaEstrella));
                tablero.getMano().remove(cartaEstrella);
                System.out.println("🤖 [BOT] Preparando el futuro: Bajó a la BANCA a " + cartaEstrella.getNombre());
            }
        }
    }

    private int evaluarPotencialDeMano(Card pokemon, List<Card> energiasEnMano, CartaEnJuego activoRival) {
        int puntaje = 0;

        // 1. Sinergia de Energía: ¿Tengo en la mano lo que este bicho come?
        if (pokemon.getAtaques() != null) {
            for (Ataque atk : pokemon.getAtaques()) {
                if (atk.getCosto() != null) {
                    for (String costoReq : atk.getCosto()) {
                        String tipoReq = normalizarTipo(costoReq);

                        if (tipoReq.equals("Colorless") && !energiasEnMano.isEmpty()) {
                            puntaje += 20; // Cualquier energía sirve
                        } else {
                            // Buscar si en la mano tengo exactamente la energía que pide
                            boolean tengoLaPosta = energiasEnMano.stream()
                                    .anyMatch(e -> normalizarTipo(e.getNombre() + " " + e.getTipo()).contains(tipoReq));
                            if (tengoLaPosta) {
                                puntaje += 100; // ¡BINGO! Lo puedo cargar este mismo turno o el que viene
                            }
                        }
                    }
                }
            }
        }

        // 2. Oportunidad de Counter: ¿Si lo voy preparando, le rompo la cabeza al rival actual?
        if (activoRival != null && activoRival.getCard().getDebilidades() != null && pokemon.getTipo() != null) {
            boolean rivalDebilAMi = activoRival.getCard().getDebilidades().stream()
                    .anyMatch(w -> w.get("tipo").equalsIgnoreCase(pokemon.getTipo()));
            if (rivalDebilAMi) {
                puntaje += 200; // Lo bajo a la banca YA MISMO para que sea mi arma secreta
            }
        }

        // 3. Penalización por Suicidio: Si soy débil al rival, prefiero quedarme en la mano
        if (activoRival != null && pokemon.getDebilidades() != null && activoRival.getCard().getTipo() != null) {
            boolean soyDebil = pokemon.getDebilidades().stream()
                    .anyMatch(w -> w.get("tipo").equalsIgnoreCase(activoRival.getCard().getTipo()));
            if (soyDebil) {
                puntaje -= 150; // Guardalo en el bolsillo, no lo regales
            }
        }

        return puntaje;
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

        // 🚩 1. EL CANDADO: Revisamos si está Dormido o Paralizado (Ignorando Mayúsculas)
        boolean estaDormido = activoBot.getCondicionesEspeciales().stream()
                .anyMatch(e -> e.equalsIgnoreCase("Asleep"));
        boolean estaParalizado = activoBot.getCondicionesEspeciales().stream()
                .anyMatch(e -> e.equalsIgnoreCase("Paralyzed"));

        if (estaDormido || estaParalizado) {
            System.out.println("🤖 [BOT] " + activoBot.getCard().getNombre() +
                    " intenta atacar, pero no puede porque está " +
                    (estaDormido ? "Dormido 💤" : "Paralizado ⚡"));
            return; // Bloqueamos el ataque aquí
        }

        // 🚩 2. SELECCIÓN DE ATAQUE
        List<Ataque> ataques = activoBot.getCard().getAtaques();
        if (ataques == null || ataques.isEmpty()) return;

        Ataque ataqueElegido = ataques.get(0); // El bot usa el primer ataque disponible

        if (!puedePagarCosto(activoBot, ataqueElegido)) {
            System.out.println("🤖 [BOT] No tiene energía para " + ataqueElegido.getNombre());
            return;
        }

        // 🚩 3. CÁLCULO DE DAÑO (Base + Debilidad/Resistencia)
        int danioBase = calcularDanioFinal(activoBot, activoJugador, ataqueElegido);

        // 🚩 4. PROCESAMIENTO DE EFECTOS Y MONEDAS
        String texto = ataqueElegido.getTexto() != null ? ataqueElegido.getTexto().toLowerCase() : "";
        int danioFinal = danioBase;
        int carasSacadas = 0;

        if (!texto.isEmpty()) {
            // -- ESCALADO POR CONTADORES DE DAÑO --
            if (texto.contains("damage counter on this") || texto.contains("damage counter on it")) {
                int hpMax = Integer.parseInt(activoBot.getCard().getHp());
                int contadores = (hpMax - activoBot.getHpActual()) / 10;
                int multiplicador = texto.contains("20 more damage") ? 20 : 10;
                danioFinal += contadores * multiplicador;
                System.out.println("🤖 [BOT] Daño por venganza: +" + (contadores * multiplicador));
            }
            // -- ESCALADO POR ENERGÍAS --
            else if (texto.contains("for each energy attached") || texto.contains("for each extra energy")) {
                int energias = activoBot.getEnergiasUnidas().size();
                int mult = texto.contains("20 more") || texto.contains("20 damage") ? 20 :
                        (texto.contains("30 more") || texto.contains("30 damage") ? 30 : 10);
                danioFinal += energias * mult;
                System.out.println("🤖 [BOT] Daño por energías unidas: +" + (energias * mult));
            }

            // -- LÓGICA DE MONEDAS --
            if (texto.contains("tails, this attack does nothing") || texto.contains("tails, that attack does nothing")) {
                if (!random.nextBoolean()) {
                    System.out.println("🤖 [BOT] ¡Falló por moneda! (CRUZ)");
                    return; // Cortamos el ataque
                }
                carasSacadas = 1;
                System.out.println("🤖 [BOT] ¡Acertó por moneda! (CARA)");
            }
            else if (texto.contains("times the number of heads") || texto.contains("x the number of heads") || texto.contains("for each heads")) {
                int monedas = texto.contains("2 coins") ? 2 : (texto.contains("3 coins") ? 3 : (texto.contains("4 coins") ? 4 : (texto.contains("5 coins") ? 5 : 1)));
                for (int i = 0; i < monedas; i++) {
                    if (random.nextBoolean()) carasSacadas++;
                }
                danioFinal = danioBase * carasSacadas;
                System.out.println("🤖 [BOT] Monedas: " + carasSacadas + " caras.");
            }
            else if (texto.contains("if heads") && (texto.contains("more damage") || texto.contains("damage plus"))) {
                if (random.nextBoolean()) {
                    danioFinal += danioBase;
                    carasSacadas = 1;
                    System.out.println("🤖 [BOT] ¡Cara! Daño extra.");
                }
            }
        }

        // 🚩 5. APLICAR DAÑO FINAL AL JUGADOR
        int nuevaHpJugador = Math.max(0, activoJugador.getHpActual() - danioFinal);
        activoJugador.setHpActual(nuevaHpJugador);

        System.out.println("🎯 [BOT] " + activoBot.getCard().getNombre() + " usó " + ataqueElegido.getNombre() + " -> Daño: " + danioFinal);

        // 🚩 6. APLICAR EFECTOS SECUNDARIOS (Opcional, según tu lógica de efectos)
        // aplicarEfectosSecundariosBot(partida, ataqueElegido, activoBot, activoJugador, carasSacadas);
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

    private boolean esPokemonBasico(Card c) {
        if (c == null) return false;

        System.out.print("🔍 [BOT SCAN] Analizando: " + c.getNombre() + " -> ");

        // 1. FILTRO ANTI-ENERGÍAS: Si dice "Energía" o su HP es 0, arafue.
        if (c.getNombre().toLowerCase().contains("energ") || "Energy".equalsIgnoreCase(c.getSupertype()) || "0".equals(c.getHp())) {
            System.out.println("❌ Es una Energía.");
            return false;
        }

        // 2. FILTRO ANTI-EVOLUCIONES: Si evoluciona de alguien, arafue.
        if (c.getEvolvesFrom() != null && !c.getEvolvesFrom().trim().isEmpty()) {
            System.out.println("❌ Es Evolución (Viene de " + c.getEvolvesFrom() + ").");
            return false;
        }

        // 3. FILTRO ANTI-STAGE: Revisamos la base de datos cruda por las dudas.
        if (c.getSubtypesJson() != null && c.getSubtypesJson().contains("Stage")) {
            System.out.println("❌ Es Fase 1 o 2 (Lo dice su JSON).");
            return false;
        }

        // Si pasó todos estos filtros de seguridad, es un Básico real.
        System.out.println("✅ ¡ES BÁSICO Y LEGAL!");
        return true;
    }

    private boolean esEnergia(Card c) {
        if (c == null || c.getSupertype() == null) return false;
        return c.getSupertype().equalsIgnoreCase("Energy");
    }
}
