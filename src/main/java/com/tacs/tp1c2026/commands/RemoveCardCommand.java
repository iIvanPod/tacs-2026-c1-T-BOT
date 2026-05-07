package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoveCardCommand extends IdentifiedCommand {

    private static final Logger log = LoggerFactory.getLogger(RemoveCardCommand.class);

    private final BackendApiClient apiClient;

    public RemoveCardCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
        super(chatLinkStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/quitar";
    }

    @Override
    public String description() {
        return "Quita una figurita de tu colección (decrementa cantidad). Uso: /quitar <cardId>";
    }

    @Override
    protected String executeAsUser(String userId, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /quitar <cardId>";
        }
        String cardId = ctx.args().trim();
        try {
            apiClient.decrementFromCollection(userId, cardId);
            return "Listo, quité una unidad de " + cardId + ".";
        } catch (BackendApiException e) {
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /quitar: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /quitar", e);
            return "No pude quitar la figurita. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /quitar", e);
            return "No pude quitar la figurita. Probá más tarde.";
        }
    }
}
