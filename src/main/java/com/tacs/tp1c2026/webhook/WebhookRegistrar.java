package com.tacs.tp1c2026.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Al arrancar registra el webhook contra Telegram (setWebhook) apuntando a la URL pública de este
 * servicio. Si no hay URL configurada (local sin túnel, o tests) no hace nada: el bot levanta igual
 * pero no recibe updates hasta que se setee WEBHOOK_URL.
 */
@Component
public class WebhookRegistrar {

    private static final Logger log = LoggerFactory.getLogger(WebhookRegistrar.class);

    private final TelegramClient telegramClient;
    private final String baseUrl;
    private final String path;
    private final String secret;

    public WebhookRegistrar(TelegramClient telegramClient,
                            @Value("${telegram.webhook.url:}") String baseUrl,
                            @Value("${telegram.webhook.path:/webhook}") String path,
                            @Value("${telegram.webhook.secret:}") String secret) {
        this.telegramClient = telegramClient;
        this.baseUrl = baseUrl;
        this.path = path;
        this.secret = secret;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        if (baseUrl.isBlank()) {
            log.warn("telegram.webhook.url vacío: no registro webhook. El bot no recibirá mensajes "
                    + "hasta setear WEBHOOK_URL (en local, exponé el puerto con un túnel tipo ngrok).");
            return;
        }
        String fullUrl = baseUrl.replaceAll("/+$", "") + path;
        SetWebhook setWebhook = SetWebhook.builder()
                .url(fullUrl)
                .secretToken(secret.isBlank() ? null : secret)
                // De a una conexión: Telegram no manda el próximo update hasta recibir el 200 del anterior.
                // Junto al executor single-thread, preserva el orden de los mensajes por chat.
                .maxConnections(1)
                .build();
        try {
            telegramClient.execute(setWebhook);
            if (secret.isBlank()) {
                log.warn("Webhook registrado en {} SIN secret token: el endpoint queda sin autenticar. "
                        + "Seteá WEBHOOK_SECRET en producción.", fullUrl);
            } else {
                log.info("Webhook registrado en {} (con secret token)", fullUrl);
            }
        } catch (TelegramApiException e) {
            log.error("No pude registrar el webhook en {}", fullUrl, e);
        }
    }
}
