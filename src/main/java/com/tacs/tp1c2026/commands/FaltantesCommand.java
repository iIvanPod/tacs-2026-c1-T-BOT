package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.FiguritaFaltante;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FaltantesCommand extends IdentifiedCommand {

    private final BackendApiClient apiClient;

    public FaltantesCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
        super(chatLinkStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/faltantes";
    }

    @Override
    public String description() {
        return "Lista las figuritas que te faltan";
    }

    @Override
    protected String executeAsUser(String userId, CommandContext ctx) {
        try {
            List<FiguritaFaltante> faltantes = apiClient.getMissingCards(userId);
            if (faltantes.isEmpty()) {
                return "No tenés figuritas marcadas como faltantes. Marcá una con /agregarFaltante <id>.";
            }
            String items = faltantes.stream()
                    .sorted(Comparator.comparingInt(FiguritaFaltante::numero))
                    .map(f -> f.numero() + ". " + f.nombre())
                    .collect(Collectors.joining("\n"));
            return "Te faltan (" + faltantes.size() + "):\n" + items;
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude obtener tus faltantes. Probá más tarde.";
        }
    }
}
