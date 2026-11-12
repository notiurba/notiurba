package com.notiurba.reporte;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.notiurba.admin.EstadoReporte;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Filtros del panel de administración, ya validados. {@code estado} trae el nombre del catálogo
 * («En trámite»). Una categoría que no existe no es error: da una lista vacía.
 */
public record FiltroPanel(String estado, Integer idCategoria, Integer comuna, LocalDate desde, LocalDate hasta) {

    public static FiltroPanel de(String estado, Integer idCategoria, Integer comuna, LocalDate desde,
            LocalDate hasta) {
        if (comuna != null && (comuna < 1 || comuna > 10)) {
            throw error("La comuna es un número de 1 a 10", "comuna");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw error("La fecha «desde» no puede ser posterior a «hasta»", "desde");
        }
        return new FiltroPanel(nombreDeEstado(estado).orElse(null), idCategoria, comuna, desde, hasta);
    }

    /** Acepta el nombre del catálogo («En trámite») o el identificador de la API (EN_TRAMITE). */
    public static Optional<String> nombreDeEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String v = valor.trim();
        for (EstadoReporte e : EstadoReporte.values()) {
            if (e.nombre().equalsIgnoreCase(v) || e.name().equalsIgnoreCase(v)) {
                return Optional.of(e.nombre());
            }
        }
        throw error("El estado debe ser Recibido, Enviado, En trámite o Resuelto", "estado");
    }

    /**
     * Condiciones {@code AND ...} sobre los alias {@code r} (reporte), {@code c} (categoría), {@code est}
     * (estado) y {@code vc} (v_reporte_comuna). Fechas de creación en hora de Colombia, ambas inclusive.
     */
    public String condiciones(MapSqlParameterSource p) {
        var sql = new StringBuilder();
        if (estado != null) {
            sql.append(" AND est.nombre = :estado");
            p.addValue("estado", estado);
        }
        if (idCategoria != null) {
            sql.append(" AND c.id_categoria = :categoria");
            p.addValue("categoria", idCategoria);
        }
        if (comuna != null) {
            sql.append(" AND vc.id_comuna = :comuna");
            p.addValue("comuna", comuna);
        }
        if (desde != null) {
            sql.append(" AND (r.creado_en AT TIME ZONE 'America/Bogota')::date >= :desde");
            p.addValue("desde", desde);
        }
        if (hasta != null) {
            sql.append(" AND (r.creado_en AT TIME ZONE 'America/Bogota')::date <= :hasta");
            p.addValue("hasta", hasta);
        }
        return sql.toString();
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
