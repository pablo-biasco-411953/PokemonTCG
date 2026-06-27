package com.pokemon.tcg.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_retorna400() {
        ResponseEntity<?> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Dato invalido"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleIllegalArgument_bodyConteneMensaje() {
        ResponseEntity<?> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Error de prueba"));
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Error de prueba", body.get("message"));
    }

    @Test
    void handleIllegalArgument_bodyConteneStatus400() {
        ResponseEntity<?> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("test"));
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(400, body.get("status"));
    }

    @Test
    void handleIllegalState_retorna409() {
        ResponseEntity<?> response = handler.handleIllegalStateException(
                new IllegalStateException("Estado invalido"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleIllegalState_bodyConteneMensaje() {
        ResponseEntity<?> response = handler.handleIllegalStateException(
                new IllegalStateException("Estado de conflicto"));
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Estado de conflicto", body.get("message"));
    }

    @Test
    void handleIllegalState_bodyConteneStatus409() {
        ResponseEntity<?> response = handler.handleIllegalStateException(
                new IllegalStateException("conflict"));
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(409, body.get("status"));
    }

    @Test
    void handleGeneralException_retorna500() {
        ResponseEntity<?> response = handler.handleGeneralException(
                new RuntimeException("Error inesperado"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void handleGeneralException_bodyContieneMensaje() {
        ResponseEntity<?> response = handler.handleGeneralException(
                new RuntimeException("crash"));
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("message").toString().contains("crash"));
    }

    @Test
    void handleGeneralException_bodyContiene500() {
        ResponseEntity<?> response = handler.handleGeneralException(
                new Exception("test error"));
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(500, body.get("status"));
    }

    @Test
    void handleValidationExceptions_retorna400ConMensajes() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "username", "no puede estar vacío"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<?> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("no puede estar vacío"));
        assertEquals(400, body.get("status"));
    }
}
