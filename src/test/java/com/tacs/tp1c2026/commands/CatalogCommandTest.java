package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogCommandTest {

	private static final long CHAT_ID = 1L;
	private static final String TOKEN = "tok";

	private SessionStore sessionStore;
	private BackendApiClient apiClient;
	private CatalogCommand command;

	@BeforeEach
	void setUp() {
		sessionStore = mock(SessionStore.class);
		apiClient = mock(BackendApiClient.class);
		command = new CatalogCommand(sessionStore, apiClient);
		when(sessionStore.get(CHAT_ID)).thenReturn(Optional.of(new Session("user-1", TOKEN)));
	}

	@Test
	void primeraPaginaSoloMuestraFlechaSiguiente() {
		when(apiClient.getCatalog(TOKEN)).thenReturn(cards(25));

		BotMessage msg = command.executeInteractive(new CommandContext(CHAT_ID, "1"));

		assertThat(msg.text()).contains("página 1/3");
		InlineKeyboardRow row = onlyRow(msg.keyboard());
		assertThat(row).hasSize(1);
		assertThat(row.get(0).getCallbackData()).isEqualTo("catalogo:2");
		assertThat(row.get(0).getText()).isEqualTo("▶️");
	}

	@Test
	void paginaIntermediaMuestraAmbasFlechas() {
		when(apiClient.getCatalog(TOKEN)).thenReturn(cards(25));

		BotMessage msg = command.executeInteractive(new CommandContext(CHAT_ID, "2"));

		assertThat(msg.text()).contains("página 2/3");
		InlineKeyboardRow row = onlyRow(msg.keyboard());
		assertThat(row).hasSize(2);
		assertThat(row.get(0).getCallbackData()).isEqualTo("catalogo:1");
		assertThat(row.get(0).getText()).isEqualTo("◀️");
		assertThat(row.get(1).getCallbackData()).isEqualTo("catalogo:3");
		assertThat(row.get(1).getText()).isEqualTo("▶️");
	}

	@Test
	void ultimaPaginaSoloMuestraFlechaAnterior() {
		when(apiClient.getCatalog(TOKEN)).thenReturn(cards(25));

		BotMessage msg = command.executeInteractive(new CommandContext(CHAT_ID, "3"));

		assertThat(msg.text()).contains("página 3/3");
		InlineKeyboardRow row = onlyRow(msg.keyboard());
		assertThat(row).hasSize(1);
		assertThat(row.get(0).getCallbackData()).isEqualTo("catalogo:2");
		assertThat(row.get(0).getText()).isEqualTo("◀️");
	}

	@Test
	void unaSolaPaginaNoLlevaTeclado() {
		when(apiClient.getCatalog(TOKEN)).thenReturn(cards(5));

		BotMessage msg = command.executeInteractive(new CommandContext(CHAT_ID, "1"));

		assertThat(msg.text()).contains("página 1/1");
		assertThat(msg.keyboard()).isNull();
	}

	@Test
	void sinSesionPideLoginYNoLlevaTeclado() {
		when(sessionStore.get(CHAT_ID)).thenReturn(Optional.empty());

		BotMessage msg = command.executeInteractive(new CommandContext(CHAT_ID, "1"));

		assertThat(msg.text()).contains("/login");
		assertThat(msg.keyboard()).isNull();
	}

	private static InlineKeyboardRow onlyRow(InlineKeyboardMarkup keyboard) {
		assertThat(keyboard).isNotNull();
		assertThat(keyboard.getKeyboard()).hasSize(1);
		return keyboard.getKeyboard().get(0);
	}

	private static List<Card> cards(int n) {
		List<Card> list = new ArrayList<>();
		for (int i = 1; i <= n; i++) {
			list.add(new Card("id" + i, i, "tipo", "Figu " + i, "AR", "Equipo", "cat"));
		}
		return list;
	}
}
