package com.pokemon.tcg.config;

import com.pokemon.tcg.service.LobbyRoomService;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigClassesTest {

    // =================== OpenApiConfig ===================

    @Test
    void openApiConfig_customOpenAPI_returnsOpenApiWithInfo() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI api = config.customOpenAPI();

        assertNotNull(api);
        assertNotNull(api.getInfo());
        assertEquals("Pokemon TCG API", api.getInfo().getTitle());
        assertEquals("1.0.0", api.getInfo().getVersion());
    }

    // =================== CorsConfig ===================

    @Test
    void corsConfig_addCorsMappings_configuresRegistry() {
        CorsConfig config = new CorsConfig();

        CorsRegistry mockRegistry = mock(CorsRegistry.class);
        org.springframework.web.servlet.config.annotation.CorsRegistration mockReg =
                mock(org.springframework.web.servlet.config.annotation.CorsRegistration.class);

        when(mockRegistry.addMapping("/**")).thenReturn(mockReg);
        when(mockReg.allowedOriginPatterns("*")).thenReturn(mockReg);
        when(mockReg.allowedMethods(any(String[].class))).thenReturn(mockReg);
        when(mockReg.allowedHeaders("*")).thenReturn(mockReg);
        when(mockReg.allowCredentials(true)).thenReturn(mockReg);

        config.addCorsMappings(mockRegistry);

        verify(mockRegistry).addMapping("/**");
        verify(mockReg).allowedOriginPatterns("*");
        verify(mockReg).allowCredentials(true);
    }

    // =================== BattleSpectatorGuardInterceptor ===================

    @Test
    void interceptor_getRequest_passesThrough() throws Exception {
        LobbyRoomService mockService = mock(LobbyRoomService.class);
        BattleSpectatorGuardInterceptor interceptor = new BattleSpectatorGuardInterceptor(mockService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");

        boolean result = interceptor.preHandle(req, mock(HttpServletResponse.class), null);

        assertTrue(result);
        verifyNoInteractions(mockService);
    }

    @Test
    void interceptor_optionsRequest_passesThrough() throws Exception {
        LobbyRoomService mockService = mock(LobbyRoomService.class);
        BattleSpectatorGuardInterceptor interceptor = new BattleSpectatorGuardInterceptor(mockService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("OPTIONS");

        boolean result = interceptor.preHandle(req, mock(HttpServletResponse.class), null);

        assertTrue(result);
    }

    @Test
    void interceptor_postSpectator_returns403() throws Exception {
        LobbyRoomService mockService = mock(LobbyRoomService.class);
        BattleSpectatorGuardInterceptor interceptor = new BattleSpectatorGuardInterceptor(mockService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/api/battle/match-123/attack");
        when(req.getHeader("X-Username")).thenReturn("spectator");
        when(mockService.isSpectator("match-123", "spectator")).thenReturn(true);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));

        boolean result = interceptor.preHandle(req, resp, null);

        assertFalse(result);
        verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void interceptor_postNonSpectator_passesThrough() throws Exception {
        LobbyRoomService mockService = mock(LobbyRoomService.class);
        BattleSpectatorGuardInterceptor interceptor = new BattleSpectatorGuardInterceptor(mockService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/api/battle/match-abc/play");
        when(req.getHeader("X-Username")).thenReturn("ash");
        when(mockService.isSpectator("match-abc", "ash")).thenReturn(false);

        boolean result = interceptor.preHandle(req, mock(HttpServletResponse.class), null);

        assertTrue(result);
    }

    @Test
    void interceptor_postStartUri_extractsNullMatchId() throws Exception {
        LobbyRoomService mockService = mock(LobbyRoomService.class);
        BattleSpectatorGuardInterceptor interceptor = new BattleSpectatorGuardInterceptor(mockService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/api/battle/start/ash");
        when(req.getHeader("X-Username")).thenReturn("ash");
        when(mockService.isSpectator(isNull(), eq("ash"))).thenReturn(false);

        boolean result = interceptor.preHandle(req, mock(HttpServletResponse.class), null);

        assertTrue(result);
    }

    @Test
    void interceptor_uriWithoutBattlePath_extractsNull() throws Exception {
        LobbyRoomService mockService = mock(LobbyRoomService.class);
        BattleSpectatorGuardInterceptor interceptor = new BattleSpectatorGuardInterceptor(mockService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("DELETE");
        when(req.getRequestURI()).thenReturn("/api/other/resource");
        when(req.getHeader("X-Username")).thenReturn("ash");
        when(mockService.isSpectator(isNull(), eq("ash"))).thenReturn(false);

        boolean result = interceptor.preHandle(req, mock(HttpServletResponse.class), null);

        assertTrue(result);
    }

    @Test
    void interceptor_matchIdNoTrailingSlash_extracted() throws Exception {
        LobbyRoomService mockService = mock(LobbyRoomService.class);
        BattleSpectatorGuardInterceptor interceptor = new BattleSpectatorGuardInterceptor(mockService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("PUT");
        when(req.getRequestURI()).thenReturn("/api/battle/match-xyz");
        when(req.getHeader("X-Username")).thenReturn("ash");
        when(mockService.isSpectator("match-xyz", "ash")).thenReturn(false);

        boolean result = interceptor.preHandle(req, mock(HttpServletResponse.class), null);

        assertTrue(result);
    }

    // =================== WebMvcConfig ===================

    @Test
    void webMvcConfig_addInterceptors_registersInterceptor() {
        BattleSpectatorGuardInterceptor mockInterceptor = mock(BattleSpectatorGuardInterceptor.class);
        WebMvcConfig config = new WebMvcConfig(mockInterceptor);

        InterceptorRegistry mockRegistry = mock(InterceptorRegistry.class);
        InterceptorRegistration mockReg = mock(InterceptorRegistration.class);
        when(mockRegistry.addInterceptor(mockInterceptor)).thenReturn(mockReg);
        when(mockReg.addPathPatterns("/api/battle/**")).thenReturn(mockReg);

        config.addInterceptors(mockRegistry);

        verify(mockRegistry).addInterceptor(mockInterceptor);
        verify(mockReg).addPathPatterns("/api/battle/**");
    }

    // =================== WebSocketConfig ===================

    @Test
    void webSocketConfig_registerWebSocketHandlers_addsHandler() {
        WebSocketConfig config = new WebSocketConfig();
        LobbyWebSocketHandler handler = mock(LobbyWebSocketHandler.class);

        org.springframework.test.util.ReflectionTestUtils.setField(config, "lobbyWebSocketHandler", handler);

        WebSocketHandlerRegistry mockRegistry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration mockReg = mock(WebSocketHandlerRegistration.class);
        when(mockRegistry.addHandler(handler, "/lobby-ws")).thenReturn(mockReg);
        when(mockReg.setAllowedOrigins("*")).thenReturn(mockReg);

        config.registerWebSocketHandlers(mockRegistry);

        verify(mockRegistry).addHandler(handler, "/lobby-ws");
        verify(mockReg).setAllowedOrigins("*");
    }
}
