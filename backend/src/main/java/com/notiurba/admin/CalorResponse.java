package com.notiurba.admin;

import java.util.List;

import com.notiurba.reporte.ReporteDetalleResponse;

/** {@code totalReportes} es la suma de los pesos; {@code truncado} indica que los puntos se cortaron en el tope. */
public record CalorResponse(long totalReportes, long totalPuntos, boolean truncado, long sinComuna,
        List<Punto> puntos, ComunasCriticas comunasCriticas) {

    /** Cada reporte pesa 1; los del mismo punto se suman. */
    public record Punto(double latitud, double longitud, long peso) {
    }

    public record ComunasCriticas(int pagina, int tamano, int totalComunas, int totalPaginas,
            List<ComunaCritica> items) {
    }

    /** {@code posicion} es el lugar en el ranking completo, no en la página. */
    public record ComunaCritica(int posicion, int comuna, long total,
            ReporteDetalleResponse.Categoria categoriaPrincipal) {
    }
}
