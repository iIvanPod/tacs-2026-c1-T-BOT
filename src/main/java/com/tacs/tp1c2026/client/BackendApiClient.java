package com.tacs.tp1c2026.client;

import com.tacs.tp1c2026.client.wire.ApiResponseWire;
import com.tacs.tp1c2026.client.wire.AuctionWire;
import com.tacs.tp1c2026.client.wire.CardWire;
import com.tacs.tp1c2026.client.wire.CollectionCardWire;
import com.tacs.tp1c2026.client.wire.MissingCardWire;
import com.tacs.tp1c2026.client.wire.NotificationWire;
import com.tacs.tp1c2026.client.wire.PaginationWire;
import com.tacs.tp1c2026.client.wire.TradePublicationWire;
import com.tacs.tp1c2026.dtos.Auction;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.dtos.Notification;
import com.tacs.tp1c2026.dtos.Page;
import com.tacs.tp1c2026.dtos.TradePublication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class BackendApiClient {

    private static final ParameterizedTypeReference<List<CardWire>> CARD_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CollectionCardWire>> COLLECTION_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<MissingCardWire>> MISSING_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponseWire<TradePublicationWire>> PUBLICATION_RESPONSE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginationWire<TradePublicationWire>> PUBLICATION_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginationWire<AuctionWire>> AUCTION_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginationWire<NotificationWire>> NOTIFICATION_PAGE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public BackendApiClient(RestClient restClient) {
        this.restClient = restClient;
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

    public TradePublication createTradePublication(String cardId, int quantity, String token) {
        ApiResponseWire<TradePublicationWire> wire = restClient.post()
                .uri("/publications")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .body(Map.of("cardId", cardId, "quantity", quantity))
                .retrieve()
                .body(PUBLICATION_RESPONSE);
        return BackendDataMapper.toTradePublication(wire.data());
    }

    public Page<TradePublication> listPublications(int page, int perPage, String token) {
        PaginationWire<TradePublicationWire> wire = restClient.get()
                .uri("/publications?page={page}&per_page={perPage}", page, perPage)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(PUBLICATION_PAGE);
        return toPage(wire, BackendDataMapper::toTradePublication);
    }

    public Page<Auction> listAuctions(int page, int perPage, String token) {
        PaginationWire<AuctionWire> wire = restClient.get()
                .uri("/auctions?page={page}&per_page={perPage}", page, perPage)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(AUCTION_PAGE);
        return toPage(wire, BackendDataMapper::toAuction);
    }

    public List<Notification> getUnreadNotifications(String userId, String token) {
        PaginationWire<NotificationWire> wire = restClient.get()
                .uri("/users/{id}/notifications?status=UNREAD&page=1&per_page=50", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(NOTIFICATION_PAGE);
        return wire.data().stream().map(BackendDataMapper::toNotification).toList();
    }

    public void markNotificationRead(String userId, String notificationId, String token) {
        restClient.put()
                .uri("/users/{id}/notifications/{notificationId}/read", userId, notificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .toBodilessEntity();
    }

    private static <W, D> Page<D> toPage(PaginationWire<W> wire, Function<W, D> mapper) {
        List<D> items = wire.data().stream().map(mapper).toList();
        int currentPage = wire.currentPage() != null ? wire.currentPage() : 1;
        int totalPages = wire.totalPages() != null ? wire.totalPages() : 1;
        return new Page<>(items, currentPage, totalPages);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
