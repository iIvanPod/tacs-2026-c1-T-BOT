package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.TradePublication;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublishTradeCommand extends IdentifiedCommand {

    private static final Logger log = LoggerFactory.getLogger(PublishTradeCommand.class);

    private static final String USAGE = "Uso: /publicar <cardId> <cantidad>";

    private final BackendApiClient apiClient;

    public PublishTradeCommand(SessionStore sessionStore, BackendApiClient apiClient) {
        super(sessionStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/publicar";
    }

    @Override
    public String description() {
        return "Publica una figurita repetida para intercambio. " + USAGE;
    }

    @Override
    protected String executeAsUser(Session session, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Faltan datos. " + USAGE;
        }
        String[] parts = ctx.args().trim().split("\\s+");
        if (parts.length < 2) {
            return "Faltan datos. " + USAGE;
        }
        String cardId = parts[0];
        int quantity;
        try {
            quantity = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return "La cantidad tiene que ser un número entero. " + USAGE;
        }
        if (quantity < 1) {
            return "La cantidad tiene que ser mayor o igual a 1. " + USAGE;
        }

        try {
            TradePublication p = apiClient.createTradePublication(cardId, quantity, session.token());
            return "Listo. Publicaste la figurita #" + p.cardNumber() + " " + p.cardDescription()
                    + ".\nCantidad publicada: " + p.initialCount()
                    + " (disponibles: " + p.remainingCount() + ")."
                    + "\nEstado: " + p.status() + ".";
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) return onSessionExpired(ctx.chatId());
            if (e.getStatus() == 404) {
                return "No existe una figurita con id " + cardId + ".";
            }
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /publicar: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /publicar", e);
            return "No pude publicar la figurita. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /publicar", e);
            return "No pude publicar la figurita. Probá más tarde.";
        }
    }
}
