package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import org.springframework.stereotype.Component;

@Component
public class QuitarCommand extends IdentifiedCommand {

    private final BackendApiClient apiClient;

    public QuitarCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
        super(chatLinkStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/quitar";
    }

    @Override
    public String description() {
        return "Quita una figurita de tu colección (decrementa cantidad). Uso: /quitar <figuritaId>";
    }

    @Override
    protected String executeAsUser(String userId, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /quitar <figuritaId>";
        }
        String figuritaId = ctx.args().trim();
        try {
            apiClient.decrementFromCollection(userId, figuritaId);
            return "Listo, quité una unidad de " + figuritaId + ".";
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude quitar la figurita. Probá más tarde.";
        }
    }
}
