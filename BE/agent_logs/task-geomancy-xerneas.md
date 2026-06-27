# Tarea: Geomancy Xerneas Bugfix

- **Fecha/Hora**: 2026-06-27 12:40 (Local)
- **Estado**: Completado
- **Última actualización**: 2026-06-27 (Commit: d1b6418)

## Cambios Realizados
- [x] **NEW [GeomancyCommand.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/model/battle/command/GeomancyCommand.java)**: Se creó el comando para iniciar la selección interactiva de hasta 2 Pokémon en Banca (para el jugador) o auto-unir energías Hada del mazo directamente (para el bot).
- [x] **MODIFIED [AttackEffectParserService.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/battle/command/AttackEffectParserService.java)**: Se añadió la condición de mapeo para Geomancy que detecta la frase de selección de banca y búsqueda de energía Hada, enrutándola a `GeomancyCommand`.
- [x] **MODIFIED [BattleEngineService.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/BattleEngineService.java)**:
  - Se agregó el caso `SELECT_BENCHED_POKEMON_FOR_GEOMANCY` en `resolverAccionPendiente` para procesar la selección de Pokémon en banca y lanzar la búsqueda en mazo de energías Hada.
  - Se agregó el caso `GEOMANCY_ENERGY_ATTACH` en la resolución general de búsqueda en mazo para asignar las energías del mazo a cada uno de los Pokémon banca seleccionados, mezclando el mazo y terminando el turno de forma limpia.
- [x] **Compilación y verificación**: El proyecto compila limpiamente mediante `mvn clean install -DskipTests`.

## Cambios Pendientes (A la mitad o pendientes)
*Ninguno para esta tarea.*

## Próximos Pasos Recomendados
1. Realizar pruebas manuales de batalla usando a Xerneas para validar el Geomancy en caliente.
2. Escribir pruebas unitarias en `BattleEngineServiceTest` para cubrir la transición de estado y el atacheo final de Geomancy.
