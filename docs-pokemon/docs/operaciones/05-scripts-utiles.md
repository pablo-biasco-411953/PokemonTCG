---
sidebar_position: 5
title: "📜 Scripts Utiles"
---

# Scripts Utiles

> Comandos frecuentes para desarrollo, build y deployment

---

## Backend

### Desarrollo

```bash
# Levantar el backend (desde la raiz del proyecto)
./mvnw -f backend/pom.xml spring-boot:run

# O desde la carpeta backend
cd backend
mvn spring-boot:run

# Con perfil especifico
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090

# Compilar sin ejecutar
mvn clean package -DskipTests

# Ejecutar tests
mvn test
```

### Build del JAR

```bash
cd backend

# Generar JAR
mvn clean package -DskipTests

# Ejecutar el JAR directamente
java -jar target/backend-0.0.1-SNAPSHOT.jar

# Con variables de entorno
DB_URL=jdbc:mysql://myserver:3306/pokemon_tcg \
DB_USERNAME=admin \
DB_PASSWORD=secret \
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## Frontend

### Desarrollo

```bash
cd frontend

# Instalar dependencias (primera vez o tras cambios en package.json)
npm install

# Servidor de desarrollo con hot-reload
npm start
# -> http://localhost:4200

# Build con watch (recompila al guardar)
npm run watch
```

### Build de Produccion

```bash
cd frontend

# Build optimizado
npm run build
# Output: dist/frontend/

# Tests
npm test

# Test unitario especifico
npm run test:unit
```

---

## Docker

### Base de Datos MySQL

```bash
# Levantar MySQL
docker-compose up -d

# Ver logs
docker-compose logs -f db

# Detener
docker-compose down

# Detener y BORRAR datos (cuidado!)
docker-compose down -v

# Ver estado
docker-compose ps
```

### Backend Dockerizado

```bash
cd backend

# Construir imagen
docker build -t pokemon-tcg-backend .

# Ejecutar con MySQL local
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/pokemon_tcg \
  pokemon-tcg-backend

# Ejecutar con variables de produccion
docker run -p 8080:8080 \
  -e DB_URL=$DB_URL \
  -e DB_USERNAME=$DB_USERNAME \
  -e DB_PASSWORD=$DB_PASSWORD \
  -e MAIL_ENABLED=true \
  pokemon-tcg-backend
```

---

## Git

### Flujo de Trabajo

```bash
# Crear rama para feature
git checkout -b feature/mi-feature

# Ver cambios
git status
git diff

# Commit
git add .
git commit -m "feat: descripcion del cambio"

# Push
git push origin feature/mi-feature
```

---

## Limpieza

```bash
# Limpiar build del backend
cd backend && mvn clean

# Limpiar node_modules del frontend
cd frontend && rm -rf node_modules && npm install

# Limpiar cache de npm
npm cache clean --force

# Limpiar volumenes Docker
docker-compose down -v
docker system prune -f
```

---

## Verificacion Rapida

Script para verificar que todo esta instalado:

```bash
echo "=== Verificacion de Herramientas ==="
echo "Java:    $(java -version 2>&1 | head -1)"
echo "Maven:   $(mvn -version 2>&1 | head -1)"
echo "Node:    $(node --version)"
echo "npm:     $(npm --version)"
echo "Git:     $(git --version)"
echo "Docker:  $(docker --version 2>/dev/null || echo 'No instalado')"
```

---

## Comandos de Debug

### Backend

```bash
# Ver endpoints disponibles (Swagger/OpenAPI)
# Abrir: http://localhost:8080/swagger-ui.html

# Health check
curl http://localhost:8080/api/health

# Ver logs en tiempo real (si se ejecuta con Maven)
# Los logs se imprimen directamente en la terminal
```

### Frontend

```bash
# Abrir DevTools del navegador
# F12 -> Console para ver errores
# F12 -> Network para ver requests HTTP/WS

# Verificar conexion WebSocket
# En consola del navegador:
# new WebSocket('ws://localhost:8080/lobby-ws')
```
