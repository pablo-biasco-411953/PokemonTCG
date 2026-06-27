package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private JugadorRepository jugadorRepo;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jugadorRepo = mock(JugadorRepository.class);
        authService = new AuthService(jugadorRepo);
    }

    private Jugador jugadorConPassword(String username, String password) {
        Jugador j = new Jugador(username);
        j.setPasswordHash(authService.hashPassword(password));
        return j;
    }

    @Test
    void login_exitoso_retornaJugador() {
        Jugador jugador = jugadorConPassword("ash", "pass1234");
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(jugador);

        Jugador result = authService.login("ash", "pass1234");

        assertNotNull(result);
        assertEquals("ash", result.getUsername());
    }

    @Test
    void login_passwordIncorrecto_lanzaExcepcion() {
        Jugador jugador = jugadorConPassword("ash", "pass1234");
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(jugador);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ash", "wrong"));
        assertEquals("Usuario o contrasena incorrectos.", ex.getMessage());
    }

    @Test
    void login_usuarioNoExiste_lanzaExcepcion() {
        when(jugadorRepo.findAuthByUsername("noexiste")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("noexiste", "pass1234"));
        assertEquals("Usuario o contrasena incorrectos.", ex.getMessage());
    }

    @Test
    void login_usernameVacio_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("", "pass1234"));
        assertEquals("El usuario es obligatorio.", ex.getMessage());
    }

    @Test
    void login_usernameNull_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "pass1234"));
        assertEquals("El usuario es obligatorio.", ex.getMessage());
    }

    @Test
    void login_passwordCorto_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ash", "abc"));
        assertEquals("La contrasena debe tener al menos 4 caracteres.", ex.getMessage());
    }

    @Test
    void login_passwordNull_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ash", null));
        assertEquals("La contrasena debe tener al menos 4 caracteres.", ex.getMessage());
    }

    @Test
    void login_trimUsernameConEspacios() {
        Jugador jugador = jugadorConPassword("ash", "pass1234");
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(jugador);

        Jugador result = authService.login("  ash  ", "pass1234");
        assertNotNull(result);
    }

    @Test
    void login_passwordHashNulo_lanzaExcepcion() {
        Jugador jugador = new Jugador("ash");
        jugador.setPasswordHash(null);
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(jugador);

        assertThrows(IllegalArgumentException.class, () -> authService.login("ash", "pass1234"));
    }

    @Test
    void register_exitoso_guardaYRetornaJugador() {
        when(jugadorRepo.findAuthByUsername("misty")).thenReturn(null);
        when(jugadorRepo.findByEmail("misty@test.com")).thenReturn(null);
        when(jugadorRepo.save(any(Jugador.class))).thenAnswer(inv -> inv.getArgument(0));

        Jugador result = authService.register("misty", "misty@test.com", "pass1234", "pass1234");

        assertNotNull(result);
        assertEquals("misty", result.getUsername());
        assertEquals("misty@test.com", result.getEmail());
        verify(jugadorRepo).save(any(Jugador.class));
    }

    @Test
    void register_usernameExistente_lanzaExcepcion() {
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(new Jugador("ash"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("ash", "new@test.com", "pass1234", "pass1234"));
        assertEquals("El nombre de usuario ya esta en uso.", ex.getMessage());
    }

    @Test
    void register_emailExistente_lanzaExcepcion() {
        when(jugadorRepo.findAuthByUsername("newuser")).thenReturn(null);
        when(jugadorRepo.findByEmail("existente@test.com")).thenReturn(new Jugador("otro"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("newuser", "existente@test.com", "pass1234", "pass1234"));
        assertEquals("Ese email ya esta asociado a otro entrenador.", ex.getMessage());
    }

    @Test
    void register_passwordNoCoincide_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("user", "user@test.com", "pass1234", "different"));
        assertEquals("Las contrasenas no coinciden.", ex.getMessage());
    }

    @Test
    void register_emailSinArroba_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("user", "notanemail", "pass1234", "pass1234"));
        assertEquals("El email es obligatorio para recuperar la cuenta.", ex.getMessage());
    }

    @Test
    void register_emailVacio_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("user", "", "pass1234", "pass1234"));
        assertEquals("El email es obligatorio para recuperar la cuenta.", ex.getMessage());
    }

    @Test
    void register_screennameVacio_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("", "user@test.com", "pass1234", "pass1234"));
        assertEquals("El nombre de usuario es obligatorio.", ex.getMessage());
    }

    @Test
    void register_passwordCorto_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("user", "user@test.com", "abc", "abc"));
        assertEquals("La contrasena debe tener al menos 4 caracteres.", ex.getMessage());
    }

    @Test
    void register_guardaPasswordHasheado() {
        when(jugadorRepo.findAuthByUsername("user")).thenReturn(null);
        when(jugadorRepo.findByEmail("user@test.com")).thenReturn(null);
        when(jugadorRepo.save(any(Jugador.class))).thenAnswer(inv -> inv.getArgument(0));

        Jugador result = authService.register("user", "user@test.com", "pass1234", "pass1234");

        assertNotNull(result.getPasswordHash());
        assertNotEquals("pass1234", result.getPasswordHash());
        assertEquals(authService.hashPassword("pass1234"), result.getPasswordHash());
    }

    @Test
    void hashPassword_mismosInputs_mismosHashes() {
        String h1 = authService.hashPassword("test123");
        String h2 = authService.hashPassword("test123");
        assertEquals(h1, h2);
    }

    @Test
    void hashPassword_distintoInput_distintoHash() {
        String h1 = authService.hashPassword("pass1");
        String h2 = authService.hashPassword("pass2");
        assertNotEquals(h1, h2);
    }
}
