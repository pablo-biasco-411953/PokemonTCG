package com.pokemon.tcg.config;

import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.AttackTranslationRepository;
import com.pokemon.tcg.repository.CardTranslationRepository;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.service.CardCatalogService;
import com.pokemon.tcg.service.MazoBackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataLoaderTest {

    @Mock
    private CardCatalogService cardCatalogService;
    @Mock
    private JugadorRepository jugadorRepo;
    @Mock
    private MazoRepository mazoRepo;
    @Mock
    private MazoBackupService mazoBackupService;
    @Mock
    private DataSource dataSource;
    @Mock
    private CardTranslationRepository cardTranslationRepo;
    @Mock
    private AttackTranslationRepository attackTranslationRepo;

    @Mock
    private Connection connection;
    @Mock
    private Statement statement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private ResultSetMetaData resultSetMetaData;

    @InjectMocks
    private DataLoader dataLoader;

    @BeforeEach
    void setUp() throws Exception {
        // Mocking database migration behavior
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.createStatement()).thenReturn(statement);
        lenient().when(statement.executeQuery(anyString())).thenReturn(resultSet);
        lenient().when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        lenient().when(resultSetMetaData.getColumnCount()).thenReturn(1);
        lenient().when(resultSetMetaData.getColumnName(1)).thenReturn("santo_coins");
    }

    @Test
    void run_createsUsersAndMigratesData() throws Exception {
        List<Card> cards = new ArrayList<>();
        Card c1 = new Card();
        c1.setId("test-1");
        c1.setSupertype("Pokemon");
        c1.setTipo("Fire");
        cards.add(c1);

        when(cardCatalogService.getCatalogo()).thenReturn(cards);

        // Simulate no users exist
        when(jugadorRepo.findByUsername(anyString())).thenReturn(null);

        dataLoader.run();

        // Verify users created
        verify(jugadorRepo, times(3)).save(any(Jugador.class));
        verify(mazoBackupService, times(1)).restoreMissingDecks();
        verify(mazoBackupService, times(1)).backupAll();
    }

    @Test
    void run_updatesUsersIfExist() throws Exception {
        List<Card> cards = new ArrayList<>();
        Card c1 = new Card();
        c1.setId("test-1");
        c1.setSupertype("Pokemon");
        c1.setTipo("Fire");
        cards.add(c1);

        when(cardCatalogService.getCatalogo()).thenReturn(cards);

        Jugador pablo = new Jugador("Pablo");
        pablo.setColeccion(new ArrayList<>());
        Jugador fran = new Jugador("Fran");
        fran.setColeccion(new ArrayList<>());
        Jugador bot = new Jugador("BOT");
        bot.setColeccion(new ArrayList<>());

        when(jugadorRepo.findByUsername("Pablo")).thenReturn(pablo);
        when(jugadorRepo.findByUsername("Fran")).thenReturn(fran);
        when(jugadorRepo.findByUsername("BOT")).thenReturn(bot);

        dataLoader.run();

        // Solo BOT es actualizado via actualizarColeccionUsuario cuando ya existe.
        // Pablo y Fran con colección vacía (< 584 cartas) no se resetean.
        verify(jugadorRepo, times(1)).save(any(Jugador.class));
    }

    @Test
    void run_catalogoThrowsException_returnsEarly() throws Exception {
        when(cardCatalogService.getCatalogo()).thenThrow(new RuntimeException("catalog error"));

        dataLoader.run();

        verify(jugadorRepo, never()).findByUsername(anyString());
        verify(mazoBackupService, never()).restoreMissingDecks();
    }

    @Test
    void run_reseteaPabloConColeccionGrande_mazoEncontrado() throws Exception {
        List<Card> catalogCards = buildCardList(1);
        when(cardCatalogService.getCatalogo()).thenReturn(catalogCards);

        Jugador pablo = new Jugador("Pablo");
        pablo.setColeccion(buildCardList(584));

        Jugador fran = new Jugador("Fran");
        fran.setColeccion(new ArrayList<>());

        Jugador bot = new Jugador("BOT");
        bot.setColeccion(new ArrayList<>());

        when(jugadorRepo.findByUsername("Pablo")).thenReturn(pablo);
        when(jugadorRepo.findByUsername("Fran")).thenReturn(fran);
        when(jugadorRepo.findByUsername("BOT")).thenReturn(bot);

        Mazo mazo = new Mazo("Mazo Test", pablo);
        mazo.setCartas(buildCardList(2));
        when(mazoRepo.findByJugador(pablo)).thenReturn(List.of(mazo));

        dataLoader.run();

        verify(jugadorRepo, atLeastOnce()).save(pablo);
    }

    @Test
    void run_reseteaPabloConColeccionGrande_sinMazo_creaMazoPorDefecto() throws Exception {
        List<Card> catalogCards = buildPokemonAndEnergyCards();
        when(cardCatalogService.getCatalogo()).thenReturn(catalogCards);

        Jugador pablo = new Jugador("Pablo");
        pablo.setColeccion(buildCardList(584));

        when(jugadorRepo.findByUsername("Pablo")).thenReturn(pablo);
        when(jugadorRepo.findByUsername("Fran")).thenReturn(null);
        when(jugadorRepo.findByUsername("BOT")).thenReturn(null);

        when(mazoRepo.findByJugador(pablo)).thenReturn(new ArrayList<>());

        dataLoader.run();

        verify(jugadorRepo, atLeastOnce()).save(pablo);
    }

    @Test
    void run_migrarTraducciones_idiomasYaExistentes_saltea() throws Exception {
        List<Card> cards = buildCardList(1);
        when(cardCatalogService.getCatalogo()).thenReturn(cards);
        when(cardTranslationRepo.existsByLang(anyString())).thenReturn(true);
        when(jugadorRepo.findByUsername(anyString())).thenReturn(null);

        dataLoader.run();

        verify(cardTranslationRepo, never()).saveAll(any());
    }

    private List<Card> buildCardList(int count) {
        return IntStream.range(0, count).mapToObj(i -> {
            Card c = new Card();
            c.setId("card-" + i);
            c.setSupertype("Pokemon");
            c.setSubtypes(List.of("Basic"));
            return c;
        }).collect(Collectors.toList());
    }

    private List<Card> buildPokemonAndEnergyCards() {
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Card c = new Card();
            c.setId("poke-" + i);
            c.setSupertype("Pokemon");
            c.setSubtypes(List.of("Basic"));
            result.add(c);
        }
        for (int i = 0; i < 10; i++) {
            Card e = new Card();
            e.setId("energy-" + i);
            e.setSupertype("Energy");
            e.setSubtypes(List.of());
            result.add(e);
        }
        return result;
    }
}

