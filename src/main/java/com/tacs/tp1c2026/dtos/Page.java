package com.tacs.tp1c2026.dtos;

import java.util.List;

/** Página de resultados del backend: los ítems más la posición dentro de la paginación. */
public record Page<T>(
        List<T> items,
        int currentPage,
        int totalPages
) {}
