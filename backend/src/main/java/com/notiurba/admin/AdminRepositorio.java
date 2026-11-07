package com.notiurba.admin;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepositorio {
    public record ReporteBloqueado(long id, String estado, int orden, OffsetDateTime vigenteHasta) { }
    public record EstadoDestino(short id, int orden) { }
    public record Asiento(String observacion, Long cambiadoPor, OffsetDateTime cambiadoEn, short estado) { }
    private record FilaReporte(long id, short estado, OffsetDateTime vigenteHasta) { }

    private final JdbcTemplate jdbc;
    public AdminRepositorio(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Incluye vencidos. Comparte el bloqueo de fila utilizado por el procesamiento de correo. */
    public Optional<ReporteBloqueado> bloquear(String codigo) {
        // Bloquear primero evita leer la etiqueta de un estado anterior si otra transacción avanzó mientras esperábamos.
        var filas = jdbc.query("""
                SELECT id_reporte, id_estado_reporte, vigente_hasta FROM reporte
                WHERE codigo_seguimiento=? FOR UPDATE
                """, (rs, n) -> new FilaReporte(rs.getLong("id_reporte"), rs.getShort("id_estado_reporte"),
                rs.getObject("vigente_hasta", OffsetDateTime.class)), codigo);
        return filas.stream().findFirst().map(r -> jdbc.queryForObject(
                "SELECT nombre, orden FROM estado_reporte WHERE id_estado_reporte=?",
                (rs, n) -> new ReporteBloqueado(r.id(), rs.getString("nombre"), rs.getInt("orden"),
                        r.vigenteHasta()), r.estado()));
    }

    public EstadoDestino destino(EstadoReporte estado) {
        return jdbc.queryForObject("SELECT id_estado_reporte, orden FROM estado_reporte WHERE nombre=?",
                (rs, n) -> new EstadoDestino(rs.getShort(1), rs.getInt(2)), estado.nombre());
    }

    public Optional<Asiento> ultimo(long reporte) {
        return jdbc.query("""
                SELECT observacion, id_usuario_cambia, cambiado_en, id_estado_reporte
                FROM historial_estado WHERE id_reporte=?
                ORDER BY cambiado_en DESC, id_historial_estado DESC LIMIT 1
                """, (rs, n) -> new Asiento(rs.getString(1), rs.getObject(2, Long.class),
                rs.getObject(3, OffsetDateTime.class), rs.getShort(4)), reporte).stream().findFirst();
    }

    /** B4 sincroniza el estado; no duplicar su UPDATE. El reloj no puede preceder al último asiento. */
    public OffsetDateTime insertar(long reporte, short estado, String observacion, long administrador) {
        return jdbc.queryForObject("""
                INSERT INTO historial_estado(id_reporte,id_estado_reporte,observacion,id_usuario_cambia,cambiado_en)
                VALUES (?,?,?,?,GREATEST(clock_timestamp(),
                    (SELECT max(cambiado_en) FROM historial_estado WHERE id_reporte=?)))
                RETURNING cambiado_en
                """, OffsetDateTime.class, reporte, estado, observacion, administrador, reporte);
    }

    public int vigencia() {
        return jdbc.queryForObject("SELECT valor::integer FROM configuracion WHERE clave='vigencia_dias'", Integer.class);
    }

    public void guardarVigencia(int dias, long administrador) {
        int modificadas = jdbc.update("""
                UPDATE configuracion SET valor=?, id_usuario_actualiza=?, actualizado_en=clock_timestamp()
                WHERE clave='vigencia_dias'
                """, Integer.toString(dias), administrador);
        if (modificadas != 1) {
            throw new IllegalStateException("Falta la configuración de vigencia de las migraciones");
        }
    }
}
