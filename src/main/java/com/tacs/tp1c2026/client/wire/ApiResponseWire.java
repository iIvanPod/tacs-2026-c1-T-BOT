package com.tacs.tp1c2026.client.wire;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Envelope de respuestas POST/PUT del backend: {@code { timestamp, message, data }}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiResponseWire<T>(T data) {}
