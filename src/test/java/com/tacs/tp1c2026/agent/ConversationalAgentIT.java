package com.tacs.tp1c2026.agent;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.tools.FiguritasTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verificación en vivo de Fase 2: confirma que Gemini interpreta la intención en lenguaje natural
 * y dispara la tool correcta. Mockea el backend y comprueba que preguntar "¿qué me falta?"
 * termina invocando {@code getMissingCards} (es decir, la tool verFaltantes).
 *
 * Nombre terminado en IT: NO corre en el {@code mvn test} normal (para no gastar tokens).
 * Se corre a mano con la key:
 *   $env:GEMINI_API_KEY="..."; .\mvnw.cmd test "-Dtest=ConversationalAgentIT"
 * Se omite (no falla) si GEMINI_API_KEY no está seteada.
 */
class ConversationalAgentIT {

	@Test
	void interpretaIntencionYDisparaLaToolDeFaltantes() {
		String apiKey = System.getenv("GEMINI_API_KEY");
		assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY no seteada; test omitido");

		BackendApiClient apiClient = mock(BackendApiClient.class);
		when(apiClient.getMissingCards("user-1", "token-abc"))
				.thenReturn(List.of(new MissingCard("c7", 7, "Di María")));

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						SpringAiRetryAutoConfiguration.class,
						ToolCallingAutoConfiguration.class,
						GoogleGenAiChatAutoConfiguration.class,
						ChatClientAutoConfiguration.class))
				.withBean(BackendApiClient.class, () -> apiClient)
				.withBean(FiguritasTools.class)
				.withBean(ConversationalAgent.class)
				.withPropertyValues(
						"spring.ai.google.genai.api-key=" + apiKey,
						"spring.ai.google.genai.chat.options.model=gemini-2.5-flash-lite")
				.run(ctx -> {
					ConversationalAgent agent = ctx.getBean(ConversationalAgent.class);
					String respuesta = agent.chat(123L, "¿qué figuritas me faltan?",
							new Session("user-1", "token-abc"));
					System.out.println("[AGENT IT] respuesta = " + respuesta);
					verify(apiClient, atLeastOnce()).getMissingCards("user-1", "token-abc");
				});
	}
}
