package com.notiurba.reporte;

import java.time.OffsetDateTime;

/** Una fila de «Mis reportes» (GET /api/reportes). El reporte completo está en GET /api/reportes/{codigo}. */
public record ReporteResumenResponse(
        String codigoSeguimiento,
        String estado,
        ReporteDetalleResponse.Categoria categoria,
        String direccionIncidente,
        ReporteDetalleResponse.Barrio barrio,
        OffsetDateTime creadoEn,
        OffsetDateTime vigenteHasta,
        ReporteDetalleResponse.Envio envio) {
}
