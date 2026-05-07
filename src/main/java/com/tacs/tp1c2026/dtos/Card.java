package com.tacs.tp1c2026.dtos;

public record Card(
        String id,
        Integer number,
        String type,
        String description,
        String country,
        String team,
        String category
) {}
