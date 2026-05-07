package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.MissingCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MissingCardsCommand extends IdentifiedCommand {

    private static final Logger log = LoggerFactory.getLogger(MissingCardsCommand.class);

    private final BackendApiClient apiClient;

    public MissingCardsCommand(ChatLinkStore chatLinkStore, BackendApiClient apiClient) {
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
            List<MissingCard> missing = apiClient.getMissingCards(userId);
            if (missing.isEmpty()) {
                return "No tenés figuritas marcadas como faltantes. Marcá una con /agregarFaltante <id>.";
            }
            String items = missing.stream()
                    .sorted(Comparator.comparingInt(MissingCard::number))
                    .map(m -> m.number() + ". " + m.description())
                    .collect(Collectors.joining("\n"));
            return "Te faltan (" + missing.size() + "):\n" + items;
        } catch (BackendApiException e) {
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /faltantes: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /faltantes", e);
            return "No pude obtener tus faltantes. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /faltantes", e);
            return "No pude obtener tus faltantes. Probá más tarde.";
        }
    }
}
