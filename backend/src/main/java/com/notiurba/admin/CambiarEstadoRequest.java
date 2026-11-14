package com.notiurba.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoRequest(
        @NotNull EstadoReporte estado,
        @Size(max = 1000) String observacion) {
}
