package com.notiurba.admin;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.notiurba.geo.ConsultaEspacial;
import com.notiurba.reporte.FiltroPanel;
import com.notiurba.reporte.ReporteDetalleResponse;

/** La comuna no se guarda en el reporte: se deriva de la vista {@code v_reporte_comuna}. */
@Repository
public class MapaAdminRepositorio {

    static final int MAXIMO_PUNTOS = 1000;

    /** Mismo criterio de vigencia que la vista {@code v_reporte_mapa}. */
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

    public MapaResponse mapa(FiltroPanel f) {
        ConsultaEspacial.sinJit(jdbc);
        var p = new MapSqlParameterSource("limite", MAXIMO_PUNTOS);
        String filtros = f.condiciones(p);
        long[] totales = new long[2]; // total y sinComuna: vienen iguales en cada fila
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

    /** {@code comuna} es {@code null} en la fila de los reportes sin comuna. */
    public record ConteoComuna(Integer comuna, long total, ReporteDetalleResponse.Categoria categoriaPrincipal) {
    }

    public record DatosCalor(List<CalorResponse.Punto> puntos, long totalPuntos, long totalReportes,
            List<ConteoComuna> comunas) {
    }

    public DatosCalor calor(FiltroPanel f) {
        ConsultaEspacial.sinJit(jdbc);
        var p = new MapSqlParameterSource("limite", MAXIMO_PUNTOS);
        String filtros = f.condiciones(p);
        long[] totales = new long[2]; // totalPuntos y totalReportes
        List<CalorResponse.Punto> puntos = jdbc.query("""
                SELECT ST_Y(r.ubicacion) AS latitud, ST_X(r.ubicacion) AS longitud, count(*) AS peso,
                       count(*) OVER () AS total_puntos, sum(count(*)) OVER () AS total_reportes
                """ + DESDE_VIGENTES_CON_UBICACION + filtros + """

                 GROUP BY ST_Y(r.ubicacion), ST_X(r.ubicacion)
                 ORDER BY peso DESC, latitud, longitud
                 LIMIT :limite""", p, (rs, i) -> {
                    totales[0] = rs.getLong("total_puntos");
                    totales[1] = rs.getLong("total_reportes");
                    return new CalorResponse.Punto(rs.getDouble("latitud"), rs.getDouble("longitud"), rs.getLong("peso"));
                });

        // categoría principal: la de más reportes; en empate, la de menor número
        List<ConteoComuna> comunas = jdbc.query("""
                WITH base AS (
                    SELECT vc.id_comuna, c.id_categoria, c.nombre AS categoria
                """ + DESDE_VIGENTES_CON_UBICACION + filtros + """

                ), conteo AS (
                    SELECT id_comuna, id_categoria, categoria, count(*) AS n
                      FROM base GROUP BY id_comuna, id_categoria, categoria
                )
                SELECT id_comuna, sum(n) AS total,
                       (array_agg(id_categoria ORDER BY n DESC, id_categoria))[1] AS id_categoria,
                       (array_agg(categoria ORDER BY n DESC, id_categoria))[1] AS categoria
                  FROM conteo
                 GROUP BY id_comuna""", p, (rs, i) -> new ConteoComuna(
                        (Integer) rs.getObject("id_comuna"), rs.getLong("total"),
                        new ReporteDetalleResponse.Categoria(rs.getInt("id_categoria"), rs.getString("categoria"))));
        return new DatosCalor(puntos, totales[0], totales[1], comunas);
    }

    /** {@code comuna} es {@code null} para los reportes sin comuna. */
    public record ConteoEstadistica(String estado, int idCategoria, String categoria, Integer comuna, long total) {
    }

    /** Cuenta todos los reportes, con o sin ubicación. */
    public List<ConteoEstadistica> estadisticas(FiltroPanel f, FiltroVigencia vigencia) {
        ConsultaEspacial.sinJit(jdbc);
        var p = new MapSqlParameterSource();
        String condicionVigencia = switch (vigencia) {
            case VIGENTES -> " AND r.vigente_hasta > now()";
            case FUERA_DE_PLAZO -> " AND r.vigente_hasta <= now()";
            case TODOS -> "";
        };
        return jdbc.query("""
                SELECT est.nombre AS estado, c.id_categoria, c.nombre AS categoria, vc.id_comuna, count(*) AS total
                  FROM reporte r
                  JOIN categoria c ON c.id_categoria = r.id_categoria
                  JOIN estado_reporte est ON est.id_estado_reporte = r.id_estado_reporte
                  LEFT JOIN v_reporte_comuna vc ON vc.id_reporte = r.id_reporte
                 WHERE TRUE""" + condicionVigencia + f.condiciones(p) + """

                 GROUP BY est.nombre, c.id_categoria, c.nombre, vc.id_comuna""", p, (rs, i) -> new ConteoEstadistica(
                        rs.getString("estado"), rs.getInt("id_categoria"), rs.getString("categoria"),
                        (Integer) rs.getObject("id_comuna"), rs.getLong("total")));
    }

    public List<ReporteDetalleResponse.Categoria> categoriasActivas() {
        return jdbc.query("SELECT id_categoria, nombre FROM categoria WHERE activa ORDER BY id_categoria",
                (rs, i) -> new ReporteDetalleResponse.Categoria(rs.getInt("id_categoria"), rs.getString("nombre")));
    }

}
