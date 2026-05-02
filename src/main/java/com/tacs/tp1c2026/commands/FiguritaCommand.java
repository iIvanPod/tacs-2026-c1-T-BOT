package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.Figurita;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class FiguritaCommand implements CommandHandler {

    private final BackendApiClient apiClient;

    public FiguritaCommand(BackendApiClient apiClient) {
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
            Figurita f = apiClient.getFiguritaById(id);
            return formatear(f);
        } catch (HttpClientErrorException.NotFound e) {
            return "No existe una figurita con id " + id + ".";
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude obtener la figurita. Probá más tarde.";
        }
    }

    private String formatear(Figurita f) {
        return """
                Figurita #%d
                Nombre: %s
                País: %s
                Id: %s""".formatted(f.numero(), f.nombre(), f.pais(), f.id());
    }
}
