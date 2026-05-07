package com.tacs.tp1c2026.client;

import com.tacs.tp1c2026.client.wire.CardWire;
import com.tacs.tp1c2026.client.wire.CollectionCardWire;
import com.tacs.tp1c2026.client.wire.MissingCardWire;
import com.tacs.tp1c2026.client.wire.UserWire;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.dtos.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class BackendApiClient {

    private static final ParameterizedTypeReference<List<CardWire>> CARD_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<UserWire>> USER_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CollectionCardWire>> COLLECTION_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<MissingCardWire>> MISSING_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public BackendApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Card> getCatalog() {
        List<CardWire> wire = restClient.get()
                .uri("/cards/catalog")
                .retrieve()
                .body(CARD_LIST);
        return wire.stream().map(BackendDataMapper::toCard).toList();
    }

    public Card getCardById(String id) {
        CardWire wire = restClient.get()
                .uri("/cards/catalog/{id}", id)
                .retrieve()
                .body(CardWire.class);
        return BackendDataMapper.toCard(wire);
    }

    public List<User> getUsers() {
        List<UserWire> wire = restClient.get()
                .uri("/users")
                .retrieve()
                .body(USER_LIST);
        return wire.stream().map(BackendDataMapper::toUser).toList();
    }

    public User getUserById(String id) {
        UserWire wire = restClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .body(UserWire.class);
        return BackendDataMapper.toUser(wire);
    }

    public List<CollectionCard> getCollection(String userId) {
        List<CollectionCardWire> wire = restClient.get()
                .uri("/users/{id}/collection", userId)
                .retrieve()
                .body(COLLECTION_LIST);
        return wire.stream().map(BackendDataMapper::toCollectionCard).toList();
    }

    public CollectionCard addToCollection(String userId, String cardId) {
        CollectionCardWire wire = restClient.post()
                .uri("/users/{id}/collection", userId)
                .body(Map.of("cardId", cardId))
                .retrieve()
                .body(CollectionCardWire.class);
        return BackendDataMapper.toCollectionCard(wire);
    }

    public void decrementFromCollection(String userId, String cardId) {
        restClient.patch()
                .uri("/users/{id}/collection/{cardId}", userId, cardId)
                .retrieve()
                .toBodilessEntity();
    }

    public List<MissingCard> getMissingCards(String userId) {
        List<MissingCardWire> wire = restClient.get()
                .uri("/users/{id}/missing-cards", userId)
                .retrieve()
                .body(MISSING_LIST);
        return wire.stream().map(BackendDataMapper::toMissingCard).toList();
    }

    public MissingCard addMissingCard(String userId, String cardId) {
        MissingCardWire wire = restClient.post()
                .uri("/users/{id}/missing-cards", userId)
                .body(Map.of("cardId", cardId))
                .retrieve()
                .body(MissingCardWire.class);
        return BackendDataMapper.toMissingCard(wire);
    }
}
