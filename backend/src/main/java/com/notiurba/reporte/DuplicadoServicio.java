package com.notiurba.reporte;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.notiurba.admin.ParametrosServicio;
import com.notiurba.config.RelojConfig;

/**
 * Reportes parecidos: de cualquier cuenta, de la misma categoría, a menos de la distancia de esa categoría y de
 * los últimos días configurados. Un reporte «Resuelto» no cuenta como duplicado (si el problema volvió, la persona
 * debe poder reportarlo) pero obliga a adjuntar una foto. Sin GPS o sin distancia para la categoría no se revisa.
 */
@Service
public class DuplicadoServicio {

    static final int LARGO_RESUMEN = 140;

    /**
     * Lo único que se muestra de un reporte parecido: cuántos hay y los datos base del que se elige (el propio, si
     * hay uno; si no, el más reciente). El código solo viaja si es de la misma persona.
     */
    public record Parecido(int cantidad, boolean propio, String codigo, String estado, String fecha, String resumen) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ParametrosServicio parametros;
    private final Clock reloj;

    public DuplicadoServicio(NamedParameterJdbcTemplate jdbc, ParametrosServicio parametros, Clock reloj) {
        this.jdbc = jdbc;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    public Optional<Parecido> buscarSinResolver(long idUsuario, int idCategoria, Double latitud, Double longitud) {
        Optional<Integer> metros = distancia(idCategoria, latitud, longitud);
        if (metros.isEmpty()) {
            return Optional.empty();
        }
        OffsetDateTime desde = OffsetDateTime.now(reloj).minusDays(parametros.ventanaDuplicadoDias());
        List<Parecido> filas = jdbc.query("""
                SELECT r.id_usuario = :usuario AS propio, r.codigo_seguimiento, est.nombre AS estado, r.creado_en,
                       r.descripcion, count(*) OVER () AS cantidad
                  FROM reporte r
                  JOIN estado_reporte est ON est.id_estado_reporte = r.id_estado_reporte
                 WHERE r.id_categoria = :categoria AND r.ubicacion IS NOT NULL
                   AND r.ubicacion && ST_Expand(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :caja)
                   AND ST_DWithin(r.ubicacion::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :metros)
                   AND est.nombre <> 'Resuelto' AND r.creado_en >= :desde
                 ORDER BY (r.id_usuario = :usuario) DESC, r.creado_en DESC
                 LIMIT 1""", parametros(idCategoria, latitud, longitud, metros.get(), desde).addValue("usuario", idUsuario),
                (rs, i) -> new Parecido(rs.getInt("cantidad"), rs.getBoolean("propio"),
                        rs.getBoolean("propio") ? rs.getString("codigo_seguimiento") : null, rs.getString("estado"),
                        rs.getObject("creado_en", OffsetDateTime.class).atZoneSameInstant(RelojConfig.ZONA_NEGOCIO).toLocalDate().toString(),
                        resumir(rs.getString("descripcion"))));
        return filas.stream().findFirst();
    }

    /** Hay un reporte «Resuelto» de cualquier cuenta cerca, resuelto dentro de los últimos días configurados. */
    public boolean repiteResuelto(int idCategoria, Double latitud, Double longitud) {
        Optional<Integer> metros = distancia(idCategoria, latitud, longitud);
        if (metros.isEmpty()) {
            return false;
        }
        OffsetDateTime desde = OffsetDateTime.now(reloj).minusDays(parametros.ventanaDuplicadoDias());
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                  SELECT 1 FROM reporte r
                  JOIN estado_reporte est ON est.id_estado_reporte = r.id_estado_reporte
                 WHERE r.id_categoria = :categoria AND r.ubicacion IS NOT NULL AND est.nombre = 'Resuelto'
                   AND r.ubicacion && ST_Expand(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), :caja)
                   AND ST_DWithin(r.ubicacion::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :metros)
                   AND COALESCE((SELECT max(h.cambiado_en) FROM historial_estado h
                                  WHERE h.id_reporte = r.id_reporte AND h.id_estado_reporte = r.id_estado_reporte),
                                r.creado_en) >= :desde)""",
                parametros(idCategoria, latitud, longitud, metros.get(), desde), Boolean.class));
    }

    private Optional<Integer> distancia(int idCategoria, Double latitud, Double longitud) {
        if (latitud == null || longitud == null) {
            return Optional.empty();
        }
        return parametros.distanciaDuplicado(idCategoria);
    }

    /** La caja usa grados de sobra (1 m son menos de 0,00001°) para que el índice espacial descarte lo lejano. */
    private static MapSqlParameterSource parametros(int categoria, double latitud, double longitud, int metros, OffsetDateTime desde) {
        return new MapSqlParameterSource().addValue("categoria", categoria).addValue("lat", latitud).addValue("lon", longitud)
                .addValue("metros", metros).addValue("caja", metros / 100_000.0).addValue("desde", desde);
    }

    /** Una línea de hasta 140 caracteres, cortada en una palabra completa y con «…» si se acortó. */
    static String resumir(String descripcion) {
        String limpio = descripcion == null ? "" : descripcion.replaceAll("\\s+", " ").trim();
        if (limpio.length() <= LARGO_RESUMEN) {
            return limpio;
        }
        int corte = limpio.lastIndexOf(' ', LARGO_RESUMEN);
        String recorte = limpio.substring(0, corte > 0 ? corte : LARGO_RESUMEN).replaceAll("[\\s,.;:]+$", "");
        return recorte + "…";
    }
}
