package com.notiurba.reporte;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a reporte con SQL directo. El código de seguimiento, la vigencia y el estado inicial
 * los pone el trigger fn_reporte_antes_insertar: aquí solo se leen con RETURNING.
 */
@Repository
public class ReporteRepositorio {

    /** requiereUbicacion: la categoría exige GPS o barrio (B3). esEmpresa: es de una empresa de servicios públicos (B6). */
    public record DatosCategoria(boolean requiereUbicacion, boolean esEmpresa) {
    }

    public record NuevoReporte(
            long idUsuario, int idCategoria, Integer idBarrio, String descripcion, Double latitud, Double longitud,
            String direccionIncidente, String matriculaCuentaServicio, String radicadoOficial) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ReporteRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DatosCategoria> categoriaActiva(int idCategoria) {
        List<DatosCategoria> filas = jdbc.query("""
                SELECT c.requiere_ubicacion,
                       t.nombre = 'Empresa de servicios públicos' AS es_empresa
                  FROM categoria c
                  JOIN dependencia d ON d.id_dependencia = c.id_dependencia
                  JOIN entidad_destino e ON e.id_entidad_destino = d.id_entidad_destino
                  JOIN tipo_entidad t ON t.id_tipo_entidad = e.id_tipo_entidad
                 WHERE c.id_categoria = :id AND c.activa""",
                new MapSqlParameterSource("id", idCategoria),
                (rs, i) -> new DatosCategoria(rs.getBoolean("requiere_ubicacion"), rs.getBoolean("es_empresa")));
        return filas.stream().findFirst();
    }

    /**
     * Inserta y devuelve lo que la base calculó. creado_en y autorizacion_envio_en usan el mismo
     * now() (ck_reporte_fechas exige autorizacion_envio_en <= creado_en).
     */
    public ReporteCreadoResponse insertar(NuevoReporte r) {
        var parametros = new MapSqlParameterSource()
                .addValue("usuario", r.idUsuario())
                .addValue("categoria", r.idCategoria())
                .addValue("barrio", r.idBarrio(), Types.INTEGER)
                .addValue("descripcion", r.descripcion())
                .addValue("lat", r.latitud(), Types.DOUBLE)
                .addValue("lon", r.longitud(), Types.DOUBLE)
                .addValue("direccion", r.direccionIncidente())
                .addValue("matricula", r.matriculaCuentaServicio())
                .addValue("radicado", r.radicadoOficial());
        return jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion,
                                     direccion_incidente, matricula_cuenta_servicio, radicado_oficial,
                                     creado_en, autorizacion_envio_en)
                VALUES (:usuario, :categoria, :barrio, :descripcion,
                        ST_SetSRID(ST_MakePoint(CAST(:lon AS double precision), CAST(:lat AS double precision)), 4326),
                        :direccion, :matricula, :radicado, now(), now())
                RETURNING codigo_seguimiento, creado_en, vigente_hasta,
                          (SELECT e.nombre FROM estado_reporte e
                            WHERE e.id_estado_reporte = reporte.id_estado_reporte) AS estado""",
                parametros,
                (rs, i) -> new ReporteCreadoResponse(
                        rs.getString("codigo_seguimiento"), rs.getString("estado"),
                        rs.getObject("creado_en", OffsetDateTime.class),
                        rs.getObject("vigente_hasta", OffsetDateTime.class)));
    }
}
