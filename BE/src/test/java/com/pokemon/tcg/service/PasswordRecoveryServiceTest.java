package com.pokemon.tcg.service;

import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.repository.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordRecoveryServiceTest {

    private JugadorRepository jugadorRepo;
    private AuthService authService;
    private JavaMailSender mailSender;
    private PasswordRecoveryService service;

    @BeforeEach
    void setUp() {
        jugadorRepo = mock(JugadorRepository.class);
        authService = mock(AuthService.class);
        mailSender = mock(JavaMailSender.class);
        service = new PasswordRecoveryService(jugadorRepo, authService, mailSender);
    }

    @Test
    void requestReset_porUsername_retornaMensajeGenerico() {
        Jugador jugador = new Jugador("ash");
        jugador.setEmail("ash@test.com");
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(jugador);
        when(authService.hashPassword(any())).thenReturn("hashToken");
        when(jugadorRepo.save(any())).thenReturn(jugador);

        String result = service.requestReset("ash", null);

        assertNotNull(result);
        assertTrue(result.contains("Si los datos existen"));
    }

    @Test
    void requestReset_porEmail_retornaMensajeGenerico() {
        Jugador jugador = new Jugador("ash");
        jugador.setEmail("ash@test.com");
        when(jugadorRepo.findByEmail("ash@test.com")).thenReturn(jugador);
        when(authService.hashPassword(any())).thenReturn("hashToken");
        when(jugadorRepo.save(any())).thenReturn(jugador);

        String result = service.requestReset(null, "ash@test.com");

        assertNotNull(result);
        assertTrue(result.contains("Si los datos existen"));
    }

    @Test
    void requestReset_jugadorNoExiste_retornaMensajeGenerico() {
        when(jugadorRepo.findAuthByUsername("noexiste")).thenReturn(null);

        String result = service.requestReset("noexiste", null);

        assertEquals("Si los datos existen, enviamos un codigo de recuperacion.", result);
        verify(jugadorRepo, never()).save(any());
    }

    @Test
    void requestReset_sinUsernameNiEmail_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.requestReset(null, null));
        assertEquals("Indica tu usuario o email.", ex.getMessage());
    }

    @Test
    void requestReset_ambosVacios_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.requestReset("", ""));
        assertEquals("Indica tu usuario o email.", ex.getMessage());
    }

    @Test
    void requestReset_emailNoCoincide_retornaMensajeGenerico() {
        Jugador jugador = new Jugador("ash");
        jugador.setEmail("real@test.com");
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(jugador);

        String result = service.requestReset("ash", "otro@test.com");

        assertEquals("Si los datos existen, enviamos un codigo de recuperacion.", result);
        verify(jugadorRepo, never()).save(any());
    }

    @Test
    void requestReset_guardaTokenHash() {
        Jugador jugador = new Jugador("ash");
        jugador.setEmail("ash@test.com");
        when(jugadorRepo.findAuthByUsername("ash")).thenReturn(jugador);
        when(authService.hashPassword(any())).thenReturn("hashedToken");
        when(jugadorRepo.save(any())).thenReturn(jugador);

        service.requestReset("ash", null);

        verify(jugadorRepo).save(jugador);
        assertEquals("hashedToken", jugador.getPasswordResetTokenHash());
    }

    @Test
    void resetPassword_tokenValido_cambiaPassword() {
        Jugador jugador = new Jugador("ash");
        jugador.setPasswordResetTokenHash("hashedToken");
        jugador.setPasswordResetTokenExpiresAt(System.currentTimeMillis() + 60_000);

        when(authService.hashPassword("token123")).thenReturn("hashedToken");
        when(jugadorRepo.findByPasswordResetTokenHash("hashedToken")).thenReturn(jugador);
        when(authService.hashPassword("newpass")).thenReturn("hashedNewPass");
        when(jugadorRepo.save(any())).thenReturn(jugador);

        service.resetPassword("token123", "newpass", "newpass");

        verify(jugadorRepo).save(jugador);
        assertEquals("hashedNewPass", jugador.getPasswordHash());
        assertNull(jugador.getPasswordResetTokenHash());
    }

    @Test
    void resetPassword_tokenExpirado_lanzaExcepcion() {
        Jugador jugador = new Jugador("ash");
        jugador.setPasswordResetTokenHash("hashedToken");
        jugador.setPasswordResetTokenExpiresAt(System.currentTimeMillis() - 1000);

        when(authService.hashPassword("token123")).thenReturn("hashedToken");
        when(jugadorRepo.findByPasswordResetTokenHash("hashedToken")).thenReturn(jugador);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("token123", "newpass", "newpass"));
        assertEquals("El codigo expiro o no es valido.", ex.getMessage());
    }

    @Test
    void resetPassword_tokenInvalido_lanzaExcepcion() {
        when(authService.hashPassword("tokenInvalido")).thenReturn("hashInvalido");
        when(jugadorRepo.findByPasswordResetTokenHash("hashInvalido")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("tokenInvalido", "newpass", "newpass"));
        assertEquals("El codigo expiro o no es valido.", ex.getMessage());
    }

    @Test
    void resetPassword_tokenVacio_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("", "newpass", "newpass"));
        assertEquals("El codigo de recuperacion es obligatorio.", ex.getMessage());
    }

    @Test
    void resetPassword_tokenNull_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(null, "newpass", "newpass"));
        assertEquals("El codigo de recuperacion es obligatorio.", ex.getMessage());
    }

    @Test
    void resetPassword_passwordCorto_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("token123", "abc", "abc"));
        assertEquals("La contrasena debe tener al menos 4 caracteres.", ex.getMessage());
    }

    @Test
    void resetPassword_passwordNoCoincide_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("token123", "pass1234", "different"));
        assertEquals("Las contrasenas no coinciden.", ex.getMessage());
    }

    @Test
    void resetPassword_limpiaTokenDespuesDeCambiar() {
        Jugador jugador = new Jugador("ash");
        jugador.setPasswordResetTokenHash("hashedToken");
        jugador.setPasswordResetTokenExpiresAt(System.currentTimeMillis() + 60_000);

        when(authService.hashPassword("token123")).thenReturn("hashedToken");
        when(jugadorRepo.findByPasswordResetTokenHash("hashedToken")).thenReturn(jugador);
        when(authService.hashPassword("newpass")).thenReturn("hashedNewPass");

        service.resetPassword("token123", "newpass", "newpass");

        assertNull(jugador.getPasswordResetTokenHash());
        assertNull(jugador.getPasswordResetTokenExpiresAt());
    }
}
