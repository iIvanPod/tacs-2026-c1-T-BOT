package com.tacs.tp1c2026.client.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TradePublicationWire(
        Integer cardNumber,
        String cardDescription,
        Integer initialCount,
        Integer remainingCount,
        String status,
        String publisherName
) {}
