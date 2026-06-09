---
sidebar_position: 6
title: "🔥 Troubleshooting"
---

# Troubleshooting - Problemas Comunes

> Soluciones a los errores mas frecuentes durante desarrollo y deployment

---

## Backend

### Puerto 8080 en uso

**Error**: `Web server failed to start. Port 8080 was already in use.`

```bash
# macOS/Linux: encontrar el proceso
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Alternativa: usar otro puerto
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

---

### Java no encontrado

**Error**: `JAVA_HOME is not set` o `java: command not found`

```bash
# Verificar instalacion
java -version

# macOS: configurar JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Linux
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Agregar al PATH permanentemente (agregar a ~/.zshrc o ~/.bashrc)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
```

---

### MySQL connection refused

**Error**: `Communications link failure` o `Connection refused to host: localhost`

```bash
# Verificar que MySQL este corriendo
docker-compose ps

# Si no esta corriendo
docker-compose up -d

# Verificar conexion
mysql -h localhost -P 3306 -u root pokemon_tcg

# Si Docker no funciona, verificar el puerto
docker-compose logs db
```

---

### cards.json no encontrado

**Error**: `No se encontro /cards.json dentro del build`

El archivo `cards.json` debe estar en `backend/src/main/resources/cards.json`. Si falta:
- Verificar que existe en el repositorio
- Recompilar: `mvn clean package`
- El archivo se empaqueta automaticamente en el JAR

---

### Hibernate ddl-auto problemas

**Error**: Schema mismatch o tablas no creadas

```bash
# Opcion 1: Recrear todo (BORRA DATOS)
# En application.properties:
# spring.jpa.hibernate.ddl-auto=create

# Opcion 2: Borrar y recrear la BD
docker-compose down -v
docker-compose up -d
# Reiniciar backend
```

| Valor | Efecto |
|-------|--------|
| `update` | Actualiza el esquema sin borrar datos (default) |
| `create` | Borra y recrea tablas al iniciar |
| `create-drop` | Crea al iniciar, borra al cerrar |
| `validate` | Solo valida, no modifica |
| `none` | No hace nada |

---

## Frontend

### npm install falla

**Error**: Dependencias no se instalan o conflictos de version

```bash
# Limpiar todo y reinstalar
rm -rf node_modules package-lock.json
npm cache clean --force
npm install

# Si hay conflictos de peer dependencies
npm install --legacy-peer-deps
```

---

### CORS Policy error

**Error**: `Access to XMLHttpRequest has been blocked by CORS policy`

El backend tiene CORS configurado como permisivo (`allowedOriginPatterns("*")`). Si aun hay problemas:

1. Verificar que el backend esta corriendo en el puerto correcto
2. Verificar que `api-config.ts` detecta el entorno correctamente
3. En DevTools -> Network, verificar la URL del request

---

### WebSocket no conecta

**Error**: `WebSocket connection to 'ws://...' failed`

Posibles causas:
1. **Backend no esta corriendo**: Verificar `http://localhost:8080`
2. **URL incorrecta**: El WebSocket se conecta a `/lobby-ws`, no a `/ws`
3. **HTTPS en produccion**: Requiere `wss://` en lugar de `ws://`

```javascript
// Test manual en consola del navegador
const ws = new WebSocket('ws://localhost:8080/lobby-ws');
ws.onopen = () => console.log('Conectado');
ws.onerror = (e) => console.log('Error:', e);
```

---

### Imagenes de cartas no cargan

**Error**: Cartas se muestran sin imagen o con placeholder

Las imagenes se cargan desde URLs externas del Pokemon TCG API. Posibles causas:
- **Red lenta**: El `ImagePreloaderService` precarga hasta 18 imagenes; puede tardar
- **URLs caidas**: Las URLs de las imagenes dependen de servidores externos
- **CORS de imagenes**: Algunos CDNs bloquean cross-origin; las imagenes usan `Image()` del DOM que no tiene esta restriccion

---

## Docker

### Docker daemon no corre

**Error**: `Cannot connect to the Docker daemon`

```bash
# macOS: abrir Docker Desktop
open -a Docker

# Linux: iniciar el servicio
sudo systemctl start docker

# Verificar
docker info
```

---

### Puerto 3306 en uso

**Error**: `Bind for 0.0.0.0:3306 failed: port is already allocated`

```bash
# Otro MySQL ya esta corriendo
# macOS
brew services stop mysql

# Linux
sudo systemctl stop mysql

# O cambiar el puerto en docker-compose.yml
ports:
  - "3307:3306"
# Y actualizar DB_URL con el nuevo puerto
```

---

## Produccion (Render)

### App se duerme

Render en plan gratuito **duerme** la app tras 15 minutos de inactividad. El primer request tras dormirse tarda ~30 segundos.

**Solucion**: Usar un servicio de ping (UptimeRobot, cron-job.org) que haga un request cada 14 minutos.

---

### Build falla en Render

Verificar:
1. El `Dockerfile` esta en la raiz de `backend/`
2. Las variables de entorno estan configuradas en el dashboard
3. El build de Maven no requiere tests (usa `-DskipTests`)
4. La version de Java en el Dockerfile coincide con la del proyecto (21)

---

## Checklist de Diagnostico

Cuando algo no funciona, verificar en orden:

1. **Java**: `java -version` muestra Java 21
2. **MySQL**: `docker-compose ps` muestra el contenedor running
3. **Backend**: `curl http://localhost:8080/api/health` responde
4. **Frontend**: `http://localhost:4200` carga
5. **WebSocket**: DevTools -> Network -> WS muestra conexion activa
6. **Logs**: Revisar la terminal del backend para errores de Spring Boot
