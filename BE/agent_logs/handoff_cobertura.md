# 🧪 Plan de Continuidad: Cobertura de Tests (RNF-03)

> **Para:** Compañero que continúa el trabajo de cobertura  
> **Fecha:** 2026-06-27  
> **Rama:** `test/coverage-improvement` — NO mergeada a main todavía  
> **Último commit:** `333a7b9` (EstrategiaBasicaMoreTest + ModelBattleMoreTest)

---

## 📊 Estado Actual de Cobertura (medición pre-commit 333a7b9)

| Métrica | Requerido (RNF-03) | **Estado Actual** | Estado |
|---------|-------------------|-------------------|--------|
| **Cobertura Global (instrucciones)** | ≥ 80% | **~81%** | ✅ Superado |
| **Cobertura Lógica (LINE por paquete)** | ≥ 90% | Varios paquetes debajo | ⚠️ En progreso |
| **Tests totales** | — | **~1104 tests** | — |

### Cobertura por paquete (última medición JaCoCo)

| Paquete | Line% | Estado |
|---------|-------|--------|
| `com.pokemon.tcg.service.battle.command` | 95%+ | ✅ |
| `com.pokemon.tcg.service.battle.chain` | 91%+ | ✅ |
| `com.pokemon.tcg.model.battle` | ~92% | ✅ (mejorado) |
| `com.pokemon.tcg.service.battle.strategy` | ~87%+ | ⚠️ Cerca |
| `com.pokemon.tcg.service` | ~82.4% | ❌ Falta |
| `com.pokemon.tcg.model` | ~89.1% | ⚠️ Cerca |
| `com.pokemon.tcg.controller` | ~66% | ❌ |
| `com.pokemon.tcg.config` | ~43.3% | ❌ |
| `com.pokemon.tcg.model.lobby` | ~75.9% | ❌ |
| `com.pokemon.tcg.exception` | ~66.7% | ❌ |

> **Nota:** `controller`, `config`, `exception` pueden estar excluidos del check JaCoCo — verificar `pom.xml` sección `<excludes>`.

---

## 📁 Tests Creados en Esta Sesión (rama test/coverage-improvement)

### Commits en esta rama (más recientes primero)

| Commit | Archivo | Tests | Descripción |
|--------|---------|-------|-------------|
| `333a7b9` | `ModelBattleMoreTest.java` | 10 | Getters/setters de model.battle: AttackTranslation, PendingBattleAction.Option, Partida fields |
| `333a7b9` | `EstrategiaBasicaMoreTest.java` | 14 | Ramas de EstrategiaBasica: CantRetreat, heal bonus, attack bloqueado, super efectivo, resistido |
| `e3a03f2` | `AttackEffectParserServiceMoreTest.java` | 66 | Todos los branches faltantes de AttackEffectParser |
| `e3a03f2` | `BattleEngineServiceStatusTest.java` | 34 | Status conditions (Burned, Asleep), noPuedeAtacar, ataqueBloqueado, etc. |
| `e3a03f2` | `BattleEngineServiceResolverBranchTest.java` | 26 | resolverAccionPendiente: CASSIUS, ATTACH_TOOL, DISCARD_RECOVERY, SELECT_POKEMON_SUPER_POTION, etc. |
| `e3a03f2` | `BattleKoServiceMoreTest.java` | 10 | KO branches: botGana por premios, auto-reemplazo, puntaje estratégico |
| `e3a03f2` | `CardCatalogServiceMoreTest.java` | 10 | getCatalogo variants: trainer/energy cards, lang handling, energy type inference |

---

## 🔴 Trabajo Pendiente para Llegar al 90% por Paquete

### 1. `com.pokemon.tcg.service` (82.4% → necesita 90%)
El cuello de botella es **`BattleEngineService`** (~2100+ instrucciones sin cubrir).

**Zonas clave sin testear:**
- `iniciarBatallaDesdeSetup()` — flujo completo de inicio
- `procesarTurnoBotConDelay()` — turnos del bot con delay
- `pasarTurno()` — transición de turnos
- `resolverAccionPendiente()` — hay más branches que no se cubrieron

**Patrón correcto (per CLAUDE.md):**
```java
BattleEngineService service = new BattleEngineService(
    mock(JugadorRepository.class),
    mock(MazoRepository.class),
    mock(CardRepository.class),
    mock(BotAIService.class),
    mock(BattleAttackService.class),
    mock(BattleKoService.class)
);
// Para registrar una partida:
service.partidasEnCurso.put(partida.getId(), partida); // campo package-private
```

### 2. `com.pokemon.tcg.service.battle.strategy` (~87% → necesita 90%)
Quedan ~9 líneas más por cubrir en `EstrategiaBasica`. Zonas candidatas:
- `ejecutarRetirada` con costo > 0 (descarta energías del activo)
- `gestionarEnergiaBot` (método no llamado desde ejecutarTurno — es dead code)
- `contarEnergiasFaltantes` con energía "rainbow"
- `puedePagarCosto` cuando no hay suficientes energías misceláneas

### 3. `com.pokemon.tcg.model` (~89.1% → necesita 90%)
Faltan ~3 líneas. Revisar getters/setters no cubiertos en `Card`, `Jugador`, etc.

---

## 🎯 Próximo Paso Recomendado

1. Correr `mvn verify` para ver el estado EXACTO de JaCoCo por paquete:
   ```
   mvn verify 2>&1 | Select-String "LINE|PACKAGE|Failed"
   ```
2. Identificar qué paquetes fallan el check de 90%
3. Si solo fallan `service` y `strategy`: crear más tests para `BattleEngineService` (el más impactante)
4. Si `model` falla también: agregar tests de getters en `Card.java` o `Jugador.java`

---

## 🛠️ Comandos de Trabajo

```powershell
# Full test suite con cobertura (verifica reglas JaCoCo)
cd BE
mvn verify

# Solo tests sin verificación de cobertura
mvn test

# Solo un test específico
mvn test -Dtest="EstrategiaBasicaMoreTest"

# Ver reporte JaCoCo: BE/target/site/jacoco/index.html
```

---

## 🔑 Patrones de Objetos de Test Útiles

```java
// Card básica (CLAUDE.md — no usar campo tipo)
private Card cardBasico(String id, String nombre) {
    Card c = new Card();
    c.setId(id); c.setNombre(nombre); c.setHp("60");
    c.setSupertype("Pokemon"); c.setSubtypes(List.of("Basic"));
    return c;
}

// Energía básica
private Card cardEnergia(String nombre, String tipo) {
    Card c = new Card();
    c.setId("e-" + nombre); c.setNombre(nombre);
    c.setSupertype("Energy"); c.setTipo(tipo); c.setHp("0");
    return c;
}

// Ataque con costo
private Ataque crearAtaque(String nombre, int danio, String... costos) {
    Ataque a = new Ataque();
    a.setNombre(nombre); a.setDanio(danio);
    a.setTiposEnergia(List.of(costos));
    return a;
}
```

---

## ✅ Checklist Final para Merge a Main

- [ ] `mvn test` — todos los tests pasan (actualmente ~1104)
- [ ] `mvn verify` — no hay paquetes que fallen el check de 90% de línea
- [ ] Cobertura global ≥ 80% confirmada en JaCoCo
- [ ] Merge `test/coverage-improvement` → `main`
- [ ] Push a remote
