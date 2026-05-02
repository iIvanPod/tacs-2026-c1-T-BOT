package com.tacs.tp1c2026.client;

import com.tacs.tp1c2026.client.wire.FiguritaColeccionWire;
import com.tacs.tp1c2026.client.wire.FiguritaFaltanteWire;
import com.tacs.tp1c2026.client.wire.FiguritaWire;
import com.tacs.tp1c2026.client.wire.UsuarioWire;
import com.tacs.tp1c2026.dtos.Figurita;
import com.tacs.tp1c2026.dtos.FiguritaColeccion;
import com.tacs.tp1c2026.dtos.FiguritaFaltante;
import com.tacs.tp1c2026.dtos.Usuario;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class BackendApiClient {

    private static final ParameterizedTypeReference<List<FiguritaWire>> FIGURITA_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<UsuarioWire>> USUARIO_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<FiguritaColeccionWire>> COLECCION_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<FiguritaFaltanteWire>> FALTANTE_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public BackendApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Figurita> getCatalog() {
        List<FiguritaWire> wire = restClient.get()
                .uri("/figuritas/catalog")
                .retrieve()
                .body(FIGURITA_LIST);
        return wire.stream().map(BackendDataMapper::toFigurita).toList();
    }

    public Figurita getFiguritaById(String id) {
        FiguritaWire wire = restClient.get()
                .uri("/figuritas/catalog/{id}", id)
                .retrieve()
                .body(FiguritaWire.class);
        return BackendDataMapper.toFigurita(wire);
    }

    public List<Usuario> getUsers() {
        List<UsuarioWire> wire = restClient.get()
                .uri("/users")
                .retrieve()
                .body(USUARIO_LIST);
        return wire.stream().map(BackendDataMapper::toUsuario).toList();
    }

    public Usuario getUserById(String id) {
        UsuarioWire wire = restClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .body(UsuarioWire.class);
        return BackendDataMapper.toUsuario(wire);
    }

    public List<FiguritaColeccion> getCollection(String userId) {
        List<FiguritaColeccionWire> wire = restClient.get()
                .uri("/users/{id}/collection", userId)
                .retrieve()
                .body(COLECCION_LIST);
        return wire.stream().map(BackendDataMapper::toFiguritaColeccion).toList();
    }

    public FiguritaColeccion addToCollection(String userId, String figuritaId) {
        FiguritaColeccionWire wire = restClient.post()
                .uri("/users/{id}/collection", userId)
                .body(Map.of("figuritaId", figuritaId))
                .retrieve()
                .body(FiguritaColeccionWire.class);
        return BackendDataMapper.toFiguritaColeccion(wire);
    }

    public void decrementFromCollection(String userId, String figuritaId) {
        restClient.patch()
                .uri("/users/{id}/collection/{figuritaId}", userId, figuritaId)
                .retrieve()
                .toBodilessEntity();
    }

    public List<FiguritaFaltante> getMissingCards(String userId) {
        List<FiguritaFaltanteWire> wire = restClient.get()
                .uri("/users/{id}/missing-cards", userId)
                .retrieve()
                .body(FALTANTE_LIST);
        return wire.stream().map(BackendDataMapper::toFiguritaFaltante).toList();
    }

    public FiguritaFaltante addMissingCard(String userId, String figuritaId) {
        FiguritaFaltanteWire wire = restClient.post()
                .uri("/users/{id}/missing-cards", userId)
                .body(Map.of("figuritaId", figuritaId))
                .retrieve()
                .body(FiguritaFaltanteWire.class);
        return BackendDataMapper.toFiguritaFaltante(wire);
    }
}
