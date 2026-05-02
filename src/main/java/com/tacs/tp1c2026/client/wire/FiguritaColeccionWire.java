package com.tacs.tp1c2026.client.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FiguritaColeccionWire(
        String figuritaId,
        Integer number,
        String description,
        String country,
        String team,
        String category,
        Integer quantity
) {}
