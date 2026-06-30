package com.tacs.tp1c2026.client.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuctionWire(
        Integer cardNumber,
        String cardDescription,
        String closeDate,
        BestOfferWire bestOffer,
        String publisherName,
        Double publisherRating
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BestOfferWire(String username, List<Object> cards) {}
}
