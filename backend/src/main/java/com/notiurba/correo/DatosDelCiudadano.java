package com.notiurba.correo;

import org.springframework.jdbc.core.JdbcTemplate;

/** Remitente de la carta: lo usan la vista previa y el correo real, para que digan lo mismo. */
final class DatosDelCiudadano {

    private DatosDelCiudadano() {
    }

    static PlantillaReporte.Datos.Builder cargar(JdbcTemplate jdbc, long idUsuario) {
        return jdbc.queryForObject("""
                SELECT u.nombres, u.apellidos, u.numero_documento, u.direccion_correspondencia, u.correo, u.telefono,
                       t.codigo AS codigo_documento, t.nombre AS tipo_documento
                  FROM usuario u JOIN tipo_documento t USING (id_tipo_documento) WHERE u.id_usuario = ?""",
                (rs, i) -> PlantillaReporte.Datos.builder()
                        .nombre(rs.getString("nombres") + " " + rs.getString("apellidos"))
                        .tipoDocumento(rs.getString("tipo_documento")).codigoDocumento(rs.getString("codigo_documento"))
                        .numeroDocumento(rs.getString("numero_documento"))
                        .direccionCorrespondencia(rs.getString("direccion_correspondencia"))
                        .correo(rs.getString("correo")).telefono(rs.getString("telefono")),
                idUsuario);
    }
}
