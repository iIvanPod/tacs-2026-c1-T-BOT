package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;

import java.util.Optional;

public abstract class IdentifiedCommand implements CommandHandler {

    private final SessionStore sessionStore;

    protected IdentifiedCommand(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public final String execute(CommandContext ctx) {
        Optional<Session> session = sessionStore.get(ctx.chatId());
        if (session.isEmpty()) {
            return "Antes tenés que identificarte con /login <email> <password>.";
        }
        return executeAsUser(session.get(), ctx);
    }

    protected final String onSessionExpired(long chatId) {
        sessionStore.remove(chatId);
        return "Tu sesión expiró. Volvé a identificarte con /login <email> <password>.";
    }

    protected abstract String executeAsUser(Session session, CommandContext ctx);
}
