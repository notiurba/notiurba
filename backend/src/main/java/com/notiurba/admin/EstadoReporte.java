package com.notiurba.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

/** Identificadores de API y nombres del catálogo existente, sin depender de sus IDs. */
public enum EstadoReporte {
    RECIBIDO("Recibido"), ENVIADO("Enviado"), EN_TRAMITE("En trámite"), RESUELTO("Resuelto");

    private final String nombre;

    EstadoReporte(String nombre) { this.nombre = nombre; }
    public String nombre() { return nombre; }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EstadoReporte desdeJson(JsonNode valor) {
        if (valor == null || !valor.isTextual()) {
            throw new IllegalArgumentException("El estado debe ser RECIBIDO, ENVIADO, EN_TRAMITE o RESUELTO");
        }
        return valueOf(valor.textValue());
    }
}
