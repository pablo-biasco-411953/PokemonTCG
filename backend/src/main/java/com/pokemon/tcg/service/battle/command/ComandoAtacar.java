package com.pokemon.tcg.service.battle.command;

import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.battle.CartaEnJuego;
import com.pokemon.tcg.model.battle.Partida;
import com.pokemon.tcg.model.battle.TableroJugador;
import com.pokemon.tcg.model.battle.command.CoinFlipCommand;
import com.pokemon.tcg.service.BattleAttackService;
import com.pokemon.tcg.service.BattleKoService;

import java.util.ArrayList;
import java.util.List;

public class ComandoAtacar implements ComandoTurno {

    private final String nombreAtaque;
    private final TableroJugador tableroAtacante;
    private final TableroJugador tableroDefensor;
    private final BattleAttackService attackService;
    private final BattleKoService koService;

    public ComandoAtacar(
            String nombreAtaque,
            TableroJugador tableroAtacante,
            TableroJugador tableroDefensor,
            BattleAttackService attackService,
            BattleKoService koService
    ) {
        this.nombreAtaque = nombreAtaque;
        this.tableroAtacante = tableroAtacante;
        this.tableroDefensor = tableroDefensor;
        this.attackService = attackService;
        this.koService = koService;
    }

    @Override
    public boolean puedeEjecutar(Partida partida) {
        CartaEnJuego activo = tableroAtacante.getActivo();
        if (activo == null || tableroDefensor.getActivo() == null) return false;
        return activo.isPuedeAtacar()
                && !activo.getCondicionesEspeciales().contains("Asleep")
                && !activo.getCondicionesEspeciales().contains("Paralyzed");
    }

    @Override
    public void ejecutar(Partida partida) {
        CartaEnJuego activoAtacante = tableroAtacante.getActivo();
        CartaEnJuego activoDefensor = tableroDefensor.getActivo();

        if (activoDefensor == null) throw new IllegalStateException("El oponente no tiene un Pokemon activo.");

        if (activoDefensor.isInvulnerable()) {
            System.out.println("[BATTLE] El ataque reboto: " + activoDefensor.getCard().getNombre() + " es invulnerable.");
            partida.setUltimasMonedasLanzadas(new ArrayList<>());
            return;
        }

        if (activoAtacante == null) throw new IllegalStateException("No tenes un Pokemon activo.");

        if (activoAtacante.getCondicionesEspeciales().contains("Asleep")
                || activoAtacante.getCondicionesEspeciales().contains("Paralyzed")) {
            throw new IllegalStateException("Tu Pokemon activo no puede atacar porque esta " +
                    (activoAtacante.getCondicionesEspeciales().contains("Asleep") ? "Dormido" : "Paralizado"));
        }

        Ataque ataqueUsado = resolverAtaque(activoAtacante);
        if (ataqueUsado == null) throw new IllegalStateException("Ataque no encontrado: " + nombreAtaque);
        if (activoAtacante.getAtaqueBloqueadoSiguienteTurno() != null
                && activoAtacante.getAtaqueBloqueadoSiguienteTurno().equalsIgnoreCase(nombreAtaque)) {
            throw new IllegalStateException("No podés usar " + nombreAtaque + " porque fue bloqueado en el turno anterior.");
        }
        if (!puedePagarCosto(activoAtacante, ataqueUsado)) {
            throw new IllegalStateException("Energias insuficientes para usar " + ataqueUsado.getNombre() + ".");
        }

        if (activoAtacante.isDebeLanzarMonedaSiAtaca()) {
            CoinFlipCommand preventAttackFlip = new CoinFlipCommand(null, null);
            preventAttackFlip.execute(partida, tableroAtacante, tableroDefensor);
            activoAtacante.setDebeLanzarMonedaSiAtaca(false);
            attackService.registrarEventoMoneda(partida, obtenerActorMoneda(partida), ataqueUsado.getNombre());

            boolean pudoAtacar = !partida.getUltimasMonedasLanzadas().isEmpty() && partida.getUltimasMonedasLanzadas().get(0);
            if (!pudoAtacar) {
                System.out.println("[BATTLE] El ataque fallo por efecto de moneda.");
                return;
            }
        }

        BattleAttackService.AttackResolution resolution = attackService.resolveAttack(
                partida, ataqueUsado, activoAtacante, activoDefensor,
                koService::resolverKO
        );

        partida.setUltimasMonedasLanzadas(resolution.historialMonedas());
        if (!resolution.historialMonedas().isEmpty()) {
            attackService.registrarEventoMoneda(partida, obtenerActorMoneda(partida), ataqueUsado.getNombre());
        }
        System.out.println("[BATTLE] Ataque finalizado.");
    }

    private String obtenerActorMoneda(Partida partida) {
        return tableroAtacante == partida.getJugador() ? partida.getJugadorUsername() : "BOT";
    }

    private Ataque resolverAtaque(CartaEnJuego activo) {
        List<Ataque> ataques = activo.getCard().getAtaques();
        if (ataques == null) return null;
        return ataques.stream()
                .filter(a -> a.getNombre().equals(nombreAtaque))
                .findFirst().orElse(null);
    }

    private static boolean puedePagarCosto(CartaEnJuego atacante, Ataque ataque) {
        if (ataque.getCosto() == null || ataque.getCosto().isEmpty()) return true;

        List<String> disponibles = atacante.getEnergiasUnidas().stream()
                .map(e -> normalizarTipo(tipoDeEnergia(e)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        List<String> pendiente = ataque.getCosto().stream()
                .map(ComandoAtacar::normalizarTipo)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        for (int i = pendiente.size() - 1; i >= 0; i--) {
            String req = pendiente.get(i);
            if (!"Colorless".equals(req)) {
                int idx = disponibles.indexOf(req);
                if (idx < 0) return false;
                disponibles.remove(idx);
                pendiente.remove(i);
            }
        }
        return disponibles.size() >= pendiente.size();
    }

    private static String tipoDeEnergia(com.pokemon.tcg.model.Card energia) {
        if (energia.getTipo() != null && !energia.getTipo().isBlank()
                && !"Energy".equalsIgnoreCase(energia.getTipo())) {
            return energia.getTipo();
        }
        return energia.getNombre();
    }

    private static String normalizarTipo(String tipo) {
        if (tipo == null) return "";
        String t = java.text.Normalizer.normalize(tipo, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        if (t.contains("grass") || t.contains("planta")) return "Grass";
        if (t.contains("fire") || t.contains("fuego")) return "Fire";
        if (t.contains("water") || t.contains("agua")) return "Water";
        if (t.contains("lightning") || t.contains("electrica") || t.contains("rayo")) return "Lightning";
        if (t.contains("psychic") || t.contains("psiquica")) return "Psychic";
        if (t.contains("fighting") || t.contains("lucha")) return "Fighting";
        if (t.contains("darkness") || t.contains("siniestra") || t.contains("oscuridad")) return "Darkness";
        if (t.contains("metal") || t.contains("acero")) return "Metal";
        if (t.contains("dragon")) return "Dragon";
        if (t.contains("fairy") || t.contains("hada")) return "Fairy";
        if (t.contains("colorless") || t.contains("incolora")) return "Colorless";
        return tipo;
    }

    @Override
    public String getNombre() { return "Atacar[" + nombreAtaque + "]"; }
}
