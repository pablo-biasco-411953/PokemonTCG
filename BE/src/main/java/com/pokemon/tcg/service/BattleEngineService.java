package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.model.battle.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BattleEngineService {

    private final JugadorRepository jugadorRepo;
    private final MazoRepository mazoRepo;
    private final CardRepository cardRepo;
    private final Random random = new Random();
    private final Map<String, Partida> partidasEnCurso = new ConcurrentHashMap<>();
    private final BotAIService botAIService;

    public BattleEngineService(JugadorRepository jugadorRepo, MazoRepository mazoRepo,
                               CardRepository cardRepo, BotAIService botAIService) {
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
        this.cardRepo = cardRepo;
        this.botAIService = botAIService;
    }

    public Partida startBattle(String username, Long mazoId) {
        Jugador jugador = jugadorRepo.findByUsername(username);
        if (jugador == null) throw new IllegalArgumentException("Jugador no encontrado: " + username);

        Mazo mazoSeleccionado = mazoRepo.findById(mazoId)
                .orElseThrow(() -> new IllegalArgumentException("Mazo no encontrado: " + mazoId));

        List<Card> cartasMazo = mazoSeleccionado.getCartas();
        if (cartasMazo.size() < 60)
            throw new IllegalStateException("El mazo debe tener 60 cartas. Tiene: " + cartasMazo.size());

        TableroJugador tableroJugador = new TableroJugador();
        TableroJugador tableroBot = new TableroJugador();

        List<Card> mazoJugador = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoJugador);
        tableroJugador.setMazo(mazoJugador);

        List<Card> mazoBot = new ArrayList<>(cartasMazo);
        Collections.shuffle(mazoBot);
        tableroBot.setMazo(mazoBot);

        prepararJuegoInicial(tableroJugador);
        prepararJuegoInicial(tableroBot);

        Partida partida = new Partida(tableroJugador, tableroBot);
        partida.setFaseActual(Partida.Fase.LANZAMIENTO_MONEDA);

        partidasEnCurso.put(partida.getId(), partida);
        System.out.println("✅ Partida creada con ID: " + partida.getId());
        return partida;
    }

    private void prepararJuegoInicial(TableroJugador tablero) {
        for (int i = 0; i < 7; i++) {
            if (!tablero.getMazo().isEmpty())
                tablero.getMano().add(tablero.getMazo().remove(0));
        }
        for (int i = 0; i < 6; i++) {
            if (!tablero.getMazo().isEmpty())
                tablero.getPremios().add(tablero.getMazo().remove(0));
        }
    }

    public boolean lanzarMoneda(String matchId) {
        Partida partida = getPartidaOThrow(matchId);
        boolean jugadorGana = random.nextBoolean();
        partida.setFaseActual(Partida.Fase.TURNO_NORMAL);
        return jugadorGana;
    }

    public void elegirTurno(String matchId, boolean vaPrimero) {
        Partida partida = getPartidaOThrow(matchId);
        if (vaPrimero) {
            partida.setTurnoActual(Partida.Turno.JUGADOR);
        } else {
            partida.setTurnoActual(Partida.Turno.BOT);
            ejecutarTurnoBot(matchId);
        }
    }

    public Partida getEstadoPartida(String matchId) {
        return partidasEnCurso.get(matchId);
    }

    public void jugarPokemon(String matchId, String cartaId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();
        Card carta = encontrarCartaEnMano(tablero, cartaId);

        if (carta == null) throw new IllegalArgumentException("La carta no está en tu mano.");
        if (!esPokemonBasico(carta)) throw new IllegalArgumentException("Solo podés bajar Pokémon básicos.");

        CartaEnJuego nuevoPokemon = new CartaEnJuego(carta);

        if (tablero.getActivo() == null) {
            tablero.setActivo(nuevoPokemon);
            tablero.getMano().remove(carta);
            System.out.println("✅ " + carta.getNombre() + " entró como Activo.");
        } else {
            if (tablero.getBanca().size() >= 5) {
                throw new IllegalStateException("La banca está llena (máximo 5).");
            }
            tablero.getBanca().add(nuevoPokemon);
            tablero.getMano().remove(carta);
            System.out.println("✅ " + carta.getNombre() + " se unió a la banca.");
        }
    }

    public void unirEnergia(String matchId, String cartaId, String energiaId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();
        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaId);
        Card energia = encontrarCartaEnMano(tablero, energiaId);

        if (objetivo == null) throw new IllegalArgumentException("Pokemon objetivo no encontrado.");
        if (energia == null || !esEnergia(energia)) throw new IllegalArgumentException("Energía no encontrada.");

        objetivo.getEnergiasUnidas().add(energia);
        tablero.getMano().remove(energia);
    }

    public void realizarRetirada(String matchId, String nuevoActivoId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);
        TableroJugador tablero = partida.getJugador();
        CartaEnJuego activoViejo = tablero.getActivo();

        if (activoViejo == null) throw new IllegalStateException("No hay un Pokémon activo para retirar.");
        if (partida.isYaSeRetiroEsteTurno()) {
            throw new IllegalStateException("Solo podés realizar una retirada por turno.");
        }

        // 🚩 BLOQUEO POR ESTADOS Y TRAMPAS
        if (activoViejo.getCondicionesEspeciales().contains("Asleep") ||
                activoViejo.getCondicionesEspeciales().contains("Paralyzed") ||
                activoViejo.getCondicionesEspeciales().contains("CantRetreat")) {
            throw new IllegalStateException("No podés retirarte por un estado alterado o efecto de ataque.");
        }

        CartaEnJuego suplente = tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(nuevoActivoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El Pokémon elegido no está en la banca."));

        int costo = activoViejo.getCard().getCostoRetirada();
        if (activoViejo.getEnergiasUnidas().size() < costo) {
            throw new IllegalStateException("Energías insuficientes. Necesitás " + costo + " para retirar.");
        }

        for (int i = 0; i < costo; i++) {
            Card energia = activoViejo.getEnergiasUnidas().remove(0);
            tablero.getPilaDescarte().add(energia);
        }

        // 🚩 CURA AL VOLVER A LA BANCA
        activoViejo.limpiarCondiciones();

        tablero.getBanca().remove(suplente);
        tablero.getBanca().add(activoViejo);
        tablero.setActivo(suplente);
        partida.setYaSeRetiroEsteTurno(true);

        System.out.println("🔄 Retirada: " + activoViejo.getCard().getNombre() + " a la banca (Curado). Entra " + suplente.getCard().getNombre());
    }

    public void subirAActivoDesdeBanca(String matchId, String cartaIdEnBanca) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();

        if (tablero.getActivo() != null) {
            throw new IllegalStateException("Ya tenés un Pokémon activo. Debés retirarlo primero.");
        }

        CartaEnJuego elegido = tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(cartaIdEnBanca))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pokémon no encontrado en la banca."));

        tablero.getBanca().remove(elegido);
        tablero.setActivo(elegido);
        System.out.println("🚀 " + elegido.getCard().getNombre() + " ahora es tu Pokémon activo.");
    }

    public void realizarAtaque(String matchId, String nombreAtaqueElegido) {
        Partida partida = partidasEnCurso.get(matchId);

        if (partida == null) return;
        if (partida.getTurnoActual() != Partida.Turno.JUGADOR) {
            throw new IllegalStateException("No es tu turno para atacar.");
        }

        CartaEnJuego activoJugador = partida.getJugador().getActivo();
        CartaEnJuego activoBot = partida.getBot().getActivo();

        if (activoBot.isInvulnerable()) {
            System.out.println("🚫 El ataque rebotó: " + activoBot.getCard().getNombre() + " es invulnerable.");
            this.pasarTurno(matchId); // Pasamos el turno sin hacer daño
            return;
        }
        if (activoJugador == null) throw new IllegalStateException("No tenés un Pokémon activo.");
        if (activoJugador.getEnergiasUnidas().isEmpty()) throw new IllegalStateException("Necesitás al menos 1 energía unida para atacar.");
        if (activoJugador.getCondicionesEspeciales().contains("Asleep") ||
                activoJugador.getCondicionesEspeciales().contains("Paralyzed")) {
            throw new IllegalStateException("Tu Pokémon activo no puede atacar porque está " +
                    (activoJugador.getCondicionesEspeciales().contains("Asleep") ? "Dormido 💤" : "Paralizado ⚡"));
        }

        if (activoBot != null) {
            Ataque ataqueUsado = null;

            List<Ataque> ataques = activoJugador.getCard().getAtaques();
            if (ataques != null) {
                for (Ataque atk : ataques) {
                    if (atk.getNombre().equals(nombreAtaqueElegido)) {
                        ataqueUsado = atk;
                        break;
                    }
                }
            }

            if (ataqueUsado == null) throw new IllegalStateException("Ataque no encontrado.");

            // 🚩 1. CREAMOS LA LIBRETA DE ANOTACIONES
            List<Boolean> historialMonedas = new ArrayList<>();

            // 🚩 2. PASAMOS LA LIBRETA A CALCULAR DAÑO
            ResultadoAtaque resultado = calcularDanioPorEfectos(ataqueUsado, activoJugador, historialMonedas);

            // 🚩 3. APLICAMOS EL DAÑO
            int nuevaHp = activoBot.getHpActual() - resultado.danioFinal();
            activoBot.setHpActual(Math.max(0, nuevaHp));

            System.out.println("⚔️ [BATTLE] " + activoJugador.getCard().getNombre() +
                    " usó [" + nombreAtaqueElegido + "] y atacó a " + activoBot.getCard().getNombre() + " por " + resultado.danioFinal());

            // 🚩 4. LECTOR DE EFECTOS (Le pasamos la libreta también)
            if (activoBot.getHpActual() > 0) {
                if (resultado.danioFinal() > 0 || ataqueUsado.getDanio() == 0) {
                    aplicarEfectosSecundarios(partida, ataqueUsado, activoJugador, activoBot, resultado.carasSacadas(), historialMonedas);
                }
            }

            // 🚩 5. VERIFICAMOS KO
            if (activoBot.getHpActual() <= 0) {
                resolverKO(partida, activoJugador, activoBot);
            }

            // 🚩 6. GUARDAMOS LA VERDAD EN LA PARTIDA
            partida.setUltimasMonedasLanzadas(historialMonedas);

            System.out.println("🔄 Ataque finalizado. Pasando turno al BOT...");
            this.pasarTurno(matchId);

        } else {
            throw new IllegalStateException("El oponente no tiene un Pokémon activo.");
        }
    }

    private ResultadoAtaque calcularDanioPorEfectos(Ataque ataque, CartaEnJuego atacante, List<Boolean> historialMonedas) {
        int danioBase = ataque.getDanio();
        String texto = ataque.getTexto() != null ? ataque.getTexto().toLowerCase() : "";

        if (texto.isEmpty()) return new ResultadoAtaque(danioBase, 0);

        // 💢 DAÑO POR VENGANZA (Flail / Outrage)
        if (texto.contains("damage counter on this") || texto.contains("damage counter on it")) {
            int hpMaximo = Integer.parseInt(atacante.getCard().getHp());
            int hpFaltante = hpMaximo - atacante.getHpActual();
            int contadores = hpFaltante / 10; // Cada 10 de vida que le falta es 1 contador

            int multiplicador = 10;
            if (texto.contains("20 more damage")) multiplicador = 20;

            int danioExtra = contadores * multiplicador;
            System.out.println("💢 " + atacante.getCard().getNombre() + " está herido (" + contadores + " contadores). ¡Hace " + danioExtra + " de daño extra!");
            return new ResultadoAtaque(danioBase + danioExtra, 0);
        }

        if (texto.contains("prevent all effects of attacks") && texto.contains("including damage")) {
            // 🚩 REGLA: ¿Dice que hay que tirar moneda?
            if (texto.contains("flip a coin")) {
                System.out.println("🪙 [MONEDA] Tirando para Inmunidad...");
                boolean esCara = random.nextBoolean();
                historialMonedas.add(esCara);

                if (esCara) { // CARA ✅
                    atacante.setInvulnerable(true);
                    System.out.println("🛡️ ¡Salió CARA! " + atacante.getCard().getNombre() + " activó su escudo de inmunidad.");
                } else { // CRUZ ❌
                    atacante.setInvulnerable(false);
                    System.out.println("💨 Salió CRUZ. " + atacante.getCard().getNombre() + " no logró protegerse.");
                }
            } else {
                // Si la carta NO dice "flip a coin", se activa directo
                atacante.setInvulnerable(true);
            }
        }

        // 🔋 DAÑO POR EXCESO DE ENERGÍA
        if (texto.contains("for each energy attached") || texto.contains("for each extra energy")) {
            int energias = atacante.getEnergiasUnidas().size();
            int multiplicador = 10;
            if (texto.contains("20 more damage") || texto.contains("20 damage")) multiplicador = 20;
            if (texto.contains("30 more damage") || texto.contains("30 damage")) multiplicador = 30;

            int danioExtra = energias * multiplicador;
            System.out.println("🔋 " + atacante.getCard().getNombre() + " canaliza sus " + energias + " energías. ¡Hace " + danioExtra + " de daño extra!");
            return new ResultadoAtaque(danioBase + danioExtra, 0);
        }

        // 🪙 MONEDAS (Si sale ceca no hace nada)
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

        // 🪙 MONEDAS (Multiplicador de daño x Caras)
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

        // 🪙 MONEDAS (Daño extra fijo si sale cara)
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

    private void aplicarEfectosSecundarios(Partida partida, Ataque ataque, CartaEnJuego atacante, CartaEnJuego defensor, int carasSacadas, List<Boolean> historialMonedas) {
        String texto = ataque.getTexto() != null ? ataque.getTexto().toLowerCase() : "";
        if (texto.isEmpty()) return;

        // 💖 1. CURACIÓN (Heal)
        if (texto.contains("heal") && (texto.contains("from this pokémon") || texto.contains("from 1 of your pokémon"))) {
            int cantidadCura = 20; // Default
            if (texto.contains("10 damage")) cantidadCura = 10;
            else if (texto.contains("30 damage")) cantidadCura = 30;
            else if (texto.contains("40 damage")) cantidadCura = 40;
            else if (texto.contains("50 damage")) cantidadCura = 50;

            int hpMaximo = Integer.parseInt(atacante.getCard().getHp());
            int nuevoHp = Math.min(hpMaximo, atacante.getHpActual() + cantidadCura);
            System.out.println("💖 " + atacante.getCard().getNombre() + " se curó " + (nuevoHp - atacante.getHpActual()) + " HP.");
            atacante.setHpActual(nuevoHp);
        }

        // ☄️ 2. DAÑO A LA BANCA RIVAL (Sniper)
        if (texto.contains("damage to 1 of your opponent's benched")) {
            int danioBanca = 10; // Default
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
                    resolverKO(partida, atacante, victima);
                }
            }
        }

        // 🃏 3. ROBAR CARTAS AL MAZO
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

        // 💥 4. DAÑO DE RETROCESO (Recoil)
        if (texto.contains("damage to itself")) {
            int autoDanio = 10;
            if (texto.contains("20 damage")) autoDanio = 20;
            else if (texto.contains("30 damage")) autoDanio = 30;
            else if (texto.contains("40 damage")) autoDanio = 40;

            atacante.setHpActual(Math.max(0, atacante.getHpActual() - autoDanio));
            System.out.println("💥 ¡Ouch! " + atacante.getCard().getNombre() + " se hizo " + autoDanio + " de daño a sí mismo por el retroceso.");

            if (atacante.getHpActual() <= 0) {
                System.out.println("💀 " + atacante.getCard().getNombre() + " se debilitó por su propio ataque.");
                resolverKO(partida, defensor, atacante);
            }
        }

        // 📉 5. DESCARTAR ENERGÍA PROPIA
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

        // ⚡ 6. PARÁLISIS
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

        // 💥 7. ROMPER ENERGÍA DEL RIVAL
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

        // 🪤 8. BLOQUEO DE RETIRADA
        if (texto.contains("can't retreat during your opponent's next turn") || texto.contains("cannot retreat")) {
            defensor.agregarCondicion("CantRetreat");
            System.out.println("🪤 ¡" + defensor.getCard().getNombre() + " quedó atrapado! No podrá huir el próximo turno.");
        }

        // ☠️ 9. VENENO
        if (texto.contains("is now poisoned")) {
            defensor.agregarCondicion("Poisoned");
            System.out.println("☠️ " + defensor.getCard().getNombre() + " fue Envenenado.");
        }

        // 💤 10. SUEÑO
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

        // 🔥 11. QUEMADURA
        if (texto.contains("is now burned")) {
            defensor.agregarCondicion("Burned");
            System.out.println("🔥 " + defensor.getCard().getNombre() + " se Quemó.");
        }

        // 🌀 12. CONFUSIÓN
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

    private void aplicarMantenimientoEntreTurnos(Partida partida) {
        System.out.println("🔄 --- INICIANDO MANTENIMIENTO ENTRE TURNOS ---");

        procesarEstado(partida.getJugador(), partida.getBot(), partida);
        procesarEstado(partida.getBot(), partida.getJugador(), partida);

        System.out.println("🔄 --- FIN MANTENIMIENTO ---");
    }

    private void procesarEstado(TableroJugador dueno, TableroJugador rival, Partida partida) {
        CartaEnJuego activo = dueno.getActivo();
        if (activo == null) return;

        if (activo.getCondicionesEspeciales().contains("Poisoned")) {
            System.out.println("☠️ Veneno: " + activo.getCard().getNombre() + " recibe 10 de daño.");
            activo.setHpActual(Math.max(0, activo.getHpActual() - 10));
        }

        if (activo.getCondicionesEspeciales().contains("Burned")) {
            System.out.println("🔥 Quemadura: " + activo.getCard().getNombre() + " recibe 20 de daño.");
            activo.setHpActual(Math.max(0, activo.getHpActual() - 20));

            if (random.nextBoolean()) {
                System.out.println("🔥 ¡Salió CARA! " + activo.getCard().getNombre() + " se curó de la Quemadura.");
                activo.getCondicionesEspeciales().remove("Burned");
            } else {
                System.out.println("🔥 Salió CRUZ. " + activo.getCard().getNombre() + " sigue Quemado.");
            }
        }

        if (activo.getCondicionesEspeciales().contains("Asleep")) {
            if (random.nextBoolean()) {
                System.out.println("💤 ¡Salió CARA! " + activo.getCard().getNombre() + " se despertó.");
                activo.getCondicionesEspeciales().remove("Asleep");
            } else {
                System.out.println("💤 Salió CRUZ. " + activo.getCard().getNombre() + " sigue Dormido.");
            }
        }

        if (activo.getHpActual() <= 0) {
            System.out.println("💀 " + activo.getCard().getNombre() + " murió por un estado alterado.");
            resolverKO(partida, rival.getActivo(), activo);
        }
    }

    public void pasarTurno(String matchId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador jugador = partida.getJugador();
        TableroJugador bot = partida.getBot(); // 🚩 Necesitamos esta referencia para limpiarle el escudo

        if (jugador.getActivo() == null && !jugador.getBanca().isEmpty()) {
            throw new IllegalStateException("Debés subir un Pokémon de tu banca a la posición Activa antes de terminar tu turno.");
        }

        // LIMPIEZA DE TU POKÉMON (PERO DEJAMOS EL ESCUDO PRENDIDO)
        if (jugador.getActivo() != null) {
            jugador.getActivo().getCondicionesEspeciales().remove("CantRetreat");
            jugador.getActivo().getCondicionesEspeciales().remove("Paralyzed");
            jugador.getActivo().setPuedeAtacar(true);
            // ❌ ACÁ YA NO SE APAGA TU ESCUDO
        }

        // 🚩 EL ESCUDO DEL BOT SE APAGA ACÁ (Ya pasó tu turno para atacarlo)
        if (bot.getActivo() != null) {
            bot.getActivo().setInvulnerable(false);
        }

        aplicarMantenimientoEntreTurnos(partida);

        partida.setYaSeRetiroEsteTurno(false);
        partida.setTurnoActual(Partida.Turno.BOT);
    }




    public void ejecutarTurnoBot(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return;

        robarCarta(partida.getBot());
        botAIService.ejecutarTurno(partida);

        // LIMPIEZA DEL POKÉMON DEL BOT (PERO DEJAMOS SU ESCUDO PRENDIDO)
        if (partida.getBot().getActivo() != null) {
            partida.getBot().getActivo().setPuedeAtacar(true);
            partida.getBot().getActivo().getCondicionesEspeciales().remove("CantRetreat");
            partida.getBot().getActivo().getCondicionesEspeciales().remove("Paralyzed");
            // ❌ ACÁ YA NO SE APAGA EL ESCUDO DEL BOT
        }

        // 🚩 TU ESCUDO SE APAGA ACÁ (El bot ya jugó su turno y te intentó pegar)
        if (partida.getJugador().getActivo() != null) {
            partida.getJugador().getActivo().setInvulnerable(false);
        }

        aplicarMantenimientoEntreTurnos(partida);

        partida.setTurnoActual(Partida.Turno.JUGADOR);
        robarCarta(partida.getJugador());
    }
    private void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        TableroVictimaYAtacante res = identificarTableros(partida, defensor, atacante);
        if (res == null) return;

        TableroJugador tableroVictima = res.victima;
        TableroJugador tableroGanador = res.ganador;

        System.out.println("💀 [SISTEMA] Procesando K.O. de: " + defensor.getCard().getNombre());

        String idADescartar = defensor.getCard().getId();

        if (tableroVictima.getActivo() != null &&
                tableroVictima.getActivo().getCard().getId().equals(idADescartar)) {
            tableroVictima.setActivo(null);
        }

        tableroVictima.getBanca().removeIf(c -> c == null || c.getCard() == null || c.getCard().getId().equals(idADescartar));
        tableroVictima.getPilaDescarte().add(defensor.getCard());

        if (!tableroGanador.getPremios().isEmpty()) {
            tableroGanador.getMano().add(tableroGanador.getPremios().remove(0));
        }

        if (tableroGanador.getPremios().isEmpty() || (tableroVictima.getActivo() == null && tableroVictima.getBanca().isEmpty())) {
            partida.setFaseActual(Partida.Fase.FIN_PARTIDA);
        } else if (tableroVictima == partida.getBot() && tableroVictima.getActivo() == null) {
            CartaEnJuego mejor = elegirMejorReemplazoBot(tableroVictima, partida.getJugador().getActivo());
            if (mejor != null) {
                tableroVictima.getBanca().remove(mejor);
                tableroVictima.setActivo(mejor);
            }
        }
    }

    private record TableroVictimaYAtacante(TableroJugador victima, TableroJugador ganador) {}

    private TableroVictimaYAtacante identificarTableros(Partida p, CartaEnJuego def, CartaEnJuego atk) {
        TableroJugador v = encontrarTableroPorCarta(p, def);
        TableroJugador g = encontrarTableroPorCarta(p, atk);
        if (v == null || g == null) return null;
        return new TableroVictimaYAtacante(v, g);
    }

    private CartaEnJuego elegirMejorReemplazoBot(TableroJugador tableroBot, CartaEnJuego activoRival) {
        return tableroBot.getBanca().stream()
                .max((c1, c2) -> Integer.compare(
                        calcularPuntajeEstrategico(c1, activoRival),
                        calcularPuntajeEstrategico(c2, activoRival)))
                .orElse(tableroBot.getBanca().get(0));
    }

    private int calcularPuntajeEstrategico(CartaEnJuego candidato, CartaEnJuego rival) {
        int puntaje = 0;
        puntaje += candidato.getEnergiasUnidas().size() * 50;
        puntaje += candidato.getHpActual();

        if (rival == null || rival.getCard() == null || rival.getCard().getTipo() == null) {
            return puntaje;
        }

        String tipoRival = rival.getCard().getTipo();
        String miTipo = candidato.getCard().getTipo();

        if (candidato.getCard().getDebilidades() != null) {
            boolean esDebil = candidato.getCard().getDebilidades().stream()
                    .anyMatch(w -> w.get("tipo").equalsIgnoreCase(tipoRival));
            if (esDebil) puntaje -= 1000;
        }

        if (candidato.getCard().getResistencias() != null) {
            boolean esResistente = candidato.getCard().getResistencias().stream()
                    .anyMatch(r -> r.get("tipo").equalsIgnoreCase(tipoRival));
            if (esResistente) puntaje += 300;
        }

        if (rival.getCard().getDebilidades() != null && miTipo != null) {
            boolean rivalEsDebil = rival.getCard().getDebilidades().stream()
                    .anyMatch(w -> w.get("tipo").equalsIgnoreCase(miTipo));
            if (rivalEsDebil) puntaje += 500;
        }

        return puntaje;
    }

    private void robarCarta(TableroJugador tablero) {
        if (!tablero.getMazo().isEmpty())
            tablero.getMano().add(tablero.getMazo().remove(0));
    }

    private Partida getPartidaOThrow(String matchId) {
        Partida p = partidasEnCurso.get(matchId);
        if (p == null) throw new IllegalArgumentException("Partida no encontrada: " + matchId);
        return p;
    }

    private void validarTurnoJugador(Partida partida) {
        if (partida.getTurnoActual() != Partida.Turno.JUGADOR)
            throw new IllegalStateException("No es tu turno.");
    }

    private Card encontrarCartaEnMano(TableroJugador tablero, String id) {
        return tablero.getMano().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
    }

    private CartaEnJuego encontrarCartaEnTablero(TableroJugador tablero, String id) {
        if (tablero.getActivo() != null && tablero.getActivo().getCard() != null) {
            if (tablero.getActivo().getCard().getId().equals(id)) {
                return tablero.getActivo();
            }
        }

        return tablero.getBanca().stream()
                .filter(c -> c != null && c.getCard() != null)
                .filter(c -> c.getCard().getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private TableroJugador encontrarTableroPorCarta(Partida p, CartaEnJuego c) {
        if (encontrarCartaEnTablero(p.getJugador(), c.getCard().getId()) != null) return p.getJugador();
        if (encontrarCartaEnTablero(p.getBot(),     c.getCard().getId()) != null) return p.getBot();
        return null;
    }

    private boolean esPokemonBasico(Card c) {
        if (c == null) return false;

        if (c.getSupertype() == null) {
            System.out.println("❌ ERROR: La carta " + c.getNombre() + " no tiene supertype.");
            return false;
        }

        boolean esPokemon = "Pokémon".equalsIgnoreCase(c.getSupertype()) || "Pokemon".equalsIgnoreCase(c.getSupertype());

        boolean esBasico = false;
        if (c.getSubtypes() != null) {
            for (String subtype : c.getSubtypes()) {
                if ("Basic".equalsIgnoreCase(subtype)) {
                    esBasico = true;
                    break;
                }
            }
        }

        return esPokemon && esBasico;
    }

    private boolean esEnergia(Card c) {
        if (c == null || c.getSupertype() == null) return false;
        return c.getSupertype().equalsIgnoreCase("Energy");
    }

    public void evolucionarPokemon(String matchId, String cartaManoId, String cartaTableroId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador tablero = partida.getJugador();

        Card cartaEvolucion = encontrarCartaEnMano(tablero, cartaManoId);
        if (cartaEvolucion == null) {
            throw new IllegalArgumentException("La carta de evolución no está en tu mano.");
        }

        CartaEnJuego objetivo = encontrarCartaEnTablero(tablero, cartaTableroId);
        if (objetivo == null) {
            throw new IllegalArgumentException("El Pokémon objetivo no está en tu tablero.");
        }

        String evolvesFrom = cartaEvolucion.getEvolvesFrom();
        String nombreObjetivo = objetivo.getCard().getNombre();

        if (evolvesFrom == null || !evolvesFrom.equalsIgnoreCase(nombreObjetivo)) {
            throw new IllegalStateException("¡Evolución inválida! " + cartaEvolucion.getNombre() +
                    " evoluciona de " + evolvesFrom + ", no de " + nombreObjetivo + ".");
        }

        System.out.println("✨ ¡Evolución inminente! " + nombreObjetivo + " está evolucionando a " + cartaEvolucion.getNombre() + "...");

        int hpMaximoAnterior = Integer.parseInt(objetivo.getCard().getHp());
        int danioAcumulado = hpMaximoAnterior - objetivo.getHpActual();

        objetivo.setCard(cartaEvolucion);

        int nuevoHpMaximo = Integer.parseInt(cartaEvolucion.getHp());
        objetivo.setHpActual(Math.max(0, nuevoHpMaximo - danioAcumulado));

        objetivo.limpiarCondiciones();

        tablero.getMano().remove(cartaEvolucion);

        System.out.println("✅ Evolución completada con éxito. HP actual: " + objetivo.getHpActual() + "/" + nuevoHpMaximo);
    }

    public Partida debugRobarCarta(String matchId, String cardId) {
        Partida partida = getPartidaOThrow(matchId);

        Card cartaMagica = cardRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carta no encontrada en DB: " + cardId));

        partida.getJugador().getMano().add(cartaMagica);
        System.out.println("🛠️ [GOD MODE] Carta inyectada a la mano: " + cartaMagica.getNombre());

        return partida;
    }

    public Partida debugForzarEstado(String matchId, String objetivo, String estado) {
        Partida partida = getPartidaOThrow(matchId);

        CartaEnJuego activo = objetivo.equals("BOT") ?
                partida.getBot().getActivo() :
                partida.getJugador().getActivo();

        if (activo != null) {
            activo.limpiarCondiciones();
            activo.agregarCondicion(estado);
            System.out.println("🛠️ [GOD MODE] Estado " + estado + " aplicado a " + activo.getCard().getNombre());
        }

        return partida;
    }

    public Partida debugSetHp(String matchId, String objetivo, int hp) {
        Partida partida = getPartidaOThrow(matchId);

        CartaEnJuego activoVictima = objetivo.equals("BOT") ? partida.getBot().getActivo() : partida.getJugador().getActivo();
        CartaEnJuego rivalAtacante = objetivo.equals("BOT") ? partida.getJugador().getActivo() : partida.getBot().getActivo();

        if (activoVictima != null) {
            activoVictima.setHpActual(Math.max(0, hp));
            System.out.println("🛠️ [GOD MODE] HP de " + activoVictima.getCard().getNombre() + " seteado a " + hp);

            if (activoVictima.getHpActual() <= 0 && rivalAtacante != null) {
                System.out.println("🛠️ [GOD MODE] Ejecutando Muerte Súbita...");
                resolverKO(partida, rivalAtacante, activoVictima);
            }
        }

        return partida;
    }

    public java.util.List<com.pokemon.tcg.model.Card> obtenerCatalogoCartasDebug() {
        return cardRepo.findAll();
    }
}