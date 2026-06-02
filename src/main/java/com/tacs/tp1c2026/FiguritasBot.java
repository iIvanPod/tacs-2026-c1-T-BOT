package com.tacs.tp1c2026;

import com.tacs.tp1c2026.agent.ConversationalAgent;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.commands.CommandDispatcher;
import com.tacs.tp1c2026.session.NotLoggedInException;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class FiguritasBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(FiguritasBot.class);

    private final String botToken;
    private final TelegramClient telegramClient;
    private final CommandDispatcher dispatcher;
    private final ConversationalAgent agent;
    private final SessionStore sessionStore;

    public FiguritasBot(@Value("${telegram.bot.token}") String botToken,
                        CommandDispatcher dispatcher,
                        ConversationalAgent agent,
                        SessionStore sessionStore) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.dispatcher = dispatcher;
        this.agent = agent;
        this.sessionStore = sessionStore;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String texto = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        enviar(chatId, responder(chatId, texto));
    }

    String responder(long chatId, String texto) {
        if (texto.startsWith("/")) {
            return dispatcher.dispatch(chatId, texto);
        }
        return conversar(chatId, texto);
    }

    private String conversar(long chatId, String texto) {
        Session session = sessionStore.get(chatId).orElse(null);
        try {
            return agent.chat(chatId, texto, session);
        } catch (NotLoggedInException e) {
            return "No estás logueado actualmente. Usá /login <email> <password> para entrar en la plataforma y consultar tus figuritas.";
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) {
                sessionStore.remove(chatId);
                return "Tu sesión expiró. Volvé a identificarte con /login <email> <password>.";
            }
            log.error("Error del backend en charla con chatId {}", chatId, e);
            return "Uff, no pude procesar eso ahora. Probá de nuevo en un rato o usá /help.";
        } catch (Exception e) {
            log.error("Error del agente conversacional para chatId {}", chatId, e);
            return "Uff, no pude procesar eso ahora. Probá de nuevo en un rato o usá /help.";
        }
    }

    private void enviar(long chatId, String texto) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .build();
        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            log.error("No pude enviar mensaje a chatId {}", chatId, e);
        }
    }
}
