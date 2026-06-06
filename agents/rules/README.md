# Reglas del Proyecto

Este directorio contiene las reglas de arquitectura y convenciones que **todo desarrollador** debe respetar.

---

## Reglas Actuales

### [patrones-diseño.md](./patrones-diseño.md)
**Respetar los 4 patrones implementados — Chain of Responsibility, Strategy, Command, State.**

- Por qué son críticos
- Cómo usarlos correctamente
- Qué cambios rompen la arquitectura
- Cómo verificar en code review

**Lectura obligatoria antes de tocar** `service/battle/`, `model/battle/state/`

---

## Cómo Usar Esta Regla

### Para Desarrolladores
Antes de tocar `service/battle/`, `model/battle/state/`, o agregar nuevas acciones:
1. Lee [patrones-diseño.md](./patrones-diseño.md)
2. Identifica a qué patrón corresponde tu cambio (Chain, Strategy, Command o State)
3. Sigue el formato específico para ese patrón
4. Pide code review y apunta a esta regla

### Para Code Review
Cuando revises un PR que toca batalla:
1. Verifica la checklist de [patrones-diseño.md](./patrones-diseño.md#árbitro-cómo-verificar-que-se-respeta)
2. Si hay cambios que violen los patrones, rechaza con "violación de arquitectura"
3. Linkeá a esta regla en el comentario

---

## Próximas Reglas (A Agregar)

- Testing — cobertura mínima por patrón
- Naming — convenciones de nombres para comandos, estrategias, handlers
- Database — cuándo crear nuevas entidades vs usar JSONs
- Performance — limits en queries, n+1 detection
