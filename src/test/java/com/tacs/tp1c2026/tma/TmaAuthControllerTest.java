package com.tacs.tp1c2026.tma;

import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import com.tacs.tp1c2026.tma.TmaAuthController.LinkRequest;
import com.tacs.tp1c2026.tma.TmaAuthController.VerifyRequest;
import com.tacs.tp1c2026.tma.TmaAuthController.VerifyResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TmaAuthControllerTest {

	private final InitDataValidator validator = mock(InitDataValidator.class);
	private final SessionStore sessionStore = mock(SessionStore.class);
	private final TmaAuthController controller = new TmaAuthController(validator, sessionStore);

	@Test
	void verifyConSesionVinculadaDevuelveElToken() {
		when(validator.validate("ok")).thenReturn(new TelegramUser(42L));
		when(sessionStore.get(42L)).thenReturn(Optional.of(new Session("user-1", "jwt-1")));

		VerifyResponse res = controller.verify(new VerifyRequest("ok"));

		assertThat(res.linked()).isTrue();
		assertThat(res.token()).isEqualTo("jwt-1");
		assertThat(res.userId()).isEqualTo("user-1");
	}

	@Test
	void verifySinSesionDevuelveNoVinculado() {
		when(validator.validate("ok")).thenReturn(new TelegramUser(42L));
		when(sessionStore.get(42L)).thenReturn(Optional.empty());

		VerifyResponse res = controller.verify(new VerifyRequest("ok"));

		assertThat(res.linked()).isFalse();
		assertThat(res.token()).isNull();
		assertThat(res.userId()).isNull();
	}

	@Test
	void linkGuardaLaSesionBajoElUserIdDeTelegram() {
		when(validator.validate("ok")).thenReturn(new TelegramUser(42L));

		controller.link(new LinkRequest("ok", "jwt-1", "user-1"));

		verify(sessionStore).save(42L, new Session("user-1", "jwt-1"));
	}

	@Test
	void initDataInvalidoPropagaLaExcepcion() {
		when(validator.validate(any())).thenThrow(new InvalidInitDataException("firma inválida"));

		assertThatThrownBy(() -> controller.verify(new VerifyRequest("malo")))
				.isInstanceOf(InvalidInitDataException.class);
	}

	@Test
	void exceptionHandlerDevuelveElShapeDeError() {
		var body = controller.onInvalid(new InvalidInitDataException("firma inválida"));

		assertThat(body)
				.containsEntry("error", "invalid_init_data")
				.containsEntry("message", "firma inválida");
	}
}
