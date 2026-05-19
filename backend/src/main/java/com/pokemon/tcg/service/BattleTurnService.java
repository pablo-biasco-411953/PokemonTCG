package com.pokemon.tcg.service;

import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
/**
 * Maneja limpieza de turno y estados que se resuelven entre turnos.
 */
public class BattleTurnService {

    @FunctionalInterface
    public interface KoResolver {
        // Se usa cuando un estado altera el HP y provoca un KO.
        void resolve(Partida partida, CartaEnJuego atacante, CartaEnJuego defensor);
    }

    public void limpiarActivoFinTurnoJugador(TableroJugador jugador) {
        // Quita bloqueos temporales que duran solo hasta el cierre del turno.
        if (jugador.getActivo() == null) {
            return;
        }

        jugador.getActivo().getCondicionesEspeciales().remove("CantRetreat");
        jugador.getActivo().getCondicionesEspeciales().remove("Paralyzed");
        jugador.getActivo().setPuedeAtacar(true);
    }

    public void limpiarActivoFinTurnoBot(TableroJugador bot) {
        // La misma limpieza, aplicada al activo del bot.
        if (bot.getActivo() == null) {
            return;
        }

        bot.getActivo().setPuedeAtacar(true);
        bot.getActivo().getCondicionesEspeciales().remove("CantRetreat");
        bot.getActivo().getCondicionesEspeciales().remove("Paralyzed");
    }

    public void aplicarMantenimientoEntreTurnos(Partida partida, Random random, KoResolver koResolver) {
        System.out.println("🔄 --- INICIANDO MANTENIMIENTO ENTRE TURNOS ---");

        procesarEstado(partida.getJugador(), partida.getBot(), partida, random, koResolver);
        procesarEstado(partida.getBot(), partida.getJugador(), partida, random, koResolver);

        System.out.println("🔄 --- FIN MANTENIMIENTO ---");
    }

    private void procesarEstado(
            TableroJugador dueno,
            TableroJugador rival,
            Partida partida,
            Random random,
            KoResolver koResolver
    ) {
        CartaEnJuego activo = dueno.getActivo();
        if (activo == null) {
            return;
        }

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
            koResolver.resolve(partida, rival.getActivo(), activo);
        }
    }
}
