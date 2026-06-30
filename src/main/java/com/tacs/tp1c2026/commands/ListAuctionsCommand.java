package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.Auction;
import com.tacs.tp1c2026.dtos.Page;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@Component
public class ListAuctionsCommand extends IdentifiedCommand implements InteractiveCommand {

    private static final Logger log = LoggerFactory.getLogger(ListAuctionsCommand.class);

    private static final int PER_PAGE = 5;

    private static final DateTimeFormatter CLOSE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** Prefijo del callback de los botones de paginación: {@code subastas:<página>}. */
    static final String CALLBACK_PREFIX = "subastas:";

    private final BackendApiClient apiClient;

    public ListAuctionsCommand(SessionStore sessionStore, BackendApiClient apiClient) {
        super(sessionStore);
        this.apiClient = apiClient;
    }

    @Override
    public String name() {
        return "/subastas";
    }

    @Override
    public String description() {
        return "Lista las subastas activas. Uso: /subastas [página]";
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
            return BotMessage.text("Página inválida. Uso: /subastas [número de página]");
        }
        if (page < 1) {
            return BotMessage.text("La página tiene que ser un número mayor o igual a 1.");
        }

        try {
            Page<Auction> result = apiClient.listAuctions(page, PER_PAGE, session.token());
            if (result.items().isEmpty()) {
                if (page == 1) {
                    return BotMessage.text("No hay subastas activas por ahora.");
                }
                return BotMessage.text("No hay página " + page + ". Hay " + result.totalPages() + " páginas.");
            }
            String items = result.items().stream()
                    .map(ListAuctionsCommand::format)
                    .collect(Collectors.joining("\n\n"));
            String text = "Subastas activas — página " + result.currentPage() + "/" + result.totalPages()
                    + ":\n\n" + items;
            if (result.totalPages() <= 1) {
                return BotMessage.text(text);
            }
            return BotMessage.withKeyboard(text, pageKeyboard(result.currentPage(), result.totalPages()));
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) return BotMessage.text(onSessionExpired(chatId));
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /subastas: {}", e.getStatus(), e.getMessage());
                return BotMessage.text(e.getMessage());
            }
            log.error("Error del backend en /subastas", e);
            return BotMessage.text("No pude obtener las subastas. Probá más tarde.");
        } catch (Exception e) {
            log.error("Error inesperado en /subastas", e);
            return BotMessage.text("No pude obtener las subastas. Probá más tarde.");
        }
    }

    private static String format(Auction a) {
        String rating = a.publisherRating() != null ? " (★ " + a.publisherRating() + ")" : "";
        String oferta = a.bestOffer() != null
                ? a.bestOffer().username() + " (" + a.bestOffer().cardCount() + " figuritas)"
                : "sin ofertas";
        return "#" + a.cardNumber() + " " + a.cardDescription()
                + "\n  Cierra: " + formatCloseDate(a.closeDate()) + " — por " + a.publisherName() + rating
                + "\n  Mejor oferta: " + oferta;
    }

    private static String formatCloseDate(String closeDate) {
        try {
            return LocalDateTime.parse(closeDate).format(CLOSE_DATE_FORMAT);
        } catch (DateTimeParseException | NullPointerException e) {
            return closeDate;
        }
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
