# Reglas de Agente del Proyecto (Project-Scoped Rules)

## Registro Obligatorio de Tareas (Agent Logs)

**REGLA CRÍTICA**: Todo agente de IA que comience, modifique o complete una tarea en este repositorio DEBE registrar y documentar su progreso en las carpetas de log designadas para cada componente:

- **Backend**: [BE/agent_logs/](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/BE/agent_logs)
- **Frontend**: [FE/agent_logs/](file:///c:/Users/benja/Desktop/Programs/Programacion_3/Protectos/PockemonTCG/PockemonRepoFacu/tpi-pokemon-2w2-09/FE/agent_logs)

### Flujo Obligatorio de Trabajo para el Agente:

1. **Lectura Previa**: Antes de realizar cualquier cambio de código, lee los archivos `.md` existentes en la carpeta `agent_logs/` correspondiente para comprender el estado actual de las tareas y cualquier cambio pendiente.
2. **Creación/Actualización del Log**:
   - Crea un archivo con el formato `task-[nombre-de-la-tarea].md` si estás iniciando una nueva tarea.
   - Si estás continuando una tarea existente, actualiza el archivo de esa tarea.
3. **Contenido del Log**:
   - Detalla claramente qué archivos modificaste, agregaste o eliminaste.
   - **Obligatorio**: Si te detienes, el turno termina o la tarea queda pausada, documenta detalladamente qué quedó a la mitad, qué falta por implementar y los próximos pasos en la sección de "Cambios Pendientes".
4. **Registro de Cobertura (JaCoCo) al Modificar Tests**:
   - **Obligatorio**: Siempre que modifiques, corrijas o agregues pruebas (tests) en el proyecto, debes ejecutar las pruebas y registrar/actualizar el reporte de cobertura de JaCoCo en `BE/agent_logs/reporte-cobertura-jacoco.md`. De esta manera, se mantiene un registro actualizado del cumplimiento de los requisitos de cobertura (RNF-03: global >= 80% y lógica crítica >= 90%).

Consulta las plantillas y el formato detallado en el README.md de cada carpeta de logs.
