---
sidebar_position: 1
title: 🔐 AuthService
---

# 🔐 AuthService - Lógica de Autenticación

> Servicio para login, registro y hashing de contraseñas

---

## 📍 Ubicación

`backend/src/main/java/com/pokemon/tcg/service/AuthService.java`

---

## 🏗️ Clase Principal

```java
@Service
public class AuthService {
    
    private final JugadorRepository jugadorRepo;
    
    public AuthService(JugadorRepository jugadorRepo) {
        this.jugadorRepo = jugadorRepo;
    }
}
```

**Responsabilidades**:
- ✅ Autenticar usuarios (login)
- ✅ Registrar usuarios nuevos
- ✅ Hash de contraseñas (SHA-256)
- ✅ Validaciones de seguridad

---

## 📡 Métodos Principales

### 1. login(String username, String password)

**Autenticar un jugador (crear si no existe)**

```java
public Jugador login(String username, String password) {
    // 1. Validar entrada
    if (username == null || username.isBlank()) {
        throw new IllegalArgumentException("El usuario es obligatorio.");
    }
    if (password == null || password.length() < 4) {
        throw new IllegalArgumentException("La contrasena debe tener al menos 4 caracteres.");
    }
    
    // 2. Limpiar inputs
    String cleanUsername = username.trim();
    String passwordHash = hashPassword(password);
    
    // 3. Buscar jugador
    Jugador jugador = jugadorRepo.findAuthByUsername(cleanUsername);
    
    // 4. Si no existe, crear
    if (jugador == null) {
        jugador = new Jugador(cleanUsername);
        jugador.setPasswordHash(passwordHash);
        jugadorRepo.save(jugador);
    } 
    // 5. Si existe pero sin contraseña, asignar
    else if (jugador.getPasswordHash() == null || jugador.getPasswordHash().isBlank()) {
        jugador.setPasswordHash(passwordHash);
        jugadorRepo.save(jugador);
    } 
    // 6. Si existe y tiene contraseña, validar
    else if (!jugador.getPasswordHash().equals(passwordHash)) {
        throw new IllegalArgumentException("Usuario o contrasena incorrectos.");
    }
    
    return jugador;
}
```

**Flujo**:
```
1. Validar username (no nulo, no vacío)
2. Validar password (mínimo 4 caracteres)
3. Limpiar inputs (trim)
4. Hashear password con SHA-256
5. Buscar jugador en BD
6. Casos:
   - No existe → Crear
   - Existe sin contraseña → Asignar
   - Existe con contraseña → Validar hash
7. Devolver jugador o lanzar excepción
```

**Excepciones**:
```java
IllegalArgumentException:
  - "El usuario es obligatorio."
  - "La contrasena debe tener al menos 4 caracteres."
  - "Usuario o contrasena incorrectos."
```

---

### 2. register(String screenName, String email, String password, String confirmPassword)

**Registrar un nuevo jugador**

```java
public Jugador register(String screenName, String email, String password, String confirmPassword) {
    // 1. Validar screenName
    if (screenName == null || screenName.isBlank()) {
        throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
    }
    
    // 2. Validar email
    if (email == null || email.isBlank() || !email.contains("@")) {
        throw new IllegalArgumentException("El email es obligatorio para recuperar la cuenta.");
    }
    
    // 3. Validar password
    if (password == null || password.length() < 4) {
        throw new IllegalArgumentException("La contrasena debe tener al menos 4 caracteres.");
    }
    
    // 4. Validar confirmación
    if (!password.equals(confirmPassword)) {
        throw new IllegalArgumentException("Las contrasenas no coinciden.");
    }
    
    // 5. Verificar username único
    String cleanScreenName = screenName.trim();
    Jugador existente = jugadorRepo.findAuthByUsername(cleanScreenName);
    if (existente != null) {
        throw new IllegalArgumentException("El nombre de usuario ya esta en uso.");
    }
    
    // 6. Verificar email único
    String cleanEmail = email.trim().toLowerCase();
    if (jugadorRepo.findByEmail(cleanEmail) != null) {
        throw new IllegalArgumentException("Ese email ya esta asociado a otro entrenador.");
    }
    
    // 7. Crear jugador
    Jugador jugador = new Jugador(cleanScreenName);
    jugador.setEmail(cleanEmail);
    jugador.setPasswordHash(hashPassword(password));
    jugadorRepo.save(jugador);
    
    return jugador;
}
```

**Validaciones**:
```
✓ screenName no vacío
✓ email válido (contiene @)
✓ password mínimo 4 caracteres
✓ password y confirmPassword coinciden
✓ username único (case-sensitive)
✓ email único (case-insensitive)
```

**Excepciones**:
```java
IllegalArgumentException:
  - "El nombre de usuario es obligatorio."
  - "El email es obligatorio para recuperar la cuenta."
  - "La contrasena debe tener al menos 4 caracteres."
  - "Las contrasenas no coinciden."
  - "El nombre de usuario ya esta en uso."
  - "Ese email ya esta asociado a otro entrenador."
```

---

### 3. hashPassword(String password)

**Hashear contraseña con SHA-256**

```java
public String hashPassword(String password) {
    try {
        // 1. Crear hasher SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        // 2. Agregar salt ("pokemon-tcg:")
        byte[] hash = digest.digest(("pokemon-tcg:" + password).getBytes(StandardCharsets.UTF_8));
        
        // 3. Convertir a hexadecimal
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        
        return hex.toString();
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("No se pudo preparar el hash de contrasena.", e);
    }
}
```

**Algoritmo**:
1. SHA-256 hash
2. Salt: `"pokemon-tcg:"` (prepended)
3. Output: hexadecimal string (64 caracteres)

**Ejemplo**:
```
Input: "MyPassword123!"
Salt: "pokemon-tcg:MyPassword123!"
SHA-256: "a1b2c3d4e5f6..."
Output: "a1b2c3d4e5f6..." (64 chars, hex)
```

---

## 🔄 Flujo Completo: Login

```
Controller              AuthService         JugadorRepository      BD
   │                        │                      │               │
   ├─ login(user, pass)    │                      │               │
   │────────────────────→  │                      │               │
   │                       ├─ Validar entrada    │               │
   │                       │                      │               │
   │                       ├─ hashPassword() ────→ │               │
   │                       │ "pokemon-tcg:pass"   │               │
   │                       │ SHA-256              │               │
   │                       │ ← hash (64 chars)    │               │
   │                       │                      │               │
   │                       ├─ findAuthByUsername()│               │
   │                       │────────────────────→ │               │
   │                       │                      ├─ SELECT... ──→ │
   │                       │                  ← Jugador ─────────┤
   │                       │                      │               │
   │                       ├─ Validar password   │               │
   │                       │ comparar hashes      │               │
   │                       │                      │               │
   │ ← Jugador ────────────┤                      │               │
```

---

## 🔐 Seguridad

✅ **Hash Algorithm**: SHA-256 (estándar)
✅ **Salt**: Prefijo "pokemon-tcg:" (no aleatorio)
✅ **Validación**: Nunca devuelve password en plaintext
✅ **Trimming**: Limpia espacios en blanco
✅ **Case-sensitivity**: username sensible a mayúsculas, email no

⚠️ **Mejora futura**: Implementar salt aleatorio por usuario

---

## 📊 Complejidad

- **login()**: O(1) - una búsqueda en BD + hash
- **register()**: O(1) - dos búsquedas + hash + insert
- **hashPassword()**: O(n) - donde n = longitud password (típicamente pequeño)

---

## 🔗 Relaciones

```
AuthService
    ├─ JugadorRepository
    │   └─ Jugador entity
    │       ├─ username (unique index)
    │       ├─ email (unique index)
    │       └─ passwordHash
    └─ MessageDigest (Java built-in)
        └─ SHA-256 algorithm
```

---

## 📝 Excepciones Personalizadas

```java
// Validación
IllegalArgumentException
  - "El usuario es obligatorio."
  - "La contrasena debe tener al menos 4 caracteres."
  - "Usuario o contrasena incorrectos."
  - "El nombre de usuario ya esta en uso."
  - "Ese email ya esta asociado a otro entrenador."
  - "Las contrasenas no coinciden."

// Sistema
IllegalStateException
  - "No se pudo preparar el hash de contrasena."
```

---

*Próximo: [CardCatalogService](/docs/componentes-detallados/backend/services/02-card-catalog-service)*
