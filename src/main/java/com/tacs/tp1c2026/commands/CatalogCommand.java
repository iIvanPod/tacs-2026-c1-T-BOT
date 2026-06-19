package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CatalogCommand extends IdentifiedCommand implements InteractiveCommand {

    private static final Logger log = LoggerFactory.getLogger(CatalogCommand.class);

    private static final int PAGE_SIZE = 10;

    /** Prefijo del callback de los botones de paginación: {@code catalogo:<página>}. */
    static final String CALLBACK_PREFIX = "catalogo:";

    private final BackendApiClient apiClient;

    public CatalogCommand(SessionStore sessionStore, BackendApiClient apiClient) {
        super(sessionStore);
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
    protected String executeAsUser(Session session, CommandContext ctx) {
        return render(session, ctx.chatId(), ctx.args()).text();
    }

    @Override
    public BotMessage executeInteractive(CommandContext ctx) {
        return sessionOf(ctx.chatId())
                .map(session -> render(session, ctx.chatId(), ctx.args()))
                .orElseGet(() -> BotMessage.text(NOT_IDENTIFIED));
    }

    private BotMessage render(Session session, long chatId, String args) {
        int page;
        try {
            page = parsePage(args);
        } catch (NumberFormatException e) {
            return BotMessage.text("Página inválida. Uso: /catalogo [número de página]");
        }
        if (page < 1) {
            return BotMessage.text("La página tiene que ser un número mayor o igual a 1.");
        }

        try {
            List<Card> catalog = apiClient.getCatalog(session.token());
            if (catalog.isEmpty()) {
                return BotMessage.text("El catálogo está vacío.");
            }
            int total = catalog.size();
            int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
            if (page > totalPages) {
                return BotMessage.text("No hay página " + page + ". El catálogo tiene "
                        + total + " figuritas (" + totalPages + " páginas).");
            }
            int from = (page - 1) * PAGE_SIZE;
            String items = catalog.stream()
                    .sorted(Comparator.comparingInt(Card::number))
                    .skip(from)
                    .limit(PAGE_SIZE)
                    .map(c -> c.number() + ". " + c.description() + " (" + c.id() + ")")
                    .collect(Collectors.joining("\n"));
            String text = "Catálogo — página " + page + "/" + totalPages
                    + " (" + total + " figuritas):\n" + items;
            if (totalPages == 1) {
                return BotMessage.text(text);
            }
            return BotMessage.withKeyboard(text, pageKeyboard(page, totalPages));
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) return BotMessage.text(onSessionExpired(chatId));
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /catalogo: {}", e.getStatus(), e.getMessage());
                return BotMessage.text(e.getMessage());
            }
            log.error("Error del backend en /catalogo", e);
            return BotMessage.text("No pude obtener el catálogo. Probá más tarde.");
        } catch (Exception e) {
            log.error("Error inesperado en /catalogo", e);
            return BotMessage.text("No pude obtener el catálogo. Probá más tarde.");
        }
    }

    /**
     * Teclado de paginación: ◀️ a la izquierda y ▶️ a la derecha. Sólo se incluye la flecha
     * disponible (no hay anterior en la primera página ni siguiente en la última).
     */
    private static InlineKeyboardMarkup pageKeyboard(int page, int totalPages) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (page > 1) {
            row.add(InlineKeyboardButton.builder()
                    .text("◀️")
                    .callbackData(CALLBACK_PREFIX + (page - 1))
                    .build());
        }
        if (page < totalPages) {
            row.add(InlineKeyboardButton.builder()
                    .text("▶️")
                    .callbackData(CALLBACK_PREFIX + (page + 1))
                    .build());
        }
        return InlineKeyboardMarkup.builder().keyboardRow(row).build();
    }

    private static int parsePage(String args) {
        if (args.isBlank()) {
            return 1;
        }
        return Integer.parseInt(args.trim());
    }
}
