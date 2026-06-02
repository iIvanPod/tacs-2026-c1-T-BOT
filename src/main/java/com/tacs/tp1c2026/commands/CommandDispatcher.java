package com.tacs.tp1c2026.commands;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CommandDispatcher {

    private final Map<String, CommandHandler> handlers;

    public CommandDispatcher(List<CommandHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toUnmodifiableMap(CommandHandler::name, h -> h));
    }

    public String dispatch(long chatId, String texto) {
        String[] parts = texto.split("\\s+", 2);
        String cmd = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        CommandHandler handler = handlers.get(cmd);
        if (handler == null) {
            return cmd + " no es un comando existente. Usá /help para ver los comandos disponibles.";
        }
        return handler.execute(new CommandContext(chatId, args));
    }

    public List<CommandHandler> all() {
        return handlers.values().stream()
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }
}
