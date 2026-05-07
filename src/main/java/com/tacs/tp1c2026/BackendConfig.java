package com.tacs.tp1c2026;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.client.wire.ApiErrorWire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Configuration
public class BackendConfig {

    @Bean
    public RestClient restClient(@Value("${backend.url}") String backendUrl,
                                 ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(backendUrl + "/api")
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    int status = response.getStatusCode().value();
                    byte[] raw = response.getBody().readAllBytes();
                    String error = null;
                    String message = null;
                    if (raw.length > 0) {
                        try {
                            ApiErrorWire wire = objectMapper.readValue(
                                    new String(raw, StandardCharsets.UTF_8),
                                    ApiErrorWire.class);
                            error = wire.error();
                            message = wire.message();
                        } catch (Exception ignored) {
                            // body no es ApiError JSON — dejamos error/message en null y caemos al "HTTP <status>"
                        }
                    }
                    throw new BackendApiException(status, error, message);
                })
                .build();
    }
}
