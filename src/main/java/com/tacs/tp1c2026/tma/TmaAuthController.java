package com.tacs.tp1c2026.tma;

import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.session.SessionStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints que consume la Telegram Mini App. La validación criptográfica de la identidad
 * vive acá porque el bot es el único servicio con el TELEGRAM_BOT_TOKEN.
 */
@RestController
@RequestMapping("/tma")
public class TmaAuthController {

    private final InitDataValidator validator;
    private final SessionStore sessionStore;

    public TmaAuthController(InitDataValidator validator, SessionStore sessionStore) {
        this.validator = validator;
        this.sessionStore = sessionStore;
    }

    /** Al abrir la TMA: si ya hay sesión vinculada para este usuario de Telegram, devuelve su JWT. */
    @PostMapping("/verify")
    public VerifyResponse verify(@RequestBody VerifyRequest body) {
        TelegramUser tg = validator.validate(body.initData());
        return sessionStore.get(tg.id())
                .map(s -> new VerifyResponse(true, s.token(), s.userId()))
                .orElseGet(() -> new VerifyResponse(false, null, null));
    }

    /** Tras un login exitoso contra el backend, el frontend vincula su JWT a este usuario de Telegram. */
    @PostMapping("/link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void link(@RequestBody LinkRequest body) {
        TelegramUser tg = validator.validate(body.initData());
        sessionStore.save(tg.id(), new Session(body.userId(), body.token()));
    }

    @ExceptionHandler(InvalidInitDataException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> onInvalid(InvalidInitDataException e) {
        return Map.of("error", "invalid_init_data", "message", e.getMessage());
    }

    public record VerifyRequest(String initData) {}

    public record VerifyResponse(boolean linked, String token, String userId) {}

    public record LinkRequest(String initData, String token, String userId) {}
}
