package com.tacs.tp1c2026.commands;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Respuesta de un comando: texto y, opcionalmente, un teclado inline.
 * {@code keyboard} es {@code null} cuando la respuesta no lleva botones.
 */
public record BotMessage(String text, InlineKeyboardMarkup keyboard) {

    public static BotMessage text(String text) {
        return new BotMessage(text, null);
    }

    public static BotMessage withKeyboard(String text, InlineKeyboardMarkup keyboard) {
        return new BotMessage(text, keyboard);
    }
}
