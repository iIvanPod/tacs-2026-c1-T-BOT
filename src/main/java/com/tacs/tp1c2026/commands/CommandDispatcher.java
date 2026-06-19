package com.tacs.tp1c2026.commands;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CommandDispatcher {

    private final Map<String, CommandHandler> handlers;

    public CommandDispatcher(List<CommandHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toUnmodifiableMap(CommandHandler::name, h -> h));
    }

    public BotMessage dispatch(long chatId, String texto) {
        String[] parts = texto.split("\\s+", 2);
        String cmd = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        CommandHandler handler = handlers.get(cmd);
        if (handler == null) {
            return BotMessage.text(
                    cmd + " no es un comando existente. Usá /help para ver los comandos disponibles.");
        }
        CommandContext ctx = new CommandContext(chatId, args);
        if (handler instanceof InteractiveCommand interactive) {
            return interactive.executeInteractive(ctx);
        }
        return BotMessage.text(handler.execute(ctx));
    }

    /**
     * Enruta el callback de un botón inline con formato {@code <comando>:<arg>} (p. ej. {@code catalogo:2})
     * al comando interactivo correspondiente. Devuelve vacío si no hay un comando interactivo para ese dato.
     */
    public Optional<BotMessage> dispatchCallback(long chatId, String data) {
        int sep = data.indexOf(':');
        String key = sep < 0 ? data : data.substring(0, sep);
        String arg = sep < 0 ? "" : data.substring(sep + 1);

        CommandHandler handler = handlers.get("/" + key);
        if (handler instanceof InteractiveCommand interactive) {
            return Optional.of(interactive.executeInteractive(new CommandContext(chatId, arg)));
        }
        return Optional.empty();
    }

    public List<CommandHandler> all() {
        return handlers.values().stream()
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }
}
