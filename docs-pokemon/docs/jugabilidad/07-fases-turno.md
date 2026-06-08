---
sidebar_position: 7
title: 🔄 Fases del Turno - Desglose Completo
---

# 🔄 Fases del Turno - Estructura Exacta

> Paso a paso de cada fase del turno

---

## 4 Fases = 1 Turno Completo

### FASE 1: INICIO
```
1. Sacar 1 carta del mazo
2. Aplicar efectos persistentes
3. Limpiar condiciones si corresponde

DURACIÓN: 30 segundos
```

### FASE 2: PRINCIPAL
```
Puedes hacer (en orden):
1. Jugar 1 Pokémon Básico
2. Unir 1 Energía
3. Usar Entrenadoras (sin límite)
4. Cambiar Pokémon activo
5. Evolucionar (1 vez)

NO puedes atacar en FASE PRINCIPAL.
```

### FASE 3: ATAQUE
```
1. Ejecutar exactamente 1 Ataque
2. Seleccionar Pokémon objetivo
3. Aplicar daño
4. Aplicar efectos secundarios
5. Oponente descarta si corresponde

NO puedes atacar si:
- Tu Pokémon no tiene energía
- Es tu primer turno
```

### FASE 4: LIMPIEZA
```
1. Fin del turno
2. Aplicar efectos de fin de turno
3. Pasar turno a oponente
```

## 📝 Ejemplo: Turno Completo Real

```
INICIO:
├─ Saco 1 carta
├─ Mazo: 59 cartas
└─ Paso a Fase Principal

PRINCIPAL:
├─ Juego Charmander (básico)
├─ Unir 1 Energía Fuego
├─ Juego 2 Pociones
├─ Mi mazo: -2 cartas
└─ Paso a Fase Ataque

ATAQUE:
├─ Ejecuto "Ember" (30 daño)
├─ Squirtle: 40 → 10 HP
└─ Paso a Fase Limpieza

LIMPIEZA:
├─ Aplicar fin de turno
└─ Turno al oponente ✅
```

---

*Próximo: [Efectos y Habilidades](/docs/jugabilidad/efectos-habilidades)*
