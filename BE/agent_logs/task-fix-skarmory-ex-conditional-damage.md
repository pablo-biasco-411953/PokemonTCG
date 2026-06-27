# Tarea: Fix Skarmory-EX Tailspin Piledriver conditional damage logic

- **Fecha/Hora**: 2026-06-27 16:23
- **Estado**: Completado

## Cambios Realizados
- **[MODIFY] BE/src/main/java/com/pokemon/tcg/model/battle/command/ConditionalDamageMultiplierCommand.java**:
  - Añadido soporte para la condición `"OPPONENT_HAS_DAMAGE_COUNTERS"` que comprueba si el Pokémon activo del defensor tiene al menos un contador de daño (es decir, su HP actual es menor que su HP máximo).
- **[MODIFY] BE/src/main/java/com/pokemon/tcg/service/battle/command/AttackEffectParserService.java**:
  - Añadido un patrón de reconocimiento de efectos para la frase `"already has any damage counters on it"`. Cuando se detecta, se parsea dinámicamente y se añade el comando `ConditionalDamageMultiplierCommand` con el modificador de daño correspondiente (40).
- **[MODIFY] BE/src/test/java/com/pokemon/tcg/service/AttackEffectParserServiceTest.java**:
  - Añadida prueba unitaria `parseaTailspinPiledriverComoOpponentHasDamageCounters` para validar que el texto de Skarmory-EX sea parseado correctamente.
- **[MODIFY] BE/src/test/java/com/pokemon/tcg/service/BattleAttackServiceTest.java**:
  - Añadidas dos pruebas de resolución de ataques:
    - `resolveAttackConTailspinPiledriverNoAplicaExtraDanioSiDefensorNoTieneDanio`
    - `resolveAttackConTailspinPiledriverAplicaExtraDanioSiDefensorTieneDanio`
- **[MODIFY] BE/agent_logs/reporte-cobertura-jacoco.md**:
  - Actualizado el reporte con los nuevos tests y la cobertura incrementada de `BattleAttackService` al **86.63%** (+2.68% de incremento).

## Próximos Pasos Recomendados
- Ninguno. El comportamiento condicional de Skarmory-EX ha sido corregido y completamente validado mediante pruebas automatizadas.
