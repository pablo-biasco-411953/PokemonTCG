package com.pokemon.tcg.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.model.Card;
import com.pokemon.tcg.model.CardTranslation;
import com.pokemon.tcg.model.battle.AttackTranslation;
import com.pokemon.tcg.model.battle.Ataque;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.model.Mazo;
import com.pokemon.tcg.repository.JugadorRepository;
import com.pokemon.tcg.repository.MazoRepository;
import com.pokemon.tcg.repository.CardTranslationRepository;
import com.pokemon.tcg.repository.AttackTranslationRepository;
import com.pokemon.tcg.service.CardCatalogService;
import com.pokemon.tcg.service.MazoBackupService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class DataLoader implements CommandLineRunner {

    private final CardCatalogService cardCatalogService;
    private final JugadorRepository jugadorRepo;
    private final MazoRepository mazoRepo;
    private final MazoBackupService mazoBackupService;
    private final DataSource dataSource;
    private final CardTranslationRepository cardTranslationRepo;
    private final AttackTranslationRepository attackTranslationRepo;

    public DataLoader(CardCatalogService cardCatalogService,
                      JugadorRepository jugadorRepo,
                      MazoRepository mazoRepo,
                      MazoBackupService mazoBackupService,
                      DataSource dataSource,
                      CardTranslationRepository cardTranslationRepo,
                      AttackTranslationRepository attackTranslationRepo) {
        this.cardCatalogService = cardCatalogService;
        this.jugadorRepo = jugadorRepo;
        this.mazoRepo = mazoRepo;
        this.mazoBackupService = mazoBackupService;
        this.dataSource = dataSource;
        this.cardTranslationRepo = cardTranslationRepo;
        this.attackTranslationRepo = attackTranslationRepo;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Eliminar columna obsoleta 'santo_coins' si existe (para evitar errores en DBs heredadas)
        try (Connection conn = dataSource.getConnection()) {
            boolean columnExists = false;
            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM jugadores LIMIT 1")) {
                java.sql.ResultSetMetaData rsmd = rs.getMetaData();
                int columns = rsmd.getColumnCount();
                for (int i = 1; i <= columns; i++) {
                    if ("santo_coins".equalsIgnoreCase(rsmd.getColumnName(i))) {
                        columnExists = true;
                        break;
                    }
                }
            }
            if (columnExists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE jugadores DROP COLUMN santo_coins");
                    System.out.println("[DataLoader] Columna obsoleta 'santo_coins' eliminada con exito.");
                }
            }
        } catch (Exception e) {
            System.out.println("[DataLoader] Info: no se pudo eliminar 'santo_coins' (es normal si ya fue eliminada o no existe): " + e.getMessage());
        }
        List<Card> todasLasCartas;

        try {
            todasLasCartas = cardCatalogService.getCatalogo();
            System.out.println("[DataLoader] Catalogo de cartas listo. Total: " + todasLasCartas.size());
        } catch (Exception e) {
            System.out.println("[DataLoader] Error cargando cartas: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Migrar traducciones ahora que las cartas base y ataques estan en la BD
        migrarTraduccionesSiEsNecesario();

        Jugador pablo = jugadorRepo.findByUsername("Pablo");
        if (pablo == null) {
            crearUsuarioTest(todasLasCartas);
        } else if (pablo.getColeccion() != null && pablo.getColeccion().size() >= 584) {
            resetearColeccionAMazoInicial(pablo, todasLasCartas);
        }

        Jugador fran = jugadorRepo.findByUsername("Fran");
        if (fran == null) {
            crearUsuarioFran(todasLasCartas);
        } else if (fran.getColeccion() != null && fran.getColeccion().size() >= 584) {
            resetearColeccionAMazoInicial(fran, todasLasCartas);
        }

        Jugador bot = jugadorRepo.findByUsername("BOT");
        if (bot == null) {
            crearBotUser(todasLasCartas);
        } else {
            actualizarColeccionUsuario(bot, todasLasCartas);
        }

        mazoBackupService.restoreMissingDecks();
        mazoBackupService.backupAll();
    }

    private void crearUsuarioTest(List<Card> todasLasCartas) {
        Jugador pablo = new Jugador("Pablo");
        pablo.setPasswordHash("2ab74e1d95f6aff7947352ee0d793c366a8ab33452a87a3e39b003b42c843cf9");
        pablo.setSobresDisponibles(10);
        pablo.setAdmin(true);

        List<Card> cartasMazo = crearMazoPorDefecto(todasLasCartas);
        pablo.setColeccion(new ArrayList<>(cartasMazo));
        jugadorRepo.save(pablo);

        Mazo mazoTest = new Mazo("Mazo Inicial Pablo", pablo);
        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("[DataLoader] Usuario Pablo listo para pruebas con mazo inicial.");
    }

    private void crearUsuarioFran(List<Card> todasLasCartas) {
        Jugador fran = new Jugador("Fran");
        fran.setPasswordHash("2ab74e1d95f6aff7947352ee0d793c366a8ab33452a87a3e39b003b42c843cf9");
        fran.setEmail("fran@pokemon.com");
        fran.setSobresDisponibles(10);
        fran.setAdmin(true);

        List<Card> cartasMazo = crearMazoPorDefecto(todasLasCartas);
        fran.setColeccion(new ArrayList<>(cartasMazo));
        jugadorRepo.save(fran);

        Mazo mazoTest = new Mazo("Mazo Inicial Fran", fran);
        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("[DataLoader] Usuario Fran listo para pruebas con mazo inicial.");
    }

    private void resetearColeccionAMazoInicial(Jugador jugador, List<Card> todasLasCartas) {
        List<Mazo> mazos = mazoRepo.findByJugador(jugador);
        List<Card> cartasMazo;
        if (!mazos.isEmpty()) {
            cartasMazo = mazos.get(0).getCartas();
        } else {
            cartasMazo = crearMazoPorDefecto(todasLasCartas);
        }
        jugador.setColeccion(new ArrayList<>(cartasMazo));
        jugadorRepo.save(jugador);
        System.out.println("[DataLoader] Coleccion de " + jugador.getUsername() + " reseteada al mazo inicial.");
    }

    private void crearBotUser(List<Card> todasLasCartas) {
        Jugador bot = new Jugador("BOT");
        bot.setPasswordHash("2ab74e1d95f6aff7947352ee0d793c366a8ab33452a87a3e39b003b42c843cf9");
        bot.setSobresDisponibles(10);
        bot.setCharacterId("ash");
        bot.setSkinColor("#ffe0bd");
        bot.setHairColor("#5c4033");
        bot.setEyeColor("#2563eb");
        bot.setHeight(0.82);

        List<Card> coleccionCompleta = new ArrayList<>();
        for (Card card : todasLasCartas) {
            for (int i = 0; i < 4; i++) {
                coleccionCompleta.add(card);
            }
        }
        bot.setColeccion(coleccionCompleta);
        jugadorRepo.save(bot);

        Mazo mazoTest = new Mazo("Mazo Bot", bot);
        List<Card> cartasMazo = crearMazoPorDefecto(todasLasCartas);
        mazoTest.setCartas(cartasMazo);
        mazoRepo.save(mazoTest);

        System.out.println("[DataLoader] Usuario BOT listo para pruebas con 4 copias de cada carta.");
    }

    private void actualizarColeccionUsuario(Jugador jugador, List<Card> todasLasCartas) {
        List<Card> currentCollection = jugador.getColeccion();
        List<Card> updatedCollection = new ArrayList<>(currentCollection);
        boolean modificada = false;

        for (Card card : todasLasCartas) {
            long count = currentCollection.stream().filter(c -> c.getId().equals(card.getId())).count();
            if (count < 4) {
                modificada = true;
                for (long i = count; i < 4; i++) {
                    updatedCollection.add(card);
                }
            }
        }

        if (modificada) {
            jugador.setColeccion(updatedCollection);
            jugadorRepo.save(jugador);
            System.out.println("[DataLoader] Coleccion de " + jugador.getUsername() + " actualizada con 4 copias de todas las cartas.");
        }
    }

    private List<Card> crearMazoPorDefecto(List<Card> todasLasCartas) {
        List<Card> soloPokemones = todasLasCartas.stream()
                .filter(c -> ("Pokemon".equalsIgnoreCase(c.getSupertype()) || "Pokémon".equalsIgnoreCase(c.getSupertype())) && !c.getSubtypes().contains("EX") && !c.getSubtypes().contains("MEGA"))
                .toList();
        List<Card> soloEnergias = todasLasCartas.stream()
                .filter(c -> "Energy".equalsIgnoreCase(c.getSupertype()))
                .toList();

        List<Card> cartasMazo = new ArrayList<>();
        List<Card> pokesParaMazo = new ArrayList<>(soloPokemones);
        Collections.shuffle(pokesParaMazo);
        cartasMazo.addAll(pokesParaMazo.subList(0, Math.min(40, pokesParaMazo.size())));

        List<Card> energiasParaMazo = new ArrayList<>(soloEnergias);
        Collections.shuffle(energiasParaMazo);
        for (int i = 0; i < 20 && !energiasParaMazo.isEmpty(); i++) {
            cartasMazo.add(energiasParaMazo.get(i % energiasParaMazo.size()));
        }
        return cartasMazo;
    }

    private void migrarTraduccionesSiEsNecesario() {
        ObjectMapper mapper = new ObjectMapper();
        String[] idiomas = {"es", "ja", "pt"};

        for (String lang : idiomas) {
            if (cardTranslationRepo.existsByLang(lang)) {
                System.out.println("[DataLoader] Traducciones para el idioma '" + lang + "' ya estan cargadas en la BD.");
                continue;
            }

            System.out.println("[DataLoader] Migrando archivo cards_" + lang + ".json a la base de datos...");
            try (InputStream inputStream = getClass().getResourceAsStream("/cards_" + lang + ".json")) {
                if (inputStream == null) {
                    System.out.println("⚠️ [DataLoader] No se encontro /cards_" + lang + ".json en resources.");
                    continue;
                }

                List<CardCatalogService.TranslatedCard> parsed = mapper.readValue(
                        inputStream, new TypeReference<List<CardCatalogService.TranslatedCard>>() {}
                );

                List<CardTranslation> cardTranslations = new ArrayList<>();
                List<AttackTranslation> attackTranslations = new ArrayList<>();

                // Obtener las cartas base desde la DB para mapear los IDs de los ataques correctly
                List<Card> baseCards = cardCatalogService.getCatalogo();
                Map<String, Card> baseCardsMap = baseCards.stream().collect(
                        java.util.stream.Collectors.toMap(Card::getId, c -> c, (a, b) -> a)
                );

                for (CardCatalogService.TranslatedCard trCard : parsed) {
                    CardTranslation ct = new CardTranslation(
                            trCard.getId(),
                            lang,
                            trCard.getNombre(),
                            trCard.getReglas()
                    );
                    cardTranslations.add(ct);

                    Card baseCard = baseCardsMap.get(trCard.getId());
                    if (baseCard != null && trCard.getAtaques() != null && baseCard.getAtaques() != null) {
                        for (int i = 0; i < trCard.getAtaques().size(); i++) {
                            if (i < baseCard.getAtaques().size() && i < trCard.getAtaques().size()) {
                                Ataque baseAtk = baseCard.getAtaques().get(i);
                                CardCatalogService.TranslatedAttack trAtk = trCard.getAtaques().get(i);
                                AttackTranslation at = new AttackTranslation(
                                        baseAtk.getId(),
                                        lang,
                                        trAtk.getNombre(),
                                        trAtk.getTexto()
                                );
                                attackTranslations.add(at);
                            }
                        }
                    }
                }

                cardTranslationRepo.saveAll(cardTranslations);
                attackTranslationRepo.saveAll(attackTranslations);
                System.out.println("[DataLoader] Migracion exitosa para el idioma '" + lang + "'.");

            } catch (Exception e) {
                System.out.println("❌ [DataLoader] Error al migrar traducciones para '" + lang + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
