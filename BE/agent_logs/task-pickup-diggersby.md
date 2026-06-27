# Tarea: Diggersby Pickup Bugfix

- **Fecha/Hora**: 2026-06-27 16:30 (Local)
- **Estado**: Completado
- **Última actualización**: 2026-06-27 (Commit: a6aa1d1)

## Cambios Realizados
- [x] **NEW [PickupCommand.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/model/battle/command/PickupCommand.java)**: Se implementó la lógica de ataque para Pickup. Si no hay items en el descarte, registra un log descriptivo y finaliza de forma inmediata sin lanzar overlays en blanco. Para el bot auto-añade hasta 2 items a la mano, y para el jugador lanza una acción de tipo `SELECT_DISCARD_ITEMS_FOR_PICKUP` que finaliza el turno al terminar.
- [x] **MODIFIED [AttackEffectParserService.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/battle/command/AttackEffectParserService.java)**: Se mapeó el texto de Pickup `"put 2 item cards from your discard pile into your hand"` para enrutar el ataque a `PickupCommand`.
- [x] **MODIFIED [BattleEngineService.java](file:///c:/Users/alber/Desktop/POKEMON_TCG/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/BattleEngineService.java)**: Se añadió el bloque condicional para resolver `SELECT_DISCARD_ITEMS_FOR_PICKUP` dentro de `resolverAccionPendiente` extrayendo las cartas elegidas del descarte y añadiéndolas a la mano del jugador.
- [x] **Compilación y verificación**: El proyecto compila limpiamente mediante `mvn clean install -DskipTests`.

## Cambios Pendientes (A la mitad o pendientes)
*Ninguno para esta tarea.*

## Próximos Pasos Recomendados
1. Realizar pruebas manuales de batalla con Diggersby para validar que al usar Pickup se devuelvan los items del descarte a la mano y que el juego finalice el turno correctamente.
