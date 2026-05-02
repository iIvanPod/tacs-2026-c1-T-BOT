package com.tacs.tp1c2026.commands;

import org.springframework.stereotype.Component;

@Component
public class StartCommand implements CommandHandler {

    @Override
    public String name() {
        return "/start";
    }

    @Override
    public String description() {
        return "Mensaje de bienvenida";
    }

    @Override
    public String execute(CommandContext ctx) {
        return """
                ¡Hola! Soy el bot de figuritas TACS.
                Mandame /help para ver lo que puedo hacer.""";
    }
}
