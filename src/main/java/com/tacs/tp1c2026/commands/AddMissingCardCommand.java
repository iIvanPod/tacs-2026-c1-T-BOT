package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AddMissingCardCommand extends IdentifiedCommand {

    private static final Logger log = LoggerFactory.getLogger(AddMissingCardCommand.class);

    private final BackendApiClient apiClient;

    public AddMissingCardCommand(SessionStore sessionStore, BackendApiClient apiClient) {
        super(sessionStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/agregarFaltante";
    }

    @Override
    public String description() {
        return "Marca una figurita como faltante. Uso: /agregarFaltante <cardId>";
    }

    @Override
    protected String executeAsUser(Session session, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /agregarFaltante <cardId>";
        }
        String cardId = ctx.args().trim();
        try {
            MissingCard m = apiClient.addMissingCard(session.userId(), cardId, session.token());
            return "Listo, marqué " + m.description() + " como faltante.";
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) return onSessionExpired(ctx.chatId());
            if (e.getStatus() == 404) {
                return "No existe una figurita con id " + cardId + ".";
            }
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /agregarFaltante: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /agregarFaltante", e);
            return "No pude marcar la figurita como faltante. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /agregarFaltante", e);
            return "No pude marcar la figurita como faltante. Probá más tarde.";
        }
    }
}
