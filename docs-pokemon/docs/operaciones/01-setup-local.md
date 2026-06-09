---
sidebar_position: 1
title: 🚀 Setup Local - Guía de Instalación
---

# 🚀 Setup Local - Comienza a Jugar y Desarrollar

> Guía completa para instalar y ejecutar Pokémon TCG en tu máquina local

---

## 📋 Requisitos Previos

### Hardware
- **RAM**: Mínimo 4GB (recomendado 8GB)
- **Disco**: 500MB libres
- **CPU**: Procesador moderno (últimos 5 años)

### Software

| Herramienta | Versión | Descripción |
|------------|---------|------------|
| **Java** | 21 LTS | Backend (OpenJDK o Oracle) |
| **Node.js** | 18+ | Frontend (descarga desde nodejs.org) |
| **npm** | 9+ | Gestor de paquetes JavaScript |
| **Git** | 2.30+ | Control de versiones |
| **Maven** | 3.8+ | Build tool (incluido en proyecto) |

:::tip Consejo del Entrenador
Si usas macOS o Linux, puedes usar **Homebrew** para instalar estas herramientas de forma rápida.
:::

---

## 💻 Instalación por Sistema Operativo

### macOS / Linux

#### 1. Instalar Java 21

```bash
# Con Homebrew (recomendado)
brew install openjdk@21

# Verificar instalación
java -version
```

**Salida esperada**:
```
openjdk version "21.0.x" LTS
```

#### 2. Instalar Node.js

```bash
# Con Homebrew
brew install node@18

# Verificar instalación
node --version
npm --version
```

#### 3. Instalar Git

```bash
# Generalmente ya viene preinstalado
git --version

# Si no lo tienes:
brew install git
```

#### 4. Clonar el Proyecto

```bash
# Navega a donde quieras el proyecto
cd ~/Desarrollo

# Clona el repositorio
git clone <URL_DEL_REPOSITORIO>
cd PokemonTCG
```

---

### Windows

#### 1. Instalar Java 21

- Descarga desde [Oracle](https://www.oracle.com/java/technologies/downloads/) o [Eclipse Adoptium](https://adoptium.net/)
- Ejecuta el instalador
- Verifica en PowerShell:

```powershell
java -version
```

#### 2. Instalar Node.js

- Descarga desde [nodejs.org](https://nodejs.org/)
- Ejecuta el instalador (elige versión LTS 18+)
- Verifica en PowerShell:

```powershell
node --version
npm --version
```

#### 3. Instalar Git

- Descarga desde [git-scm.com](https://git-scm.com/)
- Ejecuta el instalador (usa configuración por defecto)

#### 4. Clonar el Proyecto

```powershell
# En PowerShell (como administrador)
cd $env:USERPROFILE\Desktop

git clone <URL_DEL_REPOSITORIO>
cd PokemonTCG
```

---

## 🔧 Configuración del Proyecto

### 1. Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto (si no existe):

```bash
# Backend
JAVA_HOME=/usr/libexec/java_home -v 21  # macOS
# o en Linux: JAVA_HOME=/usr/lib/jvm/java-21-openjdk

SERVER_PORT=8080
SPRING_PROFILE=dev

# Base de datos (desarrollo con H2)
SPRING_DATASOURCE_URL=jdbc:h2:mem:pokemontcg
SPRING_DATASOURCE_DRIVER=org.h2.Driver
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect

# JWT
JWT_SECRET=tu-secreto-super-secreto-cambiar-en-produccion
JWT_EXPIRATION=86400000

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

# Frontend
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080
```

:::warning ⚠️ Importante
En producción, **NUNCA** commits el archivo `.env` con datos sensibles. Usa variables de entorno del servidor.
:::

---

## 🚀 Levantando el Proyecto

### Opción A: Terminal Separadas (Recomendado)

#### Terminal 1 - Backend

```bash
cd PokemonTCG

# Con Maven wrapper (incluido)
./mvnw -f backend/pom.xml spring-boot:run

# O si lo prefieres:
cd backend
mvn spring-boot:run
```

**Salida esperada**:
```
[INFO] Started BackendApplication in X.XXX seconds
[INFO] Tomcat started on port(s): 8080
```

:::success ✅ Backend listo
El backend está corriendo en `http://localhost:8080`

Para ver la consola H2 (dev): `http://localhost:8080/h2-console`
:::

#### Terminal 2 - Frontend

```bash
cd PokemonTCG/frontend

# Instalar dependencias (primera vez)
npm install

# Iniciar servidor de desarrollo
npm start
```

**Salida esperada**:
```
[vite] v5.x.x
[vite] Local:    http://localhost:5173
[vite] ready in XXX ms
```

:::success ✅ Frontend listo
La aplicación está corriendo en `http://localhost:5173` (o el puerto que muestre)
:::

---

### Opción B: Docker Compose (Fácil)

Si tienes Docker instalado:

```bash
cd PokemonTCG

# Levanta backend + frontend + base de datos
docker-compose up --build

# En otra terminal, para ver logs:
docker-compose logs -f
```

**Servicios levantados**:
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- Base de datos: `localhost:5432` (PostgreSQL)

:::info Nota
La primera vez que ejecutas esto, Docker descargará las imágenes (puede tardar 5-10 minutos).
:::

---

## 🎮 Primera Sesión - Crear tu Cuenta

Una vez que tanto backend como frontend estén corriendo:

1. Abre `http://localhost:3000` (o el puerto del frontend)
2. Haz clic en **"Registrarse"**
3. Completa:
   - Email: `tumail@example.com`
   - Username: `Entrenador123`
   - Password: `Password123!`
4. Haz clic en **"Crear Cuenta"**

:::tip Consejo
Para desarrollo, puedes usar:
- Email: `dev@example.com`
- Username: `DevEntrenador`
- Password: `Dev12345!`
:::

---

## 🧪 Verificar que Funciona Todo

### Test del Backend

```bash
# Ver estado de la API
curl http://localhost:8080/api/health

# Respuesta esperada:
# {"status":"UP"}
```

### Test del Frontend

```bash
# Abre en navegador y verifica:
# - Pantalla de login carga
# - Puedes registrarte
# - Puedes hacer login
# - Se ve el lobby
```

### Test de WebSocket (Opcional)

```javascript
// En la consola del navegador (F12 → Console)
const ws = new WebSocket('ws://localhost:8080/ws/test');
ws.onopen = () => console.log('✅ WebSocket conectado');
ws.onerror = (err) => console.log('❌ Error:', err);
```

---

## 🐛 Troubleshooting Común

### Error: "Port 8080 is already in use"

**Causa**: Otro proceso está usando el puerto 8080

**Solución macOS/Linux**:
```bash
# Encuentra qué proceso usa el puerto
lsof -i :8080

# Mata el proceso (reemplaza PID)
kill -9 <PID>

# O usa otro puerto:
./mvnw -f backend/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=8081
```

**Solución Windows**:
```powershell
# Encuentra el proceso
netstat -ano | findstr :8080

# Mata el proceso (reemplaza PID)
taskkill /PID <PID> /F

# O usa otro puerto (actualiza en .env también)
```

---

### Error: "Cannot find Java"

**Causa**: Java no está instalado o no está en PATH

**Solución**:
```bash
# Verifica que Java esté instalado
java -version

# Si no funciona, actualiza JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk     # Linux
```

---

### Error: "npm install no funciona"

**Causa**: Node.js no está instalado correctamente

**Solución**:
```bash
# Limpia caché y reinstala
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

---

### Error: "CORS policy"

**Síntoma**: En consola aparece error de CORS

**Solución**: Verifica que `CORS_ALLOWED_ORIGINS` en `.env` incluya tu URL:
```env
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

---

### Error: "Database connection refused"

**Causa**: H2 no está funcionando (dev) o PostgreSQL no está corriendo (prod)

**Solución**:
```bash
# Para dev (H2), reinicia el backend
# Para prod (PostgreSQL), verifica que está corriendo
docker-compose up postgres -d

# O conecta manualmente:
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15
```

---

## 📊 Verificar el Setup

### Checklist de Instalación

- [ ] Java 21 instalado: `java -version`
- [ ] Node.js 18+ instalado: `node --version`
- [ ] Git instalado: `git --version`
- [ ] Proyecto clonado: `cd PokemonTCG && ls`
- [ ] `.env` creado con variables
- [ ] Backend levantado: `http://localhost:8080` responde
- [ ] Frontend levantado: `http://localhost:3000` o `:5173` carga
- [ ] Puedes hacer login
- [ ] WebSocket conecta (opcional pero recomendado)

---

## 📚 Siguientes Pasos

Una vez que tengas el proyecto corriendo localmente:

1. **Lee el código**:
   - Backend: `backend/src/main/java/com/pokemon/tcg/`
   - Frontend: `frontend/src/app/`

2. **Entiende la arquitectura**:
   - [Arquitectura Backend](/docs/tecnica/arquitectura-backend)
   - [Arquitectura Frontend](/docs/tecnica/arquitectura-frontend)

3. **Haz tu primer cambio**:
   - Modifica un controlador
   - Modifica un componente
   - Verifica que funcione

4. **Corre los tests**:
   - Backend: `./mvnw test`
   - Frontend: `npm test`

---

## 🤝 Contribuir al Proyecto

Una vez que el setup funciona:

```bash
# 1. Crea una rama
git checkout -b feature/mi-feature

# 2. Haz cambios y testa localmente

# 3. Commit
git add .
git commit -m "feat: descripción de mi cambio"

# 4. Push
git push origin feature/mi-feature

# 5. Abre un Pull Request en GitHub
```

[Ver guía de Setup Local](/docs/operaciones/setup-local)

---

## 🎓 Recursos Útiles

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **React Docs**: https://react.dev
- **TypeScript Handbook**: https://www.typescriptlang.org/docs/
- **RxJS Docs**: https://rxjs.dev
- **Pokémon TCG Official**: https://www.pokemontcg.io

---

## ❓ ¿Ayuda?

Si tienes problemas:

1. Revisa el [Troubleshooting](/docs/operaciones/troubleshooting)
2. Busca en Issues de GitHub
3. Abre un issue describiendo el problema

:::success ¡Felicidades!
🎉 Ya tienes el proyecto corriendo. **¡Que comience tu aventura de desarrollo!**

**Gotta Code 'Em All!** ⚡
:::

---

*Última actualización: 2026-06-08*
