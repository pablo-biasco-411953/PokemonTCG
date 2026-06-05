# Roles sugeridos para grupo de 6

## Distribución recomendada

| Persona | Rol principal | Rol secundario | Responsabilidad central |
|---|---|---|---|
| Miembro 1 | Documentación / Coordinación | Analista funcional | Documentar reglas, criterios de aceptación, consultas a cátedra y avance del sprint |
| Miembro 2 | Backend | Arquitectura | Máquina de estados, fases de turno, validación de acciones |
| Miembro 3 | Backend | Reglas TCG | Deck builder, energía por turno, retirada, daño |
| Miembro 4 | Frontend | UX | UI de batalla, contadores, modales, feedback visual |
| Miembro 5 | Full Stack | Integración | Conectar APIs con UI, apertura de sobres, sincronización de estado |
| Miembro 6 | QA / Tester | Documentación técnica | Plan de pruebas, regresión, validación de reglas |

## Capacidad sugerida por sprint

Para evitar sobrecarga:

- Backend: 8 a 13 puntos por sprint entre Miembro 2 y 3.
- Frontend: 5 a 8 puntos por sprint.
- Full Stack: 5 a 8 puntos por sprint.
- QA: acompaña todas las tareas altas/críticas y toma tareas propias de testing.
- Documentación: avanza en paralelo, no al final.

## Reglas de asignación

- Toda tarea `Highest` debe tener al menos:
  - 1 responsable técnico.
  - 1 tester asignado.
  - 1 revisor.

- Toda tarea de reglas TCG debe tener:
  - Criterio de aceptación.
  - Caso de prueba.
  - Nota de documentación si cambia comportamiento visible.

- Toda tarea frontend que cambie flujo de usuario debe tener:
  - Captura o descripción del antes/después.
  - Prueba manual en navegador.

## Ceremonias mínimas

- Planning: elegir tareas por prioridad, no por gusto.
- Daily corta: qué hice, qué hago, bloqueo.
- Review: mostrar funcionalidad andando.
- Retro: qué rompió más tiempo y cómo evitarlo.

