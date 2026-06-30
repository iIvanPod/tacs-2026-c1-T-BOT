package com.tacs.tp1c2026.client;

import com.tacs.tp1c2026.client.wire.AuctionWire;
import com.tacs.tp1c2026.client.wire.CardWire;
import com.tacs.tp1c2026.client.wire.CollectionCardWire;
import com.tacs.tp1c2026.client.wire.MissingCardWire;
import com.tacs.tp1c2026.client.wire.NotificationWire;
import com.tacs.tp1c2026.client.wire.TradePublicationWire;
import com.tacs.tp1c2026.dtos.Auction;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.dtos.Notification;
import com.tacs.tp1c2026.dtos.TradePublication;

public final class BackendDataMapper {

    private BackendDataMapper() {}

    public static Card toCard(CardWire w) {
        return new Card(
                w.id(),
                w.number(),
                w.type(),
                w.description(),
                w.country(),
                w.team(),
                w.category()
        );
    }

    public static CollectionCard toCollectionCard(CollectionCardWire w) {
        return new CollectionCard(w.cardId(), w.number(), w.description(), w.quantity());
    }

    public static MissingCard toMissingCard(MissingCardWire w) {
        return new MissingCard(w.cardId(), w.number(), w.description());
    }

    public static TradePublication toTradePublication(TradePublicationWire w) {
        return new TradePublication(
                w.cardNumber(),
                w.cardDescription(),
                w.initialCount(),
                w.remainingCount(),
                w.status(),
                w.publisherName()
        );
    }

    public static Auction toAuction(AuctionWire w) {
        Auction.BestOffer bestOffer = null;
        if (w.bestOffer() != null) {
            int cardCount = w.bestOffer().cards() != null ? w.bestOffer().cards().size() : 0;
            bestOffer = new Auction.BestOffer(w.bestOffer().username(), cardCount);
        }
        return new Auction(
                w.cardNumber(),
                w.cardDescription(),
                w.closeDate(),
                w.publisherName(),
                w.publisherRating(),
                bestOffer
        );
    }

    public static Notification toNotification(NotificationWire w) {
        return new Notification(w.id(), w.message(), w.type(), w.referenceId());
    }
}
