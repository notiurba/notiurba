package com.notiurba.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoRequest(
        @NotNull EstadoReporte estado,
        @NotBlank @Size(max = 1000) String observacion) {
}
