---
sidebar_position: 1
title: "🌐 CORS Config"
---

# CorsConfig - Configuracion de Cross-Origin

> Permite al frontend Angular comunicarse con el backend Spring Boot

---

## Ubicacion

`backend/src/main/java/com/pokemon/tcg/config/CorsConfig.java`

---

## Codigo

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

---

## Configuracion

| Parametro | Valor | Descripcion |
|-----------|-------|-------------|
| `addMapping` | `"/**"` | Aplica a TODAS las rutas |
| `allowedOriginPatterns` | `"*"` | Cualquier origen (desarrollo) |
| `allowedMethods` | GET, POST, PUT, DELETE, OPTIONS | Todos los metodos REST + preflight |
| `allowedHeaders` | `"*"` | Cualquier header |
| `allowCredentials` | `true` | Permite cookies/auth headers |

---

## Notas

- En **produccion** se debe restringir `allowedOriginPatterns` a los dominios reales
- `OPTIONS` es necesario para las preflight requests del navegador
- `allowCredentials(true)` es necesario para que los headers de autenticacion viajen correctamente
