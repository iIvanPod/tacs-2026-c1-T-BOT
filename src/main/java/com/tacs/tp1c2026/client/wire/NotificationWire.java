package com.tacs.tp1c2026.client.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationWire(
        String id,
        String message,
        boolean read,
        String type,
        String referenceId
) {}
