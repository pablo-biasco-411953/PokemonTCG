# Stack Tecnológico

## Backend

| Tecnología | Uso |
|-----------|-----|
| Java | Lenguaje principal |
| Spring Boot | Framework web y de aplicación |
| Spring Data JPA | ORM y acceso a base de datos |
| Spring WebSocket | Comunicación en tiempo real (lobby) |
| Maven | Gestión de dependencias y build |

- Punto de entrada: `BackendApplication.java`
- Configuración: `src/main/resources/application.properties`
- Puerto por defecto: `8080`

## Frontend

| Tecnología | Uso |
|-----------|-----|
| Angular | Framework SPA |
| TypeScript | Lenguaje principal |
| Tailwind CSS | Estilos utilitarios |
| SCSS | Estilos por componente |
| RxJS | Programación reactiva / observables |

- Puerto de desarrollo: `4200`
- Build: `ng build`
- Dev server: `ng serve`

## Infraestructura

- **Docker**: el proyecto incluye `docker-compose.yml` en la raíz para levantar la base de datos.
- **Dockerfile**: el BE tiene su propio `Dockerfile` para containerizar la aplicación.

## Cómo levantar el proyecto localmente

```bash
# 1. Base de datos
docker-compose up -d

# 2. Backend
cd BE && mvn spring-boot:run

# 3. Frontend
cd FE && npm install && ng serve
```
