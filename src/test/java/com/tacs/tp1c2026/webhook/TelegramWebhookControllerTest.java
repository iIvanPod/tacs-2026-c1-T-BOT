package com.tacs.tp1c2026.webhook;

import com.tacs.tp1c2026.FiguritasBot;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TelegramWebhookControllerTest {

    private final FiguritasBot bot = mock(FiguritasBot.class);
    private final Executor executor = inlineExecutor();

    /** Executor mockeado que corre el Runnable en el acto, para poder verificar el efecto. */
    private static Executor inlineExecutor() {
        Executor e = mock(Executor.class);
        doAnswer(inv -> {
            inv.getArgument(0, Runnable.class).run();
            return null;
        }).when(e).execute(any(Runnable.class));
        return e;
    }

    @Test
    void sinSecretConfiguradoProcesaElUpdate() {
        TelegramWebhookController controller = new TelegramWebhookController(bot, executor, "");
        Update update = mock(Update.class);

        ResponseEntity<Void> res = controller.onUpdate(update, null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bot).consume(update);
    }

    @Test
    void conSecretCorrectoProcesaElUpdate() {
        TelegramWebhookController controller = new TelegramWebhookController(bot, executor, "s3cr3t");
        Update update = mock(Update.class);

        ResponseEntity<Void> res = controller.onUpdate(update, "s3cr3t");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bot).consume(update);
    }

    @Test
    void conSecretInvalidoDevuelve403YNoProcesa() {
        TelegramWebhookController controller = new TelegramWebhookController(bot, executor, "s3cr3t");

        ResponseEntity<Void> res = controller.onUpdate(mock(Update.class), "otro");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(bot);
    }

    @Test
    void conSecretConfiguradoYHeaderAusenteDevuelve403() {
        TelegramWebhookController controller = new TelegramWebhookController(bot, executor, "s3cr3t");

        ResponseEntity<Void> res = controller.onUpdate(mock(Update.class), null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(bot);
    }

    @Test
    void siElProcesamientoFallaIgualResponde200YNoPropaga() {
        TelegramWebhookController controller = new TelegramWebhookController(bot, executor, "");
        doThrow(new RuntimeException("boom")).when(bot).consume(any(Update.class));

        ResponseEntity<Void> res = controller.onUpdate(mock(Update.class), null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bot).consume(any(Update.class));
    }
}
