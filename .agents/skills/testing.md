# Skill — Testing

---

## Backend (JUnit / Mockito)

### Ubicación
```
BE/src/test/java/com/pokemon/tcg/service/
├── BattleAttackServiceTest.java
├── BattleKoServiceTest.java
├── BattleTurnServiceTest.java
└── BotAIServiceTest.java
```

### Patrón de test

```java
@ExtendWith(MockitoExtension.class)
class BattleAttackServiceTest {

    @Mock
    private BattleKoService battleKoService;

    @InjectMocks
    private BattleAttackService battleAttackService;

    @Test
    void ejecutarAtaque_conDebilidadFuego_duplicaDano() {
        // Arrange
        CartaEnJuego atacante = crearCarta("Charmander", TipoEnergia.FUEGO);
        CartaEnJuego defensor = crearCarta("Caterpie", TipoEnergia.PLANTA); // débil al fuego

        // Act
        ResultadoAtaque resultado = battleAttackService.ejecutar(atacante, defensor, ataque);

        // Assert
        assertThat(resultado.getDanoAplicado()).isEqualTo(ataque.getDano() * 2);
    }
}
```

### Reglas
- Mockeá repositorios y servicios externos. No toques la base de datos real.
- Un test = un comportamiento. No combines múltiples asserts no relacionados.
- Nombrá: `metodo_condicion_resultado`.
- Cubrí el happy path y al menos un caso de error por método crítico.

---

## Frontend (Jasmine / Karma)

### Patrón de test de servicio

```typescript
describe('BattleBoardAttackService', () => {
  let service: BattleBoardAttackService;
  let battleServiceSpy: jasmine.SpyObj<BattleService>;

  beforeEach(() => {
    battleServiceSpy = jasmine.createSpyObj('BattleService', ['attack']);
    TestBed.configureTestingModule({
      providers: [{ provide: BattleService, useValue: battleServiceSpy }]
    });
    service = TestBed.inject(BattleBoardAttackService);
  });

  it('should call battle service with correct params', () => {
    battleServiceSpy.attack.and.returnValue(of(mockResult));
    service.executeAttack(mockCard, mockAttack).subscribe();
    expect(battleServiceSpy.attack).toHaveBeenCalledWith(mockCard.id, mockAttack.id);
  });
});
```

### Reglas
- Usá `jasmine.SpyObj` para mockear servicios HTTP.
- No hagas llamadas HTTP reales en tests — usá `HttpClientTestingModule`.
- Cubrí los servicios de `core/` y los servicios de features con lógica compleja.
