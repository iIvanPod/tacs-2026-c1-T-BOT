package com.tacs.tp1c2026.client;

import com.tacs.tp1c2026.client.wire.CardWire;
import com.tacs.tp1c2026.client.wire.CollectionCardWire;
import com.tacs.tp1c2026.client.wire.LoginResponseWire;
import com.tacs.tp1c2026.client.wire.MissingCardWire;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.dtos.MissingCard;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class BackendApiClient {

    private static final ParameterizedTypeReference<List<CardWire>> CARD_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CollectionCardWire>> COLLECTION_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<MissingCardWire>> MISSING_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public BackendApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public LoginResult login(String email, String password) {
        LoginResponseWire wire = restClient.post()
                .uri("/auth/login")
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(LoginResponseWire.class);
        return new LoginResult(wire.token(), wire.user().id(), wire.user().name());
    }

    public List<Card> getCatalog(String token) {
        List<CardWire> wire = restClient.get()
                .uri("/cards/catalog")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(CARD_LIST);
        return wire.stream().map(BackendDataMapper::toCard).toList();
    }

    public Card getCardById(String id, String token) {
        CardWire wire = restClient.get()
                .uri("/cards/catalog/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(CardWire.class);
        return BackendDataMapper.toCard(wire);
    }

    public List<CollectionCard> getCollection(String userId, String token) {
        List<CollectionCardWire> wire = restClient.get()
                .uri("/users/{id}/collection", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(COLLECTION_LIST);
        return wire.stream().map(BackendDataMapper::toCollectionCard).toList();
    }

    public CollectionCard addToCollection(String userId, String cardId, String token) {
        CollectionCardWire wire = restClient.post()
                .uri("/users/{id}/collection", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(Map.of("cardId", cardId))
                .retrieve()
                .body(CollectionCardWire.class);
        return BackendDataMapper.toCollectionCard(wire);
    }

    public void decrementFromCollection(String userId, String cardId, String token) {
        restClient.patch()
                .uri("/users/{id}/collection/{cardId}", userId, cardId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .toBodilessEntity();
    }

    public List<MissingCard> getMissingCards(String userId, String token) {
        List<MissingCardWire> wire = restClient.get()
                .uri("/users/{id}/missing-cards", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(MISSING_LIST);
        return wire.stream().map(BackendDataMapper::toMissingCard).toList();
    }

    public MissingCard addMissingCard(String userId, String cardId, String token) {
        MissingCardWire wire = restClient.post()
                .uri("/users/{id}/missing-cards", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(Map.of("cardId", cardId))
                .retrieve()
                .body(MissingCardWire.class);
        return BackendDataMapper.toMissingCard(wire);
    }

    public void removeMissingCard(String userId, String cardId, String token) {
        restClient.delete()
                .uri("/users/{id}/missing-cards/{cardId}", userId, cardId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .toBodilessEntity();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    public record LoginResult(String token, String userId, String name) {}
}
