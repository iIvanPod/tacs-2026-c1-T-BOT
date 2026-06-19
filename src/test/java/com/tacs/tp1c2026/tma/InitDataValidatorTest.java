package com.tacs.tp1c2026.tma;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitDataValidatorTest {

	private static final String BOT_TOKEN = "123456:test-bot-token";

	private final InitDataValidator validator = new InitDataValidator(BOT_TOKEN, new ObjectMapper());

	@Test
	void initDataValidoDevuelveElUsuario() {
		String initData = firmarInitData(BOT_TOKEN, Map.of(
				"auth_date", ahora(),
				"query_id", "AAEx",
				"user", "{\"id\":987654321,\"first_name\":\"Guido\"}"));

		TelegramUser user = validator.validate(initData);

		assertThat(user.id()).isEqualTo(987654321L);
	}

	@Test
	void hashAdulteradoEsRechazado() {
		String valido = firmarInitData(BOT_TOKEN, Map.of(
				"auth_date", ahora(),
				"user", "{\"id\":1}"));
		String adulterado = valido.replaceAll("hash=[0-9a-f]+", "hash=deadbeef");

		assertThatThrownBy(() -> validator.validate(adulterado))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("firma inválida");
	}

	@Test
	void firmadoConOtroTokenEsRechazado() {
		String initData = firmarInitData("999:otro-token", Map.of(
				"auth_date", ahora(),
				"user", "{\"id\":1}"));

		assertThatThrownBy(() -> validator.validate(initData))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("firma inválida");
	}

	@Test
	void authDateViejoEsRechazado() {
		String initData = firmarInitData(BOT_TOKEN, Map.of(
				"auth_date", String.valueOf(Instant.now().minusSeconds(3600).getEpochSecond()),
				"user", "{\"id\":1}"));

		assertThatThrownBy(() -> validator.validate(initData))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("expirado");
	}

	@Test
	void sinHashEsRechazado() {
		assertThatThrownBy(() -> validator.validate("auth_date=123&user=%7B%7D"))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("hash");
	}

	@Test
	void initDataVacioEsRechazado() {
		assertThatThrownBy(() -> validator.validate(""))
				.isInstanceOf(InvalidInitDataException.class);
	}

	@Test
	void sinUserEsRechazado() {
		String initData = firmarInitData(BOT_TOKEN, Map.of("auth_date", ahora()));

		assertThatThrownBy(() -> validator.validate(initData))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("usuario");
	}

	@Test
	void userSinIdNumericoEsRechazado() {
		String initData = firmarInitData(BOT_TOKEN, Map.of(
				"auth_date", ahora(),
				"user", "{\"id\":\"no-numerico\"}"));

		assertThatThrownBy(() -> validator.validate(initData))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("id");
	}

	@Test
	void sinAuthDateEsRechazado() {
		String initData = firmarInitData(BOT_TOKEN, Map.of("user", "{\"id\":1}"));

		assertThatThrownBy(() -> validator.validate(initData))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("auth_date");
	}

	@Test
	void authDateNoNumericoEsRechazado() {
		String initData = firmarInitData(BOT_TOKEN, Map.of(
				"auth_date", "no-es-un-numero",
				"user", "{\"id\":1}"));

		assertThatThrownBy(() -> validator.validate(initData))
				.isInstanceOf(InvalidInitDataException.class)
				.hasMessageContaining("auth_date");
	}

	private static String ahora() {
		return String.valueOf(Instant.now().getEpochSecond());
	}

	/** Construye un initData firmado igual que lo haría Telegram (valores URL-encodeados). */
	private static String firmarInitData(String botToken, Map<String, String> campos) {
		TreeMap<String, String> ordenado = new TreeMap<>(campos);
		String dataCheckString = ordenado.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining("\n"));
		byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
				botToken.getBytes(StandardCharsets.UTF_8));
		String hash = HexFormat.of().formatHex(
				hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

		String query = ordenado.entrySet().stream()
				.map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
				.collect(Collectors.joining("&"));
		return query + "&hash=" + hash;
	}

	private static byte[] hmac(byte[] key, byte[] message) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac.doFinal(message);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
