package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoveMissingCardCommand extends IdentifiedCommand {

    private static final Logger log = LoggerFactory.getLogger(RemoveMissingCardCommand.class);

    private final BackendApiClient apiClient;

    public RemoveMissingCardCommand(SessionStore sessionStore, BackendApiClient apiClient) {
        super(sessionStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/quitarFaltante";
    }

    @Override
    public String description() {
        return "Quita una figurita de tu lista de faltantes. Uso: /quitarFaltante <cardId>";
    }

    @Override
    protected String executeAsUser(Session session, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /quitarFaltante <cardId>";
        }
        String cardId = ctx.args().trim();
        try {
            apiClient.removeMissingCard(session.userId(), cardId, session.token());
            return "Listo, saqué la figurita de tus faltantes.";
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) return onSessionExpired(ctx.chatId());
            if (e.getStatus() == 404) {
                return "No existe una figurita con id " + cardId + ".";
            }
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /quitarFaltante: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /quitarFaltante", e);
            return "No pude quitar la figurita de tus faltantes. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /quitarFaltante", e);
            return "No pude quitar la figurita de tus faltantes. Probá más tarde.";
        }
    }
}
