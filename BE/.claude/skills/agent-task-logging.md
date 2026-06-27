# Agent Task Logging Skill

## Descripción

Instrucción obligatoria para todos los agentes que trabajan en este repositorio. Define cómo registrar el progreso del trabajo en la carpeta `agent_logs/` para mantener un historial transparente y permitir que futuros agentes entiendan qué se está haciendo, qué se hizo y qué falta.

## Cuándo Usar Esta Skill

### CASO 1: Planes de Implementación Formales

Cuando el usuario **aprueba un plan de implementación** (ej: "Implementar 90% de test coverage", "Refactorizar módulo X"), DEBES:

1. **Registrar el plan UNA SOLA VEZ** en `agent_logs/task-[nombre].md`
2. **Actualizar el estado** conforme avanzes: "En progreso" → "Completado" o "Pausado"
3. **Documentar cambios realizados** en cada sesión
4. **Listar cambios pendientes** claramente
5. **Sugerir próximos pasos** para el siguiente agente

**Formato**: Usa la plantilla en `agent_logs/README.md`

### CASO 2: Cambios Individuales (Sin Plan Formal)

Cuando el usuario aprueba un **cambio específico o tarea puntual** (ej: "Arregla el bug en AuthService", "Agrega validación de mazo"), DEBES:

1. **Registrar el cambio** como entrada en un archivo de log
2. **Marcar como completado** una vez ejecutado
3. **Mantener historial** de quién hizo qué y cuándo

**Formato**: Línea de tipo `- [COMPLETED/IN_PROGRESS] [2026-06-26 14:30] [archivo.java] Descripción del cambio`

---

## Instrucciones Paso a Paso

### Paso 1: Leer el Historial Existente

ANTES de comenzar cualquier trabajo:

```bash
# Leer todas las tareas pendientes
cat agent_logs/*.md

# Ver la última tarea
ls -lt agent_logs/ | head -5
```

**Razón**: Evitar duplicar trabajo, entender el contexto, saber en qué fase estamos.

---

### Paso 2: Crear o Actualizar Archivo de Tarea

**Si es un PLAN FORMAL** (aprobado por usuario):

Crea archivo: `agent_logs/task-[nombre-descriptivo].md`

**Plantilla Inicial** (cuando el usuario APRUEBA el plan):

```markdown
# Tarea: [Nombre de la Tarea]

- **Fecha/Hora**: YYYY-MM-DD HH:MM (ejemplo: 2026-06-26 14:30)
- **Estado**: En progreso
- **Usuario**: [Quién aprobó / Para quién es]

## Descripción
[Breve descripción de qué se va a hacer y por qué]

## Cambios Realizados
- [ ] **[MODIFY / NEW / DELETE] [archivo]**: Descripción.

## Cambios Pendientes
- [ ] Detalle de lo que falta.

## Próximos Pasos Recomendados
1. Paso 1
2. Paso 2

---
**Última actualización**: [Fecha]
```

---

### Paso 3: Actualizar Durante el Trabajo

**EN CADA SESIÓN** mientras trabajas:

1. **Al iniciar sesión**: Marca tareas con ✅ si completaste algo
2. **Durante el trabajo**: Agrega bullets con cambios realizados
3. **Al finalizar sesión**: Actualiza estado y próximos pasos

**Ejemplo de actualización**:

```markdown
## Cambios Realizados

- [x] **MODIFY pom.xml**: Agregado JaCoCo plugin
- [x] **NEW task-testing-strategy.md**: Plan de 5 fases para 90% coverage
- [ ] **NEW TestCardBuilder.java**: (EN PROGRESO - 40% completado)

## Cambios Pendientes

- [ ] Crear TestCardBuilder.java
- [ ] Crear TestPartidaBuilder.java
- [ ] Refactorizar Random en BattleCommand
- [ ] Implementar Fase 1 (Controllers)

## Estado Actual

- **Fase actual**: Planning completado, Architecture refactor próximo
- **Bloqueadores**: Ninguno
- **Línea de comandos útil**: `mvn clean test jacoco:report`
```

---

### Paso 4: Marcar Cambios Completados

Cuando COMPLETES un cambio aprobado:

```markdown
## Cambios Realizados

- [x] **MODIFY AuthService.java**: Inyectada dependencia de Random
- [x] **MODIFY BattleController.java**: swapPerspective() ahora es protected
- [x] **NEW TestCardBuilder.java**: Creado con 5 métodos helper
```

---

### Paso 5: Sugerir Próximos Pasos

Al final de cada sesión, actualiza la sección "Próximos Pasos Recomendados":

```markdown
## Próximos Pasos Recomendados

1. **REFACTOR Random** (~2 horas)
   - Ubicación: BattleCommand.java, SobreService.java
   - Razón: Tests necesitan ser deterministas
   - Comando para verificar: `grep -r "new Random" src/main/java/`

2. **CREAR Test Data Builders** (~3 horas)
   - Archivos: TestCardBuilder.java, TestPartidaBuilder.java
   - Ubicación: src/test/java/com/pokemon/tcg/util/
   - Beneficio: Reutilizable en 380+ tests

3. **IMPLEMENTAR Fase 1** (~40 horas)
   - Controllers: 8 archivos
   - Target coverage: 90%+
```

---

## CASO 2: Cambios Individuales (Sin Plan)

Si NO hay un plan formal, pero el usuario aprueba cambios puntuales:

**Archivo**: `agent_logs/changes-log.md` (un único archivo de log)

**Formato**:

```markdown
# Registro de Cambios

## Sesión 2026-06-26

- [x] [14:30] **MODIFY AuthService.java**: Arreglado bug de login con espacios en username
- [x] [14:45] **MODIFY CardController.java**: Agregada validación de paginación
- [ ] [15:00] **TEST AuthServiceTest.java**: (EN PROGRESO) - 3 de 10 tests listos

## Sesión 2026-06-25

- [x] [10:00] **NEW BattleCommandBase.java**: Clase base para commands
- [x] [10:30] **MODIFY BattleCommand.java**: Refactorizada interfaz
```

---

## Reglas Clave

### ✅ HACER

- ✅ Registrar ANTES de empezar trabajo importante
- ✅ Actualizar DURANTE el trabajo (no solo al final)
- ✅ Usar checkboxes `[x]` para completados, `[ ]` para pendientes
- ✅ Incluir timestamps en formato ISO (YYYY-MM-DD HH:MM)
- ✅ Ser específico: nombrar archivos, métodos, líneas cuando sea relevante
- ✅ Sugerir próximos pasos claros y priorizados
- ✅ Mencionar bloqueadores o decisiones pendientes
- ✅ Incluir comandos útiles para verificar el trabajo

### ❌ NO HACER

- ❌ No crear múltiples archivos para la misma tarea
- ❌ No olvidar actualizar después de terminar una sesión
- ❌ No ser vago: evita "Hizo tests" → sé específico: "Hizo 8 tests de AuthController"
- ❌ No dejar tareas marcadas como "En progreso" sin detallar qué falta
- ❌ No registrar cambios que nunca se aprobaron formalmente

---

## Ejemplo Completo: Tarea de Testing

**Usuario aprueba**: "Implementar Fase 1 de testing (Controllers)"

**Agente crea**: `agent_logs/task-phase1-controller-tests.md`

**Sesión 1** (Día 1):

```markdown
# Tarea: Fase 1 - Controller Tests

- **Fecha/Hora**: 2026-06-26 14:00
- **Estado**: En progreso
- **Usuario**: Benjamin (aprobó plan)

## Descripción

Implementar tests para los 8 controllers del proyecto:
- AuthController, BattleController, MazoController, SobreController
- CardController, JugadorController, AtaqueController, LobbyRoomController

Target: 90%+ coverage en paquete controller/

## Cambios Realizados

- [x] **NEW AuthControllerTest.java**: 12 tests de login, register, forgotPassword
  - login exitoso ✅
  - login con password incorrecto ✅
  - register con email duplicado ✅
  - otros 9 casos ✅

- [x] **NEW BattleControllerTest.java**: 15 de 20 tests
  - startBattle() tests ✅
  - getEstadoPartida() tests ✅ (INCOMPLETO: falta perspectiva)
  - heartbeat() tests (EN PROGRESO)

## Cambios Pendientes

- [ ] Completar BattleController tests (5 tests restantes)
- [ ] Implementar MazoController tests (12-14 tests)
- [ ] Implementar SobreController tests (8-10 tests)
- [ ] Implementar CardController tests (10-12 tests)
- [ ] Implementar JugadorController tests (8-10 tests)
- [ ] Implementar AtaqueController tests (6-8 tests)
- [ ] Implementar LobbyRoomController tests (14-16 tests)

## Progreso

- Completados: 2 de 8 controllers
- Tests escritos: 27 de ~90-100
- Cobertura actual: 85% en controller/
- Target cobertura: 90%+

## Próximos Pasos Recomendados

1. **Completar BattleController** (~3 horas)
   - Falta: testear perspectiva swapped
   - Comando para probar: `mvn test -Dtest=BattleControllerTest`

2. **Implementar MazoController** (~4 horas)
   - Focus: validación de 60 cartas
   - Casos: creación OK, < 60 cartas (error), > 60 cartas (error)

3. **Batch: CardController + AtaqueController** (~4 horas)
   - Más simple, similar patrón

---
**Última actualización**: 2026-06-26 17:00
**Próxima revisión**: Cuando se complete BattleController
```

**Sesión 2** (Día 2):

```markdown
## Cambios Realizados

- [x] **NEW AuthControllerTest.java**: 12 tests ✅
- [x] **NEW BattleControllerTest.java**: 18 tests ✅
- [x] **NEW MazoControllerTest.java**: 12 tests ✅
- [x] **NEW SobreControllerTest.java**: 10 tests ✅
- [x] **NEW CardControllerTest.java**: 10 tests ✅
- [ ] **NEW JugadorControllerTest.java**: (EN PROGRESO - 5 de 8 tests)
- [ ] **NEW AtaqueControllerTest.java**: (PENDIENTE)
- [ ] **NEW LobbyRoomControllerTest.java**: (PENDIENTE)

## Progreso

- Completados: 5 de 8 controllers
- Tests escritos: 62 de ~90-100
- Cobertura actual: 88% en controller/
- Target cobertura: 90%+

## Próximos Pasos Recomendados

1. **Completar JugadorController** (~1 hora) - Ya en progreso
2. **Implementar AtaqueController** (~2 horas) - Simple
3. **Implementar LobbyRoomController** (~3 horas) - Complejo (WebSocket)
4. **Verificación final**: `mvn jacoco:check`

---
**Última actualización**: 2026-06-27 16:00
```

**Sesión 3** (Día 3 - Final):

```markdown
## Cambios Realizados

- [x] **NEW AuthControllerTest.java**: 12 tests ✅
- [x] **NEW BattleControllerTest.java**: 18 tests ✅
- [x] **NEW MazoControllerTest.java**: 12 tests ✅
- [x] **NEW SobreControllerTest.java**: 10 tests ✅
- [x] **NEW CardControllerTest.java**: 10 tests ✅
- [x] **NEW JugadorControllerTest.java**: 8 tests ✅
- [x] **NEW AtaqueControllerTest.java**: 6 tests ✅
- [x] **NEW LobbyRoomControllerTest.java**: 16 tests ✅

## Progreso

- Completados: 8 de 8 controllers ✅ 100%
- Tests escritos: 92 tests
- Cobertura actual: 92% en controller/
- Target cobertura: 90%+ ✅ ALCANZADO

## Estado Final

- **Fase 1 COMPLETADA**
- Todos los controllers tienen tests
- Cobertura por encima del target (92%)
- Tiempo total: 3 días, ~40 horas

## Próximos Pasos Recomendados (Para Fase 2)

1. **Comenzar Fase 2 - Core Services**
   - Archivos: AuthService, MazoService, SobreService, etc.
   - Tiempo estimado: 2 días (~30 horas)
   - Patrón: Unit tests con Mockito (no @SpringBootTest)

2. **Ejecutar verificación**: `mvn jacoco:check` (debería pasar)

---
**Estado**: Completado ✅
**Última actualización**: 2026-06-28 17:00
**Próxima tarea**: Fase 2 - Core Services Tests
```

---

## Cómo Los Futuros Agentes Usan Esta Info

Cuando un nuevo agente entra al proyecto:

1. Lee `agent_logs/task-*.md` para entender qué se hizo
2. Ve "Próximos Pasos Recomendados" para saber qué continúa
3. Evita duplicar trabajo, retoma donde quedó
4. Mantiene el mismo formato para continuidad

**Resultado**: Un registro transparente del proyecto que permite:
- ✅ Continuidad entre agentes
- ✅ No perder contexto entre sesiones
- ✅ Entender decisiones pasadas
- ✅ Evitar rehacer trabajo
- ✅ Auditar cambios fácilmente

---

## Cómo Invocar Esta Skill

Cualquier agente que vaya a trabajar en un plan debe:

1. Leer este documento
2. Crear/actualizar archivo en `agent_logs/`
3. Seguir el formato y reglas
4. Actualizar al terminar la sesión

**En próximas sesiones**, aparecerá como recordatorio automático en el context.

---

**Versión**: 1.0
**Creada**: 2026-06-26
**Responsable**: Sistema de Agent Task Logging
