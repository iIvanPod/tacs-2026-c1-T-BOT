package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CardCommand implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CardCommand.class);

    private final BackendApiClient apiClient;

    public CardCommand(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/figurita";
    }

    @Override
    public String description() {
        return "Detalle de una figurita por id. Uso: /figurita <id>";
    }

    @Override
    public String execute(CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /figurita <id>";
        }
        String id = ctx.args().trim();
        try {
            Card c = apiClient.getCardById(id);
            return formatear(c);
        } catch (BackendApiException e) {
            if (e.getStatus() == 404) {
                return "No existe una figurita con id " + id + ".";
            }
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /figurita: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /figurita", e);
            return "No pude obtener la figurita. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /figurita", e);
            return "No pude obtener la figurita. Probá más tarde.";
        }
    }

    private String formatear(Card c) {
        return """
                Figurita #%d
                Nombre: %s
                País: %s
                Id: %s""".formatted(c.number(), c.description(), c.country(), c.id());
    }
}
