# Plan de Implementación — Patrones de Diseño (Backend)

**Objetivo**: incorporar 4 patrones de diseño sin romper el funcionamiento actual.
**Estrategia**: cada patrón se introduce en paralelo al código existente → se prueba → se hace el swap → se elimina el código viejo.
**Regla de oro**: si la app rompió, el paso fue demasiado grande. Revertí y dividí.

---

## Orden de implementación

```
Fase 1: Chain of Responsibility   (menor riesgo — solo agrega clases nuevas)
Fase 2: Strategy                  (extrae lógica ya existente)
Fase 3: Command                   (envuelve llamadas ya existentes)
Fase 4: State                     (mayor riesgo — afecta el flujo central)
```

Cada fase tiene: qué crear → qué modificar → qué eliminar → cómo verificar.

---

## Fase 1 — Chain of Responsibility

**Problema actual**: `BattleAttackService.calcularDanioPorEfectos()` y `aplicarEfectosSecundarios()` son métodos enormes con if-else anidados. Agregar un nuevo efecto de ataque obliga a tocar esos métodos y arriesgarse a romper los que ya funcionan.

**Solución**: cada efecto de ataque es un eslabón. El ataque pasa por la cadena y cada handler decide si aplica o pasa.

### Estructura a crear

```
service/battle/chain/
├── ManejadorEfecto.java          ← clase abstracta base
├── EfectoContadoresDanio.java    ← "damage counter on this"
├── EfectoEscalaPorEnergias.java  ← "for each energy attached"
├── EfectoInmunidad.java          ← "prevent all effects"
├── EfectoMonedaFalla.java        ← "tails, this attack does nothing"
├── EfectoMultiMoneda.java        ← "times the number of heads"
├── EfectoMonedaExtraDanio.java   ← "if heads, more damage"
├── EfectoVeneno.java             ← "is now poisoned"
├── EfectoQuemadura.java          ← "is now burned"
├── EfectoSueno.java              ← "is now asleep"
├── EfectoParalisis.java          ← "is now paralyzed"
├── EfectoCuracion.java           ← "heal X from this pokémon"
├── EfectoDanioBanca.java         ← "damage to 1 of your opponent's benched"
├── EfectoDescartarEnergia.java   ← "discard an energy card"
├── EfectoDanioPropio.java        ← "damage to itself"
├── EfectoRobarCartas.java        ← "draw a card"
└── EfectoAtrapar.java            ← "can't retreat"
```

### Contrato de `ManejadorEfecto`

```java
public abstract class ManejadorEfecto {
    private ManejadorEfecto siguiente;

    public ManejadorEfecto encadenar(ManejadorEfecto siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void procesar(ContextoAtaque contexto) {
        manejar(contexto);
        if (siguiente != null) siguiente.procesar(contexto);
    }

    protected abstract void manejar(ContextoAtaque contexto);
}
```

### `ContextoAtaque` — objeto que viaja por la cadena

```java
public class ContextoAtaque {
    Partida partida;
    Ataque ataque;
    CartaEnJuego atacante;
    CartaEnJuego defensor;
    List<Boolean> historialMonedas;
    int danioFinal;          // modificable por la cadena
    boolean ataqueAnulado;   // si true, el ataque no hace daño
    BattleAttackService.KoResolver koResolver;
    Random random;
}
```

### Pasos de implementación

1. Crear `ManejadorEfecto.java` y `ContextoAtaque.java` en `service/battle/chain/`.
2. Implementar cada handler (uno por commit, empezando por los más simples: veneno, quemadura, sueño).
3. Crear `CadenaAtaqueFactory.java` — un método estático que ensambla la cadena completa.
4. En `BattleAttackService`, agregar un método nuevo `resolveAttackChain(...)` que use la cadena.
5. **No tocar `resolveAttack(...)` todavía.** Probar la cadena en paralelo.
6. Una vez verificada: reemplazar el cuerpo de `resolveAttack` para delegar a la cadena.
7. Eliminar `calcularDanioPorEfectos()` y `aplicarEfectosSecundarios()`.

### Verificación

- Correr los tests existentes de `BattleAttackServiceTest`.
- Probar manualmente: ataque con veneno, con moneda, con curación.
- El FE no se toca — el contrato de `resolveAttack` no cambia.

---

## Fase 2 — Strategy

**Problema actual**: `BotAIService` tiene una sola forma de jugar, hardcodeada. Además, `calcularDanioFinal()` está duplicado: existe en `BotAIService` Y en `BattleAttackService`. Si cambia la fórmula de debilidad/resistencia, hay que cambiarla en dos lugares.

**Solución**: extraer la lógica de decisión del bot en estrategias intercambiables, y unificar el cálculo de daño.

### Estructura a crear

```
service/battle/strategy/
├── EstrategiaBot.java            ← interfaz
├── EstrategiaBasica.java         ← lógica actual del bot (extraída sin cambios)
└── EstrategiaDificil.java        ← placeholder para futuro (puede quedar vacía)
```

### Contrato de `EstrategiaBot`

```java
public interface EstrategiaBot {
    void ejecutarTurno(Partida partida);
}
```

### Pasos de implementación

1. Crear `EstrategiaBot.java` (interfaz).
2. Crear `EstrategiaBasica.java` — copiar el contenido actual de `BotAIService` sin cambiar nada.
3. Modificar `BotAIService`: eliminar la lógica interna, agregar `private EstrategiaBot estrategia = new EstrategiaBasica()`, y hacer que `ejecutarTurno()` delegue en `estrategia.ejecutarTurno(partida)`.
4. Mover `calcularDanioFinal()` (debilidad/resistencia) a un helper compartido `DanioCalculator` para que tanto el bot como `BattleAttackService` lo usen.
5. Verificar que el bot sigue funcionando igual.
6. Eliminar el `calcularDanioFinal()` duplicado de `BotAIService`.

### Verificación

- Correr `BotAIServiceTest`.
- Iniciar una partida contra el bot y completar 3 turnos.
- El comportamiento del bot debe ser idéntico al anterior.

---

## Fase 3 — Command

**Problema actual**: las acciones del turno (atacar, unir energía, jugar Pokémon, retirarse) están dispersas en `BattleEngineService`. No hay un punto único donde validar si una acción es legal en el turno actual. Agregar logging, historial o deshacer sería un caos.

**Solución**: cada acción es un objeto `ComandoTurno`. El motor solo ejecuta comandos, no sabe de lógica interna.

### Estructura a crear

```
service/battle/command/
├── ComandoTurno.java             ← interfaz
├── ComandoAtacar.java
├── ComandoUnirEnergia.java
├── ComandoJugarPokemon.java
├── ComandoRetirarse.java
└── ComandoEvolucionar.java
```

### Contrato de `ComandoTurno`

```java
public interface ComandoTurno {
    boolean puedeEjecutar(Partida partida);
    Partida ejecutar(Partida partida);
    String getNombre();           // para logging/historial
}
```

### Ejemplo: `ComandoAtacar`

```java
public class ComandoAtacar implements ComandoTurno {
    private final Ataque ataque;
    private final BattleAttackService attackService;
    private final BattleKoService koService;

    @Override
    public boolean puedeEjecutar(Partida partida) {
        CartaEnJuego activo = obtenerActivoActual(partida);
        return activo != null
            && activo.isPuedeAtacar()
            && !activo.tieneCondicion("Paralyzed")
            && !activo.tieneCondicion("Asleep")
            && partida.getFaseActual() == Partida.Fase.TURNO_NORMAL;
    }

    @Override
    public Partida ejecutar(Partida partida) {
        // delega en BattleAttackService — no cambia la lógica
        ...
    }
}
```

### Pasos de implementación

1. Crear `ComandoTurno.java` (interfaz).
2. Implementar `ComandoAtacar` primero — es el más completo y sirve como referencia.
3. Implementar los demás comandos uno a uno.
4. En `BattleEngineService`, crear un método `ejecutarComando(Partida, ComandoTurno)` que llame a `puedeEjecutar` y luego `ejecutar`.
5. **No reemplazar los métodos existentes todavía.** Probar los comandos en un test unitario independiente.
6. Una vez verificados: reemplazar los métodos en `BattleEngineService` para que usen los comandos.
7. El contrato del `BattleController` (los endpoints REST) no cambia.

### Verificación

- Test unitario por cada `ComandoTurno.puedeEjecutar()`.
- Jugar una partida completa (inicio → fin) sin errores.
- Verificar que acciones inválidas (atacar cuando está paralizado) son rechazadas.

---

## Fase 4 — State

**Problema actual**: `Partida` tiene `Fase` enum pero no hay enforcement. Cualquier servicio puede modificar el estado de la partida en cualquier momento. La transición de fases está implícita en la lógica de `BattleEngineService`.

**Solución**: cada fase de la partida es un objeto que sabe qué transiciones son válidas y qué acciones puede ejecutar.

### Estructura a crear

```
model/battle/state/
├── EstadoPartida.java            ← interfaz
├── EstadoInicio.java
├── EstadoLanzamientoMoneda.java
├── EstadoTurnoNormal.java
└── EstadoFinPartida.java
```

### Contrato de `EstadoPartida`

```java
public interface EstadoPartida {
    Partida.Fase getFase();
    boolean puedeEjecutarComando(ComandoTurno comando, Partida partida);
    EstadoPartida transicionarSiCorresponde(Partida partida);
}
```

### Relación con el código existente

| `Partida.Fase` actual | `EstadoPartida` nuevo |
|----------------------|-----------------------|
| `INICIO` | `EstadoInicio` |
| `LANZAMIENTO_MONEDA` | `EstadoLanzamientoMoneda` |
| `TURNO_NORMAL` | `EstadoTurnoNormal` |
| `FIN_PARTIDA` | `EstadoFinPartida` |

**Importante**: el campo `faseActual` de `Partida` **NO se elimina** — el FE lo lee para renderizar el tablero. El estado interno sincroniza ese campo al transicionar.

### Pasos de implementación

1. Crear la interfaz `EstadoPartida.java`.
2. Implementar `EstadoTurnoNormal` primero (es el estado donde vive el 90% del juego).
3. Agregar a `Partida` un campo `private transient EstadoPartida estado` (transient para que no se serialice al FE).
4. Agregar a `Partida` un método `transicionarA(EstadoPartida nuevoEstado)` que actualice `estado` y sincronice `faseActual`.
5. En `BattleEngineService`, reemplazar los checks manuales de `getFaseActual()` por `partida.getEstado().puedeEjecutarComando(comando, partida)`.
6. Implementar los estados restantes: `EstadoInicio`, `EstadoLanzamientoMoneda`, `EstadoFinPartida`.
7. Verificar que las transiciones de fase (inicio → moneda → turno → fin) siguen el mismo flujo.

### Verificación

- Jugar una partida completa de inicio a fin.
- Verificar que el lanzamiento de moneda solo ocurre en la fase correcta.
- Verificar que no se puede atacar en `INICIO` ni en `FIN_PARTIDA`.
- El FE no debe notar ningún cambio — `faseActual` sigue viajando igual.

---

## Mapa de archivos a crear (resumen)

```
BE/src/main/java/com/pokemon/tcg/
├── model/battle/
│   └── state/
│       ├── EstadoPartida.java
│       ├── EstadoInicio.java
│       ├── EstadoLanzamientoMoneda.java
│       ├── EstadoTurnoNormal.java
│       └── EstadoFinPartida.java
└── service/battle/
    ├── chain/
    │   ├── ManejadorEfecto.java
    │   ├── ContextoAtaque.java
    │   ├── CadenaAtaqueFactory.java
    │   └── [handlers individuales...]
    ├── command/
    │   ├── ComandoTurno.java
    │   ├── ComandoAtacar.java
    │   ├── ComandoUnirEnergia.java
    │   ├── ComandoJugarPokemon.java
    │   ├── ComandoRetirarse.java
    │   └── ComandoEvolucionar.java
    └── strategy/
        ├── EstrategiaBot.java
        ├── EstrategiaBasica.java
        └── EstrategiaDificil.java
```

---

## Reglas para todo el equipo

1. **Un patrón a la vez.** No empezar la Fase 2 hasta que la Fase 1 esté funcionando y commiteada.
2. **No modificar tests existentes** para hacer pasar el nuevo código. Si un test rompe, es el código nuevo el que está mal.
3. **El contrato del controller no cambia.** Los endpoints REST siguen con los mismos paths y DTOs.
4. **Commits atómicos.** Un commit por handler de Chain, un commit por comando. Facilita el revert si algo falla.
5. **Si dudás, preguntá antes de mergear.** Un PR roto en main le corta el trabajo a todo el equipo.
