---
sidebar_position: 10
title: ⚔️ BattleBoardAttackService
---

# ⚔️ BattleBoardAttackService - Servicio de Ataques del Tablero

> Análisis de ataques: detección de coin flips, validación de energías y cálculo de faltantes

---

## 📍 Ubicación

`frontend/src/app/features/battle/services/battle-board-attack.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class BattleBoardAttackService {
  // Sin dependencias inyectadas
}
```

**Tipo**: Servicio raíz  
**Dependencias**: ninguna  
**Feature**: Battle (tablero de batalla)  
**Característica**: Todos los métodos son sincrónicos y sin HTTP

---

## 📋 Tipos

### CoinFlipConfig

```typescript
export interface CoinFlipConfig {
  cantidadMonedas: number;       // Cuántas monedas se lanzan
  danioBase: number;             // Daño garantizado sin importar el resultado
  danioExtraPorCara: number;     // Daño adicional por cada CARA
  descripcion: string;           // Texto localizado para el overlay
  esSoloEstado: boolean;         // true = el ataque solo aplica condición (no daño por moneda)
}
```

---

## 📡 Métodos

### Tabla de Referencia Rápida

| Método | Firma | Retorno | Descripción |
|--------|-------|---------|-------------|
| `detectarCoinFlipAtaque` | `(ataque, traducirDescripcion)` | `CoinFlipConfig \| null` | Detecta si el ataque requiere monedas |
| `validarEnergiaAtaque` | `(ataque, activo)` | `boolean` | Verifica si hay energías suficientes |
| `getCheckEnergiasAtaque` | `(ataque, activo)` | `any[]` | Lista qué energías están cubiertas |
| `getFaltantesAtaque` | `(ataque, activo)` | `any[]` | Lista energías que faltan |

---

### detectarCoinFlipAtaque(ataque, traducirDescripcion)

**Detectar si un ataque requiere animación de moneda y extraer su configuración**

```typescript
detectarCoinFlipAtaque(
  ataque: any,
  traducirDescripcion: (
    texto: string,
    cantidadMonedas: number,
    danioExtraPorCara: number,
    esMultiplicador: boolean,
    esFalloCruz: boolean,
    esSoloEstado: boolean
  ) => string
): CoinFlipConfig | null
```

**Retorno**: `CoinFlipConfig` si el ataque contiene coin flip, `null` si no aplica

#### Algoritmo de Parsing

**Paso 1 — Extracción del texto**  
Lee `ataque.texto || ataque.descripcion || ataque.efecto`. Si ninguno existe, retorna `null`.

**Paso 2 — Detección de coin flip**  
Busca con regex:
```
/flip\s+(\d+|a|an|one|two|three|four|five)\s+coin|lanz[aá]\s+(\d+|una?)\s+moneda/i
```
Si no hay match → retorna `null`.

**Paso 3 — Mapeo de cantidad de monedas**

| Texto en ataque | cantidadMonedas |
|-----------------|-----------------|
| `a`, `an`, `one`, `una`, `un`, `1` | 1 |
| `two`, `dos`, `2` | 2 |
| `three`, `tres`, `3` | 3 |
| `four`, `cuatro`, `4` | 4 |
| `five`, `cinco`, `5` | 5 |

**Paso 4 — Clasificación del efecto**

| Condición en texto | `esFalloCruz` | `esMultiplicador` | `danioExtraPorCara` |
|--------------------|--------------|--------------------|----------------------|
| `"does nothing"` | `true` | — | `= danioBase` (danioBase pasa a 0) |
| `"times the number of heads"` | — | `true` | Parseado del texto |
| `"for each heads"` | — | `true` | `danioBase` si es positivo, sino 10 |
| `"more damage"` / `"additional"` | — | — | Parseado del texto |
| `paralyzed/asleep/confused/poisoned` (sin daño) | — | — | `esSoloEstado = true` |

**Ejemplo completo: parsing de un ataque real**

Ataque: `"Lanza una moneda. Si es CRUZ, este ataque no hace nada."`

```
texto → "lanza una moneda. si es cruz, este ataque no hace nada."
flipMatch → ['lanza una moneda', undefined, 'una']
cantidadMonedas → 1
"does nothing" encontrado → esFalloCruz = true
danioExtraPorCara = danioBase (ej: 30)
danioBase = 0
```

Resultado:
```typescript
{
  cantidadMonedas: 1,
  danioBase: 0,
  danioExtraPorCara: 30,
  descripcion: "...",
  esSoloEstado: false
}
```

---

### validarEnergiaAtaque(ataque, activo)

**Verificar si el Pokémon activo puede pagar el costo de energía de un ataque**

```typescript
validarEnergiaAtaque(ataque: any, activo: any): boolean
```

**Algoritmo**:

1. Normalizar tipos de energías poseídas (via `normalizarTipo` privado)
2. Normalizar tipos requeridos por el costo del ataque
3. Resolver primero los tipos específicos (no Colorless)
4. Si algún tipo específico no tiene match → `false`
5. Resolver Colorless con energías sobrantes
6. Si sobran Colorless sin cubrir → `false`, sino `true`

**Normalización de tipos** (método privado `normalizarTipo`):

| Input (ejemplo) | Output canónico |
|----------------|-----------------|
| `'Grass'`, `'Planta'`, `'grass'` | `'Grass'` |
| `'Fire'`, `'Fuego'` | `'Fire'` |
| `'Water'`, `'Agua'` | `'Water'` |
| `'Lightning'`, `'Eléctrica'` | `'Lightning'` |
| `'Psychic'`, `'Psíquica'` | `'Psychic'` |
| `'Fighting'`, `'Lucha'` | `'Fighting'` |
| `'Darkness'`, `'Siniestra'` | `'Darkness'` |
| `'Metal'`, `'Acero'` | `'Metal'` |
| `'Dragon'`, `'Dragón'` | `'Dragon'` |
| `'Colorless'`, `'Incolora'` | `'Colorless'` |

---

### getCheckEnergiasAtaque(ataque, activo)

**Obtener el estado de cobertura de cada energía requerida**

```typescript
getCheckEnergiasAtaque(ataque: any, activo: any): any[]
```

**Retorno**: Array de objetos con forma:
```typescript
{ tipo: string, cumplido: boolean }[]
```

**Uso típico**: Renderizar indicadores visuales (✅/❌) para cada energía del costo del ataque en la UI del tablero.

**Algoritmo**: Itera el `ataque.costo`, busca en `activo.energiasUnidas` de a uno (consumiendo matches para evitar doble conteo). Los Colorless siempre cuentan como cubiertos si hay alguna energía disponible.

---

### getFaltantesAtaque(ataque, activo)

**Obtener el resumen de energías que faltan para poder atacar**

```typescript
getFaltantesAtaque(ataque: any, activo: any): any[]
```

**Retorno**: Array agrupado por tipo:
```typescript
{ tipo: string, cantidad: number }[]
```

**Ejemplo**:
```typescript
// Costo requerido: ['Fire', 'Fire', 'Colorless']
// Energías unidas: [{ tipo: 'Fire' }]
// Resultado:
[
  { tipo: 'Fire', cantidad: 1 },
  { tipo: 'Colorless', cantidad: 1 }
]
```

**Algoritmo**: 
1. Resta tipos específicos de los poseídos
2. Resta Colorless usando sobrantes
3. Agrupa lo que queda por tipo y cuenta

---

## ⚠️ Tipado Débil

Los parámetros `ataque` y `activo` son tipados como `any`. Esto es intencional para tolerar múltiples formas del objeto de ataque que llegan del backend (campos como `texto`, `descripcion`, `efecto`, `danio`, `dano`).

---

## 🔗 Relaciones

- **Consumido por**: `BattleBoardCombatService` (usa `CoinFlipConfig`), componentes del tablero
- **Tipos exportados**: `CoinFlipConfig`
- **Ver también**: [06-battle-service.md](./06-battle-service.md), [11-battle-board-combat-service.md](./11-battle-board-combat-service.md)

---

*Próximo: [11-battle-board-combat-service.md](./11-battle-board-combat-service.md)*
