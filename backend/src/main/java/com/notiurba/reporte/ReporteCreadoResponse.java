package com.notiurba.reporte;

import java.time.OffsetDateTime;

/**
 * Respuesta mínima del 201. Todos los valores salen de la base tras guardar; el reporte completo
 * se consulta con GET /api/reportes/{codigo}.
 */
public record ReporteCreadoResponse(
        String codigoSeguimiento, String estado, OffsetDateTime creadoEn, OffsetDateTime vigenteHasta) {
}
