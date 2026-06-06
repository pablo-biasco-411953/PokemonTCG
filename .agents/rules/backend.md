# Reglas — Backend (Java / Spring Boot)

---

## Arquitectura en capas

Respetá estrictamente la separación de responsabilidades:

```
Controller  →  Service  →  Repository  →  Model
```

- **Controller**: solo recibe requests, delega al service, devuelve respuestas HTTP. Sin lógica de negocio.
- **Service**: toda la lógica de negocio vive acá.
- **Repository**: solo acceso a datos. Sin lógica.
- **Model**: entidades del dominio. Sin lógica de presentación.
- **DTO**: objetos de transferencia para requests/responses. No expongas entidades directamente.

## Convenciones de código

- Clases en PascalCase: `BattleService`, `JugadorController`.
- Métodos y variables en camelCase: `atacarPokemon()`, `cartaActual`.
- Paquetes en minúscula: `com.pokemon.tcg.service`.
- Un archivo = una clase pública.

## Spring Boot

- Usá `@Service`, `@Repository`, `@RestController` según corresponda.
- Inyectá dependencias por constructor, no por `@Autowired` en campo.
- Los endpoints REST deben tener rutas consistentes: `/api/v1/<recurso>`.
- Manejá errores con excepciones específicas + `@ExceptionHandler` o `@ControllerAdvice`.

## Base de datos

- No escribas SQL crudo salvo que sea estrictamente necesario — usá Spring Data JPA.
- Los nombres de tablas y columnas en snake_case: `carta_en_juego`, `jugador_id`.
- No uses `CascadeType.ALL` sin pensar — podés borrar datos sin querer.

## Tests

- Los tests de servicio van en `src/test/java/com/pokemon/tcg/service/`.
- Nombrá los tests: `<método>_<condición>_<resultadoEsperado>`.
- Mockeá repositorios con `@MockBean` o Mockito. No toques la base de datos real en unit tests.
