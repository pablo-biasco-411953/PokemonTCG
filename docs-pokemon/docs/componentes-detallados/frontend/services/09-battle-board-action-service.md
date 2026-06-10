---
sidebar_position: 9
title: ⚔️ BattleBoardActionService
---

# ⚔️ BattleBoardActionService - Servicio de Acciones del Tablero

> Decisiones de acción sobre cartas de la mano y operaciones HTTP de juego que recargan el estado automáticamente

---

## 📍 Ubicación

`frontend/src/app/features/battle/services/battle-board-action.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class BattleBoardActionService {
  constructor(
    private battleService: BattleService,
    private battleBoardState: BattleBoardStateService,
    private i18n: I18nService,
  ) {}
}
```

**Tipo**: Servicio raíz  
**Dependencias**: `BattleService`, `BattleBoardStateService`, `I18nService`  
**Feature**: Battle (tablero de batalla)

---

## 📋 Tipos Internos

### CardActionType

```typescript
export type CardActionType =
  | 'unir-energia'
  | 'evolucionar'
  | 'requiere-promocion'
  | 'bajar-pokemon'
  | 'sin-accion';
```

| Valor | Significado |
|-------|-------------|
| `'unir-energia'` | La carta es una Energía → se puede unir a un Pokémon |
| `'evolucionar'` | La carta es una evolución y hay objetivo en mesa |
| `'requiere-promocion'` | Hay que promover un Pokémon de banca antes de poder jugar |
| `'bajar-pokemon'` | La carta es un Pokémon básico → se puede bajar a banca/activo |
| `'sin-accion'` | La carta no tiene acción disponible en este momento |

### CardActionDecision

```typescript
export interface CardActionDecision {
  tipo: CardActionType;
  target?: CartaEnJuego | null;   // Solo cuando tipo === 'evolucionar'
  mensaje?: string;               // Solo cuando tipo === 'requiere-promocion'
}
```

---

## 📡 Métodos

### Tabla de Referencia Rápida

| Método | Firma | Retorno | HTTP |
|--------|-------|---------|------|
| `resolverAccionCarta` | `(partida, carta)` | `CardActionDecision` | No |
| `puedePagarRetiro` | `(activo)` | `boolean` | No |
| `construirMensajeRetirada` | `(activo)` | `string` | No |
| `evolucionarYRecargar` | `(matchId, cartaEvolucionId, targetId)` | `Promise<Partida>` | Sí |
| `jugarPokemonYRecargar` | `(matchId, cartaId)` | `Promise<Partida>` | Sí |
| `unirEnergiaYRecargar` | `(matchId, activoId, energiaId)` | `Promise<Partida>` | Sí |
| `subirAActivoYRecargar` | `(matchId, cartaId)` | `Promise<Partida>` | Sí |
| `retirarPokemonYRecargar` | `(matchId, nuevoActivoId)` | `Promise<Partida>` | Sí |

---

### resolverAccionCarta(partida, carta)

**Decidir qué acción ejecutar al intentar usar una carta desde la mano**

```typescript
resolverAccionCarta(
  partida: Partida | null,
  carta: BattleActionCard | Card | null | undefined,
): CardActionDecision
```

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `partida` | `Partida \| null` | Estado actual |
| `carta` | `BattleActionCard \| Card \| null \| undefined` | Carta seleccionada de la mano |

**Algoritmo de decisión**:

```
si partida o carta son null → { tipo: 'sin-accion' }
  ↓
si carta.supertype === 'Energy'
  → { tipo: 'unir-energia' }
  ↓
si carta.supertype === 'Pokémon'
  si carta.evolvesFrom y hay objetivo en mesa
    → { tipo: 'evolucionar', target }
  si no hay activo y hay banca
    → { tipo: 'requiere-promocion', mensaje }
  → { tipo: 'bajar-pokemon' }
  ↓
→ { tipo: 'sin-accion' }
```

**Ejemplo**:
```typescript
const decision = this.actionService.resolverAccionCarta(partida, carta);
switch (decision.tipo) {
  case 'evolucionar':
    await this.actionService.evolucionarYRecargar(matchId, carta.id, decision.target.card.id);
    break;
  case 'bajar-pokemon':
    await this.actionService.jugarPokemonYRecargar(matchId, carta.id);
    break;
}
```

---

### puedePagarRetiro(activo)

**Verificar si el Pokémon activo tiene energías suficientes para retirarse**

```typescript
puedePagarRetiro(activo: CartaEnJuego | null | undefined): boolean
```

**Lógica**: `activo.energiasUnidas.length >= (activo.card.costoRetirada ?? 0)`

Un Pokémon con `costoRetirada = 0` siempre puede retirarse. Si `activo` es `null`, retorna `false`.

---

### construirMensajeRetirada(activo)

**Construir el texto de confirmación para la pantalla de retirada**

```typescript
construirMensajeRetirada(activo: CartaEnJuego): string
```

Usa `I18nService.translate('confirm.retreat', { name, cost })` para localizar el mensaje.

---

## 🔄 El Patrón xxxYRecargar()

Los métodos que terminan en `YRecargar` siguen el patrón:

```
1. Ejecutar acción en backend (POST)  →  void
2. Recargar estado completo del backend (GET getState)  →  Partida
3. Retornar el estado fresco
```

**Por qué**: El backend es la fuente de verdad. Después de cada mutación, el cliente siempre pide el estado completo actualizado en lugar de modificar el estado local. Esto evita inconsistencias por lógica de juego compleja en el backend.

**Implementación tipo**:
```typescript
async jugarPokemonYRecargar(matchId: string, cartaId: string): Promise<Partida> {
  await firstValueFrom(this.battleService.jugarPokemon(matchId, cartaId));
  return await firstValueFrom(this.battleService.getState(matchId));
}
```

---

### evolucionarYRecargar(matchId, cartaEvolucionId, targetId)

```typescript
async evolucionarYRecargar(
  matchId: string,
  cartaEvolucionId: string,
  targetId: string,
): Promise<Partida>
```

| Parámetro | Descripción |
|-----------|-------------|
| `matchId` | ID de la partida |
| `cartaEvolucionId` | ID de la carta de evolución (en mano) |
| `targetId` | ID del Pokémon base en mesa |

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/evolve` → `void`
2. `GET /api/battle/state/:matchId` → `Partida`

---

### jugarPokemonYRecargar(matchId, cartaId)

```typescript
async jugarPokemonYRecargar(matchId: string, cartaId: string): Promise<Partida>
```

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/play-pokemon` → `void`
2. `GET /api/battle/state/:matchId` → `Partida`

---

### unirEnergiaYRecargar(matchId, activoId, energiaId)

```typescript
async unirEnergiaYRecargar(
  matchId: string,
  activoId: string,
  energiaId: string,
): Promise<Partida>
```

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/attach-energy` → `void`
2. `GET /api/battle/state/:matchId` → `Partida`

---

### subirAActivoYRecargar(matchId, cartaId)

```typescript
async subirAActivoYRecargar(matchId: string, cartaId: string): Promise<Partida>
```

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/promote` → `void`
2. `GET /api/battle/state/:matchId` → `Partida`

---

### retirarPokemonYRecargar(matchId, nuevoActivoId)

```typescript
async retirarPokemonYRecargar(matchId: string, nuevoActivoId: string): Promise<Partida>
```

**Secuencia HTTP**:
1. `POST /api/battle/:matchId/retreat` → `void`
2. `GET /api/battle/state/:matchId` → `Partida`

---

## 🔗 Relaciones

- **Depende de**: `BattleService` (HTTP), `BattleBoardStateService` (predicados), `I18nService` (textos)
- **Consumido por**: Componentes del tablero de batalla
- **Ver también**: [06-battle-service.md](./06-battle-service.md), [08-battle-board-state-service.md](./08-battle-board-state-service.md)

---

*Próximo: [10-battle-board-attack-service.md](./10-battle-board-attack-service.md)*
