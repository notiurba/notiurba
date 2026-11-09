package com.notiurba.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.notiurba.NotiUrbaApplication;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.foto.AlmacenFotosNoDisponibleException;
import com.notiurba.foto.FotoGuardada;

/** Harness opt-in: servidor real y almacenamiento local solo contra PostgreSQL desechable. */
public final class Not25ServidorQA {
    public static void main(String[] args) {
        if (!"jdbc:postgresql://127.0.0.1:55425/not25_qa".equals(System.getenv("DB_URL"))) {
            throw new IllegalStateException("NOT25 QA solo admite la base local desechable designada");
        }
        SpringApplication.run(new Class<?>[] { NotiUrbaApplication.class, Local.class }, args);
    }

    @TestConfiguration
    public static class Local {
        @Bean
        @Primary
        AlmacenFotos almacenNot25Local() throws Exception {
            Path root = Path.of(System.getenv("NOT25_STORAGE_DIR"));
            Files.createDirectories(root);
            AtomicInteger attempts = new AtomicInteger();
            return new AlmacenFotos() {
                @Override
                public FotoGuardada guardar(byte[] bytes, String mime) {
                    try {
                        int attempt = attempts.incrementAndGet();
                        Files.writeString(root.resolve("eventos.jsonl"), "{\"attempt\":" + attempt + ",\"phase\":\"started\",\"bytes\":" + bytes.length + "}\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        // Retardo real y fallo controlado del primer intento: permite observar la UI pendiente.
                        Thread.sleep(attempt == 1 ? 10000 : 1200);
                        if (attempt == 1) {
                            throw new AlmacenFotosNoDisponibleException("Fallo de almacenamiento provocado solo en QA");
                        }
                        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                        Path destination = root.resolve(hash + ".jpg");
                        if (!Files.exists(destination)) Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
                        Files.writeString(root.resolve("eventos.jsonl"), "{\"attempt\":" + attempt + ",\"phase\":\"stored\",\"sha256\":\"" + hash + "\"}\n", StandardOpenOption.APPEND);
                        return new FotoGuardada("http://127.0.0.1:19026/fotos/" + hash + ".jpg", "not25-qa/" + hash);
                    } catch (AlmacenFotosNoDisponibleException e) { throw e; }
                    catch (Exception e) { throw new AlmacenFotosNoDisponibleException("Almacen local de QA no disponible", e); }
                }
                @Override
                public void borrar(String id) {
                    throw new UnsupportedOperationException("Este harness no permite borrar archivos");
                }
            };
        }

        @Bean
        ApplicationRunner datosSinteticosNot25(JdbcTemplate jdbc, PasswordEncoder encoder) {
            return args -> {
                int barrio = jdbc.queryForObject("INSERT INTO barrio(id_comuna,nombre) VALUES(1,'Barrio tecnico NOT25, solo QA aislado') ON CONFLICT(id_comuna,nombre) DO UPDATE SET nombre=EXCLUDED.nombre RETURNING id_barrio", Integer.class);
                int aviso = jdbc.queryForObject("SELECT id_aviso_privacidad FROM aviso_privacidad WHERE vigente_desde<=now() ORDER BY vigente_desde DESC LIMIT 1", Integer.class);
                jdbc.update("""
                    INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,
                        direccion_correspondencia,correo,correo_verificado_en,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en)
                    VALUES('Ciudadano de prueba','NOT25',1,'9000000025',?,'Entorno aislado de QA','not25@qa.invalid',now(),?,1,?,now())
                    """, barrio, encoder.encode("SoloPruebasNOT25-123"), aviso);
                System.out.println("NOT25_QA_READY: usuario y barrio sinteticos preparados; ningun dato de produccion");
            };
        }
    }
}
