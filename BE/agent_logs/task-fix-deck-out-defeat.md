# Tarea: Corrección de Condiciones de Derrota (Deck Out)

- **Componente**: Backend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó que cuando un jugador se queda sin cartas en su mazo (Deck Out), la partida continuaba de forma indefinida y no se decretaba la derrota.

### Análisis y Soluciones:
1. En el reglamento del TCG de Pokémon, si un jugador no puede robar una carta al inicio de su turno porque su mazo está vacío, pierde la partida inmediatamente.
2. En `BattleEngineService.java`, el método `robarCarta` simplemente validaba si el mazo no estaba vacío antes de transferir una carta a la mano del jugador, pero omitía cualquier chequeo de derrota si el mazo ya estaba vacío.
3. Se creó el método `robarCartaInicioTurno(Partida partida, TableroJugador tablero)` el cual verifica explícitamente si el mazo está vacío al inicio de un turno. Si lo está, transiciona la partida a `FIN_PARTIDA` y decreta como ganador al oponente con la razón correspondiente.
4. Se reemplazaron todas las llamadas a `robarCarta` en los inicios de turno (tanto para multijugador como para el Bot en partida local) por esta nueva validación.

## Archivos Modificados/Creados

- **Modificado** `BE/src/main/java/com/pokemon/tcg/service/BattleEngineService.java`: Implementar método de robo inicial y chequeo de Deck Out.
- **Modificado** `BE/src/test/java/com/pokemon/tcg/service/BattleEngineServicePasarTurnoTest.java`: Añadir test unitario `pasarTurno_multiPlayer_botMazoVacio_pierdePorDeckOut`.

## Verificación
- Se corrieron los tests del backend con éxito (1224 tests pasando exitosamente).
