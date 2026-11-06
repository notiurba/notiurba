package com.notiurba.geo;

import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Comuna de un punto GPS (RF-08, RN-02, RN-03). Las reglas viven en PostGIS (fn_asignar_comuna, V13)
 * y no aquí: la vista v_reporte_comuna usa la misma función, así que lo que se decide al crear el
 * reporte y lo que se lee después no pueden discrepar.
 */
@Repository
public class AsignadorDeComuna {

    /** regla: «contiene», «solape» (la de menor número) o «cercana» (punto en un hueco, dentro de la distancia máxima). */
    public record Asignacion(int idComuna, String regla, double distanciaMetros) {
    }

    /**
     * capaCargada: al menos una comuna tiene polígono. Si no hay ninguno (comuna.geom sigue en NULL)
     * no se puede decidir nada: ni asignar ni rechazar.
     */
    public record Resultado(boolean capaCargada, Optional<Asignacion> asignacion) {

        /** Hay capa y el punto no cae en ninguna comuna ni a menos de la distancia máxima de una. */
        public boolean fueraDeCobertura() {
            return capaCargada && asignacion.isEmpty();
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    public AsignadorDeComuna(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Una sola consulta: si hay capa cargada y la comuna que le toca al punto. */
    public Resultado asignar(double latitud, double longitud) {
        return jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM comuna WHERE geom IS NOT NULL) AS capa_cargada,
                       a.id_comuna, a.regla, a.distancia_m
                  FROM (SELECT ST_SetSRID(ST_MakePoint(CAST(:lon AS double precision),
                                                       CAST(:lat AS double precision)), 4326) AS punto) p
                  LEFT JOIN LATERAL fn_asignar_comuna(p.punto) a ON TRUE""",
                new MapSqlParameterSource().addValue("lat", latitud).addValue("lon", longitud),
                (rs, i) -> {
                    Integer id = (Integer) rs.getObject("id_comuna");
                    Optional<Asignacion> asignacion = id == null
                            ? Optional.empty()
                            : Optional.of(new Asignacion(id, rs.getString("regla"), rs.getDouble("distancia_m")));
                    return new Resultado(rs.getBoolean("capa_cargada"), asignacion);
                });
    }
}
