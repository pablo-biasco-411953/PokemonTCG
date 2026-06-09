---
sidebar_position: 8
title: ⚔️ BattleBoardStateService
---

# ⚔️ BattleBoardStateService - Servicio de Estado del Tablero

> Métodos puros de consulta y predicado sobre el estado de la partida, sin llamadas HTTP

---

## 📍 Ubicación

`frontend/src/app/features/battle/services/battle-board-state.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class BattleBoardStateService {
  // Sin constructor con dependencias (no usa HTTP)
}
```

**Tipo**: Servicio raíz  
**Dependencias**: ninguna (servicio puro)  
**Feature**: Battle (tablero de batalla)  
**Característica**: Todos los métodos son sincrónicos, sin efectos secundarios

---

## 🧠 Filosofía: Métodos Puros

`BattleBoardStateService` es intencionalmente un servicio **sin HTTP**. Contiene únicamente lógica de predicados y utilidades de estado que:

- Operan sobre snapshots ya existentes de `Partida`
- Son deterministas: mismo input → mismo output
- No modifican el estado, solo lo consultan
- Son fácilmente testeables en forma unitaria

Esto contrasta con `BattleBoardActionService` y `BattleBoardCombatService`, que sí hacen llamadas HTTP y usan este servicio como auxiliar.

---

## 📋 Métodos

### Tabla de Referencia Rápida

| Método | Firma | Retorno | Descripción |
|--------|-------|---------|-------------|
| `clonarPartida` | `(partida: Partida \| null)` | `Partida \| null` | Clona el estado para comparaciones |
| `tieneCondicion` | `(pokemon, condicion: string)` | `boolean` | Verifica condición especial |
| `buscarObjetivoEvolucion` | `(partida, cartaMano)` | `CartaEnJuego \| null` | Busca objetivo de evolución |
| `puedeEvolucionar` | `(partida, cartaMano)` | `boolean` | Indica si hay evolución disponible |
| `buscarCartaEnMano` | `(partida, cartaId: string)` | `Card \| undefined` | Obtiene carta de la mano por ID |

---

### clonarPartida(partida)

**Clonar snapshot de la partida para comparaciones visuales**

```typescript
clonarPartida(partida: Partida | null): Partida | null
```

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `partida` | `Partida \| null` | Estado a clonar |

**Retorno**: Copia profunda vía `JSON.parse(JSON.stringify(...))`, o `null` si la entrada es `null`

**Algoritmo**:
```typescript
return partida ? JSON.parse(JSON.stringify(partida)) : null;
```

**Cuándo usarlo**: Antes de una acción para guardar el estado previo y calcular diferencias (ej: cuánto daño se hizo comparando HP antes y después).

**Ejemplo**:
```typescript
const estadoAnterior = this.battleBoardState.clonarPartida(this.partida);
await this.battleBoardCombat.atacarYRecargar(matchId, nombreAtaque);
const danio = estadoAnterior.bot.activo.hpActual - this.partida.bot.activo.hpActual;
```

---

### tieneCondicion(pokemon, condicion)

**Comprobar si un Pokémon tiene una condición especial activa**

```typescript
tieneCondicion(
  pokemon: CartaEnJuego | null | undefined,
  condicion: string
): boolean
```

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `pokemon` | `CartaEnJuego \| null \| undefined` | Pokémon a verificar |
| `condicion` | `string` | Nombre de la condición (case-insensitive) |

**Retorno**: `true` si `condicionesEspeciales` contiene la condición indicada

**Algoritmo**: Comparación case-insensitive sobre el array `condicionesEspeciales`:
```typescript
return (pokemon?.condicionesEspeciales || []).some(
  (actual) => actual.toUpperCase() === condicion.toUpperCase(),
);
```

**Condiciones válidas** (valores en backend):
- `'ASLEEP'` — dormido
- `'PARALYZED'` — paralizado
- `'CONFUSED'` — confundido
- `'POISONED'` — envenenado
- `'BURNED'` — quemado

**Ejemplo**:
```typescript
if (this.battleBoardState.tieneCondicion(partida.jugador.activo, 'ASLEEP')) {
  // El pokémon activo está dormido
}
```

---

### buscarObjetivoEvolucion(partida, cartaMano)

**Buscar el Pokémon en mesa que puede evolucionar con una carta de la mano**

```typescript
buscarObjetivoEvolucion(
  partida: Partida | null,
  cartaMano: BattleActionCard | Card | null | undefined,
): CartaEnJuego | null
```

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `partida` | `Partida \| null` | Estado actual del tablero |
| `cartaMano` | `BattleActionCard \| Card \| null \| undefined` | Carta de evolución de la mano |

**Retorno**: La primera `CartaEnJuego` cuyo `card.nombre === cartaMano.evolvesFrom`, o `null` si no hay objetivo

**Algoritmo**:
1. Si no hay `partida` o `cartaMano.evolvesFrom` es falsy → retorna `null`
2. Verifica si el activo puede evolucionar
3. Busca en `banca` si el activo no aplica

```typescript
if (!partida || !cartaMano?.evolvesFrom) return null;

if (partida.jugador?.activo?.card?.nombre === cartaMano.evolvesFrom) {
  return partida.jugador.activo;
}

return (
  partida.jugador?.banca?.find(
    (pokemon) => pokemon.card.nombre === cartaMano.evolvesFrom
  ) || null
);
```

**Ejemplo**:
```typescript
const charmeleon = { nombre: 'Charizard', evolvesFrom: 'Charmeleon', ... };
const objetivo = this.battleBoardState.buscarObjetivoEvolucion(partida, charmeleon);
// objetivo = CartaEnJuego con card.nombre === 'Charmeleon', o null
```

---

### puedeEvolucionar(partida, cartaMano)

**Indicar si hay algún objetivo de evolución disponible**

```typescript
puedeEvolucionar(
  partida: Partida | null,
  cartaMano: BattleActionCard | Card | null | undefined,
): boolean
```

**Retorno**: `!!buscarObjetivoEvolucion(partida, cartaMano)`

Es un wrapper conveniente sobre `buscarObjetivoEvolucion` para cuando solo interesa saber si es posible evolucionar, sin necesitar el objetivo concreto.

---

### buscarCartaEnMano(partida, cartaId)

**Obtener una carta puntual de la mano del jugador por ID**

```typescript
buscarCartaEnMano(partida: Partida | null, cartaId: string): Card | undefined
```

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `partida` | `Partida \| null` | Estado actual |
| `cartaId` | `string` | ID de la carta a buscar |

**Retorno**: La `Card` de la mano cuyo `id === cartaId`, o `undefined` si no está

**Ejemplo**:
```typescript
const carta = this.battleBoardState.buscarCartaEnMano(partida, 'xy1-1');
if (carta) {
  // la carta está en mano
}
```

---

## 🔗 Uso desde BattleBoardActionService

`BattleBoardStateService` es una dependencia directa de `BattleBoardActionService`. Ejemplo de integración:

```typescript
// En BattleBoardActionService
resolverAccionCarta(partida, carta): CardActionDecision {
  // Usa puedeEvolucionar y buscarObjetivoEvolucion de este servicio
  if (carta.evolvesFrom) {
    const target = this.battleBoardState.buscarObjetivoEvolucion(partida, carta);
    if (target) return { tipo: 'evolucionar', target };
  }
  // ...
}
```

---

## 🔗 Relaciones

- **Consumido por**: `BattleBoardActionService`, `BattleBoardTurnService`
- **Tipos usados**: `Partida`, `CartaEnJuego`, `BattleActionCard`, `Card`
- **Ver también**: [05-battle-types.md](../types/05-battle-types.md)

---

*Próximo: [09-battle-board-action-service.md](./09-battle-board-action-service.md)*
