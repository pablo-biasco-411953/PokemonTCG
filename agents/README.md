# Agents — Guía Arquitectónica para Desarrolladores

Esta carpeta contiene las **reglas, convenciones y documentación arquitectónica** del proyecto. Es el lugar donde el equipo acuerda cómo se construye el código y por qué.

## 📖 Qué Encontrar Aquí

### [`rules/`](./rules/)
**Reglas arquitectónicas obligatorias.** Si las violas, code review te lo rechaza.

- **[patrones-diseño.md](./rules/patrones-diseño.md)** — Los 4 patrones que hacen que todo funcione
  - Por qué existen
  - Cómo usarlos
  - Qué NO hacer
  - Cómo verificar en code review

**Lectura obligatoria** si vas a tocar:
- `BE/src/main/java/com/pokemon/tcg/service/battle/`
- `BE/src/main/java/com/pokemon/tcg/model/battle/state/`

---

## 🎯 Para Nuevos Miembros del Equipo

1. Lee [reglas/patrones-diseño.md](./rules/patrones-diseño.md) completo
2. Mira la [documentación técnica](../BE/PATRONES_DISEÑO.md) para entender cómo se usa cada patrón
3. Lee el [plan de implementación](https://localhost) para ver cómo se construyeron
4. Pregunta dudas en el canal #architecture

---

## 🚀 Flujo de Desarrollo

### Agregando un Efecto de Ataque
→ Usa **Chain of Responsibility**
→ Nueva clase en `service/battle/chain/`
→ Registra en `CadenaAtaqueFactory`
→ Test unitario

Ver: [reglas/patrones-diseño.md#1-chain-of-responsibility](./rules/patrones-diseño.md#1-chain-of-responsibility)

### Agregando una Estrategia de Bot
→ Usa **Strategy**
→ Nueva clase en `service/battle/strategy/`
→ Cambiar una línea en `BotAIService`
→ Test de estrategia

Ver: [reglas/patrones-diseño.md#2-strategy](./rules/patrones-diseño.md#2-strategy)

### Agregando una Acción de Turno
→ Usa **Command**
→ Nueva clase en `service/battle/command/`
→ Usar `BattleEngineService.ejecutarComando()`
→ Test unitario

Ver: [reglas/patrones-diseño.md#3-command](./rules/patrones-diseño.md#3-command)

### Agregando una Fase o Cambio de Flujo
→ Usa **State**
→ Nueva clase en `model/battle/state/`
→ Usar `partida.transicionarA()`
→ **NUNCA** `setFaseActual()`

Ver: [reglas/patrones-diseño.md#4-state](./rules/patrones-diseño.md#4-state)

---

## ⚠️ Red Flags en Code Review

Si ves alguno de estos, rechaza el PR:

- ❌ Cambios en `BattleAttackService` (debe ser nuevo handler)
- ❌ Cambios en `BotAIService` además de cambiar `estrategia =` (debe ser nueva clase)
- ❌ Nuevo método público en `BattleEngineService` que ejecuta acciones (debe ser nuevo comando)
- ❌ Uso de `setFaseActual()` (debe ser `transicionarA()`)
- ❌ Ejecución de comando sin pasar por `ejecutarComando()` (salta el gate de estado)

---

## 📊 Documentación Técnica

Para entender **por qué** se eligió cada patrón, lee:
- [BE/PATRONES_DISEÑO.md](../BE/PATRONES_DISEÑO.md) — Explicación completa de cada patrón

---

## 🔄 Próximos Pasos para el Proyecto

- [ ] Frontend — aplicar los mismos patrones en Angular
- [ ] Testing — agregar cobertura mínima por patrón
- [ ] Monitoring — agregar logs de transiciones de estado
- [ ] Performance — profiling de cadena de handlers

---

## Contacto

Si tienes dudas sobre la arquitectura:
- Abre un issue con tag `[architecture]`
- Pregunta en `#architecture` del Discord
- Code review — pregunta en el PR

**Última actualización**: 2026-06-06  
**Responsable**: Benjamin Polzoni
