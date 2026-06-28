# Tarea: Corrección de Validación de Pokémon Básicos en Estrategia de Bot y Detalle de Cartas Trainer en Frontend

- **Componente**: Backend y Frontend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó dos errores en la partida:
1. El bot jugaba cartas de tipo Objeto/Entrenador (como Great Ball) como si fuesen Pokémon activos o en banca.
2. No se mostraba la descripción (reglas/efectos) de las cartas Entrenador al pasar el mouse por encima en partida.

### Análisis y Soluciones:
1. **Bot jugando Entrenadores:** En `EstrategiaBasica.java` (estrategia del bot), el método `esPokemonBasico` carecía de validación de `supertype`, por lo que consideraba como Pokémon básicos a las cartas `Trainer` que no requerían evolución ni tenían fases de "Stage" (ej: Great Ball). Añadimos validación explícita para requerir supertipo `Pokémon` o `Pokemon`.
2. **Descripción de Trainers en Frontend:** El componente `battle-board-card-detail-panel.component.html` mostraba los ataques del Pokémon, pero no tenía lógica para renderizar la lista de `reglas` (las cuales contienen la descripción de efectos de los Trainers). Añadimos la sección `cd-rules-list` para renderizarlas si existen.

## Archivos Modificados/Creados

- **Modificado** `BE/src/main/java/com/pokemon/tcg/service/battle/strategy/EstrategiaBasica.java`: Comprobar supertype del Pokémon básico para el Bot.
- **Modificado** `FE/src/app/features/battle/battle-board-card-detail-panel.component.html`: Renderizar reglas/descripciones de cartas Trainer.

## Verificación
- Se añadieron aserciones unitarias en `EstrategiaBasicaTest.java` para verificar que el Bot bloquea correctamente el juego de cartas Trainer.
- Se ejecutó `mvn clean test` y los 1223 tests pasaron exitosamente.

