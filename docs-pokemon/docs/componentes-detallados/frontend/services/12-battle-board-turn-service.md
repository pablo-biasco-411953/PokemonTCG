---
sidebar_position: 12
title: ⚔️ BattleBoardTurnService
---

# ⚔️ BattleBoardTurnService - Servicio de Turno del Tablero

> Lógica de coin flip, sueño, análisis de turno del bot y sincronización de animaciones de moneda

---

## 📍 Ubicación

`frontend/src/app/features/battle/services/battle-board-turn.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class BattleBoardTurnService {
  // Sin dependencias inyectadas (servicio puro)
}
```

**Tipo**: Servicio raíz  
**Dependencias**: ninguna  
**Feature**: Battle (tablero de batalla)  
**Característica clave**: Acepta `randomFn?: () => number` en múltiples métodos para facilitar tests unitarios deterministas

---

## 📋 Tipos Locales

### SleepCheckResult

```typescript
export interface SleepCheckResult {
  estabaDormido: boolean;   // El Pokémon tenía la condición ASLEEP antes
  sigueDormido: boolean;    // Sigue con ASLEEP después
  seDesperto: boolean;      // !sigueDormido
}
```

### BotTurnAnalysis

```typescript
export interface BotTurnAnalysis {
  botEstabaDormido: boolean;      // Bot tenía ASLEEP antes del turno
  botEstabaParalizado: boolean;   // Bot tenía PARALYZED antes del turno
  puedeActuar: boolean;           // No paralizado y (no dormido ó se despertó)
  botAtaco: boolean;              // El bot ejecutó un ataque
  hpJugadorDespues: number;       // HP del activo del jugador post-turno
  danioHecho: number;             // hpAntes - hpDespues
}
```

---

## 🎯 Patrón de Testing: randomFn Inyectable

Varios métodos del servicio aceptan un parámetro opcional `randomFn: () => number = Math.random`. Esto permite reemplazar `Math.random` en tests con una función determinista:

```typescript
// En producción
const resultado = service.resolverSiguienteMoneda(3, 1, 5, 2);
// En tests
const resultado = service.resolverSiguienteMoneda(3, 1, 5, 2, () => 0.1); // siempre 0.1
```

Métodos con `randomFn`:
- `resolverSiguienteMoneda(carasForzadas, carasAsignadas, cantidadMonedas, indiceMoneda, randomFn?)`
- `calcularTiempoPensamientoBot(randomFn?)`

---

## 📡 Métodos

### Tabla de Referencia Rápida

| Método | Firma resumida | Retorno | randomFn |
|--------|---------------|---------|----------|
| `evaluarDespertar` | `(antes, despues)` | `SleepCheckResult \| null` | No |
| `crearEstadoCoinFlip` | `(descripcion, nombreAtaque?, cantidadMonedas?)` | `AttackCoinFlipState` | No |
| `analizarTurnoBot` | `(estadoAnterior, estadoFinal, hpJugadorAntes)` | `BotTurnAnalysis` | No |
| `resolverCarasBot` | `(config, estadoFinal, danioHecho)` | `number` | No |
| `resolverCarasJugador` | `(config, habilidad, estadoFinal, danioHecho)` | `number` | No |
| `resolverSiguienteMoneda` | `(carasForzadas, carasAsignadas, cantidadMonedas, indiceMoneda, randomFn?)` | `boolean` | Sí |
| `obtenerResultadoMoneda` | `(cantidadMonedas, carasAsignadas)` | `CoinSide` | No |
| `calcularDanioMonedas` | `(config, carasAsignadas)` | `number` | No |
| `calcularTiempoPensamientoBot` | `(randomFn?)` | `number` | Sí |

---

### evaluarDespertar(antes, despues)

**Evaluar si un Pokémon dormido logró despertarse entre dos snapshots**

```typescript
evaluarDespertar(
  antes: CartaEnJuego | null | undefined,
  despues: CartaEnJuego | null | undefined,
): SleepCheckResult | null
```

**Retorno**: `null` si el Pokémon no estaba dormido. `SleepCheckResult` si estaba dormido.

**Algoritmo**:
1. Si `antes` no tiene condición `'ASLEEP'` → retorna `null`
2. Verifica si `despues` todavía tiene `'ASLEEP'`
3. Construye el resultado

---

### crearEstadoCoinFlip(descripcion, nombreAtaque?, cantidadMonedas?)

**Construir el estado inicial para el overlay de animación de monedas**

```typescript
crearEstadoCoinFlip(
  descripcion: string,
  nombreAtaque = 'FASE DE MANTENIMIENTO',
  cantidadMonedas = 1,
): AttackCoinFlipState
```

**Retorno**:
```typescript
{
  nombreAtaque,
  descripcion,
  cantidadMonedas,
  danioBase: 0,
  danioExtraPorCara: 0,
  monedas: Array(cantidadMonedas).fill({ estado: 'girando' }),
  terminado: false,
  progreso: 0,
  esSoloEstado: true,
  danioTotal: 0,
}
```

Todas las monedas inician en estado `'girando'`. El componente actualiza cada `CoinVisualState` a `'cara'` o `'cruz'` durante la animación.

---

### analizarTurnoBot(estadoAnterior, estadoFinal, hpJugadorAntes)

**Analizar el resultado del turno del bot**

```typescript
analizarTurnoBot(
  estadoAnterior: Partida | null,
  estadoFinal: Partida,
  hpJugadorAntes: number,
): BotTurnAnalysis
```

**Algoritmo**:
1. Lee condiciones del bot antes (`estadoAnterior.bot.activo`)
2. Calcula daño hecho al jugador: `hpJugadorAntes - estadoFinal.jugador.activo.hpActual`
3. `puedeActuar = !paralizado && (!dormido || se despertó)`
4. `botAtaco`: si `danioHecho > 0` → `true`; sino, heurística por costo de energías vs energías disponibles

---

### resolverCarasBot(config, estadoFinal, danioHecho)

**Estimar cuántas caras CARA produjo el ataque del bot según el estado final**

```typescript
resolverCarasBot(
  config: CoinFlipConfig,
  estadoFinal: Partida,
  danioHecho: number,
): number
```

**Algoritmo**:
- Si `esSoloEstado`: retorna `1` si el jugador tiene condición especial activa, `0` si no
- Sino: retorna `1` si `danioHecho > 0`, `0` si no

---

### resolverCarasJugador(config, habilidad, estadoFinal, danioHecho)

**Estimar cuántas caras CARA produjo el ataque del jugador según el estado final**

```typescript
resolverCarasJugador(
  config: CoinFlipConfig,
  habilidad: Ataque,
  estadoFinal: Partida,
  danioHecho: number,
): number
```

**Algoritmo (más complejo)**:
1. Si `esSoloEstado`: detecta condición en el bot → `1` o `0`
2. Si `"does nothing"` en texto: `danioHecho > 0` → `1`, sino `0`
3. Si multiplicador por caras (`heads` + `danioExtraPorCara > 0`):
   ```
   caras = round( max(0, danioHecho - danioBase) / danioExtraPorCara )
   min(caras, cantidadMonedas)
   ```
4. Default: `danioHecho > 0` → `1`

---

### resolverSiguienteMoneda(carasForzadas, carasAsignadas, cantidadMonedas, indiceMoneda, randomFn?)

**Decidir si la siguiente moneda visual debe mostrar CARA, sin romper la sincronía con el backend**

```typescript
resolverSiguienteMoneda(
  carasForzadas: number,    // Caras reales (calculadas del daño)
  carasAsignadas: number,   // Caras ya animadas
  cantidadMonedas: number,  // Total de monedas del ataque
  indiceMoneda: number,     // Índice de la moneda actual (0-based)
  randomFn: () => number = Math.random,
): boolean
```

**Retorno**: `true` = esta moneda es CARA, `false` = CRUZ

**Algoritmo de distribución probabilística**:
```
carasRestantes = carasForzadas - carasAsignadas
monedasRestantes = cantidadMonedas - indiceMoneda

si carasRestantes >= monedasRestantes → true (todas las restantes deben ser CARA)
si carasRestantes <= 0 → false (ya se asignaron todas)
sino → randomFn() < carasRestantes / monedasRestantes
```

**Garantía**: Al final de las `cantidadMonedas` monedas, exactamente `carasForzadas` serán CARA.

**Test con randomFn**:
```typescript
// Forzar que siempre sea CARA
const esCara = service.resolverSiguienteMoneda(2, 0, 3, 0, () => 0.01);
expect(esCara).toBe(true); // 2/3 > 0.01
```

---

### obtenerResultadoMoneda(cantidadMonedas, carasAsignadas)

**Determinar el resultado global del overlay de monedas**

```typescript
obtenerResultadoMoneda(cantidadMonedas: number, carasAsignadas: number): CoinSide
```

**Retorno**: `'CARA'` si `carasAsignadas > 0`, `'CRUZ'` si `carasAsignadas === 0`

*Nota*: La implementación actual retorna el mismo resultado para 1 o N monedas. Para ataques con múltiples monedas, `carasAsignadas` representa el total acumulado.

---

### calcularDanioMonedas(config, carasAsignadas)

**Calcular el daño total mostrado en el overlay según las caras obtenidas**

```typescript
calcularDanioMonedas(config: CoinFlipConfig, carasAsignadas: number): number
```

**Retorno**: `carasAsignadas * config.danioExtraPorCara`

---

### calcularTiempoPensamientoBot(randomFn?)

**Generar un delay aleatorio para "humanizar" la respuesta del bot**

```typescript
calcularTiempoPensamientoBot(randomFn: () => number = Math.random): number
```

**Retorno**: Número entero entre 1000ms y 2499ms  
**Fórmula**: `Math.floor(randomFn() * 1500) + 1000`

---

## 🔗 Relaciones

- **Tipos propios exportados**: `SleepCheckResult`, `BotTurnAnalysis`
- **Tipos usados**: `Partida`, `CartaEnJuego`, `Ataque`, `AttackCoinFlipState`, `CoinSide`, `CoinFlipConfig`
- **Consumido por**: Componentes del tablero de batalla
- **Ver también**: [10-battle-board-attack-service.md](./10-battle-board-attack-service.md), [06-battle-board-types.md](../types/06-battle-board-types.md)

---

*Próximo: [13-battle-board-ui-service.md](./13-battle-board-ui-service.md)*
