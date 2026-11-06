package com.notiurba.correo;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import com.notiurba.web.error.ApiException;

@Service
public class EnrutamientoServicio {
    private final JdbcTemplate jdbc;
    public EnrutamientoServicio(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Ruta(int idCategoria, String codigo, String categoria, String entidad, String dependencia,
            boolean requiereUbicacion, boolean admiteMatricula, boolean esGas, boolean competenciaConfirmada,
            boolean revisionClasificacion, Integer idCanal, String canal, String acceso, String correoReferencia,
            boolean permiteCorreo, String mensaje) { }

    static final String CONSULTA = """
        SELECT c.id_categoria, x.codigo, c.nombre AS categoria, e.nombre AS entidad, d.nombre AS dependencia,
          c.requiere_ubicacion, x.admite_matricula, x.es_gas, x.competencia_confirmada, x.revision_clasificacion,
          x.correo_confirmado AND NOT x.prioriza_alterno AND k.activo AND k.es_principal AS permite,
          k.acceso AS referencia, k.id_canal_atencion AS correo_id,
          a.id_canal_atencion AS alterno_id, a.acceso AS alterno, t.nombre AS tipo
        FROM configuracion_enrutamiento x JOIN categoria c USING(id_categoria)
        JOIN dependencia d USING(id_dependencia) JOIN entidad_destino e USING(id_entidad_destino)
        JOIN canal_atencion k ON k.id_canal_atencion=x.id_canal_correo AND k.id_entidad_destino=e.id_entidad_destino
        JOIN tipo_canal kt ON kt.id_tipo_canal=k.id_tipo_canal AND kt.nombre='Correo'
        JOIN canal_atencion a ON a.id_canal_atencion=x.id_canal_alterno AND a.id_entidad_destino=e.id_entidad_destino
        JOIN tipo_canal t ON t.id_tipo_canal=a.id_tipo_canal
        WHERE c.activa AND d.activa AND e.activa AND a.activo AND t.nombre<>'Correo'
        """;

    public List<Ruta> listar() {
        return jdbc.query(CONSULTA + " ORDER BY x.codigo", (rs, i) -> {
            boolean permite = rs.getBoolean("permite");
            boolean competencia = rs.getBoolean("competencia_confirmada");
            return new Ruta(rs.getInt("id_categoria"), rs.getString("codigo"), rs.getString("categoria"),
                rs.getString("entidad"), rs.getString("dependencia"), rs.getBoolean("requiere_ubicacion"),
                rs.getBoolean("admite_matricula"), rs.getBoolean("es_gas"), competencia,
                rs.getBoolean("revision_clasificacion"), rs.getInt(permite ? "correo_id" : "alterno_id"),
                permite ? "Correo" : rs.getString("tipo"), rs.getString(permite ? "referencia" : "alterno"),
                rs.getString("referencia"), permite,
                !competencia ? "Competencia propuesta, pendiente de validación. El correo no atiende emergencias."
                    : permite ? "El envío no equivale a entrega ni a radicación oficial."
                    : "Utiliza el canal oficial. No se enviará correo automáticamente ni se simulará radicación.");
        });
    }

    public Ruta resolver(int categoria, boolean emergencia) {
        Ruta ruta = listar().stream().filter(r -> r.idCategoria() == categoria).findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", "Categoría sin ruta disponible", "idCategoria"));
        if (!emergencia) return ruta;
        // Política genérica: ninguna emergencia se tramita por correo. Gas utiliza su canal 164 de la entidad.
        return new Ruta(ruta.idCategoria(), ruta.codigo(), ruta.categoria(), ruta.entidad(), ruta.dependencia(),
            ruta.requiereUbicacion(), ruta.admiteMatricula(), ruta.esGas(), ruta.competenciaConfirmada(),
            ruta.revisionClasificacion(), ruta.esGas() ? canalEmergencia(ruta.idCategoria()) : alterno(ruta.idCategoria()),
            ruta.esGas() ? "Teléfono" : "Canal oficial", ruta.esGas() ? "164" : accesoAlterno(ruta.idCategoria()),
            ruta.correoReferencia(), false, ruta.esGas() ? "Emergencia de gas: llama al 164"
                : "El correo no es un canal de atención inmediata. Usa los canales de emergencia oficiales.");
    }
    private Integer canalEmergencia(int id) { return jdbc.queryForObject("SELECT id_canal_emergencia FROM configuracion_enrutamiento WHERE id_categoria=?", Integer.class, id); }
    private Integer alterno(int id) { return jdbc.queryForObject("SELECT id_canal_alterno FROM configuracion_enrutamiento WHERE id_categoria=?", Integer.class, id); }
    private String accesoAlterno(int id) { return jdbc.queryForObject("SELECT a.acceso FROM configuracion_enrutamiento x JOIN canal_atencion a ON a.id_canal_atencion=x.id_canal_alterno WHERE x.id_categoria=?", String.class, id); }
}
