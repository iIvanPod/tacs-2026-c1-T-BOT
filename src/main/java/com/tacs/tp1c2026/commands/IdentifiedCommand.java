package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;

import java.util.Optional;

public abstract class IdentifiedCommand implements CommandHandler {

    protected static final String NOT_IDENTIFIED =
            "Antes tenés que iniciar sesión con /login.";

    private final SessionStore sessionStore;

    protected IdentifiedCommand(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public final String execute(CommandContext ctx) {
        Optional<Session> session = sessionStore.get(ctx.chatId());
        if (session.isEmpty()) {
            return NOT_IDENTIFIED;
        }
        return executeAsUser(session.get(), ctx);
    }

    protected final Optional<Session> sessionOf(long chatId) {
        return sessionStore.get(chatId);
    }

    protected final String onSessionExpired(long chatId) {
        sessionStore.remove(chatId);
        return "Tu sesión expiró. Volvé a iniciar sesión con /login.";
    }

    protected abstract String executeAsUser(Session session, CommandContext ctx);
}
