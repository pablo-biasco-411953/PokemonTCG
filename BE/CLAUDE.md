# Reglas para Agentes IA — Backend Pokemon TCG

## 1. Actualizar agent_logs después de cada commit

**OBLIGATORIO**: Inmediatamente después de hacer un `git commit`, el agente DEBE actualizar el archivo correspondiente en `agent_logs/` reflejando:

- Los archivos nuevos o modificados en ese commit
- Marcar como `[x]` las tareas completadas
- Agregar nuevas entradas en "Cambios Pendientes" si quedó trabajo incompleto
- Actualizar la línea `**Última actualización**` con la fecha y el hash del commit

**Ejemplo de flujo correcto:**
```
1. Escribir código
2. git add ...
3. git commit -m "..."   ← commit realizado
4. Actualizar agent_logs/task-*.md  ← OBLIGATORIO INMEDIATAMENTE
5. git add agent_logs/
6. git commit -m "docs: update agent log after <descripción>"
```

El log debe quedar en un commit separado inmediatamente después del commit de código.

---

## 2. Leer agent_logs al iniciar una sesión

Antes de empezar cualquier trabajo, leer el archivo relevante en `agent_logs/` para entender en qué estado quedó la tarea anterior.

---

## 3. Patrones técnicos obligatorios para tests

- **No usar `@SpringBootTest`** — todos los tests son unit tests puros con Mockito
- **Constructor de BattleEngineService**:
  ```java
  new BattleEngineService(
      mock(JugadorRepository.class),
      mock(MazoRepository.class),
      mock(CardRepository.class),
      mock(BotAIService.class),
      mock(BattleAttackService.class),
      mock(BattleKoService.class)
  )
  ```
- **Helper `cardBasico()`**: usar `setSupertype("Pokemon")` + `setSubtypes(List.of("Basic"))`, NO el campo `tipo`
- **Trainer cards**: la carta siempre va al descarte después de usarse (el `ComandoJugarTrainer` la mueve)
- **`new Partida(j, b)`** se inicializa con fase `INICIO`, no `null`
