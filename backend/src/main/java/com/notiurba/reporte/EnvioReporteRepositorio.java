package com.notiurba.reporte;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Acceso a datos del envío del reporte a la entidad (tabla registro_envio_correo). */
@Repository
public class EnvioReporteRepositorio {

    /** Último envío del reporte; {@code vencido} = sigue PENDIENTE hace más de 5 minutos (se interrumpió). */
    public record UltimoEnvio(long idRegistro, String estado, boolean vencido) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public EnvioReporteRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UltimoEnvio> ultimoEnvio(long idReporte) {
        return jdbc.query("""
                SELECT e.id_registro_envio, s.nombre AS estado,
                       (e.creado_en < now() - interval '5 minutes') AS vencido
                  FROM registro_envio_correo e JOIN estado_envio s ON s.id_estado_envio = e.id_estado_envio
                 WHERE e.id_reporte = :id ORDER BY e.creado_en DESC, e.id_registro_envio DESC LIMIT 1""",
                new MapSqlParameterSource("id", idReporte),
                (rs, i) -> new UltimoEnvio(rs.getLong("id_registro_envio"), rs.getString("estado"), rs.getBoolean("vencido")))
                .stream().findFirst();
    }

    /** El Correo principal activo de la entidad de la categoría del reporte (la regla B5 de la base lo exige). */
    public Optional<Integer> canalPrincipal(long idReporte) {
        return jdbc.query("""
                SELECT k.id_canal_atencion
                  FROM reporte r
                  JOIN categoria c ON c.id_categoria = r.id_categoria
                  JOIN dependencia d ON d.id_dependencia = c.id_dependencia
                  JOIN canal_atencion k ON k.id_entidad_destino = d.id_entidad_destino
                  JOIN tipo_canal t ON t.id_tipo_canal = k.id_tipo_canal
                 WHERE r.id_reporte = :id AND t.nombre = 'Correo' AND k.es_principal AND k.activo""",
                new MapSqlParameterSource("id", idReporte), (rs, i) -> rs.getInt(1)).stream().findFirst();
    }

    public long crearPendiente(long idReporte, int idCanal) {
        return jdbc.queryForObject("""
                INSERT INTO registro_envio_correo (id_reporte, id_canal_atencion, id_modo_envio, id_estado_envio)
                VALUES (:reporte, :canal, (SELECT id_modo_envio FROM modo_envio WHERE nombre = 'RESEND'),
                        (SELECT id_estado_envio FROM estado_envio WHERE nombre = 'PENDIENTE'))
                RETURNING id_registro_envio""",
                new MapSqlParameterSource().addValue("reporte", idReporte).addValue("canal", idCanal), Long.class);
    }

    public void marcarEnviado(long idRegistro, String idProveedor) {
        jdbc.update("""
                UPDATE registro_envio_correo
                   SET id_estado_envio = (SELECT id_estado_envio FROM estado_envio WHERE nombre = 'ENVIADO'),
                       enviado_en = now(), error = NULL, id_envio_proveedor = :proveedor
                 WHERE id_registro_envio = :id""",
                new MapSqlParameterSource().addValue("id", idRegistro).addValue("proveedor", idProveedor));
    }

    public void marcarFallido(long idRegistro, String error) {
        String recortado = error == null || error.isBlank() ? "Falló el envío." : error.substring(0, Math.min(error.length(), 1000));
        jdbc.update("""
                UPDATE registro_envio_correo
                   SET id_estado_envio = (SELECT id_estado_envio FROM estado_envio WHERE nombre = 'FALLIDO'),
                       enviado_en = NULL, error = :error
                 WHERE id_registro_envio = :id""",
                new MapSqlParameterSource().addValue("id", idRegistro).addValue("error", recortado));
    }

    /** El reporte pasa a «Enviado» (decisión 3 del contrato) solo si todavía está «Recibido»: el estado no retrocede. */
    public void pasarReporteAEnviado(long idReporte) {
        jdbc.update("""
                INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion)
                SELECT :id, e.id_estado_reporte, 'Correo enviado a la entidad.'
                  FROM estado_reporte e
                 WHERE e.nombre = 'Enviado'
                   AND (SELECT a.orden FROM reporte r JOIN estado_reporte a USING (id_estado_reporte)
                         WHERE r.id_reporte = :id) < e.orden""",
                new MapSqlParameterSource("id", idReporte));
    }

    public Optional<Long> idReporteDe(long idRegistro) {
        return jdbc.query("SELECT id_reporte FROM registro_envio_correo WHERE id_registro_envio = :id",
                new MapSqlParameterSource("id", idRegistro), (rs, i) -> rs.getLong(1)).stream().findFirst();
    }

    public Optional<DatosEnvio> cargar(long idRegistro) {
        Optional<DatosEnvio> sinFotos = jdbc.query("""
                SELECT e.id_registro_envio, r.id_reporte, r.codigo_seguimiento, c.nombre AS categoria,
                       en.nombre AS entidad, k.acceso AS destino, r.descripcion,
                       ST_Y(r.ubicacion) AS latitud, ST_X(r.ubicacion) AS longitud,
                       vc.id_comuna, b.nombre AS barrio, r.direccion_incidente, r.matricula_cuenta_servicio,
                       r.radicado_oficial, r.creado_en, r.autorizacion_envio_en,
                       u.nombres, u.apellidos, td.codigo AS tipo_documento, u.numero_documento,
                       u.direccion_correspondencia, u.correo
                  FROM registro_envio_correo e
                  JOIN reporte r ON r.id_reporte = e.id_reporte
                  JOIN categoria c ON c.id_categoria = r.id_categoria
                  JOIN dependencia d ON d.id_dependencia = c.id_dependencia
                  JOIN entidad_destino en ON en.id_entidad_destino = d.id_entidad_destino
                  JOIN canal_atencion k ON k.id_canal_atencion = e.id_canal_atencion
                  JOIN usuario u ON u.id_usuario = r.id_usuario
                  JOIN tipo_documento td ON td.id_tipo_documento = u.id_tipo_documento
                  LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
                  LEFT JOIN v_reporte_comuna vc ON vc.id_reporte = r.id_reporte
                 WHERE e.id_registro_envio = :id""",
                new MapSqlParameterSource("id", idRegistro),
                (rs, i) -> new DatosEnvio(
                        rs.getLong("id_registro_envio"), rs.getString("codigo_seguimiento"), rs.getString("categoria"),
                        rs.getString("entidad"), rs.getString("destino"), rs.getString("descripcion"),
                        (Double) rs.getObject("latitud"), (Double) rs.getObject("longitud"),
                        (Integer) rs.getObject("id_comuna"), rs.getString("barrio"),
                        rs.getString("direccion_incidente"), rs.getString("matricula_cuenta_servicio"),
                        rs.getString("radicado_oficial"), rs.getObject("creado_en", OffsetDateTime.class),
                        rs.getObject("autorizacion_envio_en", OffsetDateTime.class),
                        rs.getString("nombres"), rs.getString("apellidos"), rs.getString("tipo_documento"),
                        rs.getString("numero_documento"), rs.getString("direccion_correspondencia"),
                        rs.getString("correo"), List.of())).stream().findFirst();
        return sinFotos.map(d -> {
            List<String> fotos = jdbc.query("""
                    SELECT url FROM evidencia_reporte ev JOIN reporte r ON r.id_reporte = ev.id_reporte
                     WHERE r.codigo_seguimiento = :codigo ORDER BY ev.orden""",
                    new MapSqlParameterSource("codigo", d.codigoSeguimiento()), (rs, i) -> rs.getString(1));
            return new DatosEnvio(d.idRegistroEnvio(), d.codigoSeguimiento(), d.categoria(), d.entidad(), d.destino(),
                    d.descripcion(), d.latitud(), d.longitud(), d.comuna(), d.barrio(), d.direccionIncidente(),
                    d.matriculaCuentaServicio(), d.radicadoOficial(), d.creadoEn(), d.autorizacionEnvioEn(),
                    d.nombres(), d.apellidos(), d.tipoDocumento(), d.numeroDocumento(),
                    d.direccionCorrespondencia(), d.correoCiudadano(), fotos);
        });
    }
}
