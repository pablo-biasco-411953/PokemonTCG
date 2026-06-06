# Reglas Generales del Agente

Estas reglas aplican a CUALQUIER tarea en este proyecto, sin importar el área.

---

## Comportamiento base

- Respondé en español, a menos que el código o los logs estén en inglés.
- Respuestas cortas y directas. Sin preámbulos ni resúmenes al final.
- No expliques lo obvio. El código habla por sí mismo.
- Antes de escribir código, leé los archivos relevantes. No asumas la estructura.

## Al modificar código

- Cambiá solo lo necesario para resolver el problema. No "limpies" código alrededor.
- No agregues abstracciones, helpers ni features que no se pidieron.
- No agregues comentarios que expliquen QUÉ hace el código — solo el POR QUÉ si es no obvio.
- Validá que el cambio funciona antes de declararlo listo.

## Seguridad

- No expongas credenciales, tokens ni secrets en el código.
- No hardcodees configuraciones de entorno — usá variables de entorno o `application.properties`.
- Sanitizá inputs del usuario antes de procesarlos (especialmente en endpoints REST).

## Git y PRs

- Los commits deben ser en inglés, descriptivos y atómicos.
- Formato sugerido: `<tipo>: <descripción>` — ej: `feat: add energy card validation`, `fix: null pointer on battle end`.
- No commitees archivos de configuración local (`.idea/`, `.env`, `target/`).
- Un PR = un cambio lógico. No mezcles features con bugfixes.
