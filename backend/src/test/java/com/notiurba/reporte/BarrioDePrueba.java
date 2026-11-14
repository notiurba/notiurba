package com.notiurba.reporte;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Barrio normal para las pruebas de integración. El único barrio que traen las migraciones es el
 * del administrador, que es interno (V15) y no se puede elegir al registrarse ni al reportar.
 */
public final class BarrioDePrueba {

    private BarrioDePrueba() {
    }

    public static int id(JdbcTemplate jdbc) {
        return jdbc.queryForObject("""
                INSERT INTO barrio (id_comuna, nombre) VALUES (1, 'Barrio de prueba')
                ON CONFLICT (id_comuna, nombre) DO UPDATE SET nombre = EXCLUDED.nombre
                RETURNING id_barrio""", Integer.class);
    }
}
