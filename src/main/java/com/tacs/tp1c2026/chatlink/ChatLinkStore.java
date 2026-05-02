package com.tacs.tp1c2026.chatlink;

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

/*
 * Stub temporal de identidad chatId → userId del backend (opción A del plan).
 * Persistencia a archivo JSON local — se reemplaza por auth real del backend (opción B) cuando esté.
 */
@Component
public class ChatLinkStore {

    private static final Logger log = LoggerFactory.getLogger(ChatLinkStore.class);

    private final Path file;
    private final ObjectMapper objectMapper;
    private final Map<Long, String> links = new ConcurrentHashMap<>();

    public ChatLinkStore(@Value("${chatlinks.file:./chat-links.json}") String filePath,
                         ObjectMapper objectMapper) {
        this.file = Path.of(filePath);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        if (!Files.exists(file)) {
            log.info("ChatLinkStore: no existe {}, arrancando con mapa vacío", file);
            return;
        }
        try {
            Map<String, String> raw = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
            raw.forEach((k, v) -> links.put(Long.parseLong(k), v));
            log.info("ChatLinkStore: cargados {} mapeos desde {}", links.size(), file);
        } catch (Exception e) {
            log.warn("ChatLinkStore: no pude leer {} ({}), arrancando con mapa vacío", file, e.getMessage());
        }
    }

    public Optional<String> getUserId(long chatId) {
        return Optional.ofNullable(links.get(chatId));
    }

    public void link(long chatId, String userId) {
        links.put(chatId, userId);
        persist();
    }

    public void unlink(long chatId) {
        if (links.remove(chatId) != null) {
            persist();
        }
    }

    private void persist() {
        try {
            Map<String, String> raw = new HashMap<>();
            links.forEach((k, v) -> raw.put(String.valueOf(k), v));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), raw);
        } catch (IOException e) {
            log.error("ChatLinkStore: no pude escribir {}: {}", file, e.getMessage());
        }
    }
}
