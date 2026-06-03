# Battle Board Refactor README

> Última actualización: 2026-05-20
> Estado general: **En progreso**

## Objetivo

Refactorizar el módulo `battle-board` del frontend de forma incremental para:

- reducir el tamaño y la complejidad de `battle-board.component.ts`
- separar lógica visual, lógica de batalla y helpers de estado
- corregir texto corrupto/mojibake
- mejorar tipado y mantenibilidad
- mantener el board funcional en cada fase

## Alcance actual

Archivos principales involucrados:

- `frontend/src/app/components/battle-board/battle-board.component.ts`
- `frontend/src/app/components/battle-board/battle-board.component.html`
- `frontend/src/app/components/battle-board/battle-board.types.ts`
- `frontend/src/app/services/battle-board-ui.service.ts`
- `frontend/src/app/services/battle-board-attack.service.ts`
- `frontend/src/app/services/battle-board-action.service.ts`
- `frontend/src/app/services/battle-board-combat.service.ts`
- `frontend/src/app/services/battle-board-state.service.ts`
- `frontend/src/app/services/battle-board-turn.service.ts`

## Restricciones acordadas

- No hacer un **rewrite completo**.
- Refactorizar por etapas pequeñas.
- Mantener el board jugable después de cada cambio.
- Actualizar este archivo a medida que se implementan fases reales.

---

## Plan por fases

### Fase 1 — Saneamiento textual y UTF-8

**Estado:** ✅ Completada

**Objetivo:**

- eliminar mojibake y texto corrupto en comentarios, logs, alerts y textos visibles

**Implementado:**

- limpieza completa del mojibake restante en `battle-board.component.ts`
- normalización de textos visibles del flujo de turnos, monedas, debug y acciones

**Resultado:**

- el `.ts` quedó limpio de corrupción textual

---

### Fase 2 — Tipado base y extracción de estado/turnos

**Estado:** ✅ Completada

**Objetivo:**

- reducir `any`
- extraer cálculos puros y helpers de estado/turnos/coin flips fuera del componente

**Implementado:**

- creación de `battle-board.types.ts`
- creación de `battle-board-state.service.ts`
- creación de `battle-board-turn.service.ts`
- tipado parcial de overlays, hover, monedas, daño y partículas
- delegación de:
  - búsqueda de objetivo de evolución
  - chequeo de despertar (`Asleep`)
  - análisis del turno del bot
  - cálculo de caras reales
  - sincronización de coin flips
  - tiempo de “pensamiento” del bot

**Pendiente dentro de esta fase:**

- seguir reduciendo `any` en partes secundarias del componente
- seguir quitando lógica derivada del flujo de turnos que aún quedó inline

---

### Fase 3 — Extracción de acciones de cartas

**Estado:** ✅ Completada

**Objetivo:**
separar la lógica de acciones del jugador en un servicio dedicado.

**Candidatas principales:**

- `jugarCarta(...)`
- `gestionarBajadaPokemon(...)`
- `gestionarUnionEnergia(...)`
- `retirarPokemon(...)`
- `ejecutarEvolucionVisual(...)`
- `seleccionarBanca(...)`

**Resultado esperado:**

- componente más delgado
- responsabilidades más claras
- flujo de acciones reutilizable y más fácil de testear

**Implementado:**

- creación de `battle-board-action.service.ts`
- delegación parcial de:
  - decisión de acción de carta (`jugarCarta`)
  - evolución + recarga de estado
  - bajada de Pokémon + recarga de estado
  - unión de energía + recarga de estado
  - promoción desde banca + recarga de estado
  - retirada + recarga de estado
  - cálculo de confirmación y validación de retiro

**Resultado:**

- las acciones del jugador ya usan un servicio dedicado para decidir y ejecutar el flujo principal de backend + recarga

---

### Fase 4 — Limpieza del flujo de ataque

**Estado:** ✅ Completada

**Objetivo:**
separar mejor la secuencia de ataque, incluyendo:

- validación de energía
- check de confusión
- detección de coin flips
- impacto visual
- transición al turno del bot

**Notas:**
parte de esta lógica ya está apoyada por `battle-board-attack.service.ts` y `battle-board-turn.service.ts`, pero aún no está completamente desacoplada del componente.

**Implementado:**

- creación de `battle-board-combat.service.ts`
- delegación de:
  - configuración y resolución del check de confusión
  - ataque + recarga de estado
  - pasar turno + recarga de estado
  - ejecución del turno del bot desde backend
  - cálculo de daño efectivo antes/después
- extracción en el componente de helpers visuales para:
  - aplicar estado refrescado
  - reproducir chequeos de despertar
  - mostrar overlay del turno del bot
  - ejecutar el turno del bot con pausa de “pensamiento”

**Resultado:**

- el flujo principal de ataque, confusión, pasar turno y transición al turno del bot quedó más separado entre servicios de combate/turnos y helpers visuales del componente

---

### Fase 5 — Extracción de UI secundaria

**Estado:** ✅ Completada

**Objetivo:**
mover UI acoplada que no debería vivir en el componente principal.

**Implementado:**

- creación de `battle-board-debug-panel.component.*`
- creación de `battle-board-card-detail-panel.component.*`
- creación de `battle-board-discard-modal.component.*`
- creación de `battle-board-abilities-panel.component.*`
- reemplazo en `battle-board.component.html` de bloques inline grandes por subcomponentes standalone con `@Input()`/`@Output()`
- adaptación del componente principal para delegar eventos de filtros debug, acciones del panel de habilidades y cierre del modal de descarte

**Resultado:**

- el `battle-board.component.html` quedó mucho más liviano
- la UI secundaria ya no está embebida en un único template gigante
- el board mantiene la lógica principal en el componente padre, pero con presentación desacoplada en piezas reutilizables

---

### Fase 6 — Verificación final y hardening

**Estado:** ✅ Completada

**Objetivo:**

- revisar consistencia del refactor
- ejecutar validaciones técnicas
- dejar documentados blockers del entorno

**Implementado:**

- hardening de tipado en subcomponentes nuevos (`BattleBoardAttack`, `CardGlossaryEntry`)
- tipado más seguro para timers y timeouts del componente principal
- validación con Prettier
- validación con `tsc --noEmit`
- ejecución de `npm run test:unit`
- nueva comprobación de `npm run build` para confirmar el bloqueo del entorno

**Resultado:**

- el refactor por fases quedó consistente a nivel de formato, TypeScript y prueba unitaria disponible
- el build Angular sigue fallando por un deadlock de esbuild con Node 25 en este entorno, por lo que no se puede usar hoy como señal confiable de regresión
- queda deuda técnica futura en seguir achicando `battle-board.component.ts`, pero el plan de 6 fases acordado quedó ejecutado

---

## Estado actual de implementación

### Ya hecho

- [x] Limpiar mojibake del componente principal
- [x] Crear tipos dedicados del battle board
- [x] Crear servicio de estado (`battle-board-state.service.ts`)
- [x] Crear servicio de turnos/coin flips (`battle-board-turn.service.ts`)
- [x] Crear servicio de acciones (`battle-board-action.service.ts`)
- [x] Crear servicio de combate (`battle-board-combat.service.ts`)
- [x] Delegar parte del flujo de bot/checkup/monedas al nuevo servicio
- [x] Delegar búsqueda de objetivo de evolución al servicio de estado
- [x] Delegar el flujo principal de acciones de cartas al nuevo servicio
- [x] Delegar parte del flujo de ataque/pasar turno al nuevo servicio de combate
- [x] Formatear archivos modificados
- [x] Validar con TypeScript

### Próximo paso recomendado

- [x] Extraer UI secundaria a subcomponentes standalone
- [x] Completar **Fase 6** con hardening y verificación final

---

## Verificaciones realizadas

### OK

- `./node_modules/.bin/prettier --check ...`
- `npx tsc -p tsconfig.app.json --noEmit`
- `npm run test:unit`

### Bloqueos del entorno

- `ng build` no es confiable en este entorno actual
  - con Node 25 hubo un deadlock de esbuild
  - con Node 24 del runtime hubo crash nativo de `malloc`

**Conclusión actual:**
la validación fuerte por ahora es TypeScript + Prettier + la prueba unitaria disponible; el build Angular completo sigue bloqueado por el entorno.

---

## Criterios de éxito del refactor

Se considerará exitoso cuando:

- `battle-board.component.ts` quede sensiblemente más chico
- la lógica de turnos/ataques/acciones esté separada por responsabilidad
- desaparezcan la mayoría de `any` más riesgosos
- el board siga funcionando sin regresiones visibles
- el mantenimiento futuro no dependa de un único archivo gigante

## Estado de cierre de este plan

Las 6 fases planificadas para este refactor quedaron ejecutadas.

Pendiente futuro no bloqueante:

- seguir reduciendo `any` históricos en `battle-board.component.ts` y servicios UI/attack
- dividir más el componente principal, que sigue siendo grande aunque ya está bastante más desacoplado
- validar visualmente en navegador cuando el entorno permita levantar el frontend de manera estable

---

## Cómo actualizar este archivo

Cada vez que se complete una fase o subfase:

1. cambiar el estado de la fase
2. mover tareas de “pendiente” a “ya hecho”
3. anotar archivos nuevos o cambios estructurales
4. registrar cualquier bloqueo del entorno o decisión técnica importante
