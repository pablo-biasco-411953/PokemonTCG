# Registro de Tareas del Agente (Frontend)

Esta carpeta se utiliza para que todos los agentes de IA que trabajen en el **Frontend (FE)** registren su progreso, los cambios realizados y cualquier tarea que haya quedado pendiente o a medio camino.

## Convención de Uso

1. **Antes de empezar**: Lee el archivo de la última tarea en esta carpeta para entender en qué estado quedó el trabajo.
2. **Durante el desarrollo**: Registra los pasos, archivos modificados y decisiones tomadas.
3. **Antes de finalizar / pausar**: Actualiza o crea el archivo correspondiente a la tarea detallando qué quedó pendiente y cuáles son los siguientes pasos.

## Plantilla de Tarea

Crea un archivo llamado `task-[nombre-de-la-tarea].md` (ej: `task-fix-login-ui.md`) con la siguiente estructura:

```markdown
# Tarea: [Nombre de la Tarea]

- **Fecha/Hora**: YYYY-MM-DD HH:MM
- **Estado**: [En progreso / Pausado / Completado]

## Cambios Realizados
- [ ] **[MODIFY / NEW / DELETE] [archivo]**: Breve descripción del cambio.

## Cambios Pendientes (A la mitad o pendientes)
- [ ] Detalle de lo que falta implementar o testear.
- [ ] Posibles problemas conocidos o bugs sin resolver.

## Próximos Pasos Recomendados
1. Primer paso recomendado para el próximo agente.
2. Segundo paso recomendado.
```
