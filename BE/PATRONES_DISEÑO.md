# Patrones de Diseño — Backend

Este documento describe los 4 patrones de diseño implementados en el backend de PokemonTCG, por qué se eligieron, qué beneficios aportan, cómo funcionan y dónde encontrarlos en el código.

---

## Resumen Ejecutivo

| Patrón | Propósito | Ubicación | Estado |
|--------|-----------|-----------|--------|
| **Chain of Responsibility** | Efectos de ataque modulares | `service/battle/chain/` | ✅ Activo |
| **Strategy** | Comportamiento del bot intercambiable | `service/battle/strategy/` | ✅ Activo |
| **Command** | Acciones de turno encapsuladas | `service/battle/command/` | ✅ Activo |
| **State** | Flujo de fases centralizado | `model/battle/state/` | ✅ Activo |

---

## 1. Chain of Responsibility

### Ubicación
- **Archivos**: `BE/src/main/java/com/pokemon/tcg/service/battle/chain/`
- **Clase base**: `ManejadorEfecto.java`
- **Factory**: `CadenaAtaqueFactory.java`
- **Contexto viajero**: `ContextoAtaque.java`
- **Handlers**: 18 clases (pre-daño y post-daño)

### Qué Hace
Cada efecto de ataque (veneno, quemadura, daño por moneda, curación, etc.) es un **eslabón independiente** de una cadena. El ataque pasa por la cadena, y cada handler decide si aplica su efecto o simplemente lo deja pasar.

**Ejemplo de flujo:**
```
ComandoAtacar → BattleAttackService.resolveAttack()
  → CadenaAtaqueFactory.buildCadenaPreDanio()  [6 handlers]
    → EfectoMonedaFalla (¿sale cruz?)
    → EfectoMultiMoneda (¿multiplicar daño?)
    → EfectoEscalaPorEnergias (¿sumar daño por energías?)
    → ... [resto de handlers]
  → Aplicar daño al defensor
  → CadenaAtaqueFactory.buildCadenaEfectosSecundarios()  [12 handlers]
    → EfectoVeneno (¿aplicar?)
    → EfectoCuracion (¿curar?)
    → EfectoDanioBanca (¿daño colateral?)
    → ... [resto de handlers]
```

### Por Qué lo Usamos

**Antes**: `BattleAttackService` tenía dos métodos enormes:
- `calcularDanioPorEfectos()` — ~90 líneas de if-else anidados
- `aplicarEfectosSecundarios()` — ~160 líneas de if-else anidados

Agregar un nuevo efecto obligaba a:
1. Entender toda la lógica de ambos métodos
2. Tocar código frágil (alto riesgo de romper efectos existentes)
3. Testear manualmente que los 17 efectos previos seguían funcionando

**Beneficios con Chain of Responsibility:**
- ✅ **Aislamiento**: cada efecto vive en su propia clase
- ✅ **Extensibilidad**: agregar un efecto es crear una clase nueva, sin tocar nada existente
- ✅ **Testabilidad**: test unitario por handler
- ✅ **Claridad**: el nombre del handler dice exactamente qué hace
- ✅ **Bajo acoplamiento**: los handlers no conocen entre sí

### Handlers Implementados

**Pre-daño** (modifican `ContextoAtaque.danioFinal`):
| Handler | Efecto en juego |
|---------|-----------------|
| `EfectoMonedaFalla` | "Tails, this attack does nothing" |
| `EfectoMultiMoneda` | "Times the number of heads" |
| `EfectoMonedaExtraDanio` | "If heads, more damage" |
| `EfectoContadoresDanio` | "Damage counters on this Pokémon" |
| `EfectoEscalaPorEnergias` | "For each energy attached" |
| `EfectoInmunidad` | "Prevent all effects of attacks" |

**Post-daño** (aplican efectos al defensor):
| Handler | Efecto en juego |
|---------|-----------------|
| `EfectoCuracion` | "Heal X from this Pokémon" |
| `EfectoDanioBanca` | "Damage to 1 of opponent's benched" |
| `EfectoRobarCartas` | "Draw a card" |
| `EfectoDanioPropio` | "This Pokémon does X damage to itself" |
| `EfectoDescartarEnergiaPropia` | "Discard an energy" |
| `EfectoDescartarEnergiaRival` | "Discard an energy attached to opponent's" |
| `EfectoParalisis` | "Is now Paralyzed" |
| `EfectoAtrapar` | "Can't retreat" (CantRetreat) |
| `EfectoVeneno` | "Is now Poisoned" |
| `EfectoSueno` | "Is now Asleep" |
| `EfectoQuemadura` | "Is now Burned" |
| `EfectoConfusion` | "Is now Confused" |

---

## 2. Strategy

### Ubicación
- **Archivos**: `BE/src/main/java/com/pokemon/tcg/service/battle/strategy/`
- **Interfaz**: `EstrategiaBot.java`
- **Implementaciones**: `EstrategiaBasica.java`, `EstrategiaDificil.java` (placeholder)
- **Cliente**: `BotAIService.java` (coordinador)

### Qué Hace
El bot necesita tomar decisiones en su turno: qué Pokémon jugar, a dónde poner energía, a quién atacar. Con **Strategy**, cada forma de jugar es una clase intercambiable.

**Interfaz:**
```java
public interface EstrategiaBot {
    void ejecutarTurno(Partida partida);
}
```

**Cliente:**
```java
@Service
public class BotAIService {
    private final EstrategiaBot estrategia = new EstrategiaBasica();
    
    public void ejecutarTurno(Partida partida) {
        estrategia.ejecutarTurno(partida);  // Delega la decisión
    }
}
```

### Por Qué lo Usamos

**Antes**: `BotAIService` tenía ~250 líneas de lógica hardcodeada. Agregar una "dificultad media" obligaba a:
1. Copiar todo el método y modificarlo
2. Mantener dos versiones sincronizadas (pesadilla)
3. Duplicación de lógica (ej: `calcularDanioFinal()` existía en dos lugares)

**Beneficios con Strategy:**
- ✅ **Polimorfismo**: cambiá de estrategia en tiempo de ejecución sin tocar código existente
- ✅ **Escalabilidad**: agregar dificultad "Legendaria" es crear `EstrategiaLegendaria extends EstrategiaBot`
- ✅ **Testabilidad**: test independiente por estrategia
- ✅ **Separación de responsabilidades**: `BotAIService` solo coordina, no decide

### Implementaciones

**`EstrategiaBasica`**
- Contiene toda la lógica del bot actual (sin cambios)
- Métodos: `evaluarRetiradaEstrategica()`, `calcularAmenazaMaxima()`, `gestionarCartasEnMano()`, `gestionarEnergiaBot()`, etc.
- Usa helpers estáticos heredados: `puedePagarCosto()`, `normalizarTipo()`, `esPokemonBasico()`

**`EstrategiaDificil`**
- Placeholder para futuro — lanza `UnsupportedOperationException`
- Cuando se implemente, puede:
  - Evaluar riesgos antes de retirarse
  - Usar math.max para maximizar daño esperado vs tu vida
  - Cambiar el árbol de decisión

---

## 3. Command

### Ubicación
- **Archivos**: `BE/src/main/java/com/pokemon/tcg/service/battle/command/`
- **Interfaz**: `ComandoTurno.java`
- **Implementaciones** (6):
  - `ComandoAtacar.java`
  - `ComandoJugarPokemon.java`
  - `ComandoUnirEnergia.java`
  - `ComandoRetirarse.java`
  - `ComandoSubirActivo.java`
  - `ComandoEvolucionar.java`
- **Dispatcher**: `BattleEngineService.ejecutarComando()`

### Qué Hace
Cada acción del turno (atacar, jugar Pokémon, unir energía) es un **objeto** que encapsula:
1. Las validaciones previas (`puedeEjecutar()`)
2. La ejecución (`ejecutar()`)
3. Metadata (`getNombre()`)

**Interfaz:**
```java
public interface ComandoTurno {
    boolean puedeEjecutar(Partida partida);
    void ejecutar(Partida partida);
    String getNombre();
}
```

**Ejemplo: ComandoAtacar**
```java
ComandoAtacar cmd = new ComandoAtacar(
    "Line Force",
    tableroJugador,
    tableroBot,
    battleAttackService,
    battleKoService
);

if (cmd.puedeEjecutar(partida)) {
    cmd.ejecutar(partida);  // Validación + ejecución atómica
}
```

### Por Qué lo Usamos

**Antes**: `BattleEngineService` tenía 6 métodos públicos dispersos:
- `jugarPokemon()`, `unirEnergia()`, `realizarRetirada()`, `subirAActivoDesdeBanca()`, `realizarAtaque()`, `evolucionarPokemon()`
- Cada uno hacía `validar → buscar cartas → estado específico → ejecutar`
- Lógica duplicada para validar fase, turno, etc.
- Difícil agregar logging, historial o deshacer

**Beneficios con Command:**
- ✅ **Encapsulación**: la validación y ejecución viven juntas (no se rompen entre sí)
- ✅ **Reutilización**: el mismo `ComandoAtacar` se usa desde el controller, el bot, tests
- ✅ **Logging/Auditoría**: fácil agregar `@Log` en `ejecutarComando()` para historial
- ✅ **Deshacer/Rehacer**: estructura lista para implementar rollback (futura mejora)
- ✅ **Testing**: test unitario por comando, mock de dependencias (attackService, koService)

### Verificación en `ejecutarComando()`

```java
private void ejecutarComando(Partida partida, ComandoTurno comando) {
    // 1. Verificar que la partida está en una fase que permite acciones
    if (!partida.getEstado().permiteAccionesDeJuego()) {
        throw new IllegalStateException("Acción no permitida en fase ...");
    }
    // 2. Preguntar al comando si puede ejecutarse
    if (!comando.puedeEjecutar(partida)) {
        throw new IllegalStateException("Acción no permitida en el estado actual: ...");
    }
    // 3. Ejecutar
    comando.ejecutar(partida);
}
```

---

## 4. State

### Ubicación
- **Archivos**: `BE/src/main/java/com/pokemon/tcg/model/battle/state/`
- **Interfaz**: `EstadoPartida.java`
- **Implementaciones** (4):
  - `EstadoInicio.java`
  - `EstadoLanzamientoMoneda.java`
  - `EstadoTurnoNormal.java`
  - `EstadoFinPartida.java`
- **Cliente**: `Partida.java` (contiene `private transient EstadoPartida estado`)

### Qué Hace
Cada fase de la partida (INICIO, LANZAMIENTO_MONEDA, TURNO_NORMAL, FIN_PARTIDA) es un **objeto de estado** que:
1. Conoce qué acciones se pueden ejecutar
2. Sincroniza el enum `Partida.Fase` (para el FE)
3. Permite centralizar la lógica de transición

**Interfaz:**
```java
public interface EstadoPartida {
    boolean permiteAccionesDeJuego();  // TURNO_NORMAL = true, resto = false
    Partida.Fase getFase();            // Mapeo a enum
}
```

**Transición:**
```java
// En lugar de partida.setFaseActual(Fase.FIN_PARTIDA)
partida.transicionarA(new EstadoFinPartida());
// Ambas acciones (setter del estado + del enum) ocurren de forma atómica
```

### Por Qué lo Usamos

**Antes**: `Partida` tenía un enum `Fase` pero no había enforcement. Cualquier servicio podía setear cualquier fase en cualquier momento. Los checks estaban dispersos:
- `ComandoAtacar.puedeEjecutar()` chequeaba `getFaseActual() == TURNO_NORMAL`
- `BattleEngineService` chequeaba manualmente si era `FIN_PARTIDA`
- Los checks estaban **fuera del lugar** (no en el estado mismo)

**Beneficios con State:**
- ✅ **Centralización**: la lógica de qué puedo hacer vive en el estado, no en 6 comandos
- ✅ **Single Responsibility**: `EstadoTurnoNormal` solo sabe sobre TURNO_NORMAL
- ✅ **Sincronización**: `transicionarA()` garantiza que `estado` y `faseActual` siempre están sincronizados
- ✅ **No Breaking Change**: el enum `Partida.Fase` sigue siendo serializado al FE (`faseActual` is public)
- ✅ **Preparado para expansión**: si queremos sub-estados (ej: "TURNO_NORMAL_ESPERANDO_ENERGÍA") es fácil

### Relación con Command

State y Command trabajan juntos:

```
BattleEngineService.ejecutarComando(partida, cmd)
  ├─ if (!partida.getEstado().permiteAccionesDeJuego()) ← State gate
  ├─ if (!cmd.puedeEjecutar(partida)) ← Command-specific check
  └─ cmd.ejecutar(partida)
```

---

## Mapa Visual

```
Partida (batalla)
  ├─ Estado [State Pattern]
  │   └─ EstadoTurnoNormal.permiteAccionesDeJuego() = true
  │
  └─ jugarPokemon(cartaId)
      └─ ComandoJugarPokemon [Command Pattern]
          ├─ puedeEjecutar() → check específico
          └─ ejecutar() → BattleEngineService.ejecutarComando()
                          → valida estado + comando
                          → ejecuta

AtaqueEnProceso
  └─ BattleAttackService.resolveAttack() [Chain of Responsibility]
      ├─ buildCadenaPreDanio() [6 handlers]
      └─ buildCadenaEfectosSecundarios() [12 handlers]

BotTurno
  └─ BotAIService.ejecutarTurno()
      └─ EstrategiaBot [Strategy Pattern]
          └─ EstrategiaBasica.ejecutarTurno()
              └─ Toma decisiones, ejecuta comandos
```

---

## Resumen de Beneficios

| Aspecto | Beneficio |
|---------|-----------|
| **Mantenibilidad** | Cada patrón aísla una responsabilidad; cambiar uno no afecta a otros |
| **Escalabilidad** | Agregar un efecto/estrategia/comando es crear una clase nueva |
| **Testabilidad** | Tests unitarios sin mocks complejos; cada clase es pequeña |
| **Flexibilidad** | Estrategias intercambiables, estados modulares, cadena extensible |
| **Documentación viva** | El código es autodocumentado: nombres de clases explícitos |
| **Reversibilidad** | Si necesitas revertir, eliminar una clase no rompe el resto |

---

## Próximas Mejoras (Futura)

1. **Observer Pattern** en State — cuando `transicionarA()`, notificar listeners (FE, logs, etc.)
2. **Factory Pattern** para `EstadoPartida` — factory que crea los estados según `Fase`
3. **Decorator Pattern** en handlers — si necesitamos combinar efectos dinámicamente
4. **Command + Memento** — agregar undo/redo de acciones
5. **Strategy + Factory** — selector de estrategia por nombre (fácil agregar dificultades desde DB)

---

## Conclusión

Los 4 patrones no son "capricho arquitectónico" — **resuelven problemas concretos** del código anterior:
- Efectos gigantes → Chain (modular)
- Bot monolítico → Strategy (intercambiable)
- Acciones dispersas → Command (encapsulado)
- Fases anárquicas → State (centralizado)

El resultado es un backend **más resiliente, escalable y fácil de mantener**.
