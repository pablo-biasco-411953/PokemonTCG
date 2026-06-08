---
sidebar_position: 9
title: 🔐 Autenticación
---

# 🔐 Autenticación - JWT + PKCE

---

## JWT (JSON Web Token)

```
Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
       eyJzdWIiOiIyNSIsIm5hbWUiOiJKdWdhZG9yIn0.
       SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

Partes:
1. Header: { alg: HS256, typ: JWT }
2. Payload: { sub: 25, username: Jugador, exp: 123456 }
3. Signature: HMAC-SHA256(secret)
```

---

## Spring Security Config

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

## Login Flow

```
1. POST /api/auth/login
   { email: "user@example.com", password: "pass" }
   
2. Spring valida credenciales
   
3. Genera JWT token
   Token válido por 24 horas
   
4. Devuelve token
   { token: "eyJhbGc...", user: { ... } }
   
5. Frontend guarda en localStorage
   localStorage.setItem('token', token)
   
6. Próximas peticiones incluyen token
   Headers: Authorization: Bearer eyJhbGc...
```

---

## PKCE (Proof Key for Code Exchange)

Para OAuth seguro:
```
1. Generate code_verifier
2. Compute code_challenge = SHA256(code_verifier)
3. Send code_challenge to auth server
4. Auth server devuelve code
5. Intercambiar code + code_verifier por token
```

---

*Próximo: [Manejo de Estado](/docs/tecnica/manejo-estado)*
