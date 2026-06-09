---
sidebar_position: 1
title: 🔐 AuthService
---

# 🔐 AuthService - Autenticación del Cliente

> Servicio Angular para login, registro y manejo de autenticación

---

## 📍 Ubicación

`frontend/src/app/core/services/auth.service.ts`

---

## 🏗️ Definición del Servicio

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${getBackendUrl()}/api/auth`;
  
  constructor(private http: HttpClient) {}
}
```

**Tipo**: Servicio raíz (disponible globalmente)
**Dependencias**: HttpClient de Angular

---

## 📡 Métodos Principales

### 1. login(username: string, password: string)

**Autenticar usuario**

```typescript
login(username: string, password: string): Observable<Jugador> {
  return this.http.post<Jugador>(
    `${this.apiUrl}/login`, 
    { username, password }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `password: string` - Contraseña

**Retorno**: `Observable<Jugador>` - Datos del jugador autenticado

**Endpoint**: `POST /api/auth/login`

**Uso en componente**:
```typescript
this.authService.login('Pikachu123', 'MyPass123!').subscribe(
  (jugador: Jugador) => {
    console.log('Login exitoso:', jugador.username);
  },
  (error) => {
    console.error('Login fallido:', error);
  }
);
```

---

### 2. register(screenName: string, email: string, password: string, confirmPassword: string)

**Registrar nuevo usuario**

```typescript
register(
  screenName: string, 
  email: string, 
  password: string, 
  confirmPassword: string
): Observable<Jugador> {
  return this.http.post<Jugador>(
    `${this.apiUrl}/register`, 
    { screenName, email, password, confirmPassword }
  );
}
```

**Parámetros**:
- `screenName: string` - Nombre visible (username)
- `email: string` - Email del usuario
- `password: string` - Contraseña
- `confirmPassword: string` - Confirmación de contraseña

**Retorno**: `Observable<Jugador>` - Datos del nuevo jugador

**Endpoint**: `POST /api/auth/register`

**Uso en componente**:
```typescript
this.authService.register(
  'Pikachu123',
  'pikachu@example.com',
  'MyPass123!',
  'MyPass123!'
).subscribe(
  (jugador: Jugador) => {
    console.log('Registro exitoso');
  },
  (error) => {
    console.error('Registro fallido:', error.error);
  }
);
```

---

### 3. forgotPassword(username: string, email: string)

**Solicitar recuperación de contraseña**

```typescript
forgotPassword(username: string, email: string): Observable<string> {
  return this.http.post(
    `${this.apiUrl}/forgot-password`, 
    { username, email }, 
    { responseType: 'text' }
  );
}
```

**Parámetros**:
- `username: string` - Nombre de usuario
- `email: string` - Email registrado

**Retorno**: `Observable<string>` - Mensaje de confirmación

**Endpoint**: `POST /api/auth/forgot-password`

**Respuesta**: Mensaje de texto (no JSON)

**Uso**:
```typescript
this.authService.forgotPassword('Pikachu123', 'pikachu@example.com').subscribe(
  (mensaje: string) => {
    console.log(mensaje);  // "Email de recuperación enviado"
  }
);
```

---

### 4. resetPassword(token: string, password: string, confirmPassword: string)

**Restablecer contraseña con token**

```typescript
resetPassword(
  token: string, 
  password: string, 
  confirmPassword: string
): Observable<string> {
  return this.http.post(
    `${this.apiUrl}/reset-password`, 
    { token, password, confirmPassword }, 
    { responseType: 'text' }
  );
}
```

**Parámetros**:
- `token: string` - Token del email de recuperación
- `password: string` - Nueva contraseña
- `confirmPassword: string` - Confirmación

**Retorno**: `Observable<string>` - Mensaje de confirmación

**Endpoint**: `POST /api/auth/reset-password`

**Uso**:
```typescript
this.authService.resetPassword(
  'abc123def456',
  'NewPass123!',
  'NewPass123!'
).subscribe(
  (mensaje: string) => {
    console.log(mensaje);  // "Password actualizado..."
  }
);
```

---

## 🔄 Flujo de Autenticación Completo

```
Usuario                 LoginComponent         AuthService         Backend
   │                         │                      │                │
   ├─ Ingresa credenciales  │                      │                │
   │────────────────────→   │                      │                │
   │                         ├─ login() ──────────→ │                │
   │                         │                      ├─ POST /login ─→ │
   │                         │                      │            ← OK │
   │                         │ ← Observable<Jugador>─                │
   │                         │                      │                │
   │                         ├─ subscribe() ──────→ │                │
   │                         │ (next, error)        │                │
   │                         │                      │                │
   │ ← Redirige dashboard ─  │                      │                │
```

---

## 📋 Tipo de Datos

### Jugador (Response)

```typescript
interface Jugador {
  id?: number;
  username: string;
  email?: string;
  sobresDisponibles: number;
  santoCoins: number;
  coleccion?: Card[];
  // ... otros campos
}
```

---

## 🎯 Casos de Uso

### Caso 1: Login exitoso
```typescript
loginForm$ = this.authService.login('user', 'pass').pipe(
  tap(jugador => {
    this.currentUser = jugador;
    localStorage.setItem('user', JSON.stringify(jugador));
  }),
  catchError(error => {
    console.error('Login error:', error.message);
    return throwError(() => new Error('Auth failed'));
  })
);
```

### Caso 2: Manejo de errores
```typescript
this.authService.login(username, password).subscribe(
  {
    next: (jugador) => {
      // Éxito
    },
    error: (error) => {
      // Error: Usuario/contraseña incorrectos (401)
      // o: Servidor no disponible (500)
    },
    complete: () => {
      // Solicitud completada
    }
  }
);
```

---

## ⚠️ Notas Importantes

**Observable vs Promise**:
- ✅ Retorna Observables (RxJS)
- ✅ Lazy evaluation (no ejecuta hasta subscribe)
- ✅ Cancelable con unsubscribe()

**Errores HTTP**:
- 401: Usuario/contraseña incorrectos
- 400: Datos inválidos (email ya existe, etc)
- 500: Error del servidor

**Seguridad**:
- ✅ Usa HTTPS en producción
- ✅ Backend hashea contraseña
- ✅ No almacena password en cliente

---

## 📊 Métodos Resumen

| Método | Parámetros | Retorna | Endpoint |
|--------|-----------|---------|----------|
| `login()` | username, password | `Observable<Jugador>` | POST /login |
| `register()` | screenName, email, password, confirm | `Observable<Jugador>` | POST /register |
| `forgotPassword()` | username, email | `Observable<string>` | POST /forgot-password |
| `resetPassword()` | token, password, confirm | `Observable<string>` | POST /reset-password |

---

*Próximo: [CardService](/docs/componentes-detallados/frontend/services/02-card-service)*
