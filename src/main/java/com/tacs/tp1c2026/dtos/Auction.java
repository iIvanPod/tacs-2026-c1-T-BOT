package com.tacs.tp1c2026.dtos;

public record Auction(
        Integer cardNumber,
        String cardDescription,
        String closeDate,
        String publisherName,
        Double publisherRating,
        BestOffer bestOffer
) {
    public record BestOffer(String username, int cardCount) {}
}
