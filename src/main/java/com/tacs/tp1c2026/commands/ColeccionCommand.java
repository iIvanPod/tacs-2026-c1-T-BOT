package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.FiguritaColeccion;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ColeccionCommand extends IdentifiedCommand {

    private final BackendApiClient apiClient;

    public ColeccionCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
        super(chatLinkStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/coleccion";
    }

    @Override
    public String description() {
        return "Lista las figuritas de tu colección";
    }

    @Override
    protected String executeAsUser(String userId, CommandContext ctx) {
        try {
            List<FiguritaColeccion> coleccion = apiClient.getCollection(userId);
            if (coleccion.isEmpty()) {
                return "Tu colección está vacía. Agregá figuritas con /agregar <id>.";
            }
            String items = coleccion.stream()
                    .sorted(Comparator.comparingInt(FiguritaColeccion::numero))
                    .map(f -> f.numero() + ". " + f.nombre() + " x" + f.cantidad())
                    .collect(Collectors.joining("\n"));
            return "Tu colección (" + coleccion.size() + " figuritas):\n" + items;
        } catch (Exception e) {
            e.printStackTrace();
            return "No pude obtener tu colección. Probá más tarde.";
        }
    }
}
