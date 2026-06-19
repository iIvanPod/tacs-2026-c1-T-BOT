package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.session.SessionStore;
import org.springframework.stereotype.Component;

@Component
public class LogoutCommand implements CommandHandler {

    private final SessionStore sessionStore;

    public LogoutCommand(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public String name() {
        return "/olvidame";
    }

    @Override
    public String description() {
        return "Cierra tu sesión.";
    }

    @Override
    public String execute(CommandContext ctx) {
        if (sessionStore.get(ctx.chatId()).isEmpty()) {
            return "No tenías una sesión activa.";
        }
        sessionStore.remove(ctx.chatId());
        return "Listo, cerré tu sesión. Volvé a entrar con /login.";
    }
}
