# 🧪 Plan de Continuidad: Cobertura de Tests (RNF-03)

> **Para:** Compañero que continúa el trabajo de cobertura  
> **Fecha:** 2026-06-27  
> **Rama:** `main` — todo está pusheado y actualizado  
> **Commit de referencia:** `9d171d1` (Merge) / `2f36342` (nuestro commit principal)

---

## 📊 Estado Actual de Cobertura

| Métrica | Requerido (RNF-03) | **Estado Actual** | Falta |
|---------|-------------------|-------------------|-------|
| **Cobertura Global** | ≥ 80% | **79.8%** | ~0.2% |
| **Cobertura Lógica** | ≥ 90% | **82.94%** | ~7.06% |
| **Tests totales** | — | **934 tests ✅** | — |

> [!IMPORTANT]
> El **global está a 0.2%** del mínimo — con muy pocos tests más se supera.
> La **lógica está a 7% del 90%** — requiere trabajo en `BattleEngineService`.

---

## 📁 Tests Creados en Esta Sesión (ya en main)

### Tests Nuevos (archivos creados desde cero)

| Archivo | Tests | Clase que cubre |
|---------|-------|-----------------|
| [BattleEngineServiceResolverMoreTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/BattleEngineServiceResolverMoreTest.java) | 9 | `resolverAccionPendiente` (HEAL, CHOOSE_BENCH, DISCARD, REORDER_DECK...) |
| [LobbyRoomServiceCoverageTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/LobbyRoomServiceCoverageTest.java) | 10 | `LobbyRoomService` (syncRoomStatus, updateSettings) |
| [AttackEffectParserServiceTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/command/AttackEffectParserServiceTest.java) | ~37 | `AttackEffectParserService.parseEffects()` |
| [ComandoUsarHabilidadTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/command/ComandoUsarHabilidadTest.java) | varios | `ComandoUsarHabilidad` |
| [GeomancyCommandTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/model/battle/command/GeomancyCommandTest.java) | varios | `GeomancyCommand` |
| [PickupCommandTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/model/battle/command/PickupCommandTest.java) | varios | `PickupCommand` |
| [SearchDeckCommandTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/model/battle/command/SearchDeckCommandTest.java) | varios | `SearchDeckCommand` |

### Tests Extendidos (archivos ya existentes que agrandamos)

| Archivo | Qué se agregó |
|---------|---------------|
| [BattleAttackServiceTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/BattleAttackServiceTest.java) | Tests de habilidades: SpikyShield, DestinyBurst, ShadowCircle, HardCharm+FurCoat, ataquePotenciado, reduccionDanio, preventThreshold |
| [EstrategiaBasicaTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/battle/strategy/EstrategiaBasicaTest.java) | 17 tests de IA del bot: setup, bench, retirada táctica, Fairy Garden, normalizarTipo, esPokemonBasico |
| [BattleEngineServiceTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/BattleEngineServiceTest.java) | Tests adicionales de flujo de turnos |
| [CardCatalogServiceTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/service/CardCatalogServiceTest.java) | Tests adicionales |
| [DataLoaderTest.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/test/java/com/pokemon/tcg/config/DataLoaderTest.java) | Fix de alineación con nueva lógica del DataLoader |

---

## 🔴 Clases Prioritarias — Donde Faltan Más Instrucciones

Ejecutá `BE/check_cov.ps1` para ver el estado en tiempo real. Las clases con más instrucciones sin cubrir son:

| Clase | Instrucciones perdidas | Impacto al cubrir |
|-------|----------------------|-------------------|
| **`BattleEngineService`** | **2113** | 🔥 Altísimo |
| `AttackEffectParserService` | 308 | 🔥 Alto |
| `EstrategiaBasica` | 306 | 🔥 Alto |
| `LobbyRoomService` | 235 | Medio |
| `CardCatalogService` | 223 | Medio |
| `BattleKoService` | 137 | Medio |
| `PasswordRecoveryService` | 132 | Medio |
| `ComandoUsarHabilidad` | 117 | Bajo-Medio |
| `AddDamageByDifferentBasicEnergyTypesCommand` | 92 | Bajo |
| `ConditionalDamageMultiplierCommand` | 88 | Bajo |

---

## 🎯 Plan de Acción para Continuar

### Paso 1 — Superar el 80% global (¡Casi estamos!)

Solo necesitamos cubrir ~160 instrucciones más en **cualquier clase**. Las más fáciles:

**Opción A — `CardCatalogService` (223 instrucciones perdidas)**

Ver el archivo: [CardCatalogService.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/CardCatalogService.java)

Cosas que faltan testear:
- Filtros de búsqueda: `filtrarPorTipo()`, `filtrarPorSupertipo()`, búsqueda por nombre
- Casos edge: catálogo vacío, carta inexistente

**Opción B — `PasswordRecoveryService` (132 instrucciones perdidas)**

Ver: [PasswordRecoveryService.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/PasswordRecoveryService.java)

Cosas que faltan:
- `validateResetToken()` con token vencido
- `resetPassword()` con token inválido
- Flujo completo de email de recuperación

---

### Paso 2 — Subir la Lógica al 90% (Trabajo mayor)

El grueso del trabajo está en **`BattleEngineService`** (2113 instrucciones sin cubrir).

Ver: [BattleEngineService.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/BattleEngineService.java) (2176 líneas)

**Zonas clave sin testear:**

1. **`iniciarBatallaDesdeSetup()`** (líneas ~60–130) — Flujo completo de inicio de partida
2. **`procesarTurnoBotConDelay()`** (líneas ~400–500) — Turnos del bot con lógica de delay
3. **`pasarTurno()`** (líneas ~560–610) — Transición de turnos completa
4. **`resolverAccionPendiente()`** branches restantes — Ya cubrimos 9, faltan ramas menos comunes

**Patrón para crear tests de `BattleEngineService`:**

```java
// setUp básico que funciona (ya verificado)
@BeforeEach
void setUp() {
    service = new BattleEngineService(
        mock(JugadorRepository.class),
        mock(MazoRepository.class),
        mock(CardRepository.class),
        mock(BotAIService.class),
        mock(BattleAttackService.class),
        mock(BattleKoService.class)
    );
}

// Para tests que necesitan una partida en curso:
TableroJugador tJugador = new TableroJugador();
TableroJugador tBot = new TableroJugador();
Partida partida = new Partida(tJugador, tBot);
partida.setJugadorUsername("ash");
partida.setTurnoActual(Partida.Turno.JUGADOR);
service.partidasEnCurso.put(partida.getId(), partida); // Campo package-private
```

---

### Paso 3 — `AttackEffectParserService` (308 instrucciones perdidas)

Ver: [AttackEffectParserService.java](file:///c:/Users/nahue/OneDrive/Escritorio/tpi-pokemon-2w2-09/BE/src/main/java/com/pokemon/tcg/service/battle/command/AttackEffectParserService.java)

El test existente ya cubre muchos efectos. Los que aún faltan en el `parseEffects()`:
- `HEAL_SELF_DISCARD_ENERGY` 
- `DISCARD_ALL_OWN_ENERGY`
- `SWITCH_OWN_ACTIVE`
- `DRAW_CARDS_COIN`
- `REORDER_OWN_DECK`

---

## 🛠️ Comandos de Trabajo

```powershell
# Correr TODOS los tests con reporte de cobertura
cd BE
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\plugins\maven\lib\maven3\bin\mvn.cmd" test

# Ver el resumen de cobertura rápido
powershell -ExecutionPolicy Bypass -File check_cov.ps1

# Correr solo un test específico
& "...\mvn.cmd" -Dtest="NombreDelTest" test

# Ver el reporte HTML completo de JaCoCo
# Abrir: BE/target/site/jacoco/index.html en el browser
```

---

## 🔑 Información Clave del Proyecto

### Estructura de Tests

```
BE/src/test/java/com/pokemon/tcg/
├── config/
│   └── DataLoaderTest.java
├── model/battle/command/
│   ├── GeomancyCommandTest.java
│   ├── PickupCommandTest.java
│   └── SearchDeckCommandTest.java
├── service/
│   ├── BattleAttackServiceTest.java          ← 53 tests
│   ├── BattleEngineService*Test.java (x8)    ← ~160 tests en total
│   ├── BattleEngineServiceResolverMoreTest    ← 9 tests (nuevos)
│   ├── HabilidadesIntegrityTest.java         ← 11 tests de integridad
│   ├── LobbyRoomServiceCoverageTest.java     ← 10 tests (nuevos)
│   └── battle/command/
│       ├── AttackEffectParserServiceTest.java ← ~37 tests
│       └── ComandoUsarHabilidadTest.java
└── ... (otros servicios)
```

### Patrones de Objetos de Test Útiles

```java
// Card básica
private Card card(String id, String nombre, String hp) {
    Card c = new Card();
    c.setId(id); c.setNombre(nombre); c.setHp(hp);
    c.setSupertype("Pokemon"); c.setSubtypes(List.of("Basic"));
    return c;
}

// PendingBattleAction (para resolverAccionPendiente)
PendingBattleAction p = new PendingBattleAction();
p.setActor("ash"); p.setType("HEAL_OWN_POKEMON");
p.setAmount(30); p.setMinSelections(1); p.setMaxSelections(3);
p.setOptions(List.of(new PendingBattleAction.Option("p1", "Pikachu", null)));
partida.setPendingAction(p);

// CartaEnJuego como activo
CartaEnJuego cej = new CartaEnJuego(card("p1", "Pikachu", "60"));
cej.setHpActual(60);
partida.getJugador().setActivo(cej);
```

### Constructores Verificados

```java
// ✅ Funciona
new TableroJugador()                                        // sin args
new Partida(tJugador, tBot)                                 // con dos tableros
new PendingBattleAction.Option("id", "nombre", null)        // 3 args
new CartaEnJuego(card)                                      // con Card

// ❌ NO funciona
new TableroJugador(new Jugador("ash"))                      // no existe ese constructor
new PendingBattleAction.Option("id", "nombre")              // faltan args
```

---

## ✅ Checklist de Entrega para el Compañero

- [ ] `git pull origin main` — traer los últimos cambios
- [ ] `mvn test` — verificar que los 934 tests pasan
- [ ] `./check_cov.ps1` — ver cobertura actual
- [ ] Elegir clase prioritaria de la tabla y crear tests
- [ ] Verificar que los nuevos tests pasan con `mvn test`
- [ ] `git add` + `git commit -m "test(be): ..."` + `git push origin main`
- [ ] Actualizar `BE/agent_logs/reporte-cobertura-jacoco.md` con los nuevos números
