package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.Usuario;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class YoSoyCommand implements CommandHandler {

    private final BackendApiClient apiClient;
    private final ChatLinkStore chatLinkStore;

    public YoSoyCommand(BackendApiClient apiClient, ChatLinkStore chatLinkStore) {
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
            Usuario u = apiClient.getUserById(userId);
            chatLinkStore.link(ctx.chatId(), userId);
            return "Listo, ahora estás identificado como " + u.nombre() + " (" + u.id() + ").";
        } catch (HttpClientErrorException.NotFound e) {
            return "No existe un usuario con id " + userId + ".";
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude validar ese userId. Probá más tarde.";
        }
    }
}
