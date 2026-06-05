# Tablero Jira - Pokemon TCG UTN

## Objetivo del tablero

Organizar los bugs, refactors y tareas de documentación detectadas en el proyecto Pokemon TCG, separando responsabilidades entre frontend, backend, full stack, QA/testing y documentación.

## Proyecto sugerido

- Nombre: `Pokemon TCG UTN`
- Key sugerida: `PTCG`
- Tipo: `Scrum`
- Tablero: `Pokemon TCG UTN - Sprint Board`
- Duración de sprint sugerida: 1 semana

## Flujo de trabajo

Estados recomendados:

1. `Backlog`
2. `Selected for Development`
3. `In Progress`
4. `Code Review`
5. `QA / Testing`
6. `Done`

Reglas simples:

- Todo bug funcional debe tener criterio de aceptación.
- Ninguna tarea pasa a `Done` sin prueba manual o test automatizado documentado.
- Las tareas críticas de motor de partida deben revisarse por al menos 1 backend y 1 tester.
- Los cambios de reglas TCG deben quedar documentados con referencia al reglamento.

## Tipos de issue

- `Epic`: agrupador grande.
- `Bug`: comportamiento incorrecto existente.
- `Story`: mejora funcional visible para usuario.
- `Task`: trabajo técnico, documentación o configuración.
- `Spike`: investigación técnica o consulta a cátedra.

## Componentes

- `Battle Engine`
- `Deck Builder`
- `Battle UI`
- `Lobby / Packs`
- `Rules / Game State`
- `Documentation`
- `QA`

## Labels

- `rules`
- `tcg-compliance`
- `backend`
- `frontend`
- `fullstack`
- `qa`
- `documentation`
- `architecture`
- `ux`
- `blocking`

## Roles para equipo de 6

### Miembro 1 - Coordinación / Documentación funcional

Responsabilidades:

- Mantener documentación del proyecto.
- Documentar reglas oficiales usadas.
- Escribir criterios de aceptación.
- Preparar consultas a la cátedra.
- Mantener changelog funcional.

Issues sugeridos:

- `PTCG-DOC-001`
- `PTCG-SPIKE-001`
- Soporte de documentación en todas las tareas de reglas.

### Miembro 2 - Backend / Battle Engine

Responsabilidades:

- Máquina de estados de partida.
- Validación de acciones legales por fase.
- Restricciones de turno.
- Cálculo de daño.

Issues sugeridos:

- `PTCG-BE-001`
- `PTCG-BE-002`
- `PTCG-BE-003`
- `PTCG-BE-004`

### Miembro 3 - Backend / Reglas y persistencia

Responsabilidades:

- Validaciones de deck builder desde backend.
- Reglas de energías.
- Costos de retirada.
- Consistencia de estado servidor-cliente.

Issues sugeridos:

- `PTCG-BE-005`
- `PTCG-BE-006`
- `PTCG-BE-007`

### Miembro 4 - Frontend

Responsabilidades:

- UI de batalla.
- Contadores visibles.
- Prompt de selección de energías.
- Feedback visual de estado.

Issues sugeridos:

- `PTCG-FE-001`
- `PTCG-FE-002`
- `PTCG-FE-003`
- `PTCG-FE-004`

### Miembro 5 - Full Stack

Responsabilidades:

- Integrar backend/frontend.
- Sincronización de estado.
- Flujo de apertura de sobres.
- Conexión entre APIs y UI.

Issues sugeridos:

- `PTCG-FS-001`
- `PTCG-FS-002`
- `PTCG-FS-003`

### Miembro 6 - QA / Testing

Responsabilidades:

- Diseñar casos de prueba.
- Validar reglas TCG.
- Probar regresiones.
- Crear checklist por sprint.

Issues sugeridos:

- `PTCG-QA-001`
- `PTCG-QA-002`
- Testing de todos los bugs de prioridad alta/crítica.

## Epics propuestos

### Epic 1 - Corrección de reglas TCG

Incluye:

- Límite correcto de energías.
- Validación de Pokémon básico.
- Límite de una energía manual por turno.
- Debilidad, resistencia y contadores de daño.
- Costos de retirada con selección manual.

### Epic 2 - Motor de partida y máquina de estados

Incluye:

- Estados globales de partida.
- Estados de turno.
- Validación de acciones por fase.
- Condición de finalización.

### Epic 3 - UX de batalla e información pública

Incluye:

- Contador de mazo sincronizado.
- Contador de mano del oponente.
- Mejoras visuales para recursos públicos.

### Epic 4 - Packs, lobby y retención

Incluye:

- Softlock al abrir sobres.
- Flujo de regreso seguro.
- Verificación de persistencia de cartas obtenidas.

### Epic 5 - QA y documentación

Incluye:

- Plan de pruebas.
- Documentación de reglas.
- Consulta a cátedra.
- Evidencia de pruebas.

## Prioridad sugerida

Orden de ataque recomendado:

1. Máquina de estados de partida.
2. Validación de deck builder: energía básica y Pokémon básico.
3. Límite de una energía por turno.
4. Selección manual de energías para retirada.
5. Refactor de daño: debilidad, resistencia y contadores.
6. Softlock de apertura de sobres.
7. Contador de mazo sincronizado.
8. Contador de mano del oponente.
9. Documentación final y checklist QA.

## Definition of Done

Una issue se considera terminada cuando:

- El comportamiento esperado se cumple.
- Hay prueba manual documentada o test automatizado.
- No rompe funcionalidades existentes.
- La UI muestra errores o bloqueos de forma clara cuando la acción no es legal.
- Si toca reglas TCG, queda documentado el criterio usado.

