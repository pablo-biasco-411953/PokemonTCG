# Tarea: Testing Commands Coverage (ComandoAtacar, ComandoJugarTrainer, ComandoSubirActivo)

- **Fecha/Hora**: 2026-06-26 20:06 (Local)
- **Estado**: Completado
- **Fecha/Hora de Finalización**: 2026-06-26 20:10 (Local)

## Cambios Realizados
- [x] **NEW [ComandoAtacarTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/command/ComandoAtacarTest.java)**: Cobertura incrementada del **0.00%** al **98.57%** (277/281 instrucciones).
- [x] **NEW [ComandoJugarTrainerTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/command/ComandoJugarTrainerTest.java)**: Cobertura de comandos de entrenador incrementada del **57.62%** al **100.00%** (210/210 instrucciones).
- [x] **NEW [ComandoSubirActivoTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/command/ComandoSubirActivoTest.java)**: Cobertura incrementada del **82.43%** al **100.00%** (74/74 instrucciones).
- [x] Ejecución del suite de pruebas completa: **765 pruebas unitarias exitosas**.
- [x] Actualización del reporte de cobertura global en `reporte-cobertura-jacoco.md` (incremento general de **+0.98%** en instrucciones cubiertas).

## Cambios Pendientes (A la mitad o pendientes)
*Ninguno para esta tarea.*

## Próximos Pasos Recomendados
1. Agregar cobertura sobre `BattleEngineService.java` en los flujos complejos de Setup y Mulligans (donde se concentra la mayor cantidad de código sin cubrir).
2. Agregar pruebas unitarias para `ComandoRetirarse` y `ComandoEvolucionar` para subirlos a >90% de cobertura.
3. Abordar el test de integración fallido en `CardCatalogServiceTest` (mockeando el repositorio para evitar requerir conexión a BD real).
