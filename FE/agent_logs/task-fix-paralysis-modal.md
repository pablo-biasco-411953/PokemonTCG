# Tarea: Cambio de Alertas de Estado a Notificaciones No Bloqueantes

- **Componente**: Frontend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó que cuando un Pokémon era paralizado, dormido, envenenado o confundido en partida, emergía un modal de SweetAlert2 molestando el flujo de la partida y obligándolo a presionar "OK".

### Solución:
- Modificamos los manejadores automáticos en `verificarEstadosCurados` dentro de `battle-board.component.ts`.
- Cambiamos la severidad de las alertas automáticas de `'error'` (que dispara modales bloqueantes de SweetAlert) a `'warning'` (que dispara discretos mensajes temporales toast que desaparecen solos).
- De esta manera, solo aparecerá un modal bloqueante si el usuario intenta realizar activamente una acción ilegal estando incapacitado (ej: atacar estando paralizado o dormido).

## Archivos Modificados/Creados

- **Modificado** `FE/src/app/features/battle/battle-board.component.ts`: Cambiar tipo de notificación a 'warning'.

## Verificación
- Se compiló con éxito el frontend.
