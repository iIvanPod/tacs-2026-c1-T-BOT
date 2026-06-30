package com.tacs.tp1c2026.dtos;

public record TradePublication(
        Integer cardNumber,
        String cardDescription,
        Integer initialCount,
        Integer remainingCount,
        String status,
        String publisherName
) {}
