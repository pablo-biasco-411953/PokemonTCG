---
sidebar_position: 11
title: ⚔️ BattleBoardCombatService
---

# ⚔️ BattleBoardCombatService - Servicio de Combate del Tablero

> Resolución de combate, confusión, turno del bot y cálculo de daño real

---

## 📍 Ubicación

`frontend/src/app/features/battle/services/battle-board-combat.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class BattleBoardCombatService {
  constructor(private battleService: BattleService) {}
}
```

**Tipo**: Servicio raíz  
**Dependencias**: `BattleService`  
**Feature**: Battle (tablero de batalla)  
**Importa de**: `BattleBoardAttackService` el tipo `CoinFlipConfig`

---

## 📡 Métodos

### Tabla de Referencia Rápida

| Método | Firma | Retorno | HTTP |
|--------|-------|---------|------|
| `crearConfigConfusion` | `()` | `CoinFlipConfig` | No |
| `resolverConfusion` | `(randomFn?)` | `number` | No |
| `atacarYRecargar` | `(matchId, nombreAtaque)` | `Promise<Partida>` | Sí |
| `pasarTurnoYRecargar` | `(matchId)` | `Promise<Partida>` | Sí |
| `ejecutarTurnoBot` | `(matchId)` | `Promise<Partida>` | Sí |
| `calcularDanioHecho` | `(hpAntes, hpDespues)` | `number` | No |

---

### crearConfigConfusion()

**Devolver la configuración visual estándar para el check de confusión**

```typescript
crearConfigConfusion(): CoinFlipConfig
```

**Retorno fijo**:
```typescript
{
  descripcion: 'Tu Pokémon está confundido. Lanzá una moneda. Si sale cruz, te hacés 30 de daño y el ataque falla.',
  cantidadMonedas: 1,
  danioBase: 0,
  danioExtraPorCara: 0,
  esSoloEstado: true,
}
```

Este objeto se usa para mostrar el overlay de moneda cuando el Pokémon del jugador está confundido al intentar atacar. El resultado (CARA = ataque pasa, CRUZ = 30 de daño al propio Pokémon) es resuelto por el backend; este método solo configura la visualización.

---

### resolverConfusion(randomFn?)

**Resolver localmente el lanzamiento del check de confusión**

```typescript
resolverConfusion(randomFn: () => number = Math.random): number
```

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| `randomFn` | `Math.random` | Función de aleatoriedad (inyectable para tests) |

**Retorno**: `1` si CARA (≥ 0.5), `0` si CRUZ (< 0.5)

**Nota**: Este resultado es solo para la animación visual. La resolución real del combate ocurre en el backend.

---

## 🔄 El Patrón xxxYRecargar()

Al igual que en `BattleBoardActionService`, los métodos `atacarYRecargar` y `pasarTurnoYRecargar` siguen el patrón:

```
POST acción → void
GET getState → Partida fresca
```

### atacarYRecargar(matchId, nombreAtaque)

```typescript
async atacarYRecargar(matchId: string, nombreAtaque: string): Promise<Partida>
```

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/attack?nombreAtaque=...` → `void`
2. `GET /api/battle/state/:matchId` → `Partida`

---

### pasarTurnoYRecargar(matchId)

```typescript
async pasarTurnoYRecargar(matchId: string): Promise<Partida>
```

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/pass-turn` → `void`
2. `GET /api/battle/state/:matchId` → `Partida`

---

### ejecutarTurnoBot(matchId)

**Pedir al backend que ejecute el turno completo del bot**

```typescript
async ejecutarTurnoBot(matchId: string): Promise<Partida>
```

**Diferencia con el patrón YRecargar**: Este método hace una sola llamada HTTP porque el endpoint `/jugar-bot` ya retorna el estado completo post-turno del bot.

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/jugar-bot` → `Partida`

---

### calcularDanioHecho(hpAntes, hpDespues)

**Calcular el daño real infligido comparando HP antes y después**

```typescript
calcularDanioHecho(hpAntes: number, hpDespues: number): number
```

**Retorno**: `hpAntes - hpDespues`

**Uso típico**: Calcular cuánto daño hizo un ataque para sincronizar la animación de número de daño:

```typescript
const hpAntes = this.battleBoardState.clonarPartida(partida).bot.activo.hpActual;
const nuevaPartida = await this.combat.atacarYRecargar(matchId, ataque.nombre);
const danio = this.combat.calcularDanioHecho(hpAntes, nuevaPartida.bot.activo?.hpActual ?? 0);
// Mostrar animación con `danio`
```

---

## 🔗 Relaciones

- **Depende de**: `BattleService` (HTTP)
- **Importa**: `CoinFlipConfig` de `BattleBoardAttackService`
- **Consumido por**: Componentes del tablero de batalla
- **Ver también**: [06-battle-service.md](./06-battle-service.md), [10-battle-board-attack-service.md](./10-battle-board-attack-service.md), [12-battle-board-turn-service.md](./12-battle-board-turn-service.md)

---

*Próximo: [12-battle-board-turn-service.md](./12-battle-board-turn-service.md)*
