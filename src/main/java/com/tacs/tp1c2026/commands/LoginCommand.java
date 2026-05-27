package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiClient.LoginResult;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginCommand implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginCommand.class);

    private final BackendApiClient apiClient;
    private final SessionStore sessionStore;

    public LoginCommand(BackendApiClient apiClient, SessionStore sessionStore) {
        this.apiClient = apiClient;
        this.sessionStore = sessionStore;
    }

    @Override
    public String name() {
        return "/login";
    }

    @Override
    public String description() {
        return "Iniciá sesión con tu usuario del backend. Uso: /login <email> <password>";
    }

    @Override
    public String execute(CommandContext ctx) {
        String[] parts = ctx.args().split("\\s+", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return "Faltan datos. Uso: /login <email> <password>";
        }
        String email = parts[0].trim();
        String password = parts[1];
        try {
            LoginResult r = apiClient.login(email, password);
            sessionStore.save(ctx.chatId(), new Session(r.userId(), r.token()));
            return "Listo, sesión iniciada como " + r.name() + ".";
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) {
                return "Credenciales inválidas.";
            }
            if (e.isExpectedClientError()) {
                log.warn("Backend respondió {} en /login: {}", e.getStatus(), e.getMessage());
                return e.getMessage();
            }
            log.error("Error del backend en /login", e);
            return "No pude iniciar sesión. Probá más tarde.";
        } catch (Exception e) {
            log.error("Error inesperado en /login", e);
            return "No pude iniciar sesión. Probá más tarde.";
        }
    }
}
