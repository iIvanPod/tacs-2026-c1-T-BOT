package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.Page;
import com.tacs.tp1c2026.dtos.TradePublication;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.stream.Collectors;

@Component
public class ListPublicationsCommand extends IdentifiedCommand implements InteractiveCommand {

    private static final Logger log = LoggerFactory.getLogger(ListPublicationsCommand.class);

    private static final int PER_PAGE = 5;

    /** Prefijo del callback de los botones de paginación: {@code publicaciones:<página>}. */
    static final String CALLBACK_PREFIX = "publicaciones:";

    private final BackendApiClient apiClient;

    public ListPublicationsCommand(SessionStore sessionStore, BackendApiClient apiClient) {
        super(sessionStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/publicaciones";
    }

    @Override
    public String description() {
        return "Lista las publicaciones de intercambio activas. Uso: /publicaciones [página]";
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
            return BotMessage.text("Página inválida. Uso: /publicaciones [número de página]");
        }
        if (page < 1) {
            return BotMessage.text("La página tiene que ser un número mayor o igual a 1.");
        }

        try {
            Page<TradePublication> result = apiClient.listPublications(page, PER_PAGE, session.token());
            if (result.items().isEmpty()) {
                if (page == 1) {
                    return BotMessage.text("No hay publicaciones de intercambio activas por ahora.");
                }
                return BotMessage.text("No hay página " + page + ". Hay " + result.totalPages() + " páginas.");
            }
            String items = result.items().stream()
                    .map(ListPublicationsCommand::format)
                    .collect(Collectors.joining("\n\n"));
            String text = "Publicaciones activas — página " + result.currentPage() + "/" + result.totalPages()
                    + ":\n\n" + items;
            if (result.totalPages() <= 1) {
                return BotMessage.text(text);
            }
            return BotMessage.withKeyboard(text, pageKeyboard(result.currentPage(), result.totalPages()));
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) return BotMessage.text(onSessionExpired(chatId));
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /publicaciones: {}", e.getStatus(), e.getMessage());
                return BotMessage.text(e.getMessage());
            }
            log.error("Error del backend en /publicaciones", e);
            return BotMessage.text("No pude obtener las publicaciones. Probá más tarde.");
        } catch (Exception e) {
            log.error("Error inesperado en /publicaciones", e);
            return BotMessage.text("No pude obtener las publicaciones. Probá más tarde.");
        }
    }

    private static String format(TradePublication p) {
        return "#" + p.cardNumber() + " " + p.cardDescription()
                + "\n  Disponibles: " + p.remainingCount() + " — por " + p.publisherName();
    }

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
