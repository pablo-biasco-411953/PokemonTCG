# Tarea: Gather Energy Wigglytuff Bugfix

- **Fecha/Hora**: 2026-06-27 11:45 (Local)
- **Estado**: Completado
- **Última actualización**: 2026-06-27 (Commit: 684b5cb)

## Cambios Realizados
- [x] **MODIFIED [AttackEffectParserService.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/battle/command/AttackEffectParserService.java)**: Se agregó la regla para detectar la descripción del ataque `"search your deck for a basic energy card"` y mapearlo con el destino único `"SELECT_POKEMON_FOR_GATHER_ENERGY"`.
- [x] **MODIFIED [SearchDeckCommand.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/model/battle/command/SearchDeckCommand.java)**: Se implementó el soporte para la IA del Bot al ejecutar el comando con el destino único `"SELECT_POKEMON_FOR_GATHER_ENERGY"` para auto-unir la energía básica a su Pokémon activo o en banca.
- [x] **MODIFIED [BattleEngineService.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/BattleEngineService.java)**:
  - Se agregó la interceptación en el flujo general de búsquedas del mazo de la acción `"SELECT_POKEMON_FOR_GATHER_ENERGY"`, creando la acción pendiente `"ATTACH_ENERGY_GATHER_ENERGY"` de forma aislada.
  - Se implementó la resolución de `"ATTACH_ENERGY_GATHER_ENERGY"` en `resolverAccionPendiente` para unir la energía al Pokémon destino elegido, mezclar el mazo y pasar el turno limpiando el estado.
- [x] **Compilación y verificación**: El proyecto compila limpiamente mediante `mvn clean install -DskipTests`.

## Cambios Pendientes (A la mitad o pendientes)
*Ninguno para esta tarea.*

## Próximos Pasos Recomendados
1. Iniciar la aplicación y realizar QA manual en caliente del ataque en el Frontend (con Wigglytuff) para asegurar el flujo completo de selección.
2. Añadir tests unitarios específicos en `BattleEngineServiceTest` para el caso de resolución de acción `"ATTACH_ENERGY_GATHER_ENERGY"`.
