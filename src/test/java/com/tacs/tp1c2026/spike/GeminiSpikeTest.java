package com.tacs.tp1c2026.spike;

import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Spike de Fase 0: confirma que Gemini (gemini-2.5-flash-lite) responde con la API key real.
 * Aislado del bot: levanta solo los autoconfigs de Spring AI necesarios, sin Telegram.
 * Se omite (no falla) si GEMINI_API_KEY no esta seteada.
 */
class GeminiSpikeTest {

	@Test
	void geminiResponde() {
		String apiKey = System.getenv("GEMINI_API_KEY");
		assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY no seteada; spike omitido");

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						SpringAiRetryAutoConfiguration.class,
						ToolCallingAutoConfiguration.class,
						GoogleGenAiChatAutoConfiguration.class))
				.withPropertyValues(
						"spring.ai.google.genai.api-key=" + apiKey,
						"spring.ai.google.genai.chat.options.model=gemini-2.5-flash-lite")
				.run(ctx -> {
					GoogleGenAiChatModel model = ctx.getBean(GoogleGenAiChatModel.class);
					String respuesta = model.call("Responde unicamente con la palabra: hola");
					assertThat(respuesta).isNotBlank();
					System.out.println("[SPIKE GEMINI] respuesta = " + respuesta);
				});
	}
}
