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

            // 🚩 1. CALCULAMOS DAÑO Y MONEDAS CON EL RECORD NUEVO
            ResultadoAtaque resultado = calcularDanioPorEfectos(ataqueUsado);

            // 🚩 2. APLICAMOS EL DAÑO
            int nuevaHp = activoBot.getHpActual() - resultado.danioFinal();
            activoBot.setHpActual(Math.max(0, nuevaHp));

            System.out.println("⚔️ [BATTLE] " + activoJugador.getCard().getNombre() +
                    " usó [" + nombreAtaqueElegido + "] y atacó a " + activoBot.getCard().getNombre() + " por " + resultado.danioFinal());

            // 🚩 3. LECTOR DE EFECTOS (Le pasamos las caras)
            if (activoBot.getHpActual() > 0) {
                if (resultado.danioFinal() > 0 || ataqueUsado.getDanio() == 0) {
                    aplicarEfectosSecundarios(ataqueUsado, activoJugador, activoBot, resultado.carasSacadas());
                }
            }

            // 🚩 4. VERIFICAMOS KO
            if (activoBot.getHpActual() <= 0) {
                resolverKO(partida, activoJugador, activoBot);
            }

            System.out.println("🔄 Ataque finalizado. Pasando turno al BOT...");
            this.pasarTurno(matchId);

        } else {
            throw new IllegalStateException("El oponente no tiene un Pokémon activo.");
        }
    }

    // 🪙 EL CEREBRO DE LAS MONEDAS (Devuelve ResultadoAtaque)
    private ResultadoAtaque calcularDanioPorEfectos(Ataque ataque) {
        int danioBase = ataque.getDanio();
        String texto = ataque.getTexto() != null ? ataque.getTexto().toLowerCase() : "";

        if (texto.isEmpty()) return new ResultadoAtaque(danioBase, 0);

        if (texto.contains("tails, this attack does nothing") || texto.contains("tails, that attack does nothing")) {
            if (!random.nextBoolean()) {
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
                if (random.nextBoolean()) caras++;
            }
            int danioFinal = danioBase * caras;
            System.out.println("🪙 Se tiraron " + monedas + " monedas. Caras: " + caras + ". Daño calculado: " + danioFinal);
            return new ResultadoAtaque(danioFinal, caras);
        }

        if (texto.contains("if heads") && (texto.contains("more damage") || texto.contains("damage plus"))) {
            if (random.nextBoolean()) {
                System.out.println("🪙 ¡Salió CARA! Daño extra aplicado.");
                return new ResultadoAtaque(danioBase + danioBase, 1);
            }
            System.out.println("🪙 Salió CRUZ. Solo hace el daño base.");
            return new ResultadoAtaque(danioBase, 0);
        }

        return new ResultadoAtaque(danioBase, 0);
    }

    // 🚩 EL LECTOR DE TEXTO DE LA CARTA (Actualizado)
    private void aplicarEfectosSecundarios(Ataque ataque, CartaEnJuego atacante, CartaEnJuego defensor, int carasSacadas) {
        String texto = ataque.getTexto() != null ? ataque.getTexto().toLowerCase() : "";
        if (texto.isEmpty()) return;

        // ⚡ PARÁLISIS
        if (texto.contains("is now paralyzed")) {
            if (texto.contains("flip a coin")) {
                if (random.nextBoolean()) {
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

        // 💥 ROMPER ENERGÍA (Pinsir)
        if (texto.contains("discard an energy") || texto.contains("discard 1 energy")) {
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

        // 🪤 BLOQUEO DE RETIRADA (Pinsir)
        if (texto.contains("can't retreat during your opponent's next turn") || texto.contains("cannot retreat")) {
            defensor.agregarCondicion("CantRetreat");
            System.out.println("🪤 ¡" + defensor.getCard().getNombre() + " quedó atrapado! No podrá huir el próximo turno.");
        }

        // ☠️ VENENO
        if (texto.contains("is now poisoned")) {
            defensor.agregarCondicion("Poisoned");
            System.out.println("☠️ " + defensor.getCard().getNombre() + " fue Envenenado.");
        }

        // 💤 SUEÑO
        if (texto.contains("is now asleep")) {
            defensor.agregarCondicion("Asleep");
            System.out.println("💤 " + defensor.getCard().getNombre() + " se quedó Dormido.");
        }

        // 🔥 QUEMADURA
        if (texto.contains("is now burned")) {
            defensor.agregarCondicion("Burned");
            System.out.println("🔥 " + defensor.getCard().getNombre() + " se Quemó.");
        }

        // 🌀 CONFUSIÓN
        if (texto.contains("is now confused")) {
            if (texto.contains("flip a coin")) {
                if (random.nextBoolean()) {
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

        // Revisamos los estados del Pokémon del Jugador
        procesarEstado(partida.getJugador(), partida.getBot(), partida);
        // Revisamos los estados del Pokémon del Bot
        procesarEstado(partida.getBot(), partida.getJugador(), partida);

        System.out.println("🔄 --- FIN MANTENIMIENTO ---");
    }

    private void procesarEstado(TableroJugador dueno, TableroJugador rival, Partida partida) {
        CartaEnJuego activo = dueno.getActivo();
        if (activo == null) return;

        // ☠️ VENENO: Saca 10 de vida fijo
        if (activo.getCondicionesEspeciales().contains("Poisoned")) {
            System.out.println("☠️ Veneno: " + activo.getCard().getNombre() + " recibe 10 de daño.");
            activo.setHpActual(Math.max(0, activo.getHpActual() - 10));
        }

        // 🔥 QUEMADURA: Saca 20 de vida y tira moneda para curarse
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

        // 💤 SUEÑO: Tira moneda para despertar
        if (activo.getCondicionesEspeciales().contains("Asleep")) {
            if (random.nextBoolean()) {
                System.out.println("💤 ¡Salió CARA! " + activo.getCard().getNombre() + " se despertó.");
                activo.getCondicionesEspeciales().remove("Asleep");
            } else {
                System.out.println("💤 Salió CRUZ. " + activo.getCard().getNombre() + " sigue Dormido.");
            }
        }

        // 💀 VERIFICAR MUERTE POR ESTADOS: Si el veneno/fuego lo mató, el rival roba premio
        if (activo.getHpActual() <= 0) {
            System.out.println("💀 " + activo.getCard().getNombre() + " murió por un estado alterado.");
            resolverKO(partida, rival.getActivo(), activo);
        }
    }

    public void pasarTurno(String matchId) {
        Partida partida = getPartidaOThrow(matchId);
        validarTurnoJugador(partida);

        TableroJugador jugador = partida.getJugador();

        // REGLA: Obligatorio subir un Pokémon si tu activo murió
        if (jugador.getActivo() == null && !jugador.getBanca().isEmpty()) {
            throw new IllegalStateException("Debés subir un Pokémon de tu banca a la posición Activa antes de terminar tu turno.");
        }

        // 🚩 LIMPIAMOS TRAMPAS Y PARÁLISIS DE TU POKÉMON (Duran hasta el final de tu turno)
        if (jugador.getActivo() != null) {
            jugador.getActivo().getCondicionesEspeciales().remove("CantRetreat");
            jugador.getActivo().getCondicionesEspeciales().remove("Paralyzed"); // <-- SE CURA LA PARÁLISIS
            jugador.getActivo().setPuedeAtacar(true);
        }

        // 🚩 APLICAMOS EL VENENO Y EL FUEGO A TODOS
        aplicarMantenimientoEntreTurnos(partida);

        partida.setYaSeRetiroEsteTurno(false);
        partida.setTurnoActual(Partida.Turno.BOT);
        ejecutarTurnoBot(matchId);
    }

    @Async
    public void ejecutarTurnoBot(String matchId) {
        Partida partida = partidasEnCurso.get(matchId);
        if (partida == null) return;

        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        robarCarta(partida.getBot());
        botAIService.ejecutarTurno(partida);
        robarCarta(partida.getJugador());

        // 🚩 LIMPIAMOS TRAMPAS Y PARÁLISIS DEL BOT
        if (partida.getBot().getActivo() != null) {
            partida.getBot().getActivo().setPuedeAtacar(true);
            partida.getBot().getActivo().getCondicionesEspeciales().remove("CantRetreat");
            partida.getBot().getActivo().getCondicionesEspeciales().remove("Paralyzed"); // <-- SE CURA EL BOT
        }

        // 🚩 APLICAMOS EL VENENO Y EL FUEGO A TODOS (Versión post-turno del bot)
        aplicarMantenimientoEntreTurnos(partida);

        partida.setTurnoActual(Partida.Turno.JUGADOR);
    }

    private void resolverKO(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor) {
        TableroJugador tableroVictima  = encontrarTableroPorCarta(partida, defensor);
        TableroJugador tableroGanador  = encontrarTableroPorCarta(partida, atacante);

        if (tableroVictima == null || tableroGanador == null) return;

        tableroVictima.getPilaDescarte().add(defensor.getCard());
        if (defensor.equals(tableroVictima.getActivo())) {
            tableroVictima.setActivo(null);
        } else {
            tableroVictima.getBanca().remove(defensor);
        }

        if (!tableroGanador.getPremios().isEmpty()) {
            tableroGanador.getMano().add(tableroGanador.getPremios().remove(0));
        }

        System.out.println("💀 K.O.! Premios restantes del ganador: " + tableroGanador.getPremios().size());

        boolean ganadorSinPremios = tableroGanador.getPremios().isEmpty();
        boolean victimasinPokemon = tableroVictima.getActivo() == null && tableroVictima.getBanca().isEmpty();

        if (ganadorSinPremios || victimasinPokemon) {
            partida.setFaseActual(Partida.Fase.FIN_PARTIDA);
            System.out.println("🏆 ¡Partida terminada!");
        } else if (tableroVictima.getActivo() == null && !tableroVictima.getBanca().isEmpty()) {
            if (tableroVictima == partida.getBot()) {
                CartaEnJuego rivalActivo = partida.getJugador().getActivo();

                CartaEnJuego mejorReemplazo = tableroVictima.getBanca().stream()
                        .max((c1, c2) -> {
                            int score1 = calcularPuntajeEstrategico(c1, rivalActivo);
                            int score2 = calcularPuntajeEstrategico(c2, rivalActivo);
                            return Integer.compare(score1, score2);
                        })
                        .get();

                tableroVictima.getBanca().remove(mejorReemplazo);
                tableroVictima.setActivo(mejorReemplazo);

                System.out.println("🤖 [BOT] Analizó tipos y subió a " + mejorReemplazo.getCard().getNombre() + " como la mejor respuesta táctica.");
            }
        }
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
        if (tablero.getActivo() != null && tablero.getActivo().getCard().getId().equals(id))
            return tablero.getActivo();
        return tablero.getBanca().stream()
                .filter(c -> c.getCard().getId().equals(id))
                .findFirst().orElse(null);
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
}