package com.tacs.tp1c2026.client;

import java.util.Set;

public class BackendApiException extends RuntimeException {

    private static final Set<Integer> EXPECTED_CLIENT_ERRORS = Set.of(400, 404, 409);

    private final int status;

    public BackendApiException(int status, String backendError, String backendMessage) {
        super(buildMessage(status, backendError, backendMessage));
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public boolean isExpectedClientError() {
        return EXPECTED_CLIENT_ERRORS.contains(status);
    }

    private static String buildMessage(int status, String backendError, String backendMessage) {
        if (backendMessage != null && !backendMessage.isBlank()) {
            return backendMessage;
        }
        if (backendError != null && !backendError.isBlank()) {
            return backendError;
        }
        return "HTTP " + status;
    }
}
