package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.FiguritaColeccion;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class AgregarCommand extends IdentifiedCommand {

    private final BackendApiClient apiClient;

    public AgregarCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
        super(chatLinkStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/agregar";
    }

    @Override
    public String description() {
        return "Agrega una figurita a tu colección. Uso: /agregar <figuritaId>";
    }

    @Override
    protected String executeAsUser(String userId, CommandContext ctx) {
        if (ctx.args().isBlank()) {
            return "Falta el id. Uso: /agregar <figuritaId>";
        }
        String figuritaId = ctx.args().trim();
        try {
            FiguritaColeccion f = apiClient.addToCollection(userId, figuritaId);
            return "Listo. Tenés " + f.cantidad() + " de " + f.nombre() + ".";
        } catch (HttpClientErrorException.NotFound e) {
            return "No existe una figurita con id " + figuritaId + ".";
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude agregar la figurita. Probá más tarde.";
        }
    }
}
