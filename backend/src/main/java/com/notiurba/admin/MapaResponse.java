package com.notiurba.admin;

import java.time.OffsetDateTime;
import java.util.List;

import com.notiurba.reporte.ReporteDetalleResponse;

/** {@code total} cuenta todos los reportes del filtro; {@code truncado} indica que la lista se cortó en el tope. */
public record MapaResponse(long total, long sinComuna, boolean truncado, List<Marcador> reportes) {

    /** Sin datos del ciudadano ni descripción. */
    public record Marcador(String codigoSeguimiento, String estado, ReporteDetalleResponse.Categoria categoria,
            ReporteDetalleResponse.Barrio barrio, Integer comuna, double latitud, double longitud,
            OffsetDateTime creadoEn, OffsetDateTime vigenteHasta) {
    }
}
