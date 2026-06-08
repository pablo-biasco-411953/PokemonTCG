package com.pokemon.tcg.config;

import com.pokemon.tcg.service.LobbyRoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BattleSpectatorGuardInterceptor implements HandlerInterceptor {
    private final LobbyRoomService lobbyRoomService;

    public BattleSpectatorGuardInterceptor(LobbyRoomService lobbyRoomService) {
        this.lobbyRoomService = lobbyRoomService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if (!("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))) {
            return true;
        }

        String matchId = extractMatchId(request.getRequestURI());
        String username = request.getHeader("X-Username");
        if (lobbyRoomService.isSpectator(matchId, username)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Los espectadores no pueden modificar la partida.");
            return false;
        }
        return true;
    }

    private String extractMatchId(String uri) {
        String marker = "/api/battle/";
        int start = uri.indexOf(marker);
        if (start < 0) return null;
        String rest = uri.substring(start + marker.length());
        if (rest.startsWith("start") || rest.startsWith("debug")) return null;
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
