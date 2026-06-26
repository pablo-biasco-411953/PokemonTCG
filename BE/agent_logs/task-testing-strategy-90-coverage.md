# Tarea: Testing Strategy - 90% Code Coverage

- **Fecha/Hora**: 2026-06-26 02:15
- **Estado**: En progreso (Analysis + Planning completado, Implementation pendiente)

---

## Resumen de la Tarea

Análisis completo del proyecto y creación de una estrategia de testing para alcanzar **90% de cobertura de líneas** en 3 semanas.

**Situación inicial:**
- 175 archivos Java (main)
- 10 tests existentes (cobertura 5.7%)
- JaCoCo no configurado

---

## Cambios Realizados

### ✅ COMPLETADOS

- [x] **MODIFY pom.xml**: Agregado plugin JaCoCo 0.8.10 con:
  - `prepare-agent` goal en fase compile
  - `report` goal en fase test
  - `check` goal que verifica 90% coverage mínimo por paquete
  - Configuración de exclusiones para config y DTOs

- [x] **NEW**: Análisis exhaustivo de cobertura gaps
  - Mapeado 8 Controllers (0% coverage)
  - Mapeado 46 Services (17% coverage - solo 8 tienen tests)
  - Mapeado 82 Models (0% coverage)
  - Mapeado 50 BattleCommand classes (0% coverage)
  - Mapeado 15 EstadoPartida implementations (0% coverage)

- [x] **NEW**: Plan detallado de 5 fases (3 semanas)
  - **Fase 1 (3 días)**: Controllers (8 archivos, ~90-100 tests)
  - **Fase 2 (2 días)**: Core Services (AuthService, MazoService, etc - ~80-100 tests)
  - **Fase 3 (5 días)**: Battle Commands & States (~120-150 tests) ⚠️ MÁS COMPLEJO
  - **Fase 4 (3 días)**: Servicios complejos (BattleEngine, Parser, etc - ~70-85 tests)
  - **Fase 5 (2 días)**: Models & DTOs (~80-110 tests)
  - **TOTAL**: 380-450 tests estimados

- [x] **NEW**: Estrategia de testing documentada
  - Unit tests (Mockito puro) vs Integration tests (@WebMvcTest)
  - Mocking strategy (qué mockear, qué no)
  - Test data builders necesarios (TestCardBuilder, TestPartidaBuilder, etc)
  - Naming conventions y estructura de archivos

- [x] **NEW**: Riesgos identificados y mitigación
  - Complejidad de BattleEngine
  - Randomness en CoinFlipCommand (requiere refactorización)
  - 50 battle commands (usar parametrized tests)
  - Efectos de batalla complejos
  - Setup duplicado de Partida (usar factories)

---

## Cambios Pendientes (A la mitad o pendientes)

### ⚠️ ARQUITECTURA REQUERIDA (Antes de implementar tests)

- [ ] **REFACTOR**: Inyectar `java.util.Random` en BattleCommand y servicios que usan randomness
  - Archivos afectados:
    - `BattleCommandBase.java` (crear si no existe)
    - `CoinFlipCommand.java`
    - `SobreService.java`
    - `BotAIService.java`
  - Razón: Tests necesitan ser deterministas

- [ ] **REFACTOR**: Convertir métodos privados a protected en Controllers
  - `BattleController.swapPerspective()` → protected
  - `BattleController.toSpectatorView()` → protected
  - Razón: Necesarios para tests con MockMvc

- [ ] **NEW**: Crear Test Data Builders (reutilizables)
  - `src/test/java/com/pokemon/tcg/util/TestCardBuilder.java`
  - `src/test/java/com/pokemon/tcg/util/TestPartidaBuilder.java`
  - `src/test/java/com/pokemon/tcg/util/TestAtaqueBuilder.java`
  - `src/test/java/com/pokemon/tcg/util/TestDataFactory.java`

### ⚠️ IMPLEMENTACIÓN DE TESTS (380-450 tests)

**Fase 1 - CONTROLLERS** (Sin iniciar)
- [ ] AuthControllerTest.java (12-15 tests)
- [ ] BattleControllerTest.java (18-20 tests)
- [ ] MazoControllerTest.java (12-14 tests)
- [ ] SobreControllerTest.java (8-10 tests)
- [ ] CardControllerTest.java (10-12 tests)
- [ ] JugadorControllerTest.java (8-10 tests)
- [ ] AtaqueControllerTest.java (6-8 tests)
- [ ] LobbyRoomControllerTest.java (14-16 tests)

**Fase 2 - CORE SERVICES** (Sin iniciar)
- [ ] AuthServiceTest.java (8-10 tests)
- [ ] MazoServiceTest.java (14-16 tests) - Expandir existente
- [ ] SobreServiceTest.java (10-12 tests)
- [ ] PasswordRecoveryServiceTest.java (8-10 tests)
- [ ] BotAIServiceTest.java (15-18 tests)
- [ ] LobbyRoomServiceTest.java (12-14 tests)
- [ ] CardCatalogServiceTest.java (10-12 tests)
- [ ] MazoBackupServiceTest.java (6-8 tests)

**Fase 3 - BATTLE COMMANDS & STATES** (Sin iniciar) ⚠️ MÁS TRABAJO
- [ ] DamageCommand + HealCommand tests (15-20 tests)
- [ ] EnergyCommands tests (12-16 tests)
- [ ] StatusConditionCommands tests (14-18 tests)
- [ ] CoinFlipCommand tests (8-12 tests)
- [ ] Deck Management tests (10-14 tests)
- [ ] Special Commands tests (30-40 tests)
- [ ] EstadoPartida tests (15-20 tests)

**Fase 4 - COMPLEX SERVICES** (Sin iniciar)
- [ ] BattleEngineServiceTest.java - Expandir (18-22 tests)
- [ ] BattleAttackServiceTest.java - Expandir (10-12 tests)
- [ ] BattleTurnServiceTest.java (12-15 tests)
- [ ] BattleKoServiceTest.java (8-10 tests)
- [ ] AttackEffectParserServiceTest.java - Expandir (20-25 tests)

**Fase 5 - MODELS & UTILITIES** (Sin iniciar)
- [ ] JugadorTest.java (4-6 tests)
- [ ] CardTest.java (4-6 tests)
- [ ] MazoTest.java (4-6 tests)
- [ ] PartidaTest.java (8-12 tests)
- [ ] CartaEnJuegoTest.java (6-8 tests)
- [ ] DTOs validation tests (20-30 tests)
- [ ] GlobalExceptionHandlerTest.java (6-8 tests)
- [ ] Integration tests (15-20 tests)

---

## Próximos Pasos Recomendados

### INMEDIATO (Antes de Fase 1)

1. **REFACTOR - Inyectar Random** (~2 horas)
   - Crear `RandomProvider` interface o inyectar `Random` en constructores
   - Afecta: BattleCommand, CoinFlipCommand, SobreService, BotAIService
   - Beneficio: Tests deterministas

2. **REFACTOR - Métodos privados → protected** (~30 min)
   - BattleController.swapPerspective(), toSpectatorView()
   - Beneficio: Testeable sin reflection

3. **CREATE - Test Data Builders** (~3 horas)
   - Crear 4 clases helper en `src/test/java/com/pokemon/tcg/util/`
   - Reutilizable en todos los 380+ tests
   - Beneficio: Menos código duplicado, setup más legible

### FASE 1 - CONTROLLERS (Semana 1, días 1-3) ~40 horas

4. **START Fase 1 - AuthController** (~4 horas)
   - 12-15 tests de login, register, forgotPassword, resetPassword
   - Patrón: @WebMvcTest(AuthController.class) + MockBean AuthService
   - Validar: casos exitosos + error cases (usuario no existe, password incorrecto, etc)

5. **CONTINUE Fase 1 - BattleController** (~6 horas)
   - 18-20 tests de startBattle, getEstadoPartida, heartbeat, surrender
   - Patrón: @WebMvcTest + MockBean de BattleEngineService, LobbyRoomService
   - Validar: validaciones de input, respuestas HTTP correctas

6. **CONTINUE Fase 1 - MazoController, SobreController, CardController** (~8 horas)
   - 10-14 tests cada uno
   - Similar patrón @WebMvcTest
   - Focus: validación de datos, transacciones correctas

7. **FINISH Fase 1 - JugadorController, AtaqueController, LobbyRoomController** (~6 horas)
   - Completar los últimos 3 controllers
   - Verificar cobertura: `mvn jacoco:report` en target/site/jacoco/index.html
   - Target: 90%+ en paquete controller/

### FASE 2 - CORE SERVICES (Semana 1, días 4-5) ~30 horas

8. **START Fase 2 - AuthService** (~3 horas)
   - 8-10 tests sin @SpringBootTest (unit tests puros con Mockito)
   - Validar: login exitoso, password incorrecto, usuario no existe, registro duplicado

9. **CONTINUE Fase 2 - MazoService, SobreService** (~8 horas)
   - Expandir MazoServiceTest existente
   - SobreService: 10-12 tests (importante: randomness inyectado)
   - Validar: creación de mazo (60 cartas), apertura de sobres

10. **FINISH Fase 2 - PasswordRecoveryService, BotAIService, LobbyRoomService** (~10 horas)
    - BotAIService: 15-18 tests (más complejo, lógica de decisión)
    - Verificar cobertura en service/: 90%+

### FASE 3 - BATTLE COMMANDS & STATES (Semana 2, completa) ~60 horas

11. **START Fase 3 - Command Groups organizados** (~20 horas)
    - Damage/Heal/Draw (Grupo 1)
    - Energy Management (Grupo 2)
    - Status Conditions (Grupo 3)
    - Coin Flip (Grupo 4)
    - Deck Management (Grupo 5)

12. **CONTINUE Fase 3 - Special Commands** (~20 horas)
    - Froakie, Gogoat, Magcargo, Corsola, etc (20 clases especiales)
    - Usar @ParameterizedTest para casos similares

13. **FINISH Fase 3 - EstadoPartida implementations** (~10 horas)
    - EstadoInicio, EstadoTurnoNormal, EstadoFinPartida, etc
    - Validar transiciones de estado

### FASE 4 & 5 - REMAINING (Semana 3) ~40 horas

14. **Expandir servicios complejos** (BattleEngine, Parser, Ko, Turn)
15. **Tests de models y DTOs**
16. **Integration tests** (flujos completos)
17. **Verificación final**: `mvn clean test jacoco:check`

---

## Problemas Conocidos / Consideraciones

1. **BattleEngine es muy complejo** → Testear servicios individuales primero
2. **50 comandos de batalla** → Usar parametrized tests, no un test por caso
3. **Random no inyectado** → Refactorizar ANTES de tests
4. **Estado compartido en tests** → Usar @BeforeEach para cleanup
5. **Validaciones con @Valid** → Testear con BindingResult en MockMvc
6. **Suite de tests tardará ~5 min** → OK para esta fase

---

## Comandos Útiles para Próxima Sesión

```bash
# Ejecutar tests
mvn clean test

# Generar reporte JaCoCo
mvn jacoco:report

# Ver cobertura (abre HTML)
# Windows:
start target\site\jacoco\index.html
# Mac/Linux:
open target/site/jacoco/index.html

# Verificar 90% (fallará si no alcanza)
mvn jacoco:check

# Tests de un paquete solamente
mvn test -Dtest=com.pokemon.tcg.controller.*

# Con output verboso
mvn test -X
```

---

## Decisión: ¿Qué hacer ahora?

**Usuario debe elegir UNO de estos:**

A. **COMENZAR FASE 1 DIRECTAMENTE**
   - Requiere: Refactorización de Random + métodos privados (30 min prep)
   - Ventaja: Empezar rápido, feedback temprano

B. **REFACTORIZAR PRIMERO**
   - Hacer cambios de arquitectura antes de escribir tests
   - Ventaja: Tests más limpios después

C. **CREAR TEST DATA BUILDERS PRIMERO**
   - Implementar 4 clases helper reutilizables
   - Ventaja: Less boilerplate en 380+ tests

**RECOMENDACIÓN**: Opción B (Refactor primero) → Luego Tests Builders → Luego Fase 1
Tiempo total prep: 5-6 horas, pero salva 20+ horas de mantenimiento después.

---

## Resumen de Archivos Modificados

| Archivo | Estado | Cambio |
|---------|--------|--------|
| `pom.xml` | ✅ MODIFICADO | JaCoCo plugin agregado |
| Plan de Testing (este archivo) | 📋 DOCUMENTADO | Estrategia completa |

**Archivos PENDIENTES**: 380+ tests a crear

---

**Última actualización**: 2026-06-26 02:15
**Próxima revisión**: Cuando se inicie Fase 1 o después de refactorización
