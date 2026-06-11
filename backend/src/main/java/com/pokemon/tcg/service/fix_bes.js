const fs = require('fs');
let c = fs.readFileSync('backend/src/main/java/com/pokemon/tcg/service/BattleEngineService.java', 'utf8');

const badBlock = `        // Limpieza del estado del pokemon activo (efectos de turno anterior)
        if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
            // El jugador terminó su turno, el bot empieza.
            // Limpiamos los bloqueos y escudos del bot porque ahora es su turno de actuar
            if (partida.getBot().getActivo() != null) {
                partida.getBot().getActivo().setAtaqueBloqueadoSiguienteTurno(null);
                partida.getBot().getActivo().setInvulnerable(false);
                partida.getBot().getActivo().setCondicionRecienAplicada(false);
            }
            if (partida.getJugador().getActivo() != null) {
                partida.getJugador().getActivo().setAtaquePotenciadoSiguienteTurno(null);
                partida.getJugador().getActivo().setDanioExtraSiguienteTurno(0);
            }
            partida.getJugador().setTurnosJugados(1);`;

const correctBlock = `        if (partida.getTurnoActual() == Partida.Turno.JUGADOR) {
            partida.getJugador().setTurnosJugados(1);`;

if (c.includes(badBlock)) {
    c = c.replace(badBlock, correctBlock);
    fs.writeFileSync('backend/src/main/java/com/pokemon/tcg/service/BattleEngineService.java', c);
    console.log("Fixed!");
} else {
    // try loosely
    let startIdx = c.indexOf('// Limpieza del estado del pokemon activo (efectos de turno anterior)');
    let endIdx = c.indexOf('partida.getJugador().setTurnosJugados(1);', startIdx);
    if (startIdx !== -1 && endIdx !== -1) {
        c = c.substring(0, startIdx) + correctBlock + c.substring(endIdx + 'partida.getJugador().setTurnosJugados(1);'.length);
        fs.writeFileSync('backend/src/main/java/com/pokemon/tcg/service/BattleEngineService.java', c);
        console.log("Fixed loosely!");
    } else {
        console.log("Could not find block");
    }
}
