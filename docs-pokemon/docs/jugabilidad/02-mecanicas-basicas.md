---
sidebar_position: 2
title: 🎮 Mecánicas Básicas
---

# 🎮 Mecánicas Básicas - Los 4 Pilares del Juego

> Aprende los conceptos fundamentales que hacen que Pokémon TCG sea único

---

## 📍 Los 4 Elementos Clave

### 1. Pokémon (Criaturas)

El corazón del juego. Hay **3 categorías**:

```
Bulbasaur (Básico)
     ↓
Ivysaur (Etapa 1) 
     ↓
Venusaur (Etapa 2)
```

**Atributos de un Pokémon**:
- **HP** (Hit Points): Salud. Llega a 0 = Derrotado (KO)
- **Tipo**: Uno de los 11 tipos (Fuego, Agua, etc)
- **Ataques**: Movimientos que hacen daño
- **Habilidad**: Efecto especial del Pokémon
- **Debilidad**: Tipo que hace daño extra (2x)
- **Resistencia**: Tipo que hace menos daño

---

### 2. Energía (Recurso)

Para atacar, **necesitas energía**. Es como el "combustible" del Pokémon.

**Tipos de Energía**:
| Tipo | Símbolo | Efectividad |
|------|---------|------------|
| 🔥 Fuego | Fire | Planta, Metal |
| 💧 Agua | Water | Fuego, Tierra |
| 🌿 Planta | Grass | Agua, Tierra |
| ⚡ Eléctrico | Electric | Agua, Volador |
| 👁️ Psíquico | Psychic | Lucha, Veneno |
| 👊 Lucha | Fighting | Normal, Hielo, Metal |
| 🌑 Oscuridad | Dark | Psíquico, Fantasma |
| ⚙️ Metal | Steel | Hada, Hielo, Normal |
| 🧚 Hada | Fairy | Oscuridad, Lucha |
| 🐉 Dragón | Dragon | Dragón |
| ⭕ Incolora | Colorless | Universal (funciona para todo) |

:::tip Consejo
La **Energía Incolora** es universal - funciona como cualquier tipo. Ideal para flexibilidad.
:::

---

### 3. El Tablero (Field)

Cada jugador tiene un "tablero" con estas zonas:

```
┌─────────────────────────────────────────┐
│          OPONENTE (Ejemplo)             │
├─────────────────────────────────────────┤
│ Bench (Banco) - Hasta 5 Pokémon        │
│ [🟩] [🟩] [🟩] [🟩] [🟩]           │
│                                         │
│     ┌──────────────────────┐           │
│     │  Pokémon Activo      │           │
│     │  en Batalla          │           │
│     │  HP: 60              │           │
│     └──────────────────────┘           │
├─────────────────────────────────────────┤
│                                         │
│     ┌──────────────────────┐           │
│     │  Mi Pokémon          │           │
│     │  en Batalla          │           │
│     │  HP: 30              │           │
│     └──────────────────────┘           │
│                                         │
│ Bench (Banco) - Hasta 5 Pokémon        │
│ [🟩] [🟩] [ ] [ ] [ ]               │
├─────────────────────────────────────────┤
│          YO (Jugador)                  │
└─────────────────────────────────────────┘
```

**Zonas del Tablero**:
- **Pokémon Activo**: El que está en batalla (1 máximo)
- **Bench**: Zona de espera (5 espacios máximo)
- **Mazo**: Tus 60 cartas sin revelar
- **Descarte**: Cartas descartadas (pila pública)
- **Mano**: Cartas en tu poder (privado)
- **Premios**: Cartas que ganas al derrotar Pokémon (6 máximo)

---

### 4. El Turno (Ciclo de Juego)

Un **turno completo** tiene 4 fases:

```
┌──────────────────────────────────┐
│  FASE 1: INICIO                 │
│  • Sacar 1 carta del mazo       │
│  • Aplicar efectos persistentes  │
└──────────────────────────────────┘
           ↓
┌──────────────────────────────────┐
│  FASE 2: PRINCIPAL              │
│  • Jugar 1 Pokémon (opcional)   │
│  • Unir 1 Energía (máximo)      │
│  • Evolucionar (1 vez)           │
│  • Usar Entrenadoras             │
│  • Cambiar Pokémon activo        │
└──────────────────────────────────┘
           ↓
┌──────────────────────────────────┐
│  FASE 3: ATAQUE                 │
│  • Ejecutar 1 ataque             │
│  • Aplicar daño y efectos        │
│  • El oponente descarta cartas   │
└──────────────────────────────────┘
           ↓
┌──────────────────────────────────┐
│  FASE 4: LIMPIEZA               │
│  • Fin del turno                │
│  • Aplicar efectos fin de turno   │
│  • Turno pasa al oponente        │
└──────────────────────────────────┘
```

---

## 🎯 Ciclo Completo de una Batalla

### Ronda 1

```
1. SETUP INICIAL
   └─ Ambos jugadores:
      • Barajan el mazo
      • Sacan 7 cartas
      • Si no hay Pokémon básico → mulligan (redraw)
   
2. DECISIÓN DE PRIMER TURNO
   └─ Se sortea quién va primero
   
3. PRIMER TURNO (Jugador 1)
   └─ Acciones:
      ✅ Jugar Pokémon básico
      ✅ Unir 1 energía
      ✅ Jugar Entrenadoras
      ❌ NO PUEDE ATACAR (restricción)
      
4. SEGUNDO TURNO (Jugador 2)
   └─ Acciones:
      ✅ Jugar Pokémon básico
      ✅ Unir 1 energía
      ✅ Jugar Entrenadoras
      ✅ PUEDE ATACAR si tiene energía
```

### Rondas Siguientes

```
Turno 3+: BATALLA NORMAL
├─ Ambos jugadores pueden atacar
├─ Cada ataque hace daño
├─ Si HP llega a 0 → KO (Pokémon descartado)
├─ Derrotar Pokémon = Tomar 1 Premio
└─ Ganar = Primer en 6 Premios
```

---

## 🏃 Flujo de Acción - Paso a Paso

### Ejemplo: Turno de Juego Real

**Estado inicial**:
- Yo tengo: Charmander (40/40 HP) + Energía Fuego
- Oponente tiene: Squirtle (40/40 HP) + Energía Agua

**Mi Turno**:

```
FASE 1: INICIO
└─ Saco 1 carta del mazo
   Mazo: 59 cartas restantes

FASE 2: PRINCIPAL
├─ Juego Poción (cura 20 HP) ✅
│  Charmander: 40 → 60 HP
├─ Unir Energía Fuego ✅
│  Charmander: Energía 1/1
└─ No evoluciono

FASE 3: ATAQUE
├─ Selecciono ataque: "Flame Charge"
│  Costo: 1 Energía Fuego ✅
├─ Squirtle recibe daño: 30
│  Squirtle: 40 → 10 HP
└─ Efecto: +1 Energía a Charmander siguiente turno

FASE 4: LIMPIEZA
├─ Aplico efectos fin de turno
└─ Turno pasa a oponente
```

**Turno del Oponente**:
```
FASE 1: INICIO
└─ Saca carta

FASE 2: PRINCIPAL
└─ Unir Energía Agua

FASE 3: ATAQUE
├─ Ataque: "Bubble Beam"
│  Costo: 1 Energía Agua
├─ Charmander recibe daño: 40
│  Charmander: 60 → 20 HP
└─ Efecto adicional: Parálisis

FASE 4: LIMPIEZA
└─ Fin del turno
```

---

## 🎲 Elementos de Azar

### Monedas de Suerte

Muchos ataques requieren "lanzar una moneda":

```
"Lanza una moneda. Si es cara: efecto especial"

Resultado:
• Cara (Heads):   ✅ Efecto ocurre
• Cruz (Tails):  ❌ Efecto no ocurre
```

### Búsqueda de Cartas

Algunos ataques permiten "buscar cartas del mazo":

```
Buscar: "Selecciona hasta 3 Energías de tu mazo
         y únelas a este Pokémon.
         Baraja el resto."
```

---

## 📊 Estados Especiales

Los Pokémon pueden estar en **estados** que afectan su comportamiento:

| Estado | Efecto | Duración |
|--------|--------|----------|
| 🔥 **Quemadura** | -10 HP al final del turno | Persistente |
| 💧 **Envenenamiento** | -10 HP al final del turno | Persistente |
| ⚡ **Parálisis** | 50% de no poder atacar | Persistente |
| 😴 **Sueño** | No puede atacar, lanza moneda para despertar | Persistente |
| 😵 **Confusión** | 50% de no poder atacar | Persistente |

:::info Nota
Los estados **no se cumulan**. Si un Pokémon ya está quemado, no puede estar envenenado al mismo tiempo (en la mayoría de formatos).
:::

---

## 🎯 Objetivo del Juego

**GANA**: Primer jugador en tomar **6 Premios**

Los **Premios** se toman cuando derrotas un Pokémon del oponente.

**Ejemplo progreso**:
```
Turno 5: Derroto Charmander     → 1 Premio (1/6)
Turno 8: Derroto Charmeleon     → 2 Premios (2/6)
Turno 12: Derroto Charizard     → 3 Premios (3/6)
Turno 15: Derroto Squirtle      → 4 Premios (4/6)
Turno 18: Derroto Psyduck       → 5 Premios (5/6)
Turno 21: Derroto Poliwag       → 6 Premios (6/6) ✅ GANASTE!
```

---

## ❌ Formas de Perder

1. **Oponente toma 6 Premios** - Ha derrotado 6 de tus Pokémon
2. **Decking** - Tu mazo se agota y no puedes sacar cartas
3. **Sin Pokémon** - Tu Pokémon activo es derrotado y no tienes bench
4. **Rendición voluntaria** - Decides abandonar la batalla

---

## 🎓 Resumen de Mecánicas

```
BÁSICO:
├─ Pokémon + Energía = Capacidad de atacar
├─ Turnos = Fases organizadas
├─ Daño = Resta HP
├─ KO = Derrotar Pokémon
└─ Premios = Meta del juego

ESTRATEGIA:
├─ Qué Pokémon jugar
├─ Cuándo evolucionar
├─ Dónde poner energía
├─ Qué Entrenadoras usar
└─ Timing del ataque

SUERTE:
├─ Monedas (50/50)
├─ Cartas del mazo (¿qué sacas?)
└─ Orden de cartas (shuffled)
```

---

## ⚡ Próximos Pasos

1. [Lee Cartas, Tipos y Energía](/docs/jugabilidad/cartas-tipos-energia)
2. [Lee Evolución de Pokémon](/docs/jugabilidad/evolucion-pokemon)
3. [Lee Construcción de Mazos](/docs/jugabilidad/construccion-mazos)

:::success Felicidades
Ya entiendes las mecánicas básicas. ¡El resto del juego se construye sobre estos conceptos!

**¡Que comience la batalla!** ⚡
:::

---

*Última actualización: 2026-06-08*
