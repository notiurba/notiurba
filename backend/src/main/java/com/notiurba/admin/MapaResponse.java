package com.notiurba.admin;

import java.time.OffsetDateTime;
import java.util.List;

import com.notiurba.reporte.ReporteDetalleResponse;

/**
 * GET /api/admin/mapa (RF-22): reportes vigentes con ubicación, los más recientes primero.
 *
 * @param total     cuántos reportes cumplen los filtros (puede ser mayor que {@code reportes.size()})
 * @param sinComuna cuántos de ellos no tienen comuna determinada
 * @param truncado  {@code true} si se devolvió solo una parte por el tope de la consulta
 */
public record MapaResponse(long total, long sinComuna, boolean truncado, List<Marcador> reportes) {

    /** Un punto del mapa. Sin datos del ciudadano ni descripción (Ley 1581). */
    public record Marcador(String codigoSeguimiento, String estado, ReporteDetalleResponse.Categoria categoria,
            ReporteDetalleResponse.Barrio barrio, Integer comuna, double latitud, double longitud,
            OffsetDateTime creadoEn, OffsetDateTime vigenteHasta) {
    }
}
