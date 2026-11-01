package com.notiurba.reporte;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Campos en el orden de los pasos del formulario (así se elige el primer error que se devuelve). */
public record CrearReporteRequest(
        @NotNull Integer idCategoria,
        @DecimalMin("-90") @DecimalMax("90") Double latitud,
        @DecimalMin("-180") @DecimalMax("180") Double longitud,
        Integer idBarrio,
        @NotBlank String descripcion,
        @Size(max = 300) String direccionIncidente,
        @Size(max = 50) String matriculaCuentaServicio,
        @Size(max = 50) String radicadoOficial,
        @NotNull @AssertTrue Boolean autorizaEnvio) {
}
