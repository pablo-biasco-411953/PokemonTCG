package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.ResultadoAtaque;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
/**
 * Resuelve el daño y los efectos secundarios de un ataque.
 */
public class BattleAttackService {

    @FunctionalInterface
    public interface KoResolver {
        // Permite delegar el KO sin acoplar este servicio al motor principal.
        void resolve(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor);
    }

    public record AttackResolution(ResultadoAtaque resultado, List<Boolean> historialMonedas) {}

    private final Random random = new Random();

    public AttackResolution resolveAttack(
            Partida partida,
            Ataque ataque,
            CartaEnJuego atacante,
            CartaEnJuego defensor,
            KoResolver koResolver
    ) {
        if (!cumpleCostoEnergia(atacante.getEnergiasUnidas(), ataque.getCosto())) {
            throw new IllegalStateException("El Pokémon activo no tiene suficiente energía del tipo requerido para realizar este ataque.");
        }

        // Calcula el ataque completo y devuelve también el historial de monedas.
        List<Boolean> historialMonedas = new ArrayList<>();
        ResultadoAtaque resultado = calcularDanioPorEfectos(ataque, atacante, historialMonedas);

        int nuevaHp = defensor.getHpActual() - resultado.danioFinal();
        defensor.setHpActual(Math.max(0, nuevaHp));

        System.out.println("⚔️ [BATTLE] " + atacante.getCard().getNombre() +
                " usó [" + ataque.getNombre() + "] y atacó a " + defensor.getCard().getNombre() + " por " + resultado.danioFinal());

        if (defensor.getHpActual() > 0 && (resultado.danioFinal() > 0 || ataque.getDanio() == 0)) {
            aplicarEfectosSecundarios(partida, ataque, atacante, defensor, resultado.carasSacadas(), historialMonedas, koResolver);
        }

        if (defensor.getHpActual() <= 0) {
            koResolver.resolve(partida, atacante, defensor);
        }

        return new AttackResolution(resultado, historialMonedas);
    }

    private ResultadoAtaque calcularDanioPorEfectos(Ataque ataque, CartaEnJuego atacante, List<Boolean> historialMonedas) {
        // Interpreta texto del ataque para modificar el daño base.
        int danioBase = ataque.getDanio();
        String texto = ataque.getTexto() != null ? ataque.getTexto().toLowerCase() : "";

        if (texto.isEmpty()) return new ResultadoAtaque(danioBase, 0);

        if (texto.contains("damage counter on this") || texto.contains("damage counter on it")) {
            int hpMaximo = Integer.parseInt(atacante.getCard().getHp());
            int hpFaltante = hpMaximo - atacante.getHpActual();
            int contadores = hpFaltante / 10;

            int multiplicador = 10;
            if (texto.contains("20 more damage")) multiplicador = 20;

            int danioExtra = contadores * multiplicador;
            System.out.println("💢 " + atacante.getCard().getNombre() + " está herido (" + contadores + " contadores). ¡Hace " + danioExtra + " de daño extra!");
            return new ResultadoAtaque(danioBase + danioExtra, 0);
        }

        if (texto.contains("prevent all effects of attacks") && texto.contains("including damage")) {
            if (texto.contains("flip a coin")) {
                System.out.println("🪙 [MONEDA] Tirando para Inmunidad...");
                boolean esCara = random.nextBoolean();
                historialMonedas.add(esCara);

                if (esCara) {
                    atacante.setInvulnerable(true);
                    System.out.println("🛡️ ¡Salió CARA! " + atacante.getCard().getNombre() + " activó su escudo de inmunidad.");
                } else {
                    atacante.setInvulnerable(false);
                    System.out.println("💨 Salió CRUZ. " + atacante.getCard().getNombre() + " no logró protegerse.");
                }
            } else {
                atacante.setInvulnerable(true);
            }
        }

        if (texto.contains("for each energy attached") || texto.contains("for each extra energy")) {
            int energias = atacante.getEnergiasUnidas().size();
            int multiplicador = 10;
            if (texto.contains("20 more damage") || texto.contains("20 damage")) multiplicador = 20;
            if (texto.contains("30 more damage") || texto.contains("30 damage")) multiplicador = 30;

            int danioExtra = energias * multiplicador;
            System.out.println("🔋 " + atacante.getCard().getNombre() + " canaliza sus " + energias + " energías. ¡Hace " + danioExtra + " de daño extra!");
            return new ResultadoAtaque(danioBase + danioExtra, 0);
        }

        if (texto.contains("tails, this attack does nothing") || texto.contains("tails, that attack does nothing")) {
            boolean esCara = random.nextBoolean();
            historialMonedas.add(esCara);

            if (!esCara) {
                System.out.println("🪙 Salió CRUZ. El ataque falló completamente.");
                return new ResultadoAtaque(0, 0);
            }
            System.out.println("🪙 Salió CARA. ¡El ataque acierta!");
            return new ResultadoAtaque(danioBase, 1);
        }

        if (texto.contains("times the number of heads") || texto.contains("x the number of heads") || texto.contains("for each heads")) {
            int monedas = 1;
            if (texto.contains("2 coins")) monedas = 2;
            else if (texto.contains("3 coins")) monedas = 3;
            else if (texto.contains("4 coins")) monedas = 4;
            else if (texto.contains("5 coins")) monedas = 5;

            int caras = 0;
            for (int i = 0; i < monedas; i++) {
                boolean esCara = random.nextBoolean();
                historialMonedas.add(esCara);
                if (esCara) caras++;
            }
            int danioFinal = danioBase * caras;
            System.out.println("🪙 Se tiraron " + monedas + " monedas. Caras: " + caras + ". Daño calculado: " + danioFinal);
            return new ResultadoAtaque(danioFinal, caras);
        }

        if (texto.contains("if heads") && (texto.contains("more damage") || texto.contains("damage plus"))) {
            boolean esCara = random.nextBoolean();
            historialMonedas.add(esCara);

            if (esCara) {
                System.out.println("🪙 ¡Salió CARA! Daño extra aplicado.");
                return new ResultadoAtaque(danioBase + danioBase, 1);
            }
            System.out.println("🪙 Salió CRUZ. Solo hace el daño base.");
            return new ResultadoAtaque(danioBase, 0);
        }

        return new ResultadoAtaque(danioBase, 0);
    }

    private void aplicarEfectosSecundarios(
            Partida partida,
            Ataque ataque,
            CartaEnJuego atacante,
            CartaEnJuego defensor,
            int carasSacadas,
            List<Boolean> historialMonedas,
            KoResolver koResolver
    ) {
        // Aplica estados, curación, descarte, robo y daño colateral.
        String texto = ataque.getTexto() != null ? ataque.getTexto().toLowerCase() : "";
        if (texto.isEmpty()) return;

        if (texto.contains("heal") && (texto.contains("from this pokémon") || texto.contains("from 1 of your pokémon") || texto.contains("from this pokã©mon") || texto.contains("from 1 of your pokã©mon"))) {
            int cantidadCura = 20;
            if (texto.contains("10 damage")) cantidadCura = 10;
            else if (texto.contains("30 damage")) cantidadCura = 30;
            else if (texto.contains("40 damage")) cantidadCura = 40;
            else if (texto.contains("50 damage")) cantidadCura = 50;

            int hpMaximo = Integer.parseInt(atacante.getCard().getHp());
            int nuevoHp = Math.min(hpMaximo, atacante.getHpActual() + cantidadCura);
            System.out.println("💖 " + atacante.getCard().getNombre() + " se curó " + (nuevoHp - atacante.getHpActual()) + " HP.");
            atacante.setHpActual(nuevoHp);
        }

        if (texto.contains("damage to 1 of your opponent's benched")) {
            int danioBanca = 10;
            if (texto.contains("does 20 damage")) danioBanca = 20;
            else if (texto.contains("does 30 damage")) danioBanca = 30;
            else if (texto.contains("does 40 damage")) danioBanca = 40;

            TableroJugador tableroRival = (partida.getJugador().getActivo() == defensor) ? partida.getJugador() : partida.getBot();

            if (!tableroRival.getBanca().isEmpty()) {
                CartaEnJuego victima = tableroRival.getBanca().get(random.nextInt(tableroRival.getBanca().size()));
                int hpRestante = Math.max(0, victima.getHpActual() - danioBanca);
                victima.setHpActual(hpRestante);

                System.out.println("☄️ ¡Daño colateral! " + victima.getCard().getNombre() + " (Banca) recibió " + danioBanca + " de daño.");

                if (hpRestante <= 0) {
                    koResolver.resolve(partida, atacante, victima);
                }
            }
        }

        if (texto.contains("draw a card") || texto.contains("draw 1 card") || texto.contains("draw 2 cards") || texto.contains("draw 3 cards")) {
            int aRobar = 1;
            if (texto.contains("2 cards")) aRobar = 2;
            else if (texto.contains("3 cards")) aRobar = 3;

            TableroJugador tableroAtacante = (partida.getJugador().getActivo() == atacante) ? partida.getJugador() : partida.getBot();

            for (int i = 0; i < aRobar; i++) {
                if (!tableroAtacante.getMazo().isEmpty()) {
                    tableroAtacante.getMano().add(tableroAtacante.getMazo().remove(0));
                }
            }
            System.out.println("🃏 " + atacante.getCard().getNombre() + " hizo que su entrenador robe " + aRobar + " carta(s).");
        }

        if (texto.contains("damage to itself")) {
            int autoDanio = 10;
            if (texto.contains("20 damage")) autoDanio = 20;
            else if (texto.contains("30 damage")) autoDanio = 30;
            else if (texto.contains("40 damage")) autoDanio = 40;

            atacante.setHpActual(Math.max(0, atacante.getHpActual() - autoDanio));
            System.out.println("💥 ¡Ouch! " + atacante.getCard().getNombre() + " se hizo " + autoDanio + " de daño a sí mismo por el retroceso.");

            if (atacante.getHpActual() <= 0) {
                System.out.println("💀 " + atacante.getCard().getNombre() + " se debilitó por su propio ataque.");
                koResolver.resolve(partida, defensor, atacante);
            }
        }

        if (texto.contains("discard an energy card attached to") || texto.contains("discard 1 energy card attached to") || texto.contains("discard 2 energy")) {
            if (texto.contains("attached to this") || texto.contains("attached to " + atacante.getCard().getNombre().toLowerCase())) {
                int aDescartar = texto.contains("discard 2") ? 2 : 1;
                TableroJugador tableroAtacante = (partida.getJugador().getActivo() == atacante) ? partida.getJugador() : partida.getBot();

                for (int i = 0; i < aDescartar; i++) {
                    if (!atacante.getEnergiasUnidas().isEmpty()) {
                        Card energiaDescartada = atacante.getEnergiasUnidas().remove(0);
                        tableroAtacante.getPilaDescarte().add(energiaDescartada);
                        System.out.println("📉 " + atacante.getCard().getNombre() + " descartó su energía [" + energiaDescartada.getNombre() + "].");
                    }
                }
            }
        }

        if (texto.contains("is now paralyzed")) {
            if (texto.contains("flip a coin")) {
                boolean esCara = random.nextBoolean();
                historialMonedas.add(esCara);
                if (esCara) {
                    defensor.agregarCondicion("Paralyzed");
                    System.out.println("⚡ ¡Salió CARA! " + defensor.getCard().getNombre() + " fue Paralizado.");
                } else {
                    System.out.println("💨 Salió CRUZ. Se salvó de la Parálisis.");
                }
            } else {
                defensor.agregarCondicion("Paralyzed");
                System.out.println("⚡ " + defensor.getCard().getNombre() + " fue Paralizado (100% de chance).");
            }
        }

        if (texto.contains("discard an energy") || texto.contains("discard 1 energy")) {
            if (!texto.contains("attached to this") && !texto.contains("attached to " + atacante.getCard().getNombre().toLowerCase())) {
                int aRomper = 1;
                if (texto.contains("for each heads")) {
                    aRomper = carasSacadas;
                }

                for (int i = 0; i < aRomper; i++) {
                    if (!defensor.getEnergiasUnidas().isEmpty()) {
                        Card energiaRota = defensor.getEnergiasUnidas().remove(0);
                        System.out.println("💥 ¡CRÍTICO! " + atacante.getCard().getNombre() +
                                " le destrozó la energía [" + energiaRota.getNombre() + "] a " + defensor.getCard().getNombre());
                    }
                }
            }
        }

        if (texto.contains("can't retreat during your opponent's next turn") || texto.contains("cannot retreat")) {
            defensor.agregarCondicion("CantRetreat");
            System.out.println("🪤 ¡" + defensor.getCard().getNombre() + " quedó atrapado! No podrá huir el próximo turno.");
        }

        if (texto.contains("is now poisoned")) {
            defensor.agregarCondicion("Poisoned");
            System.out.println("☠️ " + defensor.getCard().getNombre() + " fue Envenenado.");
        }

        if (texto.contains("is now asleep")) {
            if (texto.contains("flip a coin")) {
                boolean esCara = random.nextBoolean();
                historialMonedas.add(esCara);
                if (esCara) {
                    defensor.agregarCondicion("Asleep");
                    System.out.println("💤 ¡Salió CARA! " + defensor.getCard().getNombre() + " se quedó Dormido.");
                }
            } else {
                defensor.agregarCondicion("Asleep");
                System.out.println("💤 " + defensor.getCard().getNombre() + " se quedó Dormido.");
            }
        }

        if (texto.contains("is now burned")) {
            defensor.agregarCondicion("Burned");
            System.out.println("🔥 " + defensor.getCard().getNombre() + " se Quemó.");
        }

        if (texto.contains("is now confused")) {
            if (texto.contains("flip a coin")) {
                boolean esCara = random.nextBoolean();
                historialMonedas.add(esCara);
                if (esCara) {
                    defensor.agregarCondicion("Confused");
                    System.out.println("🌀 ¡Salió CARA! " + defensor.getCard().getNombre() + " se Confundió.");
                }
            } else {
                defensor.agregarCondicion("Confused");
                System.out.println("🌀 " + defensor.getCard().getNombre() + " se Confundió.");
            }
        }
    }

    private boolean cumpleCostoEnergia(List<Card> energiasUnidas, List<String> costoRequerido) {
        if (costoRequerido == null || costoRequerido.isEmpty()) {
            return true;
        }

        // Obtener los símbolos de energía que proporciona cada carta
        List<String> poolDisponible = new ArrayList<>();
        for (Card c : energiasUnidas) {
            poolDisponible.addAll(getEnergiasProvistas(c));
        }

        // Crear una lista mutable de los requerimientos en minúsculas
        List<String> reqs = new ArrayList<>();
        for (String req : costoRequerido) {
            reqs.add(req.toLowerCase());
        }

        // Intentar resolver la correspondencia usando backtracking
        boolean exito = matchEnergias(0, reqs, poolDisponible);

        if (!exito) {
            // Imprimir log detallado en la consola si falla la validación
            System.out.println("❌ [BATTLE_ATTACK_VALIDATION_FAILED]");
            System.out.println("  - Costo requerido por el ataque: " + costoRequerido);
            System.out.println("  - Energías unidas al Pokémon:");
            if (energiasUnidas.isEmpty()) {
                System.out.println("    (ninguna)");
            } else {
                for (Card c : energiasUnidas) {
                    System.out.println("    * [" + c.getId() + "] " + c.getNombre() + 
                                       " (tipo: " + c.getTipo() + ", supertype: " + c.getSupertype() + ")");
                }
            }
            System.out.println("  - Símbolos disponibles computados: " + poolDisponible);
        }

        return exito;
    }

    private List<String> getEnergiasProvistas(Card c) {
        List<String> list = new ArrayList<>();
        if (c == null) return list;

        boolean isEnergy = "Energy".equalsIgnoreCase(c.getSupertype()) 
                || (c.getSupertype() != null && c.getSupertype().toLowerCase().contains("energy"));
        if (!isEnergy) return list;

        String name = c.getNombre() != null ? c.getNombre().toLowerCase() : "";
        String tipo = c.getTipo() != null ? c.getTipo().toLowerCase() : "";

        if (name.contains("double colorless")) {
            list.add("colorless");
            list.add("colorless");
        } else if (name.contains("rainbow")) {
            list.add("rainbow");
        } else if (name.contains("grass") || name.contains("planta") || tipo.equals("grass")) {
            list.add("grass");
        } else if (name.contains("fire") || name.contains("fuego") || tipo.equals("fire")) {
            list.add("fire");
        } else if (name.contains("water") || name.contains("agua") || tipo.equals("water")) {
            list.add("water");
        } else if (name.contains("lightning") || name.contains("eléctrica") || name.contains("electrica") || tipo.equals("lightning")) {
            list.add("lightning");
        } else if (name.contains("psychic") || name.contains("psíquica") || name.contains("psiquica") || tipo.equals("psychic")) {
            list.add("psychic");
        } else if (name.contains("fighting") || name.contains("lucha") || tipo.equals("fighting")) {
            list.add("fighting");
        } else if (name.contains("darkness") || name.contains("siniestra") || name.contains("oscura") || tipo.equals("darkness")) {
            list.add("darkness");
        } else if (name.contains("metal") || tipo.equals("metal")) {
            list.add("metal");
        } else if (name.contains("fairy") || name.contains("hada") || tipo.equals("fairy")) {
            list.add("fairy");
        } else {
            if (!tipo.isEmpty() && !tipo.equals("energy")) {
                list.add(tipo);
            } else {
                list.add("colorless");
            }
        }
        return list;
    }

    private boolean matchEnergias(int reqIdx, List<String> reqs, List<String> availablePool) {
        if (reqIdx == reqs.size()) {
            return true;
        }

        String req = reqs.get(reqIdx);

        for (int i = 0; i < availablePool.size(); i++) {
            String avail = availablePool.get(i);

            boolean matches = false;
            if (req.equals("colorless")) {
                matches = true; // Colorless matches any available symbol
            } else if (avail.equals("rainbow")) {
                matches = true; // Rainbow matches any requirement
            } else if (avail.equals(req)) {
                matches = true; // Exact match
            }

            if (matches) {
                // Hacer una copia del pool y remover el símbolo usado
                List<String> nextPool = new ArrayList<>(availablePool);
                nextPool.remove(i);

                if (matchEnergias(reqIdx + 1, reqs, nextPool)) {
                    return true;
                }
            }
        }

        return false;
    }
}
