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
            String direccionIncidente, String matriculaCuentaServicio, String radicadoOficial, boolean requiereEvidencia) {
    }

    /** Texto para el ciudadano: el detalle técnico de registro_envio_correo.error no sale de la API. */
    static final String MOTIVO_ENVIO_FALLIDO = "No pudimos entregar el correo a la entidad.";

    private final NamedParameterJdbcTemplate jdbc;

    /** Una emergencia la marca el ciudadano o la delata la descripción en una categoría de gas. */
    public boolean esEmergencia(int idCategoria, String descripcion, boolean marcada) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT :marcada OR (EXISTS(SELECT 1 FROM configuracion_enrutamiento x
             WHERE x.id_categoria=:categoria AND x.es_gas) AND lower(CAST(:descripcion AS text)) ~ '(fuga|olor.*gas|escape.*gas)')
            """, new MapSqlParameterSource("marcada", marcada).addValue("categoria", idCategoria)
                .addValue("descripcion", descripcion), Boolean.class));
    }

    public void prepararEnrutamiento(String codigo, boolean emergencia) {
        jdbc.update("""
            UPDATE reporte r SET emergencia=:emergencia,
             requiere_revision=r.requiere_revision OR COALESCE((SELECT x.revision_clasificacion
              FROM configuracion_enrutamiento x WHERE x.id_categoria=r.id_categoria),FALSE)
            WHERE codigo_seguimiento=:codigo
            """, new MapSqlParameterSource("codigo",codigo).addValue("emergencia",emergencia));
    }

    public ReporteRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Pone en fila a quien crea reportes a la vez: así el tope diario no se salta con dos envíos simultáneos. */
    public void bloquearCiudadano(long idUsuario) {
        jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE id_usuario = :id FOR UPDATE",
                new MapSqlParameterSource("id", idUsuario), Long.class);
    }

    /** Reportes del ciudadano creados desde ese momento (de cualquier estado). */
    public int contarDesde(long idUsuario, OffsetDateTime desde) {
        return jdbc.queryForObject("SELECT count(*) FROM reporte WHERE id_usuario = :id AND creado_en >= :desde",
                new MapSqlParameterSource("id", idUsuario).addValue("desde", desde), Integer.class);
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
                .addValue("radicado", r.radicadoOficial())
                .addValue("evidencia", r.requiereEvidencia());
        return jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion,
                                     direccion_incidente, matricula_cuenta_servicio, radicado_oficial,
                                     requiere_evidencia, creado_en, autorizacion_envio_en)
                VALUES (:usuario, :categoria, :barrio, :descripcion,
                        ST_SetSRID(ST_MakePoint(CAST(:lon AS double precision), CAST(:lat AS double precision)), 4326),
                        :direccion, :matricula, :radicado, :evidencia, now(), now())
                RETURNING codigo_seguimiento, creado_en, vigente_hasta,
                          (SELECT e.nombre FROM estado_reporte e
                            WHERE e.id_estado_reporte = reporte.id_estado_reporte) AS estado""",
                parametros,
                (rs, i) -> new ReporteCreadoResponse(
                        rs.getString("codigo_seguimiento"), rs.getString("estado"),
                        rs.getObject("creado_en", OffsetDateTime.class),
                        rs.getObject("vigente_hasta", OffsetDateTime.class)));
    }

    /**
     * El reporte de ese código SOLO si es de ese usuario: si no existe o es de otra persona el resultado
     * es el mismo (vacío), para no revelar qué códigos existen.
     */
    public Optional<ReporteDetalleResponse> buscarPropio(long idUsuario, String codigo) {
        var parametros = new MapSqlParameterSource().addValue("usuario", idUsuario).addValue("codigo", codigo);
        List<ReporteDetalleResponse> filas = jdbc.query("""
                SELECT r.id_reporte, r.codigo_seguimiento, est.nombre AS estado,
                       c.id_categoria, c.nombre AS categoria, r.descripcion,
                       ST_Y(r.ubicacion) AS latitud, ST_X(r.ubicacion) AS longitud,
                       r.id_comuna, b.id_barrio, b.nombre AS barrio,
                       r.direccion_incidente, r.matricula_cuenta_servicio, r.radicado_oficial,
                       r.creado_en, r.vigente_hasta
                  FROM reporte r
                  JOIN categoria c ON c.id_categoria = r.id_categoria
                  JOIN estado_reporte est ON est.id_estado_reporte = r.id_estado_reporte
                  LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
                 WHERE r.codigo_seguimiento = :codigo AND r.id_usuario = :usuario""",
                parametros, (rs, i) -> {
                    long idReporte = rs.getLong("id_reporte");
                    Integer idBarrio = (Integer) rs.getObject("id_barrio");
                    return new ReporteDetalleResponse(
                            rs.getString("codigo_seguimiento"), rs.getString("estado"),
                            new ReporteDetalleResponse.Categoria(rs.getInt("id_categoria"), rs.getString("categoria")),
                            rs.getString("descripcion"),
                            (Double) rs.getObject("latitud"), (Double) rs.getObject("longitud"),
                            (Integer) rs.getObject("id_comuna"),
                            idBarrio == null ? null : new ReporteDetalleResponse.Barrio(idBarrio, rs.getString("barrio")),
                            rs.getString("direccion_incidente"), rs.getString("matricula_cuenta_servicio"),
                            rs.getString("radicado_oficial"),
                            fotos(idReporte),
                            rs.getObject("creado_en", OffsetDateTime.class),
                            rs.getObject("vigente_hasta", OffsetDateTime.class),
                            historial(idReporte), envio(idReporte));
                });
        return filas.stream().findFirst();
    }

    /**
     * Bloquea (FOR UPDATE) la fila del reporte si es de ese usuario, hasta que termine la transacción.
     * Vacío si no existe o es de otra persona.
     */
    public Optional<Long> bloquearReportePropio(long idUsuario, String codigo) {
        List<Long> ids = jdbc.query(
                "SELECT id_reporte FROM reporte WHERE codigo_seguimiento = :codigo AND id_usuario = :usuario FOR UPDATE",
                new MapSqlParameterSource().addValue("usuario", idUsuario).addValue("codigo", codigo),
                (rs, i) -> rs.getLong("id_reporte"));
        return ids.stream().findFirst();
    }

    public List<Integer> ordenesDeFotos(long idReporte) {
        return jdbc.query("SELECT orden FROM evidencia_reporte WHERE id_reporte = :id ORDER BY orden",
                new MapSqlParameterSource("id", idReporte), (rs, i) -> rs.getInt("orden"));
    }

    public void insertarFoto(long idReporte, long idUsuario, String url, String idPublico, int orden) {
        jdbc.update("""
                INSERT INTO evidencia_reporte (id_reporte, id_usuario, url, id_publico_proveedor, orden)
                VALUES (:reporte, :usuario, :url, :idPublico, :orden)""",
                new MapSqlParameterSource().addValue("reporte", idReporte).addValue("usuario", idUsuario)
                        .addValue("url", url).addValue("idPublico", idPublico).addValue("orden", orden));
    }

    private List<ReporteDetalleResponse.Foto> fotos(long idReporte) {
        return jdbc.query("SELECT orden, url FROM evidencia_reporte WHERE id_reporte = :id ORDER BY orden",
                new MapSqlParameterSource("id", idReporte),
                (rs, i) -> new ReporteDetalleResponse.Foto(rs.getInt("orden"), rs.getString("url")));
    }

    private List<ReporteDetalleResponse.CambioDeEstado> historial(long idReporte) {
        return jdbc.query("""
                SELECT e.nombre AS estado, h.observacion, h.cambiado_en,
                       (h.id_usuario_cambia IS NOT NULL AND h.id_usuario_cambia = r.id_usuario) AS del_ciudadano
                  FROM historial_estado h JOIN estado_reporte e ON e.id_estado_reporte = h.id_estado_reporte
                  JOIN reporte r ON r.id_reporte = h.id_reporte
                 WHERE h.id_reporte = :id ORDER BY h.cambiado_en, h.id_historial_estado""",
                new MapSqlParameterSource("id", idReporte),
                (rs, i) -> new ReporteDetalleResponse.CambioDeEstado(rs.getString("estado"),
                        rs.getString("observacion"), rs.getObject("cambiado_en", OffsetDateTime.class),
                        rs.getBoolean("del_ciudadano")));
    }

    /** El último envío del reporte; null si todavía no hay ninguno. */
    private ReporteDetalleResponse.Envio envio(long idReporte) {
        return jdbc.query("""
                SELECT s.nombre AS estado
                  FROM registro_envio_correo e JOIN estado_envio s ON s.id_estado_envio = e.id_estado_envio
                 WHERE e.id_reporte = :id ORDER BY e.creado_en DESC, e.id_registro_envio DESC LIMIT 1""",
                new MapSqlParameterSource("id", idReporte),
                (rs, i) -> {
                    String estado = rs.getString("estado");
                    return new ReporteDetalleResponse.Envio(estado, "FALLIDO".equals(estado) ? MOTIVO_ENVIO_FALLIDO : null);
                }).stream().findFirst().orElse(null);
    }
}
