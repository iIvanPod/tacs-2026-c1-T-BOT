package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdentifyCommand implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(IdentifyCommand.class);

    private final BackendApiClient apiClient;
    private final ChatLinkStore chatLinkStore;

    public IdentifyCommand(BackendApiClient apiClient, ChatLinkStore chatLinkStore) {
        this.apiClient = apiClient;
        this.chatLinkStore = chatLinkStore;
    }

    @Override
    public String name() {
        return "/yosoy";
    }

    @Override
    public String description() {
        return "Asocia tu chat a un usuario del backend. Uso: /yosoy <userId>";
    }

    @Override
    public String execute(CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el userId. Uso: /yosoy <userId>";
        }
        String userId = ctx.args().trim();
        try {
            User u = apiClient.getUserById(userId);
            chatLinkStore.link(ctx.chatId(), userId);
            return "Listo, ahora estás identificado como " + u.name() + " (" + u.id() + ").";
        } catch (BackendApiException e) {
            if (e.getStatus() == 404) {
                return "No existe un usuario con id " + userId + ".";
            }
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /yosoy: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /yosoy", e);
            return "No pude validar ese userId. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /yosoy", e);
            return "No pude validar ese userId. Probá más tarde.";
        }
    }
}
