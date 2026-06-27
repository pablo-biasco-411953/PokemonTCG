# Tarea: Double Evolution Same Turn Limit

- **Fecha/Hora**: 2026-06-27 15:50 (Local)
- **Estado**: Completado
- **Última actualización**: 2026-06-27 (Commit: f4ca910)

## Cambios Realizados
- [x] **MODIFIED [CartaEnJuego.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/model/battle/CartaEnJuego.java)**: Se agregó la propiedad `ultimoTurnoEvolucionado` (inicializada en `-1`) con su correspondiente getter/setter para registrar cuándo evolucionó un Pokémon.
- [x] **MODIFIED [ComandoEvolucionar.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/battle/command/ComandoEvolucionar.java)**:
  - Se agregó la validación para impedir la evolución si el Pokémon objetivo ya evolucionó en el turno actual (`ultimoTurnoEvolucionado == numeroTurno`).
  - Se configuró el lanzamiento de un mensaje de error descriptivo: `"No podés evolucionar el mismo Pokémon más de una vez en el mismo turno."`.
  - Se actualiza `ultimoTurnoEvolucionado` con el turno actual al completarse la evolución.
- [x] **MODIFIED [BattleEngineService.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/BattleEngineService.java)**:
  - Se agregó el filtrado de opciones en la carta **Evosoda** (`xy1-116`) para que no proponga como elegibles a Pokémon que ya evolucionaron en el turno actual (o que entraron en juego en el turno actual).
  - Se registra el turno actual en `ultimoTurnoEvolucionado` del Pokémon al resolverse la evolución mediante Evosoda.
- [x] **Compilación y verificación**: El proyecto compila limpiamente mediante `mvn clean install -DskipTests`.

## Cambios Pendientes (A la mitad o pendientes)
*Ninguno para esta tarea.*

## Próximos Pasos Recomendados
1. Iniciar la aplicación y realizar QA manual en caliente usando Stoutland/Herdier/Lillipup o Evosoda para verificar que el backend efectivamente devuelva el error y bloquee la doble evolución.
