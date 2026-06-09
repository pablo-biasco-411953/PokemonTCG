---
sidebar_position: 3
title: 🃏 Cartas, Tipos y Energía
---

# 🃏 Cartas, Tipos y Energía - Todo lo que Necesitas Saber

> Guía completa sobre los diferentes tipos de cartas y sus características

---

## 📋 Tipos de Cartas

Hay **3 categorías principales** de cartas:

### 1. 🟩 Pokémon

Las criaturas del juego. Cada carta tiene:

```
┌─────────────────────────────┐
│         Charmander          │  ← Nombre
│                             │
│     [Fuego/Fire]            │  ← Tipo
│          🔥                 │
│                             │
│     HP: 50                  │  ← Salud
│                             │
│  Habilidad:                 │  ← Efecto especial
│  [Quema Activa]             │
│    Efecto...                │
│                             │
│  Ataque:                    │  ← Movimiento
│  [Ember] (1 Energía Fuego)  │  Costo:  energía
│  Daño: 30                   │  Efecto: ...
│                             │
│  Debilidad: 💧 (+20)        │  ← Efectos especiales
│  Resistencia: —             │
│  Costo de Retirada: 1       │
│                             │
│  Evoluciona de: Pichu       │
└─────────────────────────────┘
```

**Tres Categorías**:

| Categoría | Descripción | Ejemplo |
|-----------|------------|---------|
| **Básico (Etapa 0)** | Entra directamente al juego | Pikachu, Charmander |
| **Etapa 1** | Evoluciona de Básico | Charmeleon, Pikachu → Raichu |
| **Etapa 2** | Evoluciona de Etapa 1 | Charizard, Raichu → Mega Raichu |

---

### 2. ⚡ Energía

Cartas de recurso. Sin energía, **no puedes atacar**.

```
┌─────────────────┐
│  ENERGÍA FUEGO  │
│       🔥        │  ← Tipo
│                 │
│ Proporciona    │
│ 1 Energía      │
│ de tipo Fuego  │
└─────────────────┘
```

**Los 11 Tipos de Energía**:

```
Energía = Tipo que proporciona
```

| Tipo | Símbolo | Color | Efectivo Contra |
|------|---------|-------|-----------------|
| **Fuego** | 🔥 | Rojo | Planta, Metal |
| **Agua** | 💧 | Azul | Fuego, Tierra |
| **Planta** | 🌿 | Verde | Agua, Tierra |
| **Eléctrico** | ⚡ | Amarillo | Agua, Volador |
| **Psíquico** | 👁️ | Morado | Lucha, Veneno |
| **Lucha** | 👊 | Marrón | Normal, Hielo, Metal |
| **Oscuridad** | 🌑 | Negro | Psíquico, Fantasma |
| **Metal** | ⚙️ | Gris | Hada, Hielo, Normal |
| **Hada** | 🧚 | Rosa | Oscuridad, Lucha |
| **Dragón** | 🐉 | Multicolor | Dragón |
| **Incolora** | ⭕ | Gris claro | **Todas** (Universal) |

:::tip Consejo Estratégico
La **Energía Incolora** es comodín universal. Úsala para flexibilidad en mazos mixtos.
:::

---

### 3. 🎁 Entrenador

Cartas de efecto especial. Se dividen en 3 subtipos:

#### **Objeto**
Se juega, resuelve su efecto y se descarta.

```
┌──────────────────────┐
│      POCIÓN          │
│                      │
│ Restaura 20 HP       │
│ a un Pokémon.        │
│                      │
│ [JUGAR Y DESCARTAR]  │
└──────────────────────┘
```

#### **Soportador (Support)**
Permanece en juego mientras proporcionan efecto continuo.

```
┌──────────────────────┐
│   PROFESOR ROBLE     │
│  (Supporter Card)    │
│                      │
│ Efecto al entrar:    │
│ • Saca 3 cartas      │
│ • Oponente saca 3    │
│                      │
│ Permanece en juego   │
└──────────────────────┘
```

#### **Estadio**
Permanece en juego y afecta a **ambos jugadores**.

```
┌──────────────────────┐
│   ESTADIO FUEGO      │
│  (Stadium Card)      │
│                      │
│ Efecto persistente:  │
│ Los Pokémon Fuego   │
│ hacen +10 daño      │
│ (Ambos lados)       │
│                      │
│ Reemplaza anterior   │
└──────────────────────┘
```

---

## 🎯 Atributos de Pokémon

### HP (Hit Points)

**Puntos de Salud del Pokémon**. Ejemplos:

```
HP 30  = Débil, muere rápido
HP 60  = Promedio
HP 120 = Fuerte, tankea daño
HP 200 = Legendario
```

Llega a 0 = **KO** (Pokémon descartado, tomas 1 premio).

---

### Tipo

**Determina efectividades**. Cada Pokémon tiene 1 tipo:

```
Ejemplo:
Charmander = Fuego 🔥
Squirtle   = Agua 💧
Bulbasaur  = Planta 🌿
```

---

### Debilidad

**Tipo que hace daño extra** (generalmente 2x).

```
Ejemplo:
┌─────────────────┐
│  Charmander     │
│  Tipo: Fuego 🔥 │
│  HP: 50         │
│                 │
│ Debilidad:      │
│ Agua 💧 (+20)    │
└─────────────────┘

Ataque de agua hace:
30 daño base + 20 debilidad = 50 daño total
```

---

### Resistencia

**Tipo que hace daño menos** (generalmente -20).

```
Ejemplo:
┌──────────────────┐
│  Charmander      │
│  Tipo: Fuego 🔥  │
│  HP: 50          │
│                  │
│ Resistencia:     │
│ Planta 🌿 (-20)   │
└──────────────────┘

Ataque de planta hace:
40 daño base - 20 resistencia = 20 daño total
```

---

### Costo de Retirada

**Energía que cuesta cambiar este Pokémon por uno del banco**.

```
Costo de Retirada: 2

Para cambiar:
1. Discard 2 Energías del Pokémon
2. Pokémon va al Bench
3. Juega otro del Bench
```

---

## 📊 Tabla de Efectividades

Referencia rápida de qué tipo es efectivo contra cuál:

```
FUEGO 🔥
├─ Efectivo: Planta, Metal
└─ Débil: Agua, Lucha, Tierra

AGUA 💧
├─ Efectivo: Fuego, Tierra
└─ Débil: Planta, Eléctrico

PLANTA 🌿
├─ Efectivo: Agua, Tierra
└─ Débil: Fuego, Hielo, Veneno

ELÉCTRICO ⚡
├─ Efectivo: Agua, Volador
└─ Débil: Tierra

PSÍQUICO 👁️
├─ Efectivo: Lucha, Veneno
└─ Débil: Oscuridad, Fantasma

LUCHA 👊
├─ Efectivo: Normal, Hielo, Metal
└─ Débil: Volador, Psíquico, Hada

OSCURIDAD 🌑
├─ Efectivo: Psíquico, Fantasma
└─ Débil: Lucha, Hada

METAL ⚙️
├─ Efectivo: Hada, Hielo, Normal
└─ Débil: Fuego, Lucha, Tierra

HADA 🧚
├─ Efectivo: Oscuridad, Lucha
└─ Débil: Metal, Veneno

DRAGÓN 🐉
├─ Efectivo: Dragón
└─ Débil: Dragón, Hielo

INCOLORA ⭕
├─ Efectivo: Nada (neutral)
└─ Débil: Nada (neutral)
```

---

## 🎨 Rareza de Cartas

Las cartas tienen **símbolos de rareza** que determinan su escasez:

| Símbolo | Rareza | Frecuencia |
|---------|--------|-----------|
| ◯ | Común | Muy frecuente |
| ◇ | No Común | Frecuente |
| ★ | Rara | Poco frecuente |
| ★H | Holo Rara | Rara + brillo especial |
| ★★ | Ultra Rara | Muy rara |
| ☆ | Secret Rare | Coleccionista |

:::info Colección
Las cartas **raras brillosas (Holo)** son más valiosas para colección pero tienen el mismo efecto de juego que la versión no-brillo.
:::

---

## ⚡ Energía en el Juego

### Cómo Funciona

```
Pokémon necesita energía para atacar:

Ataque Ejemplo:
"Flame Charge"
Costo: 1 Energía Fuego 🔥

Para atacar:
✅ Tienes 1 Energía Fuego en Charmander
✅ Ejecutas "Flame Charge"
❌ Si no tienes energía, NO puedes atacar
```

### Unir Energía (Main Action)

Solo puedes unir **1 energía por turno** en fase principal.

```
Turno 1: Unir 1 Energía → Total 1
Turno 2: Unir 1 Energía → Total 2
Turno 3: Unir 1 Energía → Total 3
         (Ahora tienes energía para "Flame Charge" que cuesta 3)
```

### Energía Incolora

**Universal** - funciona como cualquier tipo.

```
Ataque necesita: 1 Fuego + 1 Incolora

Satisfecho por:
• 1 Energía Fuego + 1 Energía Incolora ✅
• 1 Energía Fuego + 1 Energía Agua (como incolora) ✅
```

---

## 🎓 Resumen Visual

```
Carta Pokémon = Criatura
  ├─ HP: Salud
  ├─ Tipo: Fuego/Agua/etc
  ├─ Ataques: Coste + Daño
  ├─ Habilidad: Efecto especial
  └─ Debilidad/Resistencia

Carta Energía = Recurso
  └─ Proporciona 1 energía de su tipo

Carta Entrenador = Efecto
  ├─ Objeto: Descarta tras usarse
  ├─ Soportador: Permanece
  └─ Estadio: Permanece, afecta a ambos
```

---

## 📚 Próximos Pasos

1. [Lee Evolución de Pokémon](/docs/jugabilidad/evolucion-pokemon)
2. [Lee Construcción de Mazos](/docs/jugabilidad/construccion-mazos)
3. [Lee Reglas de Batalla](/docs/jugabilidad/batalla-reglas)

:::success ¡Listo!
Ya entiendes qué cartas hay y cómo funcionan. Ahora necesitas saber cómo **combinarlas en un mazo ganador**.

**¡Eres un entrenador cada vez más sabio!** ⚡
:::

---

*Última actualización: 2026-06-08*
