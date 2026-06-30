package com.tacs.tp1c2026.webhook;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WebhookRegistrarTest {

    private final TelegramClient client = mock(TelegramClient.class);

    @Test
    void conUrlRegistraElWebhookConUrlCompletaYSecret() throws Exception {
        // baseUrl con barra final: se normaliza antes de concatenar el path.
        WebhookRegistrar registrar = new WebhookRegistrar(client,
                "https://bot.example.com/", "/webhook", "s3cr3t");

        registrar.registerWebhook();

        ArgumentCaptor<SetWebhook> captor = ArgumentCaptor.forClass(SetWebhook.class);
        verify(client).execute(captor.capture());
        SetWebhook sent = captor.getValue();
        assertThat(sent.getUrl()).isEqualTo("https://bot.example.com/webhook");
        assertThat(sent.getSecretToken()).isEqualTo("s3cr3t");
        assertThat(sent.getMaxConnections()).isEqualTo(1);
    }

    @Test
    void sinUrlNoRegistraNada() {
        WebhookRegistrar registrar = new WebhookRegistrar(client, "", "/webhook", "s3cr3t");

        registrar.registerWebhook();

        verifyNoInteractions(client);
    }

    @Test
    void sinSecretMandaSecretTokenNull() throws Exception {
        WebhookRegistrar registrar = new WebhookRegistrar(client,
                "https://bot.example.com", "/webhook", "");

        registrar.registerWebhook();

        ArgumentCaptor<SetWebhook> captor = ArgumentCaptor.forClass(SetWebhook.class);
        verify(client).execute(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo("https://bot.example.com/webhook");
        assertThat(captor.getValue().getSecretToken()).isNull();
    }
}
