---
sidebar_position: 1
title: 🔐 AuthController
---

# 🔐 AuthController - Autenticación de Jugadores

> Endpoints para registro, login y recuperación de contraseña

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/controller/AuthController.java`

---

## 🏗️ Clase Principal

```java
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", 
     description = "Endpoints para registro, login y recuperación de contraseña")
public class AuthController {
    
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;
    
    // Constructor con inyección de dependencias
    public AuthController(AuthService authService, 
                         PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
    }
}
```

**Anotaciones**:
- `@RestController` → Spring reconoce como controlador REST (devuelve JSON)
- `@RequestMapping("/api/auth")` → Prefijo base para todos los endpoints
- `@Tag` → Documentación OpenAPI/Swagger

---

## 📡 Endpoints

### 1. POST /api/auth/login

**Autenticar un jugador**

```java
@PostMapping("/login")
@Operation(summary = "Iniciar sesión", 
           description = "Autentica un jugador con sus credenciales")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    // Delegar a servicio
    Jugador jugador = authService.login(request.getUsername(), 
                                       request.getPassword());
    
    // Validar resultado
    if (jugador == null) {
        return ResponseEntity.status(401)
            .body("Usuario no válido");
    }
    
    // Devolver respuesta exitosa
    return ResponseEntity.ok(toAuthResponse(jugador));
}
```

**Request**:
```json
{
  "username": "Pikachu123",
  "password": "MyPassword123!"
}
```

**Response (200)**:
```json
{
  "username": "Pikachu123",
  "sobresDisponibles": 5,
  "nivel": 0
}
```

**Response (401)**:
```json
"Usuario no válido"
```

---

### 2. POST /api/auth/register

**Crear nuevo jugador**

```java
@PostMapping("/register")
@Operation(summary = "Registrar nuevo usuario", 
           description = "Crea un nuevo jugador en el sistema")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    // Llamar servicio de registro
    Jugador jugador = authService.register(
        request.getScreenName(),     // Nombre visible
        request.getEmail(),          // Email único
        request.getPassword(),       // Password
        request.getConfirmPassword() // Confirmación
    );
    
    return ResponseEntity.ok(toAuthResponse(jugador));
}
```

**Request**:
```json
{
  "screenName": "Pikachu123",
  "email": "pikachu@example.com",
  "password": "MyPassword123!",
  "confirmPassword": "MyPassword123!"
}
```

**Response (200)**:
```json
{
  "username": "Pikachu123",
  "sobresDisponibles": 0,
  "nivel": 0
}
```

---

### 3. POST /api/auth/forgot-password

**Solicitar recuperación de contraseña**

```java
@PostMapping("/forgot-password")
@Operation(summary = "Solicitar recuperación de contraseña", 
           description = "Envía un token de recuperación al email del jugador")
public ResponseEntity<?> forgotPassword(
    @RequestBody ForgotPasswordRequest request) {
    
    // Servicio genera token y envía email
    return ResponseEntity.ok(
        passwordRecoveryService.requestReset(
            request.getUsername(), 
            request.getEmail()
        )
    );
}
```

**Request**:
```json
{
  "username": "Pikachu123",
  "email": "pikachu@example.com"
}
```

**Response (200)**:
```json
{
  "message": "Email de recuperación enviado",
  "tokenSent": true
}
```

---

### 4. POST /api/auth/reset-password

**Restablecer contraseña**

```java
@PostMapping("/reset-password")
@Operation(summary = "Restablecer contraseña", 
           description = "Cambia la contraseña usando token de recuperación")
public ResponseEntity<?> resetPassword(
    @RequestBody ResetPasswordRequest request) {
    
    // Validar token y actualizar password
    passwordRecoveryService.resetPassword(
        request.getToken(),          // Token enviado por email
        request.getPassword(),       // Nueva contraseña
        request.getConfirmPassword() // Confirmación
    );
    
    return ResponseEntity.ok(
        "Password actualizado. Ya podés iniciar sesión."
    );
}
```

**Request**:
```json
{
  "token": "abc123def456",
  "password": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

**Response (200)**:
```json
"Password actualizado. Ya podés iniciar sesión."
```

---

## 🔧 Métodos Privados

### toAuthResponse()

```java
private JugadorDTO toAuthResponse(Jugador jugador) {
    return new JugadorDTO(
        jugador.getUsername(),       // Nombre de usuario
        jugador.getSobresDisponibles(), // Sobres abiertos
        0                            // Nivel
    );
}
```

**Purpose**: Convertir Entity a DTO (no exponer datos internos)

---

## 📊 Anotaciones Importantes

### @Valid
Valida el objeto antes de procesarlo
```java
@Valid @RequestBody LoginRequest request
// Valida que LoginRequest tenga todos los campos requeridos
```

### @RequestBody
Parsea JSON del request a objeto Java
```java
@RequestBody LoginRequest request
// JSON → LoginRequest instance
```

### @Operation (Swagger)
Documenta el endpoint automáticamente
```java
@Operation(summary = "...", description = "...")
// Aparece en Swagger UI en /swagger-ui.html
```

---

## 🔄 Flujo Completo: Login

```
Cliente                  AuthController          AuthService         BD
   │                           │                     │                │
   ├─ POST /api/auth/login    │                     │                │
   │  { username, password }  │                     │                │
   │────────────────────────→ │                     │                │
   │                          │                     │                │
   │                          ├─ login(u, p) ────→ │                │
   │                          │                     │                │
   │                          │                     ├─ SELECT... ──→ │
   │                          │                     │   WHERE username
   │                          │                     │                │
   │                          │                   ← Jugador entity ──┤
   │                          │                     │                │
   │                          │ ← Jugador instance ─┤                │
   │                          │                     │                │
   │                          ├─ toAuthResponse()   │                │
   │                          │  (Entity → DTO)     │                │
   │                          │                     │                │
   │ 200 OK + JugadorDTO ← ─ ─┤                     │                │
   │ {username, sobres, nivel}│                     │                │
   │                          │                     │                │
```

---

## 🎯 Validaciones

```
POST /login
├─ Username no vacío ✓
├─ Password no vacío ✓
├─ Usuario existe en BD ✓
├─ Password correcto (bcrypt) ✓
└─ Devolver datos de usuario

POST /register
├─ Email válido (formato) ✓
├─ Email único ✓
├─ Password strong ✓
├─ Passwords coinciden ✓
└─ Crear jugador en BD
```

---

## 🔐 Seguridad

✅ Passwords hasheadas (bcrypt) en BD
✅ No devuelve password en respuesta
✅ Validación de entrada (@Valid)
✅ CORS configurado
✅ HTTPS en producción

---

## 📝 DTOs Usadas

```java
// Request
class LoginRequest {
    String username;
    String password;
}

// Request
class RegisterRequest {
    String screenName;
    String email;
    String password;
    String confirmPassword;
}

// Response
class JugadorDTO {
    String username;
    Integer sobresDisponibles;
    Integer nivel;
}
```

---

*Próximo: [BattleController](/docs/componentes-detallados/backend/controllers/02-battle-controller)*
