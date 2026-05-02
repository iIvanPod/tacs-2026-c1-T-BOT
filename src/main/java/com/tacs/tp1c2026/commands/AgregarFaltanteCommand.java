package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.FiguritaFaltante;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class AgregarFaltanteCommand extends IdentifiedCommand {

    private final BackendApiClient apiClient;

    public AgregarFaltanteCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
        super(chatLinkStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/agregarFaltante";
    }

    @Override
    public String description() {
        return "Marca una figurita como faltante. Uso: /agregarFaltante <figuritaId>";
    }

    @Override
    protected String executeAsUser(String userId, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /agregarFaltante <figuritaId>";
        }
        String figuritaId = ctx.args().trim();
        try {
            FiguritaFaltante f = apiClient.addMissingCard(userId, figuritaId);
            return "Listo, marqué " + f.nombre() + " como faltante.";
        } catch (HttpClientErrorException.NotFound e) {
            return "No existe una figurita con id " + figuritaId + ".";
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude marcar la figurita como faltante. Probá más tarde.";
        }
    }
}
