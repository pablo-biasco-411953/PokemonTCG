# Tarea: Inclusión de Cartas Entrenador en los Sobres

- **Componente**: Frontend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó que las cartas de tipo Entrenador (Trainer) no salían en los sobres al abrirlos.
Tras investigar el backend, se determinó que las cartas `Trainer` se omitían en el pool de generación de sobres.
No se requieren cambios en el frontend ya que la vista de apertura de sobres renderiza dinámicamente cualquier carta que le retorne el backend en el sobre de 10 cartas.

## Verificación
- Se validó visualmente en la interfaz que al abrir sobres se muestren cartas de tipo Entrenador (Trainer) junto con las de Pokémon y Energía.
