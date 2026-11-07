package com.notiurba.admin;

import java.time.OffsetDateTime;

public record CambioEstadoResponse(String codigoSeguimiento, EstadoReporte estado, String observacion,
        OffsetDateTime cambiadoEn, Long cambiadoPor, OffsetDateTime vigenteHasta) {
}
