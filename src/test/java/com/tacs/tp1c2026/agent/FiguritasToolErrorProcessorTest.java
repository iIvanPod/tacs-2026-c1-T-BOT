package com.tacs.tp1c2026.agent;

import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.session.NotLoggedInException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FiguritasToolErrorProcessorTest {

	private final FiguritasToolErrorProcessor processor = new FiguritasToolErrorProcessor();
	private final ToolDefinition anyTool = mock(ToolDefinition.class);

	private ToolExecutionException wrap(Throwable cause) {
		return new ToolExecutionException(anyTool, cause);
	}

	@Test
	void propagaEl401() {
		BackendApiException unauthorized = new BackendApiException(401, "unauthorized", "token vencido");
		assertThatThrownBy(() -> processor.process(wrap(unauthorized)))
				.isInstanceOf(BackendApiException.class);
	}

	@Test
	void devuelveElMensajeParaOtrosErroresDeBackend() {
		BackendApiException notFound = new BackendApiException(404, "not found", "Figurita inexistente");
		String result = processor.process(wrap(notFound));
		assertThat(result).isEqualTo("Figurita inexistente");
	}

	@Test
	void propagaCuandoNoHaySesion() {
		assertThatThrownBy(() -> processor.process(wrap(new NotLoggedInException("sin sesión"))))
				.isInstanceOf(NotLoggedInException.class);
	}
}
