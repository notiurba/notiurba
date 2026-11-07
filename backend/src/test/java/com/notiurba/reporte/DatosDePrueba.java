package com.notiurba.reporte;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/** Usuarios y reportes insertados directo en la base para las pruebas de integración (más rápido que pasar por la API). */
public final class DatosDePrueba {

    public static final int CIUDADANO = 1;
    public static final int ADMIN = 2;

    private DatosDePrueba() {
    }

    public record Reporte(long id, String codigo) {
    }

    /** @return el id del usuario; el correo lo pone quien llama para poder firmar su token */
    public static long usuario(JdbcTemplate jdbc, int idBarrio, String correo, int idRol) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), 'hash-de-prueba', ?, 1, now())
                RETURNING id_usuario""", Long.class,
                UUID.randomUUID().toString().substring(0, 20), idBarrio, correo, idRol);
    }

    /** Un reporte en el punto de prueba (dentro de la capa de comunas), creado hace {@code diasAtras} días. */
    public static Reporte reporte(JdbcTemplate jdbc, long idUsuario, int idCategoria, int idBarrio, int diasAtras) {
        return jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, direccion_incidente, ubicacion,
                                     autorizacion_envio_en, creado_en)
                VALUES (?, ?, ?, 'Hay un hueco enorme frente al parque', 'Cra 5 # 10-20',
                        ST_SetSRID(ST_MakePoint(-76.1950, 4.0867), 4326),
                        now() - make_interval(days => ?), now() - make_interval(days => ?))
                RETURNING id_reporte, codigo_seguimiento""",
                (rs, i) -> new Reporte(rs.getLong(1), rs.getString(2)), idUsuario, idCategoria, idBarrio,
                diasAtras, diasAtras);
    }

    public static void foto(JdbcTemplate jdbc, Reporte reporte, long idUsuario, int orden, String idPublico) {
        jdbc.update("""
                INSERT INTO evidencia_reporte (id_reporte, id_usuario, url, id_publico_proveedor, orden)
                VALUES (?, ?, ?, ?, ?)""", reporte.id(), idUsuario, "https://res.cloudinary.com/demo/" + idPublico + ".jpg",
                idPublico, orden);
    }

    public static void pasarAEstado(JdbcTemplate jdbc, Reporte reporte, String estado) {
        jdbc.update("""
                INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion)
                SELECT ?, id_estado_reporte, 'Cambio de prueba' FROM estado_reporte WHERE nombre = ?""",
                reporte.id(), estado);
    }
}
