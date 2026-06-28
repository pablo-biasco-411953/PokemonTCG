# Tarea: Cambio de Alertas de Estado a Notificaciones No Bloqueantes

- **Componente**: Backend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó que cuando un Pokémon era paralizado, dormido, envenenado o confundido en partida, emergía un modal de SweetAlert2 molestando el flujo de la partida y obligándolo a presionar "OK".
No se requirieron cambios en el backend ya que la decisión de cómo notificar los cambios de estado (mediante modal o toast) se gestiona en la capa del frontend.

## Verificación
- Verificado mediante compilación exitosa del frontend.
