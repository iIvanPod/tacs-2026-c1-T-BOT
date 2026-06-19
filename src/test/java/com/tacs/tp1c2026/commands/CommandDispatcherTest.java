package com.tacs.tp1c2026.commands;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandDispatcherTest {

	@Test
	void comandoInexistenteAvisa() {
		CommandDispatcher dispatcher = new CommandDispatcher(List.of());

		BotMessage respuesta = dispatcher.dispatch(1L, "/noexiste algo");

		assertThat(respuesta.text()).isEqualTo(
				"/noexiste no es un comando existente. Usá /help para ver los comandos disponibles.");
		assertThat(respuesta.keyboard()).isNull();
	}

	@Test
	void comandoExistenteDelegaEnElHandler() {
		CommandHandler handler = mock(CommandHandler.class);
		when(handler.name()).thenReturn("/test");
		when(handler.execute(any())).thenReturn("ok");
		CommandDispatcher dispatcher = new CommandDispatcher(List.of(handler));

		BotMessage respuesta = dispatcher.dispatch(1L, "/test con args");

		assertThat(respuesta.text()).isEqualTo("ok");
		assertThat(respuesta.keyboard()).isNull();
	}

	@Test
	void comandoInteractivoSeRuteaPorExecuteInteractive() {
		FakeInteractive fake = new FakeInteractive();
		CommandDispatcher dispatcher = new CommandDispatcher(List.of(fake));

		BotMessage respuesta = dispatcher.dispatch(7L, "/fake 2");

		assertThat(respuesta.text()).isEqualTo("interactivo:2");
		assertThat(fake.lastArgs).isEqualTo("2");
	}

	@Test
	void dispatchCallbackRuteaAlComandoInteractivo() {
		FakeInteractive fake = new FakeInteractive();
		CommandDispatcher dispatcher = new CommandDispatcher(List.of(fake));

		Optional<BotMessage> respuesta = dispatcher.dispatchCallback(7L, "fake:3");

		assertThat(respuesta).isPresent();
		assertThat(respuesta.get().text()).isEqualTo("interactivo:3");
		assertThat(fake.lastArgs).isEqualTo("3");
	}

	@Test
	void dispatchCallbackDeComandoDesconocidoEsVacio() {
		CommandDispatcher dispatcher = new CommandDispatcher(List.of());

		assertThat(dispatcher.dispatchCallback(7L, "noexiste:3")).isEmpty();
	}

	@Test
	void dispatchCallbackDeComandoNoInteractivoEsVacio() {
		CommandHandler handler = mock(CommandHandler.class);
		when(handler.name()).thenReturn("/test");
		CommandDispatcher dispatcher = new CommandDispatcher(List.of(handler));

		assertThat(dispatcher.dispatchCallback(7L, "test:3")).isEmpty();
	}

	private static class FakeInteractive implements CommandHandler, InteractiveCommand {
		private String lastArgs;

		@Override
		public String name() {
			return "/fake";
		}

		@Override
		public String description() {
			return "fake";
		}

		@Override
		public String execute(CommandContext ctx) {
			return "texto";
		}

		@Override
		public BotMessage executeInteractive(CommandContext ctx) {
			this.lastArgs = ctx.args();
			return BotMessage.text("interactivo:" + ctx.args());
		}
	}
}
