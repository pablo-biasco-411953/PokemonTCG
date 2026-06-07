# AI Agents — Project Guidelines

Esta carpeta contiene las reglas, contexto y skills que cualquier agente de IA debe seguir al trabajar en este proyecto.

**Es tool-agnóstica**: funciona con Claude, Codex, Antigravity, Cursor, Copilot, o cualquier otro agente.

---

## Estructura

```
agents/
├── rules/          ← Reglas de comportamiento del agente
│   ├── general.md      - Reglas transversales a todo el proyecto
│   ├── backend.md      - Reglas específicas del BE (Java/Spring Boot)
│   └── frontend.md     - Reglas específicas del FE (Angular/TypeScript)
├── skills/         ← Conocimiento de dominio del proyecto
│   ├── battle-engine.md    - Cómo funciona el motor de batalla
│   ├── deck-builder.md     - Cómo funciona el armado de mazos
│   └── testing.md          - Patrones de test usados en el proyecto
└── context/        ← Contexto técnico general
    ├── architecture.md     - Arquitectura del sistema
    ├── stack.md            - Stack tecnológico y versiones
    └── conventions.md      - Convenciones de código y naming
```

---

## Cómo usar con tu agente

### Claude / Claude Code
Copiá el contenido relevante al inicio de tu conversación, o referenciá los archivos directamente.
Claude Code carga automáticamente `CLAUDE.md` si existe en la raíz — podés symlinkearlo a `agents/rules/general.md`.

### GitHub Copilot / Codex
Adjuntá los archivos como contexto adicional en tu prompt, o usá la funcionalidad de "custom instructions" de tu herramienta.

### Cursor
Usá `.cursorrules` en la raíz y pegá el contenido de `rules/general.md`. Cursor también soporta adjuntar archivos como contexto.

### Antigravity / otros
Incluí los archivos de `rules/` y `context/` como system prompt o contexto inicial de la sesión.

---

## Regla de oro

**Si aprendiste algo nuevo sobre el proyecto que no está documentado aquí, agregalo.**
Esta carpeta solo es útil si se mantiene actualizada.
