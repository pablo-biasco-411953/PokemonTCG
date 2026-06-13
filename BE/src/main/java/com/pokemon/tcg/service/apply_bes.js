const fs = require('fs');

let c = fs.readFileSync('backend/src/main/java/com/pokemon/tcg/service/BattleEngineService.java', 'utf8');

c = c.replace(
    `            if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
                partida.setTurnoActual(Partida.Turno.BOT);
                partida.getBot().setTurnosJugados(partida.getBot().getTurnosJugados() + 1);
                robarCarta(partida.getBot());
                agregarLog(partida, "TURN_STARTED", partida.getBotUsername());
            } else {
                partida.setTurnoActual(Partida.Turno.JUGADOR);
                partida.getJugador().setTurnosJugados(partida.getJugador().getTurnosJugados() + 1);
                robarCarta(partida.getJugador());
                agregarLog(partida, "TURN_STARTED", partida.getJugadorUsername());
            }`,
    `            if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
                partida.setTurnoActual(Partida.Turno.BOT);
                partida.getBot().setTurnosJugados(partida.getBot().getTurnosJugados() + 1);
                if (!robarCartaInicioTurno(partida, partida.getBot())) return;
                agregarLog(partida, "TURN_STARTED", partida.getBotUsername());
            } else {
                partida.setTurnoActual(Partida.Turno.JUGADOR);
                partida.getJugador().setTurnosJugados(partida.getJugador().getTurnosJugados() + 1);
                if (!robarCartaInicioTurno(partida, partida.getJugador())) return;
                agregarLog(partida, "TURN_STARTED", partida.getJugadorUsername());
            }`
);

c = c.replace(
    `            board.setActivo(suplente);
            agregarLog(partida, "ACTIVE_SWITCHED", callerUsername, suplente.getCard().getNombre());
        } else {`,
    `            board.setActivo(suplente);
            agregarLog(partida, "ACTIVE_SWITCHED", callerUsername, suplente.getCard().getNombre());
        } else if ("SWITCH_OPPONENT_ACTIVE".equals(pending.getType())) {
            if (ids.isEmpty()) {
                throw new IllegalArgumentException("Se requiere seleccionar un Pokémon de la banca del rival.");
            }
            TableroJugador oponente = getTableroOponente(partida, callerUsername);
            String selectedId = ids.get(0);
            CartaEnJuego suplente = oponente.getBanca().stream()
                    .filter(c -> c.getCard().getId().equals(selectedId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El Pokémon elegido no está en la banca del rival."));

            CartaEnJuego activoViejo = oponente.getActivo();
            if (activoViejo != null) {
                activoViejo.limpiarCondiciones();
                oponente.getBanca().remove(suplente);
                oponente.getBanca().add(activoViejo);
            }
            oponente.setActivo(suplente);
            agregarLog(partida, "OPPONENT_FORCED_SWITCH", callerUsername, suplente.getCard().getNombre());
        } else {`
);

c = c.replace(
    `        partida.getUltimasMonedasLanzadas().clear();
        robarCarta(partida.getBot());
        botAIService.ejecutarTurno(partida);`,
    `        partida.getUltimasMonedasLanzadas().clear();
        if (!robarCartaInicioTurno(partida, partida.getBot())) return;
        botAIService.ejecutarTurno(partida);`
);

c = c.replace(
    `        // 🚩 TU ESCUDO SE APAGA ACÁ (El bot ya jugó su turno y te intentó pegar)
        if (partida.getJugador().getActivo() != null) {
            partida.getJugador().getActivo().setInvulnerable(false);
        }`,
    `        // 🚩 TU ESCUDO SE APAGA ACÁ (El bot ya jugó su turno y te intentó pegar)
        if (partida.getJugador().getActivo() != null) {
            partida.getJugador().getActivo().setAtaqueBloqueadoSiguienteTurno(null);
            partida.getJugador().getActivo().setInvulnerable(false);
        }
        
        if (partida.getBot().getActivo() != null) {
            partida.getBot().getActivo().setAtaquePotenciadoSiguienteTurno(null);
            partida.getBot().getActivo().setDanioExtraSiguienteTurno(0);
        }`
);

c = c.replace(
    `        partida.setNumeroTurno(partida.getNumeroTurno() + 1);
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.getJugador().setTurnosJugados(partida.getJugador().getTurnosJugados() + 1);
        robarCarta(partida.getJugador());
        partida.getUltimasMonedasLanzadas().clear();
    }

    private void robarCarta(TableroJugador tablero) {`,
    `        partida.setNumeroTurno(partida.getNumeroTurno() + 1);
        partida.setTurnoActual(Partida.Turno.JUGADOR);
        partida.getJugador().setTurnosJugados(partida.getJugador().getTurnosJugados() + 1);
        if (!robarCartaInicioTurno(partida, partida.getJugador())) return;
        partida.getUltimasMonedasLanzadas().clear();
    }

    private boolean robarCartaInicioTurno(Partida partida, TableroJugador tablero) {
        if (!tablero.getMazo().isEmpty()) {
            tablero.getMano().add(tablero.getMazo().remove(0));
            return true;
        }

        String perdedor = tablero == partida.getJugador()
                ? partida.getJugadorUsername()
                : (partida.getBotUsername() != null ? partida.getBotUsername() : "BOT");
        String ganador = tablero == partida.getJugador()
                ? (partida.getBotUsername() != null ? partida.getBotUsername() : "BOT")
                : partida.getJugadorUsername();

        partida.transicionarA(new EstadoFinPartida());
        partida.setGanador(ganador);
        partida.setRazonFinPartida("El jugador no pudo robar una carta al inicio del turno");
        agregarLog(partida, "DECK_OUT", perdedor);
        return false;
    }

    private void robarCarta(TableroJugador tablero) {`
);

c = c.replace(
    `        partida.setNumeroTurno(1);
        if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
            partida.getJugador().setTurnosJugados(1);
        } else {
            partida.getBot().setTurnosJugados(1);
        }`,
    `        partida.setNumeroTurno(1);
        // Limpieza del estado del pokemon activo (efectos de turno anterior)
        if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
            // El jugador terminó su turno, el bot empieza.
            // Limpiamos los bloqueos y escudos del bot porque ahora es su turno de actuar
            if (partida.getBot().getActivo() != null) {
                partida.getBot().getActivo().setAtaqueBloqueadoSiguienteTurno(null);
                partida.getBot().getActivo().setInvulnerable(false);
            }
            if (partida.getJugador().getActivo() != null) {
                partida.getJugador().getActivo().setAtaquePotenciadoSiguienteTurno(null);
                partida.getJugador().getActivo().setDanioExtraSiguienteTurno(0);
            }
            partida.getJugador().setTurnosJugados(1);
        } else {
            partida.getBot().setTurnosJugados(1);
        }`
);

fs.writeFileSync('backend/src/main/java/com/pokemon/tcg/service/BattleEngineService.java', c);
console.log("Fully restored and applied changes!");
