package com.notiurba.correo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.notiurba.reporte.BorradorDeReporte;

/** El borrador del reporte, sin fotos (solo cuántas) y sin autorización: nada de esto se guarda. */
public record VistaPreviaRequest(
        @NotNull Integer idCategoria,
        @DecimalMin("-90") @DecimalMax("90") Double latitud,
        @DecimalMin("-180") @DecimalMax("180") Double longitud,
        Integer idBarrio,
        @NotBlank String descripcion,
        @Size(max = 300) String direccionIncidente,
        @Size(max = 50) String matriculaCuentaServicio,
        @Min(0) @Max(3) Integer totalFotos,
        Boolean emergencia) implements BorradorDeReporte {
}
