package com.notiurba.admin;

import java.math.BigDecimal;
import java.util.List;

/** {@code atendidos} son los reportes en estado Resuelto; el porcentaje lleva un decimal. */
public record EstadisticasResponse(String vigencia, long total, long atendidos, BigDecimal porcentajeAtendidos,
        long sinComuna, List<PorEstado> porEstado, List<PorCategoria> porCategoria, List<PorComuna> porComuna) {

    public record PorEstado(String estado, long total) {
    }

    public record PorCategoria(int idCategoria, String categoria, long total) {
    }

    public record PorComuna(int comuna, long total) {
    }
}
