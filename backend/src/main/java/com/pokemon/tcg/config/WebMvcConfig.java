package com.pokemon.tcg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final BattleSpectatorGuardInterceptor battleSpectatorGuardInterceptor;

    public WebMvcConfig(BattleSpectatorGuardInterceptor battleSpectatorGuardInterceptor) {
        this.battleSpectatorGuardInterceptor = battleSpectatorGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(battleSpectatorGuardInterceptor)
                .addPathPatterns("/api/battle/**");
    }
}
