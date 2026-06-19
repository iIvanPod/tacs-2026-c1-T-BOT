package com.tacs.tp1c2026.tma;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Valida el {@code initData} que Telegram inyecta en una Mini App, siguiendo el algoritmo
 * oficial: <a href="https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app">
 * Validating data received via the Mini App</a>.
 */
@Component
public class InitDataValidator {

    private static final Duration MAX_AGE = Duration.ofMinutes(15); // ventana anti-stale/replay

    private final byte[] secretKey;
    private final ObjectMapper objectMapper;

    public InitDataValidator(@Value("${telegram.bot.token}") String botToken,
                             ObjectMapper objectMapper) {
        // secret_key = HMAC_SHA256(key = "WebAppData", msg = botToken)
        this.secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                              botToken.getBytes(StandardCharsets.UTF_8));
        this.objectMapper = objectMapper;
    }

    /** Valida firma y frescura del initData. Devuelve el usuario de Telegram o tira excepción. */
    public TelegramUser validate(String initData) {
        if (initData == null || initData.isBlank()) {
            throw new InvalidInitDataException("initData vacío");
        }

        Map<String, String> fields = new TreeMap<>(); // TreeMap => orden alfabético por clave
        String hash = null;
        for (String pair : initData.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = pair.substring(0, eq);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            if ("hash".equals(key)) {
                hash = value; // se excluye SOLO 'hash' del data_check_string
            } else {
                fields.put(key, value);
            }
        }
        if (hash == null) {
            throw new InvalidInitDataException("falta el campo hash");
        }

        String dataCheckString = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        String expected = HexFormat.of().formatHex(
                hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                   hash.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidInitDataException("firma inválida");
        }

        String authDate = fields.get("auth_date");
        if (authDate == null) {
            throw new InvalidInitDataException("falta auth_date");
        }
        Instant when;
        try {
            when = Instant.ofEpochSecond(Long.parseLong(authDate));
        } catch (NumberFormatException e) {
            throw new InvalidInitDataException("auth_date inválido");
        }
        if (when.plus(MAX_AGE).isBefore(Instant.now())) {
            throw new InvalidInitDataException("initData expirado");
        }

        return new TelegramUser(extractUserId(fields.get("user")));
    }

    private long extractUserId(String userJson) {
        if (userJson == null) {
            throw new InvalidInitDataException("falta el usuario en initData");
        }
        JsonNode idNode;
        try {
            idNode = objectMapper.readTree(userJson).get("id");
        } catch (Exception e) {
            throw new InvalidInitDataException("usuario inválido en initData");
        }
        if (idNode == null || !idNode.canConvertToLong()) {
            throw new InvalidInitDataException("usuario sin id válido en initData");
        }
        return idNode.asLong();
    }

    private static byte[] hmac(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (Exception e) {
            throw new IllegalStateException("No pude calcular HMAC-SHA256", e);
        }
    }
}
