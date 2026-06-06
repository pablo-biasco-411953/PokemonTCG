# Buenas Prácticas de Spring Boot (Integración de Roadmap)

Este proyecto incorpora varios de los estándares recomendados en el **Spring Boot Roadmap** de chapadiex para garantizar un desarrollo robusto, limpio y auto-documentado:

## 1. Validación de Datos (Roadmap Milestone 10)
Nunca se debe confiar en los datos de entrada de la API. Hemos integrado `jakarta.validation` para validar las solicitudes en la capa de controladores:
- **Anotaciones de validación:** Se usan en los DTOs como `RegisterRequest` y `LoginRequest` (ej. `@NotBlank`, `@Email`, `@Size(min=...)`).
- **Activación:** Se utiliza la anotación `@Valid` en los parámetros de los controladores (ej. `AuthController.login(@Valid @RequestBody LoginRequest request)`) para que Spring valide automáticamente las peticiones entrantes.

## 2. Manejo de Errores Centralizado (Roadmap Milestone 11)
Para evitar el uso de bloques `try-catch` repetitivos y engorrosos en cada endpoint de los controladores, centralizamos las excepciones:
- **GlobalExceptionHandler:** Una clase anotada con `@RestControllerAdvice` intercepta todas las excepciones lanzadas por los servicios o la validación.
- **Tipos de excepciones manejados:**
  - `MethodArgumentNotValidException` (Errores de validación de campos) -> Retorna `400 Bad Request` con listado de errores específicos.
  - `IllegalArgumentException` (Parámetros inválidos) -> Retorna `400 Bad Request` con mensaje de error descriptivo.
  - `IllegalStateException` (Estado inválido de partida/recurso) -> Retorna `409 Conflict`.
  - `Exception` (General) -> Retorna `500 Internal Server Error`.
- **Estructura JSON:** Todas las respuestas de error siguen una estructura limpia y consistente para facilitar la integración en el frontend.

## 3. Inyección de Dependencias Recomendada (Roadmap Milestone 15)
Se prioriza **Constructor Injection** sobre `@Autowired` en campos (field injection) para todas las clases de servicios y controladores (ej. `BattleEngineService`, `AuthController`). Esto hace que las clases sean:
- Más fáciles de testear unitariamente (sin necesidad de frameworks de mocking invasivos).
- Inmutables (atributos declarados como `final`).
- Más explícitas sobre cuáles son sus dependencias requeridas.

## 4. Documentación Automática con Swagger/OpenAPI (Roadmap Milestone 14 y 16)
Se integró `springdoc-openapi` para documentar los endpoints y modelos expuestos por el backend de manera interactiva:
- **OpenApiConfig:** Clase de configuración con `@Configuration` y un `@Bean` de `OpenAPI` que define la metadata de la API.
- **Swagger UI:** Accesible localmente en: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) al correr el backend.
- **Anotaciones descriptivas:** Se usan anotaciones como `@Tag` y `@Operation` en los controladores (ej. `AuthController`) para describir y estructurar el catálogo de endpoints de la API.
