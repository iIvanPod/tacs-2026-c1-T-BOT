package com.tacs.tp1c2026.client;

import com.tacs.tp1c2026.client.wire.FiguritaColeccionWire;
import com.tacs.tp1c2026.client.wire.FiguritaFaltanteWire;
import com.tacs.tp1c2026.client.wire.FiguritaWire;
import com.tacs.tp1c2026.client.wire.UsuarioWire;
import com.tacs.tp1c2026.dtos.Figurita;
import com.tacs.tp1c2026.dtos.FiguritaColeccion;
import com.tacs.tp1c2026.dtos.FiguritaFaltante;
import com.tacs.tp1c2026.dtos.Usuario;

public final class BackendDataMapper {

    private BackendDataMapper() {}

    public static Figurita toFigurita(FiguritaWire w) {
        return new Figurita(
                w.id(),
                w.description(),
                w.number(),
                w.country(),
                null
        );
    }

    public static Usuario toUsuario(UsuarioWire w) {
        return new Usuario(w.id(), w.name(), w.email());
    }

    public static FiguritaColeccion toFiguritaColeccion(FiguritaColeccionWire w) {
        return new FiguritaColeccion(w.figuritaId(), w.number(), w.description(), w.quantity());
    }

    public static FiguritaFaltante toFiguritaFaltante(FiguritaFaltanteWire w) {
        return new FiguritaFaltante(w.figuritaId(), w.number(), w.description());
    }
}
