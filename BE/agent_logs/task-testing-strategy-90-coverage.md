# Tarea: Testing Strategy - 90% Code Coverage

- **Fecha/Hora última actualización**: 2026-06-27 18:18 (sesión 5)
- **Estado**: En progreso — 76.82% cobertura real (ver [reporte-cobertura-jacoco.md](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/agent_logs/reporte-cobertura-jacoco.md)), objetivo 80% global / 90% lógico (RNF-03)

---

## Resumen de la Tarea

Alcanzar **90% de cobertura de líneas** (JaCoCo) en el backend Spring Boot del juego Pokemon TCG.
Branch activo: `feat/implement-comprehensive-90-percent-code-coverage`

**Situación inicial:**
- 10 tests existentes (cobertura ~5.7%)
- JaCoCo no configurado

---

## Cambios Realizados

### ✅ COMPLETADOS

- [x] **MODIFY pom.xml**: JaCoCo 0.8.10 con `prepare-agent`, `report`, y `check` al 90%

- [x] **NEW controller/AuthControllerTest.java** — login, register, forgotPassword (~15 tests)
- [x] **NEW controller/BattleControllerTest.java** — startBattle, getEstado, heartbeat, surrender (~20 tests)
- [x] **NEW controller/CardControllerTest.java** — getCatalog, getById (~10 tests)
- [x] **NEW controller/JugadorControllerTest.java** — getProfile, updateProfile (~10 tests)
- [x] **NEW controller/LobbyRoomControllerTest.java** — create, join, list rooms (~14 tests)
- [x] **NEW controller/MazoControllerTest.java** — CRUD mazo (~14 tests)
- [x] **NEW controller/SobreControllerTest.java** — abrirSobre (~10 tests)

- [x] **NEW exception/GlobalExceptionHandlerTest.java** — manejo de excepciones HTTP (~15 tests)

- [x] **NEW model/battle/state/EstadosPartidaTest.java** — 14 estados de partida (~25 tests)

- [x] **NEW model/battle/command/BattleCommandsTest.java** — DamageCommand, HealCommand, DrawCommand, etc. (~30 tests)
- [x] **NEW model/battle/command/BattleCommandsExtendedTest.java** — ConditionalDamage, AddDamageByDamageCounters, SelfBenchDamage, AttachEnergyFromDiscard, SwitchOpponentActive, MoveEnergy, etc. (~34 tests)
- [x] **NEW model/battle/command/BattleCommandsRemainingTest.java** — AtaquePotenciado, SetAttackBlock, SetCannotAttack, SetPreventDamage, TormentBlock, DiscardAttachedEnergy, MoveOpponentEnergy, OptionalDiscardEnergy, PeekTopDeck, DiscardTopDeckAttachEnergy, RandomAsleepOrPoisoned, OpponentShuffleHandDraw (~25 tests)

- [x] **NEW service/AuthServiceTest.java** — login, register, errores (~10 tests)
- [x] **NEW service/AttackEffectParserServiceTest.java** — parsing de efectos de ataque (~20 tests)
- [x] **NEW service/BattleAttackServiceTest.java** — aplicarAtaque, debilidades, resistencias (~12 tests)
- [x] **NEW service/BattleKoServiceTest.java** — resolverKO, premios (~10 tests)
- [x] **NEW service/BattleServiceTest.java** (~10 tests)
- [x] **NEW service/BattleTurnServiceTest.java** (~12 tests)
- [x] **NEW service/BotAIServiceTest.java** (~15 tests)
- [x] **NEW service/CardCatalogServiceTest.java** — ⚠️ FALLA por necesitar BD real (integración)
- [x] **NEW service/LobbyRoomServiceTest.java** (~12 tests)
- [x] **NEW service/MazoBackupServiceTest.java** (~8 tests)
- [x] **NEW service/MazoServiceTest.java** + **MazoServiceExtendedTest.java** (~30 tests)
- [x] **NEW service/PasswordRecoveryServiceTest.java** (~10 tests)
- [x] **NEW service/SobreServiceTest.java** (~12 tests)

- [x] **NEW service/BattleEngineServiceMoreTest.java** (35 tests):
  - lanzarMoneda (5), actualizarLoading (3), actualizarHandshakeMoneda (4)
  - elegirTurno (5), evaluarSetupInitialDraw (3), ejecutarMulligan (3)
  - colocarActivoSetup (6), colocarBancaSetup (3), confirmarBancaSetup (3)

- [x] **NEW service/BattleEngineServiceSetupTest.java** (22 tests):
  - rendirse (6), colocarPremios (4), confirmarRevealSetup (2)
  - getEstadoPartida (3), registrarHeartbeat (2), resolverCartasExtra (2), not-found throws (3)

- [x] **NEW service/BattleEngineServiceTurnTest.java** (11 tests):
  - validarTurno (2), jugarPokemon (3), unirEnergia (3), realizarRetirada (2), realizarAtaque primerTurno (1)

- [x] **NEW service/BattleEngineServicePasarTurnoTest.java** (10 tests):
  - pasarTurno SP/MP, flags reset, Paralyzed cleanup, invulnerable reset, Poisoned maintenance, jugarTrainer errores, subirAActivoDesdeBanca

- [x] **NEW service/BattleEngineServiceTrainerTest.java** (17 tests):
  - Cassius (xy1-115), Evosoda (xy1-116), Great Ball (xy1-118), Hard Charm (xy1-119)
  - Max Revive (xy1-120), Professor Sycamore (xy1-122), Professor's Letter (xy1-123)
  - Red Card (xy1-124), Roller Skates (xy1-125), Shauna (xy1-127)
  - Super Potion (xy1-128), Team Flare Grunt (xy1-129)

- [x] **NEW service/BattleEngineServiceResolverTest.java** (12 tests):
  - resolverAccionPendiente: error paths (3), Cassius, DISCARD_RECOVERY, SUPER_POTION
  - DISCARD_OPPONENT_ENERGY, SWITCH_ACTIVE, SEARCH_DECK→HAND, CHOOSE_OPPONENT_BENCH, HEAL_OWN_POKEMON

- [x] **NEW service/BattleEngineServiceEvolveSetupTest.java** (11 tests):
  - evolucionarPokemon (4), ejecutarSetupBot (4), getTableroDeJugador/Oponente (3)

- [x] **NEW service/BattleEngineServiceResolverExtendedTest.java** (10 tests):
  - DISCARD_TO_TOP_DECK, REORDER_TOP_DECK, MOVE_ENERGY_TO_OPPONENT_BENCH (2)
  - CHOOSE_OPPONENT_BENCH_TO_DAMAGE, DISCARD_POTION_ENERGY
  - SEARCH_DECK→ATTACH_ACTIVE, SEARCH_DECK→ATTACH_ACTIVE_AND_SWITCH
  - SEARCH_DECK sin selección, endsTurn flag

- [x] **NEW service/BattleEngineServiceBotTurnTest.java** (12 tests):
  - ejecutarTurnoBot: match inexistente, cambio turno, robo carta, Paralyzed cleanup
  - invulnerable reset, partida terminada en turno bot, noPuedeAtacar ciclos
  - jugarTrainer Fairy Garden (xy1-117) y Shadow Circle (xy1-126)
  - aplicarMantenimiento Burned y Asleep

- [x] **NEW service/BattleEngineServiceEvoSodaChainTest.java** (4 tests):
  - SELECT_POKEMON_EVOSODA → SEARCH_EVOLUTION chain
  - SEARCH_EVOLUTION evoluciona, SEARCH_EVOLUTION sin selección cancela
  - ATTACH_TOOL resolución real

- [x] **NEW model/ModelTest.java** (16 tests):
  - Jugador (4), Mazo (1), Card (2), CartaEnJuego (4), TableroJugador (2), Partida (3)

- [x] **NEW service/battle/strategy/EstrategiaBasicaTest.java** (10 tests):
  - ejecutarSetup (5), ejecutarTurno (4), constructor (1)

- [x] **NEW service/battle/command/ComandosTurnoTest.java** (22 tests):
  - ComandoJugarPokemon (5), ComandoUnirEnergia (6), ComandoEvolucionar (5), ComandoRetirarse (6)

- [x] **NEW service/battle/chain/ChainHandlersTest.java** — handlers de efectos (~20 tests)
- [x] **NEW service/battle/EnergyCostCalculatorTest.java** (~10 tests)
- [x] **NEW service/battle/command/ComandoUnirEnergiaTest.java** (~8 tests)

- [x] **NEW model/battle/command/BattleCommandsMissingTest.java** (16 tests):
  - MoveDiscardCardToTopDeckCommand: bot auto-selecciona, jugador→pending, descarte vacío, Target.OPPONENT
  - SelectOwnPokemonToHealCommand: bot cura más dañado, cap en maxHP, sin pokemon, jugador→pending, benchedOnly
  - AttachEnergyFromDiscardToBenchByCoinsCommand: sin banca, sin energía tipo, con energía, null energyType
  - RhydonMadMountainCommand: 2 monedas siempre, sin activo, con damage counters

- [x] **NEW service/battle/strategy/EstrategiaDificilTest.java** (5 tests):
  - ejecutarSetup delega a EstrategiaBasica (con mano, sin mano, con activo y banca)
  - ejecutarTurno lanza UnsupportedOperationException (2 variantes)

- [x] **NEW dto/DtoTest.java** (8 tests):
  - LobbyMessage: setters/getters completos, text/emote, challenge fields, default constructor
  - SantoroTrackingRequest: setters/getters, default false
  - DebugSetSobresRequest: setters/getters, default cero

- [x] **Sesión 5 (2026-06-27)**:
  - **MODIFY [ComandosTurnoTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/command/ComandosTurnoTest.java)**: Agregadas 12 pruebas completas para `ComandoRetirarse`, `ComandoUnirEnergia` y `ComandoEvolucionar`, subiendo su cobertura a >97%.
  - **MODIFY [BattleAttackServiceTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/BattleAttackServiceTest.java)**: Agregadas 5 pruebas de cálculo de daño (Weakness, Resistance, Muscle Band, Hard Charm, Shadow Circle), subiendo la cobertura de `BattleAttackService` a **83.95%** y `EnergyCostCalculator` a **92.53%**.
  - **MODIFY [BattleKoServiceTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/BattleKoServiceTest.java)**: Agregadas 6 pruebas de condiciones de victoria y casos límite de K.O., subiendo `BattleKoService` a **82.50%**.
  - **MODIFY [LobbyRoomServiceTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/LobbyRoomServiceTest.java)**: Agregados 8 tests exhaustivos de cobertura para `LobbyRoomService`, subiendo su cobertura del **56.54%** al **79.16%**.
  - **MODIFY [AttackEffectParserServiceTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/command/AttackEffectParserServiceTest.java)**: Agregadas múltiples pruebas de parsing de efectos con expresiones regulares, incrementando significativamente la cobertura de este componente.
  - **NEW [SearchDeckCommandTest.java](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/model/battle/command/SearchDeckCommandTest.java)**: Cobertura añadida para las funciones aisladas de `SearchDeckCommand`.

---

## Cambios Pendientes

### ⚠️ COBERTURA GAPS RESTANTES

- [ ] **config/** package (WebSocket handlers) — 0% cobertura, ~305 instrucciones missed
  - Muy difícil sin Spring context completo; considerar excluir en JaCoCo si no es posible

- [x] **BattleEngineService.ejecutarTurnoBot()** — cubierto en BattleEngineServiceBotTurnTest.java
- [x] **resolverAccionPendiente** — todos los branches principales cubiertos
- [x] **aplicarMantenimientoEntreTurnos** — Burned y Asleep cubiertos vía ejecutarTurnoBot
- [x] **jugarTrainer** — Fairy Garden (xy1-117) y Shadow Circle (xy1-126) cubiertos
- [x] **MoveDiscardCardToTopDeckCommand** — cubierto en BattleCommandsMissingTest (4 tests)
- [x] **SelectOwnPokemonToHealCommand** — cubierto en BattleCommandsMissingTest (5 tests)
- [x] **AttachEnergyFromDiscardToBenchByCoinsCommand** — cubierto en BattleCommandsMissingTest (4 tests)
- [x] **RhydonMadMountainCommand** — cubierto en BattleCommandsMissingTest (3 tests)
- [x] **EstrategiaDificil** — cubierto en EstrategiaDificilTest (5 tests)
- [x] **DTOs** — LobbyMessage, SantoroTrackingRequest, DebugSetSobresRequest cubiertos en DtoTest
- [x] **Models** — Jugador, Card, Mazo, CartaEnJuego, TableroJugador, Partida cubiertos en ModelTest

- [x] **CardCatalogServiceTest** — Resuelto y configurado usando H2 in-memory
 
- [ ] **startBattle / startBattleOnline** — no testeados (requieren repos mockeados con mazo real)

### ⚠️ CONOCIDO: Todos los tests pasan exitosamente
- Se solucionó la carga del contexto de aplicación en `CardCatalogServiceTest` configurando H2 directamente en la anotación `@SpringBootTest(properties = {...})`.

**Total actual: 809 tests, 0 failures, 0 errores**

---

## Próximos Pasos Recomendados

1. **Aumentar la cobertura sobre `BattleEngineService`:**
   - Incrementar las pruebas sobre los flujos de setup, mulligans y resolución de efectos interactivos.
2. **Implementar caso de uso de Partida Completa**:
   - Crear un test de integración en el backend que inicialice una partida real entre dos jugadores y simule las transiciones de turnos y jugadas básicas.
3. **Configurar e implementar tests E2E en Frontend**:
   - Configurar Cypress/Playwright en `FE/` para simular la experiencia completa de juego del usuario.

### Comandos útiles
```bash
# Generar reporte JaCoCo
mvn clean verify -Dmaven.test.failure.ignore=true

# Ver cobertura en browser
start target\site\jacoco\index.html

# Correr solo tests nuevos rápidamente
mvn test -Dtest="BattleEngineService*"
```

---

## Decisiones Técnicas Tomadas

- **No usar Spring context** en ningún test nuevo — todos son unit tests puros con `new Service(mock(...))`
- **cardBasico()** helper: usar `setSupertype("Pokemon")` + `setSubtypes(List.of("Basic"))`, NO el campo `tipo`
- **Trainer cards**: la carta siempre va al descarte post-uso (contar +1 en assertions de `pilaDescarte`)
- **ComandoEvolucionar**: requiere `tablero.setTurnosJugados(2)` y `activo.setTurnoEntrada(turnoAnterior)`
- **actualizarLoading SP**: pasar `null` como username activa el branch de single-player auto-complete

---

**Última actualización**: 2026-06-27 — sesión 5 (post-commit)
