package com.tacs.tp1c2026.tma;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS acotado a los endpoints de la Mini App: solo el origen del webview puede llamarlos.
 * Sin allowCredentials porque el frontend autentica con header Bearer, no con cookies.
 */
@Configuration
public class TmaWebConfig implements WebMvcConfigurer {

    private final String allowedOrigin;

    public TmaWebConfig(@Value("${tma.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/tma/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("POST", "OPTIONS");
    }
}
