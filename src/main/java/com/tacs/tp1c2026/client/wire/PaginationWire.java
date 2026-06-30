package com.tacs.tp1c2026.client.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Envelope de los GET paginados del backend: {@code { data, currentPage, totalPages }}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaginationWire<T>(
        List<T> data,
        Integer currentPage,
        Integer totalPages
) {}
