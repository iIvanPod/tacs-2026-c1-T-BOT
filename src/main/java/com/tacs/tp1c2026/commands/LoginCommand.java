package com.tacs.tp1c2026.commands;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

/**
 * Abre la Mini App de TACS con un botón web_app. El login ocurre dentro del webview
 * (formulario del frontend), así las credenciales nunca quedan en el historial del chat.
 */
@Component
public class LoginCommand implements CommandHandler, InteractiveCommand {

    private final String tmaUrl;

    public LoginCommand(@Value("${tma.url}") String tmaUrl) {
        this.tmaUrl = tmaUrl;
    }

    @Override
    public String name() {
        return "/login";
    }

    @Override
    public String description() {
        return "Iniciá sesión de forma segura en la Mini App de TACS";
    }

    @Override
    public String execute(CommandContext ctx) {
        return executeInteractive(ctx).text(); // fallback de texto para clientes sin web_app
    }

    @Override
    public BotMessage executeInteractive(CommandContext ctx) {
        InlineKeyboardRow fila = new InlineKeyboardRow();
        fila.add(InlineKeyboardButton.builder()
                .text("🔐 Iniciar sesión")
                .webApp(WebAppInfo.builder().url(tmaUrl).build())
                .build());

        InlineKeyboardMarkup teclado = InlineKeyboardMarkup.builder()
                .keyboardRow(fila)
                .build();

        return BotMessage.withKeyboard(
                "Tocá el botón para iniciar sesión en TACS bot.",
                teclado);
    }
}
