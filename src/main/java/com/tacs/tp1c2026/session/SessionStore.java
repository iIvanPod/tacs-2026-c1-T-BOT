package com.tacs.tp1c2026.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);

    private final Path file;
    private final ObjectMapper objectMapper;
    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public SessionStore(@Value("${sessions.file:./sessions.json}") String filePath,
                        ObjectMapper objectMapper) {
        this.file = Path.of(filePath);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        if (!Files.exists(file)) {
            log.info("SessionStore: no existe {}, arrancando con mapa vacío", file);
            return;
        }
        try {
            Map<String, Session> raw = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
            raw.forEach((k, v) -> sessions.put(Long.parseLong(k), v));
            log.info("SessionStore: cargadas {} sesiones desde {}", sessions.size(), file);
        } catch (Exception e) {
            log.warn("SessionStore: no pude leer {} ({}), arrancando con mapa vacío", file, e.getMessage());
        }
    }

    public Optional<Session> get(long chatId) {
        return Optional.ofNullable(sessions.get(chatId));
    }

    public void save(long chatId, Session session) {
        sessions.put(chatId, session);
        persist();
    }

    public void remove(long chatId) {
        if (sessions.remove(chatId) != null) {
            persist();
        }
    }

    private void persist() {
        try {
            Map<String, Session> raw = new HashMap<>();
            sessions.forEach((k, v) -> raw.put(String.valueOf(k), v));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), raw);
        } catch (IOException e) {
            log.error("SessionStore: no pude escribir {}: {}", file, e.getMessage());
        }
    }
}
