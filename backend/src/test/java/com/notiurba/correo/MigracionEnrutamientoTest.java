package com.notiurba.correo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import java.util.UUID;

/** Ensaya actualización V12 -> V13 con intentos históricos; jamás usa la BD de la aplicación. */
@EnabledIfEnvironmentVariable(named="NOTIURBA_TEST_DB_URL",matches="jdbc:postgresql:.*")
class MigracionEnrutamientoTest {
    @Test void conservaPendientesHistoricosYElTriggerSincronizaReporte() {
        String url=System.getenv("NOTIURBA_TEST_DB_URL");
        if(!url.matches(".*[/]notiurba_test[^/?]*(\\?.*)?")) throw new IllegalArgumentException("Solo una BD desechable notiurba_test...");
        var admin=new DriverManagerDataSource(url,System.getenv("NOTIURBA_TEST_DB_USER"),System.getenv("NOTIURBA_TEST_DB_PASSWORD"));
        var control=new JdbcTemplate(admin);
        control.execute("CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public");
        control.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public");
        String schema="routing_upgrade_"+UUID.randomUUID().toString().replace("-", "");
        control.execute("CREATE SCHEMA "+schema);
        String urlPruebas=url+(url.contains("?")?"&":"?")+"currentSchema="+schema+",public";
        var ds=new DriverManagerDataSource(urlPruebas,System.getenv("NOTIURBA_TEST_DB_USER"),System.getenv("NOTIURBA_TEST_DB_PASSWORD"));
        try {
            Flyway.configure().dataSource(ds).defaultSchema(schema).schemas(schema)
                .placeholders(Map.of("adminPassword","PruebaSintetica2026")).target("12").load().migrate();
            var jdbc=new JdbcTemplate(ds);
            long reporte=jdbc.queryForObject("""
                INSERT INTO reporte(id_usuario,id_categoria,id_barrio,descripcion,autorizacion_envio_en)
                SELECT id_usuario,1,id_barrio,'Reporte histórico sintético para la migración',now() FROM usuario LIMIT 1 RETURNING id_reporte
                """,Long.class);
            for(int i=0;i<2;i++) jdbc.update("""
                INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio)
                VALUES (?,1,1,1)
                """,reporte);
            var flyway=Flyway.configure().dataSource(ds).defaultSchema(schema).schemas(schema)
                .placeholders(Map.of("adminPassword","PruebaSintetica2026")).load();
            flyway.migrate();flyway.validate();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_envio_correo WHERE id_reporte=?",Integer.class,reporte)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM configuracion_enrutamiento",Integer.class)).isEqualTo(14);
            assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_trigger WHERE tgrelid='historial_estado'::regclass
                 AND tgname='tg_historial_sincroniza' AND tgenabled='O'
                """,Integer.class)).isEqualTo(1);
            jdbc.update("INSERT INTO historial_estado(id_reporte,id_estado_reporte,observacion) VALUES (?,2,'Aceptación simulada')",reporte);
            assertThat(jdbc.queryForObject("SELECT id_estado_reporte FROM reporte WHERE id_reporte=?",Integer.class,reporte)).isEqualTo(2);
            // Dos intentos NUEVOS pendientes del mismo reporte siguen prohibidos.
            jdbc.update("""
                INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio,clave_idempotencia)
                VALUES (?,1,1,1,'clave-sintetica')
                """,reporte);
            assertThatThrownBy(()->jdbc.update("""
                INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio,clave_idempotencia)
                VALUES (?,1,1,1,'otra-clave-sintetica')
                """,reporte)).isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        } finally {
            // schema es un identificador generado localmente, exclusivamente en la BD de pruebas validada arriba.
            control.execute("DROP SCHEMA "+schema+" CASCADE");
        }
    }
}
