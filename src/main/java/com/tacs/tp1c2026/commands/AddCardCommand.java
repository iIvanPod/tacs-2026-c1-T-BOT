package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.CollectionCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AddCardCommand extends IdentifiedCommand {

    private static final Logger log = LoggerFactory.getLogger(AddCardCommand.class);

    private final BackendApiClient apiClient;

    public AddCardCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
        super(chatLinkStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/agregar";
    }

    @Override
    public String description() {
        return "Agrega una figurita a tu colección. Uso: /agregar <cardId>";
    }

    @Override
    protected String executeAsUser(String userId, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /agregar <cardId>";
        }
        String cardId = ctx.args().trim();
        try {
            CollectionCard c = apiClient.addToCollection(userId, cardId);
            return "Listo. Tenés " + c.quantity() + " de " + c.description() + ".";
        } catch (BackendApiException e) {
            if (e.getStatus() == 404) {
                return "No existe una figurita con id " + cardId + ".";
            }
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /agregar: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /agregar", e);
            return "No pude agregar la figurita. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /agregar", e);
            return "No pude agregar la figurita. Probá más tarde.";
        }
    }
}
