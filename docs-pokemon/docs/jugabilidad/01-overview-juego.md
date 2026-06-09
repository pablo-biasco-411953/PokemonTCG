---
sidebar_position: 1
title: 🎮 Visión General del Juego
---

# 🎮 Visión General - ¿Qué es Pokémon TCG?

> Bienvenido a la documentación de Pokémon Trading Card Game - El juego de cartas más icónico del mundo

---

## 📖 ¿Qué es Pokémon TCG?

**Pokémon Trading Card Game (TCG)** es un juego de cartas estratégico donde:

- 🎮 **2 jugadores** se enfrentan con sus Pokémon
- 💪 Los Pokémon atacan usando **energía** como recurso
- 🏆 El objetivo es derrotar 6 Pokémon del oponente
- 🎁 Cada victoria da acceso a **mejores cartas**

Es un juego de:
- **Estrategia**: Decidir qué cartas jugar
- **Suerte**: Monerías y rarezas del mazo
- **Colección**: Buscar cartas raras y valiosas

---

## 🎯 Objetivo del Juego

**Gana cuando**:
✅ Tomas 6 **Premios** (derrotas 6 Pokémon del oponente)

**Pierdes cuando**:
❌ No tienes Pokémon en el banco (activo es derrotado)
❌ Tu mazo se agota (Decking)
❌ Rendirse voluntariamente

---

## 🎲 Conceptos Fundamentales (4 Pilares)

### 1️⃣ Pokémon (Las Criaturas)

- **Básico**: Entra directamente al juego
- **Etapa 1**: Evoluciona de un Básico
- **Etapa 2**: Evoluciona de un Etapa 1

Ejemplo:
```
Bulbasaur (Básico) → Ivysaur (Etapa 1) → Venusaur (Etapa 2)
```

Cada Pokémon tiene:
- **HP**: Puntos de salud (0 = Derrotado = KO)
- **Tipo**: Fuego, Agua, Planta, Eléctrico, etc (11 tipos totales)
- **Ataques**: Acciones que hacen daño
- **Habilidad**: Efecto especial pasivo

---

### 2️⃣ Energía (El Recurso)

Para atacar, necesitas **Energía** en tu Pokémon.

Los 11 tipos de Energía:
- 🔥 **Fuego**
- 💧 **Agua**
- 🌿 **Planta**
- ⚡ **Eléctrico**
- 👁️ **Psíquico**
- 👊 **Lucha**
- 🌑 **Oscuridad**
- ⚙️ **Metal**
- 🧚 **Hada**
- 🐉 **Dragón**
- ⭕ **Incolora** (universal)

---

### 3️⃣ Turno (El Ciclo)

Un **turno** tiene 4 fases:

```
┌─ FASE DE INICIO ─────────────────┐
│ • Sacar 1 carta del mazo         │
└─────────────────────────────────┘
        ↓
┌─ FASE PRINCIPAL ─────────────────┐
│ • Jugar 1 Pokémon (opcional)    │
│ • Unir 1 Energía (máximo)       │
│ • Usar Entrenadoras (sin límite) │
│ • Evolucionar (1 vez)           │
└─────────────────────────────────┘
        ↓
┌─ FASE DE ATAQUE ──────────────────┐
│ • Ejecutar 1 Ataque              │
│ • Aplicar daño y efectos         │
└─────────────────────────────────┘
        ↓
┌─ FASE DE LIMPIEZA ────────────────┐
│ • Fin de turno                    │
│ • Aplicar efectos persistentes    │
└─────────────────────────────────┘
```

:::tip Consejo del Entrenador
Cada fase tiene reglas específicas. Aprenderás a jugarlas naturalmente con la práctica.
:::

---

### 4️⃣ Mazo (El Deck)

Tu arsenal de 60 cartas:
- **Mínimo 20 energías** (recomendado)
- **Máximo 4 copias** de cada carta (excepto Básicas)
- **60 cartas exactas** (ni más, ni menos)

Composición típica:
- 15-20 Pokémon
- 25-30 Energías
- 15-20 Entrenadoras

---

## 🏃 Flujo Básico de una Batalla

```
1. INICIO
   └─ Ambos jugadores barajan sus mazos
   
2. MULLIGAN (Redraw)
   ├─ Jugador 1 saca 7 cartas
   ├─ Si no hay Pokémon, puede devolver y redibujar
   └─ Jugador 2 repite
   
3. PRIMEROS TURNOS
   ├─ Jugador 1 juega su Pokémon y pone en banco
   ├─ Jugador 2 igual
   ├─ Jugador 1 hace su primer turno (NO PUEDE ATACAR)
   └─ Jugador 2 hace su turno (PUEDE ATACAR)
   
4. BATALLA CONTINÚA
   ├─ Turno 3+: Ambos pueden atacar
   ├─ Cada ataque hace daño
   ├─ Si HP llega a 0 → Pokémon KO
   ├─ Derrotar Pokémon = Tomar 1 Premio
   └─ Primer en 6 Premios → GANA
```

---

## 🎮 Pantallas Principales

El juego digital tiene estas pantallas:

### 1. **Lobby** 🏠
```
┌─────────────────────────────┐
│   POKÉMON TCG LOBBY         │
├─────────────────────────────┤
│                             │
│  [👤 Mi Perfil]             │
│  [🎁 Abrir Sobres]           │
│  [🛠️ Construir Mazo]        │
│  [⚔️ Buscar Batalla]         │
│                             │
└─────────────────────────────┘
```

---

### 2. **Tablero de Batalla** ⚔️

```
┌────────────────────────────────────────┐
│        TABLERO DE BATALLA              │
├─ OPONENTE ──────────────────────── HP 60 ┤
│                                         │
│  [🟩] [🟩] [🟩] [🟩] [🟩] BENCH (5)  │
│                   ┌─────────────┐      │
│                   │  Pokémon    │      │
│                   │  Activo     │      │
│                   │   HP 30     │      │
│                   └─────────────┘      │
│                                         │
├────────────────────────────────────────┤
│                                         │
│                   ┌─────────────┐      │
│                   │  Mi Pokémon │      │
│                   │   HP 60     │      │
│                   └─────────────┘      │
│  [🟩] [🟩] [ ] [ ] [ ] MI BENCH     │
│                                         │
├─ YO ───────────────────────────── HP 60┤
│ Mano: 8 cartas | Mazo: 32            │
│ [🎴] [⚡] [⚔️] [🎁] [PASAR TURNO] │
└────────────────────────────────────────┘
```

---

### 3. **Constructor de Mazo** 🛠️

```
┌──────────────────────────┐
│   CONSTRUCTOR DE MAZO    │
├──────────────────────────┤
│                          │
│  Nombre: Mi Mazo Fuego  │
│  Cartas: 60/60 ✅        │
│                          │
│  [Buscar]  [Validar]    │
│                          │
│  Pokémon (15)            │
│  ├─ Charmander x4        │
│  ├─ Charmeleon x2        │
│  └─ Charizard x1         │
│                          │
│  Energías (25)           │
│  └─ Fuego x25            │
│                          │
│  Entrenadoras (20)       │
│  ├─ Poción x3            │
│  └─ ...                  │
│                          │
│  [GUARDAR] [CANCELAR]   │
└──────────────────────────┘
```

---

### 4. **Apertura de Sobres** 🎁

```
┌─────────────────────────┐
│    ABRIR SOBRE          │
├─────────────────────────┤
│                         │
│   🎁 Sobre de Cartas   │
│   Precio: 100 Monedas  │
│                         │
│   [💰 COMPRAR]         │
│                         │
│   Garantías:            │
│   • 1 Rara Garantizada  │
│   • 2 No Comunes        │
│   • 8 Comunes           │
│                         │
└─────────────────────────┘

DESPUÉS DE ABRIR:
┌─────────────────────────┐
│ [🎴] [🎴] [🎴]...      │
│ (10 cartas reveladas)   │
│                         │
│ [AÑADIR AL MAZO] [OK]  │
└─────────────────────────┘
```

---

## 🎯 Flujo de Juego Típico

### Sesión 1: Entrenador Novato
1. Registrarse / Login
2. Tutorial (opcional)
3. Abrir 3 sobres iniciales (Starter Pack)
4. Construir primer mazo
5. Jugar batalla práctica contra IA

### Sesión 2+: Entrenador Veterano
1. Abrir sobres
2. Ajustar mazo
3. Buscar batalla (Ranked o Casual)
4. Jugar 1-3 batallas
5. Ganar recompensas (monedas, cartas)

---

## 📊 Reglas de Construcción de Mazo

| Regla | Requisito |
|-------|-----------|
| **Total de cartas** | Exactamente 60 |
| **Máximo de copias** | 4 de la misma (excepto Energía) |
| **Mínimo energías** | 20 (recomendado) |
| **Máximo energías** | Ilimitado (pero limita Pokémon) |
| **Pokémon básicos** | Mínimo 1 (no hay límite máximo) |

### ❌ Mazo INVÁLIDO
```
• 55 cartas (no es 60)
• 5 copias de Pikachu (máximo 4)
• 0 energías (necesitas al menos 20)
• Cartas no disponibles en el formato
```

### ✅ Mazo VÁLIDO
```
• Exactamente 60 cartas
• Máximo 4 Pikachu
• 25 Energías Eléctrico
• 20 Pokémon
• 15 Entrenadoras
```

---

## 🎨 Colores y Efectos de Tipos

Los 11 tipos tienen colores y efectividades:

```
🔥 FUEGO          Efectivo contra: Planta, Metal
   Débil a: Agua, Lucha, Tierra

💧 AGUA           Efectivo contra: Fuego, Tierra
   Débil a: Planta, Eléctrico

🌿 PLANTA         Efectivo contra: Agua, Tierra
   Débil a: Fuego, Hielo, Veneno

⚡ ELÉCTRICO      Efectivo contra: Agua, Volador
   Débil a: Tierra

👁️ PSÍQUICO       Efectivo contra: Lucha, Veneno
   Débil a: Oscuridad, Fantasma

👊 LUCHA          Efectivo contra: Normal, Hielo, Metal
   Débil a: Volador, Psíquico, Hada

🌑 OSCURIDAD      Efectivo contra: Psíquico, Fantasma
   Débil a: Lucha, Hada

⚙️ METAL          Efectivo contra: Hada, Hielo, Normal
   Débil a: Fuego, Lucha, Tierra

🧚 HADA           Efectivo contra: Oscuridad, Lucha
   Débil a: Metal, Veneno

🐉 DRAGÓN         Efectivo contra: Dragón
   Débil a: Dragón, Hielo

⭕ INCOLORA       Efectivo contra: Nada
   Débil a: Nada (Universal)
```

---

## 🏆 Sistema de Ranking (Futuro)

*(Implementado en futuras versiones)*

- **ELO Rating**: Puntaje que sube/baja con victorias/derrotas
- **Ligas**: Bronce, Plata, Oro, Platino, Diamante
- **Recompensas**: Mejores premios en ligas superiores

---

## 📚 Próximos Pasos

Ahora que entiendes la visión general:

1. [Lee Mecánicas Básicas](/docs/jugabilidad/mecanicas-basicas) - Entiende cómo jugar
2. [Lee Tipos y Energía](/docs/jugabilidad/cartas-tipos-energia) - Sistemas de tipos
3. [Lee Construcción de Mazos](/docs/jugabilidad/construccion-mazos) - Crea tu primer deck

---

## 🎓 Resumen en 30 segundos

> Dos jugadores con 60 cartas se enfrentan. Juegan Pokémon (criaturas), les dan Energía (recurso) y atacan. Quien derrote 6 Pokémon enemigos gana. Hay elementos aleatorios (sobres, cartas), estrategia (qué jugar) y colección.

:::success ¡Estás listo!
Ya entiendes el concepto básico. Continúa aprendiendo los detalles.

**¡Adelante, Entrenador!** ⚡
:::

---

*Última actualización: 2026-06-08*
