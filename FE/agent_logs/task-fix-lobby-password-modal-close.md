# Tarea: Close lobby password modal upon successful actions

- **Fecha/Hora**: 2026-06-27 15:50
- **Estado**: Completado

## Cambios Realizados
- [x] **[MODIFY] FE/src/app/features/lobby/lobby.component.ts**:
  - Se modificó `joinSelectedRoom()` para que asigne `roomPasswordModalOpen = false` y `roomJoinPassword = ''` en el bloque `next` de éxito al unirse a la sala.
  - Se modificó `spectateSelectedRoom()` para realizar el mismo cierre del modal y reseteo del input en ambos flujos de éxito (unión al lobby como espectador o navegación inmediata a la pantalla de batalla).

## Cambios Pendientes (A la mitad o pendientes)
- Ninguno. El modal ahora se cierra correctamente tras un éxito en el ingreso de contraseña.

## Próximos Pasos Recomendados
- Ninguno. El parche ha sido aplicado y completado.
