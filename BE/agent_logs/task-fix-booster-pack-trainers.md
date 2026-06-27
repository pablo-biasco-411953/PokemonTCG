# Tarea: Inclusión de Cartas Entrenador en los Sobres

- **Componente**: Backend
- **Estado**: Completado ✅
- **Fecha**: 2026-06-27

## Descripción

El usuario reportó que las cartas de tipo Entrenador (Trainer) no salían en los sobres al abrirlos.
Tras investigar `SobreService.java`, descubrimos que el método `abrirSobre()` únicamente clasificaba y filtraba las cartas de la base de datos en dos listas: `energias` y `pokemones`. Las cartas con supertype `Trainer` quedaban completamente excluidas de la selección.

## Archivos Modificados/Creados

- **Modificado** `BE/src/main/java/com/pokemon/tcg/service/SobreService.java`: Clasificar cartas `Trainer` e incluirlas en la composición aleatoria del sobre de 10 cartas.
- **Modificado** `BE/src/test/java/com/pokemon/tcg/service/SobreServiceTest.java`: Añadir soporte de cartas `Trainer` en la base del catálogo de test y añadir una prueba específica para verificar su apertura.

## Verificación
- Se ejecutó `mvn clean test` y los tests pasaron exitosamente.
