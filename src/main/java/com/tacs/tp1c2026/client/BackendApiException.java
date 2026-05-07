package com.tacs.tp1c2026.client;

import java.util.Set;

public class BackendApiException extends RuntimeException {

    private static final Set<Integer> EXPECTED_CLIENT_ERRORS = Set.of(400, 404, 409);

    private final int status;
    private final String backendError;
    private final String backendMessage;

    public BackendApiException(int status, String backendError, String backendMessage) {
        super(buildMessage(status, backendError, backendMessage));
        this.status = status;
        this.backendError = backendError;
        this.backendMessage = backendMessage;
    }

    public int getStatus() {
        return status;
    }

    public String getBackendError() {
        return backendError;
    }

    public String getBackendMessage() {
        return backendMessage;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }

    public boolean isServerError() {
        return status >= 500;
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
