package com.notiurba.reporte;

/** Respuesta de POST /api/reportes/{codigo}/fotos: la foto quedó con ese orden y ese enlace público. */
public record FotoSubidaResponse(String codigoSeguimiento, ReporteDetalleResponse.Foto foto) {
}
