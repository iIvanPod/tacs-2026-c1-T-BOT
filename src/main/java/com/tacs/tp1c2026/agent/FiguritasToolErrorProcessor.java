package com.tacs.tp1c2026.agent;

import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.session.NotLoggedInException;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.stereotype.Component;

/**
 * Decide qué hacer cuando una @Tool falla durante la conversación.
 *
 * Si devuelve un String, ese texto se le entrega al modelo para que rearme una respuesta natural
 * (útil para 404, etc.). Si lanza, la excepción se propaga hasta el bot.
 *
 * Política: se propagan dos casos para que {@code FiguritasBot} dé una respuesta determinística —
 * "no logueado" ({@link NotLoggedInException}) y el 401 (sesión vencida, limpia la sesión y pide
 * re-login). El resto de los errores los maneja el modelo.
 *
 * Reemplaza al {@code DefaultToolExecutionExceptionProcessor} de Spring AI (que es @ConditionalOnMissingBean).
 */
@Component
public class FiguritasToolErrorProcessor implements ToolExecutionExceptionProcessor {

	@Override
	public String process(ToolExecutionException exception) {
		Throwable cause = exception.getCause();
		if (cause instanceof NotLoggedInException notLoggedIn) {
			throw notLoggedIn;
		}
		if (cause instanceof BackendApiException backendError && backendError.getStatus() == 401) {
			throw backendError;
		}
		String message = cause != null ? cause.getMessage() : exception.getMessage();
		return message != null ? message : "No pude completar esa acción.";
	}
}
