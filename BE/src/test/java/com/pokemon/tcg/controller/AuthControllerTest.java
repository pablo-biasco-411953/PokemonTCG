package com.pokemon.tcg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.dto.ForgotPasswordRequest;
import com.pokemon.tcg.dto.LoginRequest;
import com.pokemon.tcg.dto.RegisterRequest;
import com.pokemon.tcg.dto.ResetPasswordRequest;
import com.pokemon.tcg.model.Jugador;
import com.pokemon.tcg.service.AuthService;
import com.pokemon.tcg.service.PasswordRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private PasswordRecoveryService passwordRecoveryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        passwordRecoveryService = mock(PasswordRecoveryService.class);
        AuthController controller = new AuthController(authService, passwordRecoveryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_exitoso_retorna200ConUsuario() throws Exception {
        Jugador jugador = new Jugador("ash");
        when(authService.login("ash", "pass1234")).thenReturn(jugador);

        LoginRequest req = new LoginRequest();
        req.setUsername("ash");
        req.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ash"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        when(authService.login("ash", "wrong"))
                .thenThrow(new IllegalArgumentException("Usuario o contrasena incorrectos."));

        LoginRequest req = new LoginRequest();
        req.setUsername("ash");
        req.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_usuarioNoExiste_retorna401() throws Exception {
        when(authService.login("noexiste", "pass1234"))
                .thenThrow(new IllegalArgumentException("Usuario o contrasena incorrectos."));

        LoginRequest req = new LoginRequest();
        req.setUsername("noexiste");
        req.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_retorna401ConMensajeDeError() throws Exception {
        String mensaje = "Usuario o contrasena incorrectos.";
        when(authService.login("ash", "bad")).thenThrow(new IllegalArgumentException(mensaje));

        LoginRequest req = new LoginRequest();
        req.setUsername("ash");
        req.setPassword("bad");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(mensaje));
    }

    @Test
    void register_exitoso_retorna200ConUsuario() throws Exception {
        Jugador jugador = new Jugador("misty");
        when(authService.register("misty", "misty@test.com", "pass1234", "pass1234"))
                .thenReturn(jugador);

        RegisterRequest req = new RegisterRequest();
        req.setScreenName("misty");
        req.setEmail("misty@test.com");
        req.setPassword("pass1234");
        req.setConfirmPassword("pass1234");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("misty"));
    }

    @Test
    void register_retornaAdminFalse_porDefecto() throws Exception {
        Jugador jugador = new Jugador("brock");
        when(authService.register("brock", "brock@test.com", "pass1234", "pass1234"))
                .thenReturn(jugador);

        RegisterRequest req = new RegisterRequest();
        req.setScreenName("brock");
        req.setEmail("brock@test.com");
        req.setPassword("pass1234");
        req.setConfirmPassword("pass1234");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void forgotPassword_retorna200() throws Exception {
        when(passwordRecoveryService.requestReset("ash", "ash@test.com"))
                .thenReturn("Si los datos existen, enviamos un codigo de recuperacion.");

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setUsername("ash");
        req.setEmail("ash@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_conDemoToken_retornaMensaje() throws Exception {
        String respuesta = "Si los datos existen, enviamos un codigo de recuperacion. Token demo: abc123";
        when(passwordRecoveryService.requestReset("ash", null)).thenReturn(respuesta);

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setUsername("ash");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(respuesta));
    }

    @Test
    void resetPassword_exitoso_retorna200() throws Exception {
        doNothing().when(passwordRecoveryService).resetPassword("token123", "newpass", "newpass");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("token123");
        req.setPassword("newpass");
        req.setConfirmPassword("newpass");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password actualizado. Ya podes iniciar sesion."));
    }
}
