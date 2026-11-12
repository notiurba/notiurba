package com.notiurba.geo;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public final class ConsultaEspacial {

    private ConsultaEspacial() {
    }

    /**
     * PostGIS declara costos muy altos y el JIT de PostgreSQL compila estas consultas aunque sean pequeñas:
     * tarda más en compilar que en ejecutar. {@code SET LOCAL} lo apaga solo para la transacción en curso.
     */
    public static void sinJit(NamedParameterJdbcTemplate jdbc) {
        jdbc.getJdbcOperations().execute("SET LOCAL jit = off");
    }
}
