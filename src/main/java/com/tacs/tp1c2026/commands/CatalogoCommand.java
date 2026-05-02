package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.Figurita;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CatalogoCommand implements CommandHandler {

    private final BackendApiClient apiClient;

    public CatalogoCommand(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/catalogo";
    }

    @Override
    public String description() {
        return "Lista todas las figuritas del catálogo";
    }

    @Override
    public String execute(CommandContext ctx) {
        try {
            List<Figurita> catalogo = apiClient.getCatalog();
            if (catalogo.isEmpty()) {
                return "El catálogo está vacío.";
            }
            return catalogo.stream()
                    .map(f -> f.numero() + ". " + f.nombre())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude obtener el catálogo. Probá más tarde.";
        }
    }
}
