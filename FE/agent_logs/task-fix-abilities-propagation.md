# Tarea: Corrección de la Propagación de Habilidades en Partida

- **Componente**: Frontend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó que las habilidades de los Pokémon "no hacen nada". Tras una auditoría del backend, determinamos que el frontend no recibía las habilidades de las cartas en la colección, mazos o partidas debido a que eran omitidas al clonarse/cargarse de forma lazy en el backend.
No se requieren cambios en el código de Angular del frontend ya que la lógica y UI (`battle-board.component.html` y `battle-board.component.ts`) ya implementan el soporte para renderizar y disparar las habilidades activas y pasivas una vez que la propiedad `habilidades` se propague de manera íntegra desde el backend.

## Próximos pasos
- Validar que una vez corregido el backend, el panel de habilidades en el detalle del Pokémon renderice los botones de acción e información correctos.
