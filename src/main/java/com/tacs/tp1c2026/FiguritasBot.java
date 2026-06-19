package com.tacs.tp1c2026;

import com.tacs.tp1c2026.agent.ConversationalAgent;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.commands.BotMessage;
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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
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
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String texto = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        enviar(chatId, responder(chatId, texto));
    }

    BotMessage responder(long chatId, String texto) {
        if (texto.startsWith("/")) {
            return dispatcher.dispatch(chatId, texto);
        }
        return BotMessage.text(conversar(chatId, texto));
    }

    private void handleCallback(CallbackQuery callback) {
        MaybeInaccessibleMessage message = callback.getMessage();
        if (!(message instanceof Message accesible)) {
            // El mensaje original es inaccesible (muy viejo o borrado): no se puede editar en sitio.
            answerCallback(callback.getId(), "Ese mensaje es muy viejo. Usá /catalogo de nuevo.");
            return;
        }
        answerCallback(callback.getId(), null);
        String data = callback.getData();
        if (data == null) {
            return;
        }
        long chatId = accesible.getChatId();
        Integer messageId = accesible.getMessageId();
        dispatcher.dispatchCallback(chatId, data)
                .ifPresent(respuesta -> editar(chatId, messageId, respuesta));
    }

    private String conversar(long chatId, String texto) {
        Session session = sessionStore.get(chatId).orElse(null);
        try {
            return agent.chat(chatId, texto, session);
        } catch (NotLoggedInException e) {
            return "No estás logueado actualmente. Usá /login para iniciar sesión de forma segura en la app.";
        } catch (BackendApiException e) {
            if (e.getStatus() == 401) {
                sessionStore.remove(chatId);
                return "Tu sesión expiró. Volvé a iniciar sesión con /login.";
            }
            log.error("Error del backend en charla con chatId {}", chatId, e);
            return "Uff, no pude procesar eso ahora. Probá de nuevo en un rato o usá /help.";
        } catch (Exception e) {
            log.error("Error del agente conversacional para chatId {}", chatId, e);
            return "Uff, no pude procesar eso ahora. Probá de nuevo en un rato o usá /help.";
        }
    }

    private void enviar(long chatId, BotMessage mensaje) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(mensaje.text())
                .replyMarkup(mensaje.keyboard())
                .build();
        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            log.error("No pude enviar mensaje a chatId {}", chatId, e);
        }
    }

    private void editar(long chatId, Integer messageId, BotMessage mensaje) {
        EditMessageText edicion = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(mensaje.text())
                .replyMarkup(mensaje.keyboard())
                .build();
        try {
            telegramClient.execute(edicion);
        } catch (TelegramApiException e) {
            // Tocar dos veces el mismo botón reenvía la misma página: Telegram rechaza el edit
            // idéntico con "message is not modified". Es inofensivo (el mensaje ya muestra esa página).
            if (esMensajeNoModificado(e)) {
                return;
            }
            log.error("No pude editar el mensaje {} en chatId {}", messageId, chatId, e);
        }
    }

    private static boolean esMensajeNoModificado(TelegramApiException e) {
        return e.getMessage() != null && e.getMessage().contains("message is not modified");
    }

    private void answerCallback(String callbackId, String text) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("No pude responder el callback {}", callbackId, e);
        }
    }
}
