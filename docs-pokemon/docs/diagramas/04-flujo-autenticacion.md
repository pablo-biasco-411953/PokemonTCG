---
sidebar_position: 4
title: "🔑 Flujo de Autenticacion"
---

# Flujo de Autenticacion

> Registro, login y recuperacion de password

---

## Arquitectura de Auth

El sistema usa **autenticacion basada en SHA-256** sin JWT ni sesiones del servidor. El username se envia como header `X-Username` en cada request.

```mermaid
graph LR
    FE[Frontend] -->|X-Username header| BE[Backend]
    BE -->|SHA-256 compare| DB[(MySQL)]
```

---

## Registro

```mermaid
sequenceDiagram
    participant U as Usuario
    participant FE as LoginComponent
    participant AS as AuthService (FE)
    participant AC as AuthController
    participant AUTH as AuthService (BE)
    participant DB as MySQL

    U->>FE: Completa formulario de registro
    FE->>FE: Validar campos localmente
    FE->>AS: register(screenName, email, password, confirm)
    AS->>AC: POST /api/auth/register

    AC->>AUTH: register(screenName, email, password, confirm)
    AUTH->>AUTH: Validar screenName no vacio
    AUTH->>AUTH: Validar email con @
    AUTH->>AUTH: Validar password min 4 chars
    AUTH->>AUTH: Validar password == confirmPassword

    AUTH->>DB: findAuthByUsername(screenName)
    alt Username existe
        AUTH-->>AC: Error "nombre ya en uso"
        AC-->>FE: 400 Error
    end

    AUTH->>DB: findByEmail(email)
    alt Email existe
        AUTH-->>AC: Error "email ya asociado"
        AC-->>FE: 400 Error
    end

    AUTH->>AUTH: hashPassword("pokemon-tcg:" + password)
    AUTH->>DB: save(nuevo Jugador)
    AUTH-->>AC: Jugador creado
    AC-->>FE: JugadorDTO (username, sobres)
    FE->>FE: Guardar username en localStorage
    FE->>U: Redirigir al Lobby
```

---

## Login

```mermaid
sequenceDiagram
    participant U as Usuario
    participant FE as LoginComponent
    participant AC as AuthController
    participant AUTH as AuthService (BE)
    participant DB as MySQL

    U->>FE: Ingresa username + password
    FE->>AC: POST /api/auth/login

    AC->>AUTH: login(username, password)
    AUTH->>AUTH: hashPassword("pokemon-tcg:" + password)
    AUTH->>DB: findAuthByUsername(username)

    alt Usuario no existe
        AUTH->>AUTH: Crear nuevo jugador con ese hash
        AUTH->>DB: save(nuevo Jugador)
        AUTH-->>AC: Jugador (auto-registro)
    else Existe sin password
        AUTH->>AUTH: Asignar hash al jugador existente
        AUTH->>DB: save(jugador actualizado)
        AUTH-->>AC: Jugador
    else Password no coincide
        AUTH-->>AC: Error "credenciales incorrectas"
        AC-->>FE: 401 Unauthorized
    else Password coincide
        AUTH-->>AC: Jugador autenticado
    end

    AC-->>FE: JugadorDTO
    FE->>FE: Guardar username en localStorage
    FE->>U: Redirigir al Lobby
```

**Nota**: El login tiene auto-registro: si el username no existe, crea la cuenta automaticamente.

---

## Hashing de Password

```mermaid
graph LR
    P["password"] --> C["'pokemon-tcg:' + password"]
    C --> SHA["SHA-256"]
    SHA --> HEX["Hex string (64 chars)"]
```

```java
// Salt fijo: "pokemon-tcg:"
MessageDigest.getInstance("SHA-256")
    .digest(("pokemon-tcg:" + password).getBytes(UTF_8))
```

No usa bcrypt ni salt aleatorio. El prefijo `"pokemon-tcg:"` actua como salt estatico.

---

## Recuperacion de Password

```mermaid
sequenceDiagram
    participant U as Usuario
    participant FE as LoginComponent
    participant AC as AuthController
    participant PRS as PasswordRecoveryService
    participant SMTP as Servidor SMTP
    participant DB as MySQL

    U->>FE: Click "Olvide mi password"
    FE->>AC: POST /api/auth/forgot-password
    AC->>PRS: requestReset(username, email)
    PRS->>DB: Buscar jugador por username + email
    PRS->>PRS: Generar token aleatorio
    PRS->>DB: Guardar hash del token
    PRS->>SMTP: Enviar email con link de reset
    PRS-->>FE: "Email enviado"

    Note over U: Usuario recibe email con token

    U->>FE: Ingresa token + nuevo password
    FE->>AC: POST /api/auth/reset-password
    AC->>PRS: resetPassword(token, password, confirm)
    PRS->>DB: Buscar jugador por token hash
    PRS->>PRS: hashPassword(nuevo password)
    PRS->>DB: Actualizar password_hash
    PRS->>DB: Limpiar token
    PRS-->>FE: "Password actualizado"
```

---

## Endpoints de Auth

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login (con auto-registro) |
| POST | `/api/auth/register` | Registro explicito |
| POST | `/api/auth/forgot-password` | Solicitar reset |
| POST | `/api/auth/reset-password` | Cambiar password con token |

---

## Seguridad del Frontend

```mermaid
graph TD
    A[LoginComponent] -->|login exitoso| B[localStorage.username]
    B --> C[AuthService.getUsername]
    C --> D["Header X-Username en cada request"]
    
    E[Cualquier ruta] --> F{username en localStorage?}
    F -->|No| G[Redirigir a /login]
    F -->|Si| H[Cargar componente]
```

El frontend almacena el username en `localStorage` y lo incluye como header en cada request HTTP. No hay JWT ni tokens de sesion.
