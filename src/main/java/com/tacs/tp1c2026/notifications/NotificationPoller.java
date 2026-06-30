package com.tacs.tp1c2026.notifications;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.Notification;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;

/**
 * Entrega proactiva de notificaciones del backend al chat. Mientras el bot está despierto, cada tick
 * baja las notificaciones sin leer de cada sesión y las manda por Telegram. No hay cron ni keep-alive:
 * lo pendiente se entrega al prender y mientras siga vivo (entrega diferida).
 */
@Component
public class NotificationPoller {

    private static final Logger log = LoggerFactory.getLogger(NotificationPoller.class);

    private final SessionStore sessionStore;
    private final BackendApiClient apiClient;
    private final TelegramClient telegramClient;

    public NotificationPoller(SessionStore sessionStore,
                              BackendApiClient apiClient,
                              TelegramClient telegramClient) {
        this.sessionStore = sessionStore;
        this.apiClient = apiClient;
        this.telegramClient = telegramClient;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void poll() {
        for (Map.Entry<Long, Session> entry : sessionStore.all().entrySet()) {
            long chatId = entry.getKey();
            Session session = entry.getValue();
            try {
                deliver(chatId, session);
            } catch (BackendApiException e) {
                if (e.getStatus() == 401) {
                    send(chatId, "Tu sesión venció. Usá /login para seguir recibiendo alertas.");
                    sessionStore.remove(chatId);
                } else {
                    log.warn("Backend respondió {} al traer notificaciones de chatId {}: {}",
                            e.getStatus(), chatId, e.getMessage());
                }
            } catch (ResourceAccessException e) {
                log.warn("No pude contactar al backend para las alertas de chatId {}: {}",
                        chatId, e.getMessage());
            } catch (Exception e) {
                log.warn("Error inesperado entregando alertas a chatId {}", chatId, e);
            }
        }
    }

    private void deliver(long chatId, Session session) {
        List<Notification> notifications =
                apiClient.getUnreadNotifications(session.userId(), session.token());
        for (Notification n : notifications) {
            // Marco como leída sólo si el envío salió bien; si falla, el próximo tick reintenta.
            if (send(chatId, "🔔 " + n.message())) {
                try {
                    apiClient.markNotificationRead(session.userId(), n.id(), session.token());
                } catch (BackendApiException e) {
                    if (e.getStatus() == 401) {
                        throw e; // sesión vencida: lo maneja el catch de poll()
                    }
                    log.warn("No pude marcar como leída la notificación {} de chatId {}: {}",
                            n.id(), chatId, e.getMessage());
                } catch (Exception e) {
                    log.warn("No pude marcar como leída la notificación {} de chatId {}: {}",
                            n.id(), chatId, e.getMessage());
                }
            }
        }
    }

    private boolean send(long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(msg);
            return true;
        } catch (TelegramApiException e) {
            log.warn("No pude enviar una alerta a chatId {}: {}", chatId, e.getMessage());
            return false;
        }
    }
}
