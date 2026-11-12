package com.notiurba.reporte;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.notiurba.geo.ConsultaEspacial;

/** Listados de reportes: los propios del ciudadano («Mis reportes») y todos para el panel de administración. */
@Repository
public class ListadoReportesRepositorio {

    static final int MAXIMO_PROPIOS = 100;
    static final int MAXIMO_ADMIN = 500;

    private final NamedParameterJdbcTemplate jdbc;

    public ListadoReportesRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Los reportes de ese usuario, los más recientes primero; {@code estado} (nombre del catálogo) es opcional. */
    public List<ReporteResumenResponse> listarPropios(long idUsuario, String estado) {
        var p = new MapSqlParameterSource("usuario", idUsuario);
        String filtro = "";
        if (estado != null) {
            filtro = " AND est.nombre = :estado";
            p.addValue("estado", estado);
        }
        return jdbc.query("""
                SELECT r.codigo_seguimiento, est.nombre AS estado, c.id_categoria, c.nombre AS categoria,
                       r.direccion_incidente, b.id_barrio, b.nombre AS barrio, r.creado_en, r.vigente_hasta,
                       ult.estado AS envio
                  FROM reporte r
                  JOIN categoria c ON c.id_categoria = r.id_categoria
                  JOIN estado_reporte est ON est.id_estado_reporte = r.id_estado_reporte
                  LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
                  LEFT JOIN LATERAL (
                        SELECT s.nombre AS estado
                          FROM registro_envio_correo e JOIN estado_envio s ON s.id_estado_envio = e.id_estado_envio
                         WHERE e.id_reporte = r.id_reporte
                         ORDER BY e.creado_en DESC, e.id_registro_envio DESC LIMIT 1) ult ON TRUE
                 WHERE r.id_usuario = :usuario""" + filtro + """

                 ORDER BY r.creado_en DESC, r.id_reporte DESC LIMIT\s""" + MAXIMO_PROPIOS,
                p, (rs, i) -> {
                    Integer idBarrio = (Integer) rs.getObject("id_barrio");
                    return new ReporteResumenResponse(
                            rs.getString("codigo_seguimiento"), rs.getString("estado"),
                            new ReporteDetalleResponse.Categoria(rs.getInt("id_categoria"), rs.getString("categoria")),
                            rs.getString("direccion_incidente"),
                            idBarrio == null ? null : new ReporteDetalleResponse.Barrio(idBarrio, rs.getString("barrio")),
                            rs.getObject("creado_en", OffsetDateTime.class),
                            rs.getObject("vigente_hasta", OffsetDateTime.class),
                            envio(rs.getString("envio")));
                });
    }

    /**
     * Todos los reportes (los más recientes primero, hasta {@value #MAXIMO_ADMIN}) con la misma forma que el
     * detalle. Fotos, historial y último envío se piden en lote (3 consultas), no una vez por reporte.
     */
    public List<ReporteDetalleResponse> listarParaAdmin(FiltroPanel f) {
        ConsultaEspacial.sinJit(jdbc);
        var p = new MapSqlParameterSource();
        String donde = " WHERE TRUE" + f.condiciones(p);

        record Fila(long idReporte, ReporteDetalleResponse base) {
        }
        List<Fila> filas = jdbc.query("""
                SELECT r.id_reporte, r.codigo_seguimiento, est.nombre AS estado, c.id_categoria, c.nombre AS categoria,
                       r.descripcion, ST_Y(r.ubicacion) AS latitud, ST_X(r.ubicacion) AS longitud,
                       vc.id_comuna, b.id_barrio, b.nombre AS barrio, r.direccion_incidente,
                       r.matricula_cuenta_servicio, r.radicado_oficial, r.creado_en, r.vigente_hasta
                  FROM reporte r
                  JOIN categoria c ON c.id_categoria = r.id_categoria
                  JOIN estado_reporte est ON est.id_estado_reporte = r.id_estado_reporte
                  LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
                  LEFT JOIN v_reporte_comuna vc ON vc.id_reporte = r.id_reporte""" + donde + """

                 ORDER BY r.creado_en DESC, r.id_reporte DESC LIMIT\s""" + MAXIMO_ADMIN,
                p, (rs, i) -> {
                    Integer idBarrio = (Integer) rs.getObject("id_barrio");
                    return new Fila(rs.getLong("id_reporte"), new ReporteDetalleResponse(
                            rs.getString("codigo_seguimiento"), rs.getString("estado"),
                            new ReporteDetalleResponse.Categoria(rs.getInt("id_categoria"), rs.getString("categoria")),
                            rs.getString("descripcion"),
                            (Double) rs.getObject("latitud"), (Double) rs.getObject("longitud"),
                            (Integer) rs.getObject("id_comuna"),
                            idBarrio == null ? null : new ReporteDetalleResponse.Barrio(idBarrio, rs.getString("barrio")),
                            rs.getString("direccion_incidente"), rs.getString("matricula_cuenta_servicio"),
                            rs.getString("radicado_oficial"), List.of(),
                            rs.getObject("creado_en", OffsetDateTime.class),
                            rs.getObject("vigente_hasta", OffsetDateTime.class), List.of(), null));
                });
        if (filas.isEmpty()) {
            return List.of();
        }

        var ids = new MapSqlParameterSource("ids", filas.stream().map(Fila::idReporte).toList());
        Map<Long, List<ReporteDetalleResponse.Foto>> fotos = new LinkedHashMap<>();
        jdbc.query("SELECT id_reporte, orden, url FROM evidencia_reporte WHERE id_reporte IN (:ids) ORDER BY id_reporte, orden",
                ids, rs -> {
                    fotos.computeIfAbsent(rs.getLong("id_reporte"), k -> new ArrayList<>())
                            .add(new ReporteDetalleResponse.Foto(rs.getInt("orden"), rs.getString("url")));
                });
        Map<Long, List<ReporteDetalleResponse.CambioDeEstado>> historial = new LinkedHashMap<>();
        jdbc.query("""
                SELECT h.id_reporte, e.nombre AS estado, h.observacion, h.cambiado_en
                  FROM historial_estado h JOIN estado_reporte e ON e.id_estado_reporte = h.id_estado_reporte
                 WHERE h.id_reporte IN (:ids) ORDER BY h.cambiado_en, h.id_historial_estado""",
                ids, rs -> {
                    historial.computeIfAbsent(rs.getLong("id_reporte"), k -> new ArrayList<>())
                            .add(new ReporteDetalleResponse.CambioDeEstado(rs.getString("estado"),
                                    rs.getString("observacion"), rs.getObject("cambiado_en", OffsetDateTime.class)));
                });
        Map<Long, ReporteDetalleResponse.Envio> envios = new LinkedHashMap<>();
        jdbc.query("""
                SELECT DISTINCT ON (e.id_reporte) e.id_reporte, s.nombre AS estado
                  FROM registro_envio_correo e JOIN estado_envio s ON s.id_estado_envio = e.id_estado_envio
                 WHERE e.id_reporte IN (:ids)
                 ORDER BY e.id_reporte, e.creado_en DESC, e.id_registro_envio DESC""",
                ids, rs -> {
                    envios.put(rs.getLong("id_reporte"), envio(rs.getString("estado")));
                });

        return filas.stream().map(fila -> {
            var b = fila.base();
            return new ReporteDetalleResponse(b.codigoSeguimiento(), b.estado(), b.categoria(), b.descripcion(),
                    b.latitud(), b.longitud(), b.comuna(), b.barrio(), b.direccionIncidente(),
                    b.matriculaCuentaServicio(), b.radicadoOficial(), fotos.getOrDefault(fila.idReporte(), List.of()),
                    b.creadoEn(), b.vigenteHasta(), historial.getOrDefault(fila.idReporte(), List.of()),
                    envios.get(fila.idReporte()));
        }).toList();
    }

    private static ReporteDetalleResponse.Envio envio(String estado) {
        return estado == null ? null
                : new ReporteDetalleResponse.Envio(estado,
                        "FALLIDO".equals(estado) ? ReporteRepositorio.MOTIVO_ENVIO_FALLIDO : null);
    }
}
