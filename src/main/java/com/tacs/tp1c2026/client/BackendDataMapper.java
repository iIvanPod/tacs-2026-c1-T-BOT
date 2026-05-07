package com.tacs.tp1c2026.client;

import com.tacs.tp1c2026.client.wire.CardWire;
import com.tacs.tp1c2026.client.wire.CollectionCardWire;
import com.tacs.tp1c2026.client.wire.MissingCardWire;
import com.tacs.tp1c2026.client.wire.UserWire;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.dtos.User;

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

    public static User toUser(UserWire w) {
        return new User(w.id(), w.name(), w.email());
    }

    public static CollectionCard toCollectionCard(CollectionCardWire w) {
        return new CollectionCard(w.cardId(), w.number(), w.description(), w.quantity());
    }

    public static MissingCard toMissingCard(MissingCardWire w) {
        return new MissingCard(w.cardId(), w.number(), w.description());
    }
}
