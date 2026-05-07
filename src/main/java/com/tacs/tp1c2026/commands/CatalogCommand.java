package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CatalogCommand implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CatalogCommand.class);

    private static final int PAGE_SIZE = 10;

    private final BackendApiClient apiClient;

    public CatalogCommand(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/catalogo";
    }

    @Override
    public String description() {
        return "Lista el catálogo paginado (10 por página). Uso: /catalogo [página]";
    }

    @Override
    public String execute(CommandContext ctx) {
        int page;
        try {
            page = parsePage(ctx.args());
        } catch (NumberFormatException e) {
            return "Página inválida. Uso: /catalogo [número de página]";
        }
        if (page < 1) {
            return "La página tiene que ser un número mayor o igual a 1.";
        }

        try {
            List<Card> catalog = apiClient.getCatalog();
            if (catalog.isEmpty()) {
                return "El catálogo está vacío.";
            }
            int total = catalog.size();
            int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
            if (page > totalPages) {
                return "No hay página " + page + ". El catálogo tiene "
                        + total + " figuritas (" + totalPages + " páginas).";
            }
            int from = (page - 1) * PAGE_SIZE;
            String items = catalog.stream()
                    .sorted(Comparator.comparingInt(Card::number))
                    .skip(from)
                    .limit(PAGE_SIZE)
                    .map(c -> c.number() + ". " + c.description() + " (" + c.id() + ")")
                    .collect(Collectors.joining("\n"));
            StringBuilder out = new StringBuilder();
            out.append("Catálogo — página ").append(page).append("/").append(totalPages)
               .append(" (").append(total).append(" figuritas):\n")
               .append(items);
            if (page < totalPages) {
                out.append("\n\nSiguiente: /catalogo ").append(page + 1);
            }
            return out.toString();
        } catch (BackendApiException e) {
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /catalogo: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /catalogo", e);
            return "No pude obtener el catálogo. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /catalogo", e);
            return "No pude obtener el catálogo. Probá más tarde.";
        }
    }

    private static int parsePage(String args) {
        if (args.isBlank()) {
            return 1;
        }
        return Integer.parseInt(args.trim());
    }
}
