---
sidebar_position: 1
title: 📦 Stack Tecnológico
---

# 📦 Stack Tecnológico - Las Herramientas que Usamos

> Tecnologías, versiones y justificación de cada elección

---

## 🏗️ Stack General

```
┌─────────────────────────────────────────┐
│  Frontend (React + TypeScript)          │
│  • React 18+                            │
│  • TypeScript 5+                        │
│  • RxJS (State Management)              │
│  • Three.js (Gráficos 3D)               │
│  • Node.js 18+                          │
└─────────────┬───────────────────────────┘
              │ HTTP + WebSocket
              ↓
┌─────────────────────────────────────────┐
│  Backend (Spring Boot + Java)           │
│  • Java 21 LTS                          │
│  • Spring Boot 3.2.4                    │
│  • Spring Data JPA                      │
│  • Spring WebSocket                     │
│  • Spring Security                      │
│  • Maven 3.8+                           │
└─────────────┬───────────────────────────┘
              │ JDBC/JPA
              ↓
┌─────────────────────────────────────────┐
│  Database                               │
│  • H2 (Desarrollo en memoria)           │
│  • PostgreSQL (Producción)              │
└─────────────────────────────────────────┘
```

---

## 💻 Backend

### Java 21 LTS
**Por qué**: 
- ✅ LTS (Long Term Support)
- ✅ Virtual Threads (concurrencia mejorada)
- ✅ Record Types (datos inmutables)
- ✅ Sealed Classes (tipos seguros)

```java
// Java 21: Record para DTOs
record JugadorDTO(
    Long id,
    String username,
    String email
) {}
```

### Spring Boot 3.2.4
**Por qué**:
- ✅ Framework enterprise más popular
- ✅ Inyección de dependencias automática
- ✅ Configuración mínima
- ✅ Ecosistema enorme

**Módulos usados**:
- **spring-boot-starter-web** - REST API
- **spring-boot-starter-data-jpa** - ORM (Object-Relational Mapping)
- **spring-boot-starter-security** - Autenticación/Autorización
- **spring-boot-starter-websocket** - Comunicación tiempo real
- **h2-database** - BD en desarrollo
- **spring-boot-starter-validation** - Validación de datos

### Maven
**Por qué**:
- ✅ Build tool estándar
- ✅ Gestión de dependencias
- ✅ Plugins para testing, packaging

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Tests
./mvnw test
```

---

## 🎨 Frontend

### React 18+
**Por qué**:
- ✅ Librería UI más popular
- ✅ Virtual DOM (rendimiento)
- ✅ Component-based (modular)
- ✅ Hooks (manejo de estado simple)

```typescript
// React Component
export const BattleBoard: React.FC = () => {
  const [state, setState] = useState(initialState);
  
  return (
    <div className="battle-board">
      {/* UI aquí */}
    </div>
  );
};
```

### TypeScript 5+
**Por qué**:
- ✅ Tipos estáticos
- ✅ Menos bugs en runtime
- ✅ Mejor autocompletion
- ✅ Documentación auto-generada

```typescript
// TypeScript: Tipos seguros
interface Pokemon {
  id: string;
  name: string;
  hp: number;
  type: EnergyType;
}

const pokemon: Pokemon = {
  id: "025",
  name: "Pikachu",
  hp: 35,
  type: "ELECTRIC"
};
```

### RxJS
**Por qué**:
- ✅ Programación reactiva
- ✅ Flujos de datos asíncronos
- ✅ Manejo limpio de WebSocket
- ✅ State Management sin Redux

```typescript
// RxJS: Observables y operadores
const battle$ = battleService.getBattle(battleId).pipe(
  filter(b => b.state === 'ACTIVE'),
  map(b => b.player1),
  switchMap(p => playerService.getStats(p.id))
);
```

### Three.js
**Por qué**:
- ✅ Gráficos 3D en web
- ✅ Visualización de Pokémon
- ✅ Animaciones de batalla
- ✅ Librería madura

```typescript
// Three.js: Renderizar Pokémon en 3D
const scene = new THREE.Scene();
const pokemonModel = await loader.load('pikachu.gltf');
scene.add(pokemonModel.scene);
```

---

## 🗄️ Database

### H2 (Desarrollo)
```properties
# application.properties
spring.datasource.url=jdbc:h2:mem:pokemontcg
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Console en http://localhost:8080/h2-console
spring.h2.console.enabled=true
```

**Ventajas**:
- ✅ En memoria (rápido para desarrollo)
- ✅ No requiere instalación
- ✅ SQL estándar

### PostgreSQL (Producción)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pokemontcg
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL10Dialect
```

**Ventajas**:
- ✅ BD relacional robusta
- ✅ Escalable
- ✅ ACID compliant
- ✅ Open source

---

## 🏗️ Arquitectura - Capas

```
┌────────────────────────────────────┐
│  Controller Layer (HTTP)           │
│  @RestController endpoints         │
└────────────────┬───────────────────┘
                 ↓
┌────────────────────────────────────┐
│  Service Layer (Lógica)            │
│  @Service business logic           │
└────────────────┬───────────────────┘
                 ↓
┌────────────────────────────────────┐
│  Repository Layer (Datos)          │
│  JpaRepository acceso BD           │
└────────────────┬───────────────────┘
                 ↓
┌────────────────────────────────────┐
│  Entity Layer (Modelos)            │
│  @Entity JPA classes               │
└────────────────┬───────────────────┘
                 ↓
┌────────────────────────────────────┐
│  Database (H2 o PostgreSQL)        │
└────────────────────────────────────┘
```

---

## 🔄 Flujo de una Petición

```
1. Cliente (React) envía petición HTTP
   GET http://localhost:8080/api/cartas/025
   
2. Spring despachador enruta a Controller
   @GetMapping("/api/cartas/{id}")
   public ResponseEntity<CardDTO> getCard(@PathVariable String id)
   
3. Controller delega a Service
   cardService.findById(id)
   
4. Service ejecuta lógica
   validar entrada
   buscar en BD
   aplicar transformaciones
   
5. Repository consulta BD
   cardRepository.findById(id)
   
6. BD devuelve Entity
   Card entity
   
7. Service convierte a DTO
   new CardDTO(entity)
   
8. Controller devuelve respuesta
   ResponseEntity.ok(cardDTO)
   
9. Spring serializa a JSON
   
10. Cliente recibe:
    {
      "id": "025",
      "name": "Pikachu",
      "type": "ELECTRIC",
      "hp": 35
    }
```

---

## 📊 Resumen Tech Stack

| Capa | Tecnología | Versión | Rol |
|------|-----------|---------|-----|
| **Frontend** | React | 18+ | UI |
| **Language** | TypeScript | 5+ | Tipado |
| **State** | RxJS | 7+ | Observables |
| **3D** | Three.js | Latest | Gráficos |
| **Runtime** | Node.js | 18+ | Ejecución |
| **Backend** | Spring Boot | 3.2.4 | API |
| **Language** | Java | 21 LTS | Lógica |
| **ORM** | JPA/Hibernate | Latest | BD Mapping |
| **Build** | Maven | 3.8+ | Build tool |
| **Dev DB** | H2 | Latest | En memoria |
| **Prod DB** | PostgreSQL | 12+ | Relacional |

---

## 🚀 Por Qué Estas Herramientas

### Filosofía de Selección

✅ **Maduras**: Todas tienen 5+ años de uso en producción
✅ **Populares**: Comunidad grande, fácil conseguir devs
✅ **Open Source**: Transparencia y flexibilidad
✅ **Escalables**: Crecen con el proyecto
✅ **Bien documentadas**: Documentación oficial completa
✅ **Enterprise-ready**: Usadas por grandes compañías

### Alternativas Consideradas y Descartadas

| Aspecto | Elegido | Alternativa | Por qué NO |
|--------|---------|------------|-----------|
| Backend | Spring Boot | Node.js | Menos robusto para lógica compleja |
| Language | Java | Python | Menos performance crítica |
| Frontend | React | Vue | Menos ecosystem |
| State | RxJS | Redux | RxJS es más flexible |
| DB | PostgreSQL | MongoDB | Relational mejor para TCG |

---

## 📚 Próximos Pasos

1. [Arquitectura Backend](/docs/tecnica/arquitectura-backend)
2. [Arquitectura Frontend](/docs/tecnica/arquitectura-frontend)
3. [Diseño de BD](/docs/tecnica/database-design)

---

*Última actualización: 2026-06-08*
