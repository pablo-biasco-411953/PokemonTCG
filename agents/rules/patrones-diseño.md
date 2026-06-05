# Regla: Respetar Patrones de Diseño

## Descripción

Los 4 patrones de diseño implementados en el backend (Chain of Responsibility, Strategy, Command, State) **no son opcionales**. Son la estructura sobre la que se construye toda la lógica de batalla.

Modificar, saltear o ignorar estos patrones causa:
- **Regresión**: código duplicado
- **Fragilidad**: cambios en un lado rompen el otro
- **Deuda técnica**: deuda exponencial a cada nueva feature

Esta regla obliga a **siempre respetar** la arquitectura existente.

---

## Reglas Específicas

### 1. Chain of Responsibility — Efectos de Ataque

**Regla**: Si necesitás agregar un efecto de ataque (veneno, quemadura, curación, moneda, etc.), **NUNCA** modificar `BattleAttackService` directamente.

**Cómo**:
1. Crear una clase nueva en `service/battle/chain/` que extienda `ManejadorEfecto`
2. Implementar `protected void manejar(ContextoAtaque ctx)`
3. Registrar el handler en `CadenaAtaqueFactory.buildCadenaPreDanio()` o `buildCadenaEfectosSecundarios()`
4. Escribir test unitario

**Por qué**:
- El ataque anterior funciona, y querés que **siga funcionando**
- Si tocas `BattleAttackService`, riesgo de romper los 18 efectos existentes
- Crear una clase nueva = cero riesgo

**Red flag**: 
- ❌ `if (ataque.getEfecto().equals("MiNuevoEfecto"))` en `BattleAttackService`
- ❌ Modificar `calcularDanioPorEfectos()` o `aplicarEfectosSecundarios()`
- ✅ Nueva clase `EfectoMiNuevoEfecto extends ManejadorEfecto`

---

### 2. Strategy — Comportamiento del Bot

**Regla**: Si necesitás cambiar cómo juega el bot, **NO** modificar `BotAIService` directamente.

**Cómo**:
1. Crear una clase nueva en `service/battle/strategy/` que implemente `EstrategiaBot`
2. Implementar `void ejecutarTurno(Partida partida)`
3. En `BotAIService`, cambiar solo esta línea:
   ```java
   private final EstrategiaBot estrategia = new TuNuevaEstrategia();
   ```
4. Escribir test para la nueva estrategia

**Por qué**:
- La estrategia actual (`EstrategiaBasica`) funciona y NO debe cambiar
- Si modificas la lógica directa, la próxima persona que agregue una estrategia "Media" tendrá que copiar-pegar **todo**
- Dos estrategias → deuda infinita

**Red flag**:
- ❌ Copiar `EstrategiaBasica` y renombrar a `EstrategiaMedia`
- ❌ Modificar métodos internos de `EstrategiaBasica` "para hacerla más agresiva"
- ✅ Nueva clase `EstrategiaMedia extends EstrategiaBot`

---

### 3. Command — Acciones de Turno

**Regla**: Si necesitás agregar una acción nueva (ej: "jugar Supporter"), **NUNCA** modificar `BattleEngineService` directamente.

**Cómo**:
1. Crear una clase nueva en `service/battle/command/` que implemente `ComandoTurno`
2. Implementar `boolean puedeEjecutar(Partida partida)` y `void ejecutar(Partida partida)`
3. En el controller, instanciar el comando y pasarlo a `ejecutarComando(partida, comando)`
4. Escribir test para validaciones

**Por qué**:
- Las 6 acciones actuales funcionan
- Si tocas `BattleEngineService`, riesgo de romper las 6
- El dispatcher `ejecutarComando()` está ahí para **eso** — centralizar validación

**Red flag**:
- ❌ Agregar un método público `jugarSupporter()` en `BattleEngineService`
- ❌ Duplicar código de validación de otras acciones
- ✅ Comando nuevo + `ejecutarComando()`

---

### 4. State — Fases de la Partida

**Regla**: **NUNCA** usar `partida.setFaseActual()` directamente. Siempre usar `partida.transicionarA()`.

**Cómo**:
```java
// ❌ MAL
partida.setFaseActual(Partida.Fase.FIN_PARTIDA);

// ✅ BIEN
partida.transicionarA(new EstadoFinPartida());
```

**Por qué**:
- `transicionarA()` sincroniza **ambas cosas**: `estado` + `faseActual`
- Si usas `setFaseActual()` directamente, el estado interno y el enum se desincronizar
- Resultado: FE no sabe que la partida terminó, backend sí → bugs raros

**Red flag**:
- ❌ `partida.setFaseActual(...)` en cualquier servicio
- ✅ `partida.transicionarA(new EstadoXXX())`

---

### 5. Cross-Cutting: Respetá el Gate de Estado

**Regla**: Cuando agregues un comando o una acción, **NO saltés** la verificación de estado en `ejecutarComando()`.

**Cómo funciona ahora**:
```java
private void ejecutarComando(Partida partida, ComandoTurno comando) {
    // Primero: ¿la partida está en una fase que permite acciones?
    if (!partida.getEstado().permiteAccionesDeJuego()) {
        throw new IllegalStateException("Acción no permitida en fase " + partida.getFaseActual());
    }
    // Segundo: ¿el comando específico puede ejecutarse?
    if (!comando.puedeEjecutar(partida)) {
        throw new IllegalStateException("Acción no permitida: " + comando.getNombre());
    }
    // Tercero: ejecuta
    comando.ejecutar(partida);
}
```

Si creas un comando nuevo y lo ejecutas **sin pasar por `ejecutarComando()`**, el gate de estado se salta. Resultado: se puede atacar en `FIN_PARTIDA`.

**Red flag**:
- ❌ `comando.ejecutar(partida)` directo en el controller
- ✅ `ejecutarComando(partida, comando)`

---

## Árbitro: Cómo Verificar que se Respeta

En code review, buscar:

1. **Chain**: ¿Hay nuevas líneas en `BattleAttackService`? ❌ Debería ser nueva clase
2. **Strategy**: ¿Hay cambios en `BotAIService` además de cambiar `estrategia =`? ❌ Debería ser nueva estrategia
3. **Command**: ¿Hay nuevos métodos públicos en `BattleEngineService` que ejecutan acciones? ❌ Debería ser nuevo comando
4. **State**: ¿Se usa `setFaseActual()` en algo que no sea `transicionarA()`? ❌ Debe cambiar
5. **Cross-Cutting**: ¿Se ejecutan comandos sin pasar por `ejecutarComando()`? ❌ Debe arreglarse

---

## Beneficio a Largo Plazo

| Momento | Si respetas patrones | Si los ignorás |
|---------|----------------------|-----------------|
| **1 feature nueva** | +1 archivo, +0 cambios a existentes | +1-3 cambios a servicios, riesgo bajo |
| **5 features nuevas** | +5 archivos, +0 cambios a existentes | +10-15 cambios dispersos, riesgo medio |
| **10 features nuevas** | +10 archivos, +0 cambios a existentes | +20-30 cambios, riesgo alto, bugs ocultos |
| **20 features nuevas** | +20 archivos, arquitectura clara | Spaghetti code, imposible de mantener |

**La deuda técnica crece exponencialmente si los patrones se ignoran.**

---

## Excepciones (Rarísimas)

La única excepción es si **descubrís un bug en la arquitectura misma** (ej: State Pattern no sirve para sub-estados). Entonces:

1. Documentá el problema en un issue/PR
2. Propone una solución alternativa **antes** de tocar código
3. Coordina con el equipo
4. Refactoriza la arquitectura de forma ordenada

Pero "necesito agregar rápido" **no es excepción**.

---

## Links Útiles

- [Documentación completa de patrones](../BE/PATRONES_DISEÑO.md)
- [Plan de implementación](./IMPLEMENTATION_PLAN.md)
- [Convenciones del código](./conventions.md)
