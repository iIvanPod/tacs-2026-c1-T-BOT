package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CollectionCommand extends IdentifiedCommand {

    private static final Logger log = LoggerFactory.getLogger(CollectionCommand.class);

    private final BackendApiClient apiClient;

    public CollectionCommand(SessionStore sessionStore, BackendApiClient apiClient) {
        super(sessionStore);
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
    protected String executeAsUser(Session session, CommandContext ctx) {
        try {
            List<CollectionCard> collection = apiClient.getCollection(session.userId(), session.token());
            if (collection.isEmpty()) {
                return "Tu colección está vacía. Agregá figuritas con /agregar <id>.";
            }
            String items = collection.stream()
                    .sorted(Comparator.comparingInt(CollectionCard::number))
                    .map(c -> c.number() + ". " + c.description() + " x" + c.quantity())
                    .collect(Collectors.joining("\n"));
            return "Tu colección (" + collection.size() + " figuritas):\n" + items;
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) return onSessionExpired(ctx.chatId());
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /coleccion: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /coleccion", e);
            return "No pude obtener tu colección. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /coleccion", e);
            return "No pude obtener tu colección. Probá más tarde.";
        }
    }
}
