package com.tacs.tp1c2026.tools;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.client.BackendApiException;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.session.NotLoggedInException;
import com.tacs.tp1c2026.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FiguritasToolsTest {

	private static final Session SESSION = new Session("user-1", "token-abc");

	private BackendApiClient apiClient;
	private FiguritasTools tools;

	@BeforeEach
	void setUp() {
		apiClient = mock(BackendApiClient.class);
		tools = new FiguritasTools(apiClient);
	}

	private static ToolContext withSession() {
		return new ToolContext(Map.of(FiguritasTools.SESSION_KEY, SESSION));
	}

	private static ToolContext withoutSession() {
		return new ToolContext(Map.of());
	}

	@Test
	void verColeccionConSesionDevuelveLaColeccion() {
		List<CollectionCard> esperada = List.of(new CollectionCard("c1", 1, "Messi", 2));
		when(apiClient.getCollection("user-1", "token-abc")).thenReturn(esperada);

		List<CollectionCard> resultado = tools.verColeccion(withSession());

		assertThat(resultado).isEqualTo(esperada);
		verify(apiClient).getCollection("user-1", "token-abc");
	}

	@Test
	void verFaltantesUsaUserIdYTokenDeLaSesion() {
		when(apiClient.getMissingCards("user-1", "token-abc"))
				.thenReturn(List.of(new MissingCard("c9", 9, "Di María")));

		List<MissingCard> resultado = tools.verFaltantes(withSession());

		assertThat(resultado).hasSize(1);
		verify(apiClient).getMissingCards("user-1", "token-abc");
	}

	@Test
	void agregarAColeccionDelegaEnElBackend() {
		CollectionCard actualizada = new CollectionCard("c1", 1, "Messi", 3);
		when(apiClient.addToCollection("user-1", "c1", "token-abc")).thenReturn(actualizada);

		CollectionCard resultado = tools.agregarAColeccion("c1", withSession());

		assertThat(resultado.quantity()).isEqualTo(3);
		verify(apiClient).addToCollection("user-1", "c1", "token-abc");
	}

	@Test
	void quitarDeColeccionInvocaElBackendYConfirma() {
		String resultado = tools.quitarDeColeccion("c1", withSession());

		verify(apiClient).decrementFromCollection("user-1", "c1", "token-abc");
		assertThat(resultado).contains("c1");
	}

	@Test
	void buscarEnCatalogoFiltraPorEquipoIgnorandoMayusculas() {
		when(apiClient.getCatalog("token-abc")).thenReturn(List.of(
				new Card("c1", 1, "base", "Messi", "Argentina", "Argentina", "Delantero"),
				new Card("c2", 2, "base", "Neymar", "Brasil", "Brasil", "Delantero")));

		List<Card> resultado = tools.buscarEnCatalogo("argentina", null, null, withSession());

		assertThat(resultado).extracting(Card::team).containsExactly("Argentina");
	}

	@Test
	void buscarEnCatalogoSinFiltrosDevuelveTodoElCatalogo() {
		when(apiClient.getCatalog("token-abc")).thenReturn(List.of(
				new Card("c1", 1, "base", "Messi", "Argentina", "Argentina", "Delantero")));

		List<Card> resultado = tools.buscarEnCatalogo(null, null, null, withSession());

		assertThat(resultado).hasSize(1);
	}

	@Test
	void sinSesionLanzaNotLoggedIn() {
		assertThatThrownBy(() -> tools.verColeccion(withoutSession()))
				.isInstanceOf(NotLoggedInException.class);
	}

	@Test
	void propagaErroresDelBackend() {
		when(apiClient.getCollection(any(), any()))
				.thenThrow(new BackendApiException(500, "error", "boom"));

		assertThatThrownBy(() -> tools.verColeccion(withSession()))
				.isInstanceOf(BackendApiException.class);
	}
}
