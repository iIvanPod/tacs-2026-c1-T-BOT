package com.tacs.tp1c2026;

import com.tacs.tp1c2026.agent.ConversationalAgent;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.commands.CommandDispatcher;
import com.tacs.tp1c2026.session.NotLoggedInException;
import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FiguritasBotRoutingTest {

	private CommandDispatcher dispatcher;
	private ConversationalAgent agent;
	private SessionStore sessionStore;
	private FiguritasBot bot;

	@BeforeEach
	void setUp() {
		dispatcher = mock(CommandDispatcher.class);
		agent = mock(ConversationalAgent.class);
		sessionStore = mock(SessionStore.class);
		bot = new FiguritasBot("dummy-token", dispatcher, agent, sessionStore);
	}

	@Test
	void textoConBarraVaAlDispatcher() {
		when(dispatcher.dispatch(5L, "/coleccion")).thenReturn("tu colección");

		String respuesta = bot.responder(5L, "/coleccion");

		assertThat(respuesta).isEqualTo("tu colección");
		verify(dispatcher).dispatch(5L, "/coleccion");
		verifyNoInteractions(agent);
	}

	@Test
	void textoLibreVaAlAgenteConLaSesion() {
		Session session = new Session("user-1", "token-abc");
		when(sessionStore.get(5L)).thenReturn(Optional.of(session));
		when(agent.chat(5L, "¿qué me falta?", session)).thenReturn("te faltan...");

		String respuesta = bot.responder(5L, "¿qué me falta?");

		assertThat(respuesta).isEqualTo("te faltan...");
		verify(agent).chat(5L, "¿qué me falta?", session);
		verifyNoInteractions(dispatcher);
	}

	@Test
	void textoLibreSinSesionPasaNullAlAgente() {
		when(sessionStore.get(5L)).thenReturn(Optional.empty());
		when(agent.chat(5L, "hola", null)).thenReturn("pedí /login");

		String respuesta = bot.responder(5L, "hola");

		assertThat(respuesta).isEqualTo("pedí /login");
		verify(agent).chat(5L, "hola", null);
	}

	@Test
	void siElAgenteFallaDevuelveMensajeAmable() {
		when(sessionStore.get(5L)).thenReturn(Optional.empty());
		when(agent.chat(anyLong(), anyString(), any())).thenThrow(new RuntimeException("gemini caído"));

		String respuesta = bot.responder(5L, "hola");

		assertThat(respuesta).contains("/help");
	}

	@Test
	void sinLoginDevuelveMensajeDeLogin() {
		when(sessionStore.get(5L)).thenReturn(Optional.empty());
		when(agent.chat(anyLong(), anyString(), any()))
				.thenThrow(new NotLoggedInException("sin sesión"));

		String respuesta = bot.responder(5L, "mostrame mi colección");

		assertThat(respuesta).contains("/login");
	}

	@Test
	void un401LimpiaLaSesionYPideRelogin() {
		when(sessionStore.get(5L)).thenReturn(Optional.of(new Session("user-1", "token-abc")));
		when(agent.chat(anyLong(), anyString(), any()))
				.thenThrow(new BackendApiException(401, "unauthorized", "token vencido"));

		String respuesta = bot.responder(5L, "mi colección");

		verify(sessionStore).remove(5L);
		assertThat(respuesta).contains("/login");
	}

	@Test
	void unErrorDeBackendNo401DevuelveMensajeGenerico() {
		when(sessionStore.get(5L)).thenReturn(Optional.of(new Session("user-1", "token-abc")));
		when(agent.chat(anyLong(), anyString(), any()))
				.thenThrow(new BackendApiException(500, "error", "boom"));

		String respuesta = bot.responder(5L, "mi colección");

		assertThat(respuesta).contains("/help");
	}
}
