# Tarea: Corrección de Condiciones de Derrota (Deck Out)

- **Componente**: Frontend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó que cuando un jugador se queda sin cartas en su mazo (Deck Out), la partida continuaba de forma indefinida y no se decretaba la derrota.
No se requirieron cambios en el frontend ya que la lógica de fin de partida es comandada por el estado enviado por el backend.

## Verificación
- Se comprobó en backend mediante tests automatizados que el fin de partida por Deck Out funciona correctamente.
