package com.tacs.tp1c2026;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfig {

    /** Cliente para hablar con la API de Telegram (enviar mensajes, registrar el webhook). */
    @Bean
    public TelegramClient telegramClient(@Value("${telegram.bot.token}") String botToken) {
        return new OkHttpTelegramClient(botToken);
    }

    /**
     * Procesa los updates del webhook fuera del hilo HTTP de Telegram: el controller responde 200 al toque
     * (evita timeouts y reintentos) y el manejo —que puede tardar por llamadas al backend o a Gemini— corre
     * acá. Un solo hilo serializa el procesamiento; sumado a maxConnections=1 en el webhook (Telegram entrega
     * de a uno) se preserva el orden, como el viejo consumer de long-polling. Al apagar, Spring espera a que
     * se drene la cola (hasta el timeout) para no perder updates ya aceptados durante un redeploy.
     */
    @Bean
    public ThreadPoolTaskExecutor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setThreadNamePrefix("webhook-update-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
