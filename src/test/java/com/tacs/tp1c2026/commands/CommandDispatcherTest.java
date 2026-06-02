package com.tacs.tp1c2026.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandDispatcherTest {

	@Test
	void comandoInexistenteAvisa() {
		CommandDispatcher dispatcher = new CommandDispatcher(List.of());

		String respuesta = dispatcher.dispatch(1L, "/noexiste algo");

		assertThat(respuesta).isEqualTo(
				"/noexiste no es un comando existente. Usá /help para ver los comandos disponibles.");
	}

	@Test
	void comandoExistenteDelegaEnElHandler() {
		CommandHandler handler = mock(CommandHandler.class);
		when(handler.name()).thenReturn("/test");
		when(handler.execute(any())).thenReturn("ok");
		CommandDispatcher dispatcher = new CommandDispatcher(List.of(handler));

		String respuesta = dispatcher.dispatch(1L, "/test con args");

		assertThat(respuesta).isEqualTo("ok");
	}
}
