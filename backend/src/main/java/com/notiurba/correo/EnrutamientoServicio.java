package com.notiurba.correo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
            boolean permiteCorreo, String mensaje, List<Canal> canales) { }

    /** Canal de la entidad distinto del correo, en el orden en que se recomienda. */
    public record Canal(String tipo, String acceso, String finalidad, String horario, String detalle) { }

    static final String CANALES = """
        SELECT k.id_entidad_destino, t.nombre AS tipo, k.acceso, f.nombre AS finalidad, k.horario, k.detalle
        FROM canal_atencion k JOIN tipo_canal t USING(id_tipo_canal) JOIN finalidad_canal f USING(id_finalidad_canal)
        WHERE k.activo AND t.nombre<>'Correo' AND k.id_entidad_destino IS NOT NULL
        ORDER BY k.id_entidad_destino, k.orden, k.id_canal_atencion
        """;

    static final String CONSULTA = """
        SELECT c.id_categoria, x.codigo, e.id_entidad_destino, c.nombre AS categoria, e.nombre AS entidad, d.nombre AS dependencia,
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
        Map<Integer, List<Canal>> canales = canalesPorEntidad();
        return jdbc.query(CONSULTA + " ORDER BY x.codigo", (rs, i) -> {
            boolean permite = rs.getBoolean("permite");
            boolean competencia = rs.getBoolean("competencia_confirmada");
            return new Ruta(rs.getInt("id_categoria"), rs.getString("codigo"), rs.getString("categoria"),
                rs.getString("entidad"), rs.getString("dependencia"), rs.getBoolean("requiere_ubicacion"),
                rs.getBoolean("admite_matricula"), rs.getBoolean("es_gas"), competencia,
                rs.getBoolean("revision_clasificacion"), rs.getInt(permite ? "correo_id" : "alterno_id"),
                permite ? "Correo" : rs.getString("tipo"), rs.getString(permite ? "referencia" : "alterno"),
                rs.getString("referencia"), permite,
                !competencia ? "Todavía confirmamos qué entidad atiende este tipo de reportes. El correo no es para emergencias."
                    : permite ? "Enviar el reporte no asegura que lo lean de inmediato; el radicado oficial lo asigna la entidad."
                    : "Esta entidad atiende por otro canal. Use el que se indica aquí; NotiUrba no le enviará correo.",
                canales.getOrDefault(rs.getInt("id_entidad_destino"), List.of()));
        });
    }

    private Map<Integer, List<Canal>> canalesPorEntidad() {
        return jdbc.query(CANALES, (rs, i) -> Map.entry(rs.getInt("id_entidad_destino"), new Canal(rs.getString("tipo"),
                rs.getString("acceso"), rs.getString("finalidad"), rs.getString("horario"), rs.getString("detalle"))))
            .stream().collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableList())));
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
            ruta.correoReferencia(), false, ruta.esGas() ? "Emergencia de gas: llame al 164"
                : "Para una emergencia, llame al canal de emergencias: el correo no es inmediato.", ruta.canales());
    }
    private Integer canalEmergencia(int id) { return jdbc.queryForObject("SELECT id_canal_emergencia FROM configuracion_enrutamiento WHERE id_categoria=?", Integer.class, id); }
    private Integer alterno(int id) { return jdbc.queryForObject("SELECT id_canal_alterno FROM configuracion_enrutamiento WHERE id_categoria=?", Integer.class, id); }
    private String accesoAlterno(int id) { return jdbc.queryForObject("SELECT a.acceso FROM configuracion_enrutamiento x JOIN canal_atencion a ON a.id_canal_atencion=x.id_canal_alterno WHERE x.id_categoria=?", String.class, id); }
}
