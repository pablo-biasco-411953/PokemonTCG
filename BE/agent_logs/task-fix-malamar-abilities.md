# Tarea: Fix Malamar's Mental Trash and Mental Panic abilities

- **Fecha/Hora**: 2026-06-27 17:15
- **Estado**: Completado

## Cambios Realizados
- **[MODIFY] BE/src/main/java/com/pokemon/tcg/service/battle/command/AttackEffectParserService.java**:
  - Añadida regla de parseo de efectos para detectar la frase `"opponent flips ... coins. For each tails ... discard"`. Asocia esta regla a `DiscardRandomHandCardsByCoinTailsCommand`.
- **[MODIFY] BE/src/test/java/com/pokemon/tcg/service/AttackEffectParserServiceTest.java**:
  - Añadida prueba unitaria `parseaMentalTrashComoDiscardRandomHandCardsByCoinTailsCommand`.
- **[MODIFY] BE/src/test/java/com/pokemon/tcg/service/BattleAttackServiceTest.java**:
  - Añadido test de resolución de ataques `resolveAttackConMentalTrashEjecutaYDescartaCartasPorCadaCruz` para validar el descarte correcto por cada cruz en `Mental Trash`.
- **[MODIFY] FE/src/app/features/battle/services/battle-board-attack.service.ts**:
  - Corregida la expresión regular en `detectarCoinFlipAtaque()` para soportar `flips` (con 's') y `coins` (con 's') y sus variantes en español. Esto permite detectar correctamente los lanzamientos de monedas de Malamar (`Mental Trash` y `Mental Panic`).
- **[MODIFY] FE/src/app/features/battle/battle-board.component.ts**:
  - Añadido soporte en `reproducirCoinFlipAtaqueJugador()` y `reproducirCoinFlipAtaqueRemoto()` para crear una configuración visual de moneda dinámica (`CoinFlipConfig`) cuando se detectan monedas lanzadas en el servidor pero no están descritas directamente en el texto del ataque (como el debuf/restricción de `Mental Panic` / `debeLanzarMonedaSiAtaca`).
- **[MODIFY] BE/agent_logs/reporte-cobertura-jacoco.md**:
  - Actualizado el reporte con los nuevos tests y cobertura de `DiscardRandomHandCardsByCoinTailsCommand` al **98.04%**.

## Próximos Pasos Recomendados
- Ninguno. Ambas habilidades de Malamar han sido corregidas, probadas y validadas.
