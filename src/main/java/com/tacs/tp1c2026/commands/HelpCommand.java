package com.tacs.tp1c2026.commands;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class HelpCommand implements CommandHandler {

    private final CommandDispatcher dispatcher;

    public HelpCommand(@Lazy CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public String name() {
        return "/help";
    }

    @Override
    public String description() {
        return "Muestra esta ayuda";
    }

    @Override
    public String execute(CommandContext ctx) {
        String lista = dispatcher.all().stream()
                .map(c -> c.name() + " - " + c.description())
                .collect(Collectors.joining("\n"));
        return "Comandos disponibles:\n" + lista;
    }
}
