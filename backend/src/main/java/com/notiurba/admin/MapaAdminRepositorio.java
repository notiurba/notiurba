package com.notiurba.admin;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.notiurba.reporte.ReporteDetalleResponse;

/**
 * Consultas del mapa, del mapa de calor y de las estadísticas del panel (NOT-129). La comuna no se guarda en
 * el reporte: sale de la vista {@code v_reporte_comuna} (la misma regla que al crear el reporte).
 */
@Repository
public class MapaAdminRepositorio {

    /** Tope de filas por consulta: con más reportes el mapa se vuelve ilegible y lento. */
    static final int MAXIMO_PUNTOS = 1000;

    /**
     * Uniones comunes. El mapa y el calor solo muestran reportes con ubicación y vigentes: el mismo criterio de
     * la vista {@code v_reporte_mapa} (RN-20). Los vencidos siguen en el listado del panel.
     */
    private static final String DESDE_VIGENTES_CON_UBICACION = """
              FROM reporte r
              JOIN categoria c ON c.id_categoria = r.id_categoria
              JOIN estado_reporte est ON est.id_estado_reporte = r.id_estado_reporte
              LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
              LEFT JOIN v_reporte_comuna vc ON vc.id_reporte = r.id_reporte
             WHERE r.ubicacion IS NOT NULL AND r.vigente_hasta > now()""";

    private final NamedParameterJdbcTemplate jdbc;

    public MapaAdminRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Reportes vigentes con ubicación, los más recientes primero, hasta {@value #MAXIMO_PUNTOS}. */
    public MapaResponse mapa(FiltroMapa f) {
        var p = new MapSqlParameterSource("limite", MAXIMO_PUNTOS);
        String filtros = filtros(f, p);
        long[] totales = new long[2]; // [total, sinComuna]: iguales en todas las filas (funciones de ventana)
        List<MapaResponse.Marcador> marcadores = jdbc.query("""
                SELECT r.codigo_seguimiento, est.nombre AS estado, c.id_categoria, c.nombre AS categoria,
                       b.id_barrio, b.nombre AS barrio, vc.id_comuna,
                       ST_Y(r.ubicacion) AS latitud, ST_X(r.ubicacion) AS longitud, r.creado_en, r.vigente_hasta,
                       count(*) OVER () AS total,
                       count(*) FILTER (WHERE vc.id_comuna IS NULL) OVER () AS sin_comuna
                """ + DESDE_VIGENTES_CON_UBICACION + filtros + """

                 ORDER BY r.creado_en DESC, r.id_reporte DESC
                 LIMIT :limite""", p, (rs, i) -> {
                    totales[0] = rs.getLong("total");
                    totales[1] = rs.getLong("sin_comuna");
                    Integer idBarrio = (Integer) rs.getObject("id_barrio");
                    return new MapaResponse.Marcador(
                            rs.getString("codigo_seguimiento"), rs.getString("estado"),
                            new ReporteDetalleResponse.Categoria(rs.getInt("id_categoria"), rs.getString("categoria")),
                            idBarrio == null ? null : new ReporteDetalleResponse.Barrio(idBarrio, rs.getString("barrio")),
                            (Integer) rs.getObject("id_comuna"),
                            rs.getDouble("latitud"), rs.getDouble("longitud"),
                            rs.getObject("creado_en", OffsetDateTime.class),
                            rs.getObject("vigente_hasta", OffsetDateTime.class));
                });
        return new MapaResponse(totales[0], totales[1], totales[0] > marcadores.size(), marcadores);
    }

    /**
     * Condiciones de los filtros comunes, en el mismo formato que el listado del panel (RF-19): fechas de
     * creación en hora de Colombia y ambas inclusive.
     */
    static String filtros(FiltroMapa f, MapSqlParameterSource p) {
        var sql = new StringBuilder();
        if (f.estado() != null) {
            sql.append(" AND est.nombre = :estado");
            p.addValue("estado", f.estado());
        }
        if (f.idCategoria() != null) {
            sql.append(" AND c.id_categoria = :categoria");
            p.addValue("categoria", f.idCategoria());
        }
        if (f.comuna() != null) {
            sql.append(" AND vc.id_comuna = :comuna");
            p.addValue("comuna", f.comuna());
        }
        if (f.desde() != null) {
            sql.append(" AND (r.creado_en AT TIME ZONE 'America/Bogota')::date >= :desde");
            p.addValue("desde", f.desde());
        }
        if (f.hasta() != null) {
            sql.append(" AND (r.creado_en AT TIME ZONE 'America/Bogota')::date <= :hasta");
            p.addValue("hasta", f.hasta());
        }
        return sql.toString();
    }
}
