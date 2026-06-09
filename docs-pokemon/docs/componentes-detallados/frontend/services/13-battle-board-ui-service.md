---
sidebar_position: 13
title: 🖥️ BattleBoardUiService
---

# 🖥️ BattleBoardUiService - Servicio de UI del Tablero

> Métodos de presentación: sprites, barras de HP, colores de energía, glosario de estados y formato de textos de ataque

---

## 📍 Ubicación

`frontend/src/app/features/battle/services/battle-board-ui.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class BattleBoardUiService {
  constructor(private sanitizer: DomSanitizer) {}
}
```

**Tipo**: Servicio raíz  
**Dependencias**: `DomSanitizer` (Angular)  
**Feature**: Battle (tablero de batalla)  
**Característica**: Solo lógica de presentación, sin HTTP ni estado de partida

---

## ⚠️ Tipado Débil

Varios métodos usan parámetros tipados como `any`. Esto es intencional para tolerar distintas formas del objeto carta que llegan de distintos contextos (tablero, mano, banca).

Métodos afectados: `extraerGlosario`, `getHpPercent`, `getHpMax`, `esEnergia`, `esPokemon`

---

## 📡 Métodos

### Tabla de Referencia Rápida

| Método | Firma | Retorno | Descripción |
|--------|-------|---------|-------------|
| `extraerGlosario` | `(carta: any)` | `any[]` | Extrae estados especiales del texto de ataques |
| `formatTextoAtaque` | `(texto: string)` | `SafeHtml` | Resalta keywords con spans CSS |
| `getSpriteBack` | `(nombreCarta: string)` | `string` | URL del sprite trasero animado |
| `getSpriteFront` | `(nombreCarta: string)` | `string` | URL del sprite frontal animado |
| `getHpPercent` | `(pokemon: any)` | `number` | Porcentaje de HP para barra de vida (0-100) |
| `getHpMax` | `(pokemon: any)` | `number` | HP máximo del Pokémon |
| `getImagenCarta` | `(id: string)` | `string` | Ruta pública de imagen de carta |
| `getEmptySlots` | `(n: number)` | `number[]` | Array de placeholders para banca |
| `esEnergia` | `(carta: any)` | `boolean` | Detecta si la carta es energía |
| `esPokemon` | `(carta: any)` | `boolean` | Detecta si la carta es Pokémon |
| `getEnergyName` | `(tipo: string)` | `string` | Nombre localizado del tipo de energía |
| `getEnergyColor` | `(tipo: string)` | `string` | Color hexadecimal del tipo de energía |

---

### extraerGlosario(carta)

**Extraer las condiciones especiales mencionadas en los ataques de una carta para mostrar como glosario**

```typescript
extraerGlosario(carta: any): any[]
```

**Retorno**: Array de `CardGlossaryEntry`:
```typescript
{ nombre: string, css: string, desc: string }[]
```

**Detección de condiciones**:

| Condición | CSS class | Descripción |
|-----------|-----------|-------------|
| Paralizado | `kw-paralyze` | El Pokémon no puede atacar ni retirarse este turno |
| Envenenado | `kw-poison` | Recibe 10 de daño entre turnos |
| Dormido | `kw-sleep` | No puede atacar ni retirar. Lanza moneda al final del turno para despertar |
| Confundido | `kw-confuse` | Lanza moneda al atacar. Si es cruz, falla y recibe 30 de daño |
| Quemado | `kw-burn` | Recibe 20 de daño entre turnos. Lanza moneda para curarse |

**Nota**: Detecta condiciones en inglés y español concatenando todos los textos de ataques de la carta.

---

### formatTextoAtaque(texto)

**Resaltar palabras clave dentro del texto de un ataque con spans HTML**

```typescript
formatTextoAtaque(texto: string): SafeHtml
```

**Retorno**: HTML sanitizado via `DomSanitizer.bypassSecurityTrustHtml`

**Palabras clave y sus clases CSS**:

| Pattern (regex, case-insensitive) | CSS class |
|-----------------------------------|-----------|
| paralyzed, paralyzes, paraliza, paralizado | `kw-paralyze` |
| poisoned, poisons, envenena, envenenado | `kw-poison` |
| asleep, sleeps, duerme, dormido | `kw-sleep` |
| confused, confuses, confunde, confundido | `kw-confuse` |
| burned, burns, quema, quemado | `kw-burn` |
| does nothing, no hace nada | `kw-neutral` |

**Ejemplo**:
```typescript
const html = this.uiService.formatTextoAtaque('The Defending Pokémon is now paralyzed.');
// '<span class="kw-paralyze">paralyzed</span>'
```

---

### getSpriteBack(nombreCarta)

**Obtener la URL del sprite trasero animado del Pokémon**

```typescript
getSpriteBack(nombreCarta: string): string
```

**Retorno**: `/sprites/pokemon/showdown/back/:num.gif` o `''` si no se encuentra el número de Pokédex

**Fuente de datos**: Diccionario privado `pokedexNum` con ~200 entradas de Gen 1-3 (más algunos extras)

---

### getSpriteFront(nombreCarta)

**Obtener la URL del sprite frontal animado del Pokémon**

```typescript
getSpriteFront(nombreCarta: string): string
```

**Retorno**: `/sprites/pokemon/showdown/:num.gif` o `''`

**Algoritmo de búsqueda** (método privado `getPokemonNum`):
1. Normaliza el nombre (minúsculas, sin acentos)
2. Busca en `pokedexNum` directamente
3. Si no encuentra, intenta con palabras del nombre en orden inverso
4. Si no encuentra, intenta parsear un número del nombre

---

### getHpPercent(pokemon)

**Calcular el porcentaje de HP para la barra de vida**

```typescript
getHpPercent(pokemon: any): number
```

**Retorno**: Entero entre 0 y 100 (clamp aplicado)  
**Fórmula**: `(hpActual / hpMax) * 100`

---

### getHpMax(pokemon)

**Obtener el HP máximo del Pokémon**

```typescript
getHpMax(pokemon: any): number
```

**Prioridad**: `pokemon.hpMax` → `parseInt(pokemon.card.hp)` → `100` (default)

---

### getImagenCarta(id)

**Construir la ruta pública de la imagen de una carta**

```typescript
getImagenCarta(id: string): string
```

**Retorno**: `/images/cards/:id.png`

*Nota*: El código tiene una rama `if (/^xy/i.test(id))` que retorna el mismo valor, lo que sugiere que en el futuro podría tener rutas diferenciadas por set.

---

### getEmptySlots(n)

**Generar un array de placeholders para completar los 5 slots de banca**

```typescript
getEmptySlots(n: number): number[]
```

**Parámetro**: `n` = cantidad de Pokémon actuales en banca  
**Retorno**: `Array(max(0, 5 - n)).fill(0)`

**Uso típico en template**:
```html
<div *ngFor="let slot of uiService.getEmptySlots(banca.length)" class="empty-slot"></div>
```

---

### esEnergia(carta)

```typescript
esEnergia(carta: any): boolean
```

**Retorno**: `carta?.supertype === 'Energy'`

---

### esPokemon(carta)

```typescript
esPokemon(carta: any): boolean
```

**Retorno**: `carta?.supertype === 'Pokémon' || carta?.supertype === 'Pokemon'`  
*(Tolera ambas grafías — con y sin acento)*

---

### getEnergyName(tipo)

**Obtener el nombre localizado del tipo de energía**

```typescript
getEnergyName(tipo: string): string
```

| `tipo` (input) | Nombre retornado |
|----------------|-----------------|
| `grass` | Planta |
| `fire` | Fuego |
| `water` | Agua |
| `lightning` | Electrico |
| `psychic` | Psiquico |
| `fighting` | Lucha |
| `darkness` | Siniestro |
| `metal` | Acero |
| `colorless` | Incolora |
| `fairy` | Hada |
| `dragon` | Dragon |
| (otro) | `'Energia'` |

**Normalización**: convierte a minúsculas antes de buscar.

---

### getEnergyColor(tipo)

**Obtener el color hexadecimal del tipo de energía para CSS**

```typescript
getEnergyColor(tipo: string): string
```

| `tipo` | Color |
|--------|-------|
| `grass` | `#78C850` |
| `fire` | `#F08030` |
| `water` | `#6890F0` |
| `lightning` | `#F8D030` |
| `psychic` | `#F85888` |
| `fighting` | `#C03028` |
| `darkness` | `#705848` |
| `metal` | `#B8B8D0` |
| `colorless` | `#A8A878` |
| `fairy` | `#EE99AC` |
| `dragon` | `#7038F8` |
| (otro) | `#A8A878` |

---

## 🔗 Relaciones

- **Depende de**: `DomSanitizer` (Angular core)
- **Tipos usados**: `SafeHtml` (Angular)
- **Consumido por**: Componentes del tablero de batalla (templates)
- **Ver también**: [06-battle-board-types.md](../types/06-battle-board-types.md)

---

*Próximo: [types/01-models.md](../types/01-models.md)*
