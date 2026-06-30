package com.tacs.tp1c2026.webhook;

import com.tacs.tp1c2026.FiguritasBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.Executor;

/**
 * Recibe los updates que Telegram entrega por webhook (reemplaza al long-polling). Responde 200
 * de inmediato y delega el procesamiento al executor, para no bloquear el hilo HTTP ni provocar
 * reintentos de Telegram si el manejo tarda.
 */
@RestController
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final FiguritasBot bot;
    private final Executor webhookExecutor;
    private final String secret;

    public TelegramWebhookController(FiguritasBot bot,
                                     Executor webhookExecutor,
                                     @Value("${telegram.webhook.secret:}") String secret) {
        this.bot = bot;
        this.webhookExecutor = webhookExecutor;
        this.secret = secret;
    }

    @PostMapping("${telegram.webhook.path:/webhook}")
    public ResponseEntity<Void> onUpdate(@RequestBody Update update,
                                         @RequestHeader(value = SECRET_HEADER, required = false) String token) {
        if (!secret.isBlank() && !secret.equals(token)) {
            log.warn("Update con secret token inválido — lo descarto");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        webhookExecutor.execute(() -> {
            try {
                bot.consume(update);
            } catch (Exception e) {
                // El procesamiento corre fuera del hilo HTTP: si algo no controlado explota acá, lo logueamos
                // (si no, moriría silencioso en el worker) para poder diagnosticarlo.
                log.error("Error procesando el update", e);
            }
        });
        return ResponseEntity.ok().build();
    }
}
