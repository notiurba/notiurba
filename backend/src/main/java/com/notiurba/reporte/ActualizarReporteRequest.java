package com.notiurba.reporte;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * «Actualizar mi reporte» (PATCH /api/reportes/{codigo}/actualizacion).
 *
 * @param tipo     RESPUESTA («Me respondieron») o RESUELTO («Ya se resolvió»)
 * @param radicado número que asignó la entidad; opcional, vacío = no cambia el que ya había
 */
public record ActualizarReporteRequest(
        @NotBlank String tipo,
        @Size(max = 50) String radicado) {
}
