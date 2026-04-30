package com.tacs.tp1c2026;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BackendConfig {

    @Bean
    public RestClient restClient(@Value("${backend.url}") String backendUrl) {
        return RestClient.builder()
                .baseUrl(backendUrl)
                .build();
    }
}