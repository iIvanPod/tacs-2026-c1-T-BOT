package com.tacs.tp1c2026.dtos;

public record Notification(
        String id,
        String message,
        String type,
        String referenceId
) {}
