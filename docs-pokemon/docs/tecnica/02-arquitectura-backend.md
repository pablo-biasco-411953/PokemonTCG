---
sidebar_position: 2
title: 🔧 Arquitectura Backend
---

# 🔧 Arquitectura Backend - Spring Boot

> Estructura de capas y patrones implementados

---

## 📦 Estructura de Carpetas

```
backend/src/main/java/com/pokemon/tcg/
├── controller/
│   ├── AuthController.java
│   ├── BattleController.java
│   ├── CardController.java
│   ├── JugadorController.java
│   ├── MazoController.java
│   └── SobreController.java
│
├── service/
│   ├── AuthService.java
│   ├── BattleService.java
│   ├── CardService.java
│   ├── JugadorService.java
│   ├── MazoService.java
│   └── SobreService.java
│
├── repository/
│   ├── JugadorRepository.java
│   ├── CardRepository.java
│   ├── MazoRepository.java
│   └── ParidaRepository.java
│
├── model/ (Entities)
│   ├── Jugador.java
│   ├── Card.java
│   ├── Mazo.java
│   ├── Partida.java
│   └── enums/
│       ├── EnergyType.java
│       ├── CardRarity.java
│       └── BattleState.java
│
├── dto/
│   ├── JugadorDTO.java
│   ├── CardDTO.java
│   ├── LoginRequest.java
│   └── (20+ DTOs)
│
├── config/
│   ├── CorsConfig.java
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   └── DataLoader.java
│
├── exception/
│   ├── CardNotFoundException.java
│   ├── InvalidBattleActionException.java
│   └── (Custom exceptions)
│
└── BackendApplication.java
```

---

## 🎯 Controller Layer

```java
@RestController
@RequestMapping("/api/batalla")
public class BattleController {

    @Autowired
    private BattleService battleService;
    
    @PostMapping("/iniciar")
    public ResponseEntity<BattleDTO> startBattle(
        @RequestBody StartBattleRequest request) {
        // Iniciar partida
        Partida partida = battleService.iniciar(request);
        return ResponseEntity.ok(mapToDTO(partida));
    }
    
    @PostMapping("/{id}/accion")
    public ResponseEntity<BattleStateDTO> executeAction(
        @PathVariable Long id,
        @RequestBody BattleActionRequest action) {
        // Ejecutar acción (atacar, unir energía, etc)
        BattleState state = battleService.executeAction(id, action);
        return ResponseEntity.ok(mapToDTO(state));
    }
}
```

**Responsabilidades**:
- ✅ Recibir peticiones HTTP
- ✅ Validar entrada básica
- ✅ Delegar a Service
- ✅ Devolver respuesta HTTP

---

## 🔧 Service Layer

```java
@Service
public class BattleService {

    @Autowired
    private BattleRepository battleRepository;
    
    @Autowired
    private CardService cardService;
    
    public Partida iniciar(StartBattleRequest request) {
        // Validar jugadores
        Jugador j1 = jugadorRepository.findById(request.getJugador1Id());
        Jugador j2 = jugadorRepository.findById(request.getJugador2Id());
        
        // Crear partida
        Partida partida = new Partida();
        partida.setJugador1(j1);
        partida.setJugador2(j2);
        partida.setEstado(BattleState.LOBBY);
        
        // Guardar
        return battleRepository.save(partida);
    }
    
    public BattleState executeAction(Long battleId, BattleActionRequest action) {
        Partida partida = battleRepository.findById(battleId);
        
        // Validar acción
        if (!isActionValid(partida, action)) {
            throw new InvalidBattleActionException();
        }
        
        // Ejecutar lógica según tipo
        switch(action.getType()) {
            case JUGAR_POKEMON -> jugarPokemon(partida, action);
            case UNIR_ENERGIA -> unirEnergia(partida, action);
            case ATACAR -> atacar(partida, action);
            case PASAR_TURNO -> pasarTurno(partida);
        }
        
        // Guardar y devolver
        battleRepository.save(partida);
        return partida.getEstado();
    }
}
```

**Responsabilidades**:
- ✅ Lógica de negocio
- ✅ Validaciones
- ✅ Orquestación
- ✅ Transacciones

---

## 💾 Repository Layer

```java
public interface BattleRepository extends JpaRepository<Partida, Long> {
    
    // Spring genera SQL automáticamente
    Partida findByEstado(BattleState estado);
    
    List<Partida> findByJugador1(Jugador jugador);
    
    List<Partida> findByJugador2(Jugador jugador);
    
    @Query("SELECT p FROM Partida p WHERE p.ganador IS NULL")
    List<Partida> findActiveBattles();
}
```

**Responsabilidades**:
- ✅ Acceso a BD (CRUD)
- ✅ Queries especializadas
- ✅ Spring genera SQL

---

## 📊 Entity Layer

```java
@Entity
@Table(name = "cartas")
public class Card {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    
    @Enumerated(EnumType.STRING)
    private EnergyType tipo;
    
    private Integer hp;
    
    @Column(columnDefinition = "JSON")
    private List<Ataque> ataques;
    
    @Column(columnDefinition = "JSON")
    private Habilidad habilidad;
    
    @ManyToOne
    @JoinColumn(name = "evolucion_de_id")
    private Card evolucionDe;
    
    @Enumerated(EnumType.STRING)
    private CardRarity rareza;
    
    // Getters y setters
}
```

---

## 📡 Flujo Completo

```
1. Cliente envía: POST /api/batalla/1/accion
   {
     "type": "JUGAR_POKEMON",
     "pokemonId": "025"
   }

2. BattleController recibe
   → valida estructura
   → delega a BattleService

3. BattleService
   → obtiene partida
   → valida acción
   → ejecuta lógica (jugarPokemon)
   → guarda cambios

4. BattleRepository
   → ejecuta UPDATE en BD

5. Controller devuelve
   { "estado": "WAITING_FOR_OPPONENT" }

6. Cliente recibe respuesta
```

---

## ⚙️ Patrones Implementados

✅ **Service Pattern**: Controllers → Services (separación responsabilidad)
✅ **Repository Pattern**: Abstracción de BD
✅ **DTO Pattern**: Data Transfer Objects (no exponer entities)
✅ **Dependency Injection**: Autowired (IoC)
✅ **Transactional**: @Transactional para consistencia

---

*Próximo: [Arquitectura Frontend](/docs/tecnica/arquitectura-frontend)*
