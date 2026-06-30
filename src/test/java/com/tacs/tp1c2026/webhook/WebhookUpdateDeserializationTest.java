package com.tacs.tp1c2026.webhook;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El controller recibe el update con @RequestBody Update, o sea Spring deserializa el JSON de Telegram
 * al POJO de la librería con Jackson. Este test verifica esa ruta (la parte que más fácil se rompe)
 * con un mapper configurado igual que el de Spring (tolerante a campos desconocidos).
 */
class WebhookUpdateDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void deserializaUnMensajeDeTexto() throws Exception {
        String json = """
            {
              "update_id": 123456789,
              "message": {
                "message_id": 11,
                "from": {"id": 42, "is_bot": false, "first_name": "Pepe"},
                "chat": {"id": 42, "type": "private", "first_name": "Pepe"},
                "date": 1700000000,
                "text": "/coleccion"
              }
            }
            """;

        Update update = mapper.readValue(json, Update.class);

        assertThat(update.hasMessage()).isTrue();
        assertThat(update.getMessage().hasText()).isTrue();
        assertThat(update.getMessage().getText()).isEqualTo("/coleccion");
        assertThat(update.getMessage().getChatId()).isEqualTo(42L);
    }

    @Test
    void deserializaUnCallbackQuery() throws Exception {
        String json = """
            {
              "update_id": 123456790,
              "callback_query": {
                "id": "abc",
                "from": {"id": 42, "is_bot": false, "first_name": "Pepe"},
                "data": "cat:2",
                "chat_instance": "x",
                "message": {
                  "message_id": 12,
                  "chat": {"id": 42, "type": "private"},
                  "date": 1700000000,
                  "text": "catalogo"
                }
              }
            }
            """;

        Update update = mapper.readValue(json, Update.class);

        assertThat(update.hasCallbackQuery()).isTrue();
        assertThat(update.getCallbackQuery().getData()).isEqualTo("cat:2");
    }
}
