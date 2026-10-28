package com.notiurba.usuario;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Consultas de solo lectura a los catálogos que exige el alta de un usuario.
 * Se usan con JdbcTemplate para no mapear entidades que nadie más necesita todavía.
 */
@Component
public class CatalogosRegistro {

    private final JdbcTemplate jdbc;

    public CatalogosRegistro(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existeTipoDocumento(Short idTipoDocumento) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM tipo_documento WHERE id_tipo_documento = ?)",
                Boolean.class, idTipoDocumento));
    }

    public boolean existeBarrio(Integer idBarrio) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM barrio WHERE id_barrio = ?)", Boolean.class, idBarrio));
    }

    /** Versión del aviso de privacidad vigente hoy: la más reciente con vigente_desde ya cumplida. */
    public Short avisoPrivacidadVigente() {
        List<Short> ids = jdbc.queryForList(
                "SELECT id_aviso_privacidad FROM aviso_privacidad WHERE vigente_desde <= now()"
                        + " ORDER BY vigente_desde DESC LIMIT 1",
                Short.class);
        if (ids.isEmpty()) {
            throw new IllegalStateException("No hay un aviso de privacidad vigente");
        }
        return ids.get(0);
    }
}
