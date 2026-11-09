package com.notiurba.qa;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.notiurba.NotiUrbaApplication;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.foto.AlmacenFotosNoDisponibleException;
import com.notiurba.foto.FotoGuardada;

/** Arranque manual de pruebas: no se empaqueta ni se descubre en la aplicacion productiva. */
public final class Not71ServidorQA {
    private static final String JDBC = "jdbc:postgresql://127.0.0.1:15435/notiurba_barrios_qa";

    public static void main(String[] args) throws Exception {
        if (!JDBC.equals(System.getenv("DB_URL")) || !"qa_barrios".equals(System.getenv("DB_USER")))
            throw new IllegalStateException("Solo se permite la base QA local designada");
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) throw new IllegalStateException("Falta clave QA");
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("server.address", "127.0.0.1"); p.put("server.port", "18092");
        p.put("spring.datasource.url", JDBC); p.put("spring.datasource.username", "qa_barrios");
        p.put("spring.datasource.password", password);
        p.put("spring.jpa.hibernate.ddl-auto", "validate"); p.put("spring.jpa.open-in-view", "false");
        // La autenticacion real es JWT propia; no generar/loguear una clave Basic inutilizada.
        p.put("spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration");
        p.put("spring.flyway.enabled", "false"); // Reutiliza esquema existente; nunca aplica V17.
        p.put("notiurba.diagnostics.enabled", "false"); p.put("notiurba.diagnostics.photos-enabled", "false");
        p.put("notiurba.correo.reportes-habilitados", "false");
        p.put("notiurba.correo.destino-prueba", ""); p.put("notiurba.correo.webhook-secret", "");
        p.put("notiurba.correo.resend.url", "http://127.0.0.1:9");
        p.put("notiurba.correo.resend.api-key", ""); p.put("notiurba.correo.resend.remitente", "qa@example.test");
        p.put("notiurba.fotos.cloudinary.url", "http://127.0.0.1:9");
        p.put("notiurba.fotos.cloudinary.cloud-name", "qa"); p.put("notiurba.fotos.cloudinary.api-key", "");
        p.put("notiurba.fotos.cloudinary.api-secret", ""); p.put("notiurba.fotos.cloudinary.carpeta", "qa");
        p.put("notiurba.cors.allowed-origins", "http://127.0.0.1:5176");
        byte[] secret = new byte[48]; new SecureRandom().nextBytes(secret);
        p.put("notiurba.jwt.secreto", Base64.getEncoder().encodeToString(secret));
        p.put("notiurba.jwt.expiracion-minutos", "60");
        p.put("notiurba.google.client-id", "qa.apps.googleusercontent.com");
        p.put("notiurba.verificacion.enlace", "http://127.0.0.1:5176/verificar-correo");
        p.put("notiurba.recuperacion.enlace", "http://127.0.0.1:5176/nueva-contrasena");
        p.put("notiurba.recuperacion.mostrar-enlace-en-log", "false");
        p.put("spring.servlet.multipart.max-file-size", "1MB");
        p.put("spring.servlet.multipart.max-request-size", "2MB");
        Path temporary = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path root = Path.of(System.getenv("NOT71_QA_STORAGE_DIR")).toAbsolutePath().normalize();
        if (!root.startsWith(temporary) || !root.getFileName().toString().startsWith("not71-fotos-"))
            throw new IllegalStateException("El almacen debe ser temporal y exclusivo de NOT71");
        // LoggingApplicationListener se ejecuta antes de los ApplicationContextInitializer.
        System.setProperty("logging.file.name", root.resolve("backend.log").toString());
        // FileAppender sin rotacion: el RollingFileAppender por defecto falla con '{' en rutas sandbox.
        Path logConfig = root.resolve("logback-qa.xml");
        Files.writeString(logConfig, """
            <configuration>
              <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
                <encoder><pattern>%d{ISO8601} %-5level %logger{40} - %msg%n</pattern></encoder>
              </appender>
              <appender name="FILE" class="ch.qos.logback.core.FileAppender">
                <file>${LOG_FILE}</file><append>true</append>
                <encoder><pattern>%d{ISO8601} %-5level %logger{40} - %msg%n</pattern></encoder>
              </appender>
              <root level="INFO"><appender-ref ref="CONSOLE"/><appender-ref ref="FILE"/></root>
            </configuration>
            """);
        System.setProperty("logging.config", logConfig.toUri().toString());
        p.put("logging.file.name", root.resolve("backend.log").toString());
        SpringApplication app = new SpringApplication(NotiUrbaApplication.class, Local.class);
        app.setAddCommandLineProperties(false);
        // Prioridad maxima: variables ambientales o application.yml no pueden apuntar a proveedores reales.
        app.addInitializers(ctx -> ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("not71QaFija", p)));
        app.run();
    }

    // Fuente explicita con @Bean, sin anotaciones test ausentes del JAR ejecutable.
    // El launcher copia solo estas clases; nunca agrega target/test-classes completo.
    public static class Local {
        @Bean @Primary
        LocalPhotoStore almacenFotosNot71() throws Exception {
            return new LocalPhotoStore(Path.of(System.getenv("NOT71_QA_STORAGE_DIR")));
        }
        @Bean
        ApplicationRunner comprobarBaseNot71(JdbcTemplate jdbc) {
            return args -> {
                if (!"notiurba_barrios_qa".equals(jdbc.queryForObject("SELECT current_database()", String.class))
                    || !"qa_barrios".equals(jdbc.queryForObject("SELECT current_user", String.class)))
                    throw new IllegalStateException("Identidad SQL fuera de QA");
                Integer count = jdbc.queryForObject("SELECT count(*) FROM usuario WHERE correo=? AND numero_documento=? AND correo_verificado_en IS NOT NULL",
                    Integer.class, "qa.not71.20261108.2147@example.test", "9711082147");
                if (count == null || count != 1) throw new IllegalStateException("Falta la cuenta QA verificada");
                Integer reports = jdbc.queryForObject("SELECT count(*) FROM reporte r JOIN usuario u USING(id_usuario) WHERE r.codigo_seguimiento=? AND u.correo=?",
                    Integer.class, "NU-000001", "qa.not71.20261108.2147@example.test");
                if (reports == null || reports != 1) throw new IllegalStateException("Falta NU-000001 de la cuenta QA existente");
                Files.writeString(Path.of(System.getenv("NOT71_QA_STORAGE_DIR")).resolve("ready.json"),
                    "{\"database\":\"notiurba_barrios_qa\",\"sqlUser\":\"qa_barrios\",\"report\":\"NU-000001\",\"accountVerified\":true,\"storage\":\"local\",\"mailEnabled\":false,\"flywayEnabled\":false}");
                System.out.println("NOT71_QA_LOCAL_READY: base aislada y NU-000001 existentes, archivos locales, correo deshabilitado");
            };
        }
    }

    /** Archivo real + HTTP de lectura local; no intercepta ni simula los endpoints Spring. */
    public static final class LocalPhotoStore implements AlmacenFotos, AutoCloseable {
        private final Path root;
        private final HttpServer server;
        LocalPhotoStore(Path directory) throws Exception {
            this(directory, 19027);
        }
        LocalPhotoStore(Path directory, int port) throws Exception {
            root = directory.toAbsolutePath().normalize(); Files.createDirectories(root);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
            server.createContext("/fotos/", exchange -> {
                try {
                    String name = exchange.getRequestURI().getPath().substring("/fotos/".length());
                    if (!"GET".equals(exchange.getRequestMethod()) || !name.matches("[a-f0-9]{64}\\.(png|jpg)")) {
                        exchange.sendResponseHeaders(404,-1); return;
                    }
                    Path file = root.resolve(name);
                    if (!Files.isRegularFile(file)) { exchange.sendResponseHeaders(404,-1); return; }
                    byte[] body = Files.readAllBytes(file);
                    exchange.getResponseHeaders().set("Content-Type",name.endsWith(".png")?"image/png":"image/jpeg");
                    exchange.getResponseHeaders().set("X-Content-Type-Options","nosniff");
                    exchange.sendResponseHeaders(200,body.length);
                    exchange.getResponseBody().write(body);
                } finally { exchange.close(); }
            });
            server.start();
        }
        @Override public FotoGuardada guardar(byte[] bytes,String mime) {
            try {
                String extension = switch(mime) { case "image/png" -> ".png"; case "image/jpeg" -> ".jpg"; default -> throw new IllegalArgumentException("Formato no admitido"); };
                String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                String name = hash+extension;
                if (!Files.exists(root.resolve(name))) Files.write(root.resolve(name),bytes,StandardOpenOption.CREATE_NEW);
                Files.writeString(root.resolve("eventos.jsonl"),"{\"sha256\":\""+hash+"\",\"bytes\":"+bytes.length+",\"mime\":\""+mime+"\"}\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                return new FotoGuardada("http://127.0.0.1:"+server.getAddress().getPort()+"/fotos/"+name,"not71-qa/"+name);
            } catch(Exception e) { throw new AlmacenFotosNoDisponibleException("Almacen de archivos QA no disponible",e); }
        }
        @Override public void borrar(String id) { throw new UnsupportedOperationException("Este QA conserva los archivos para evidencia"); }
        @Override public void close() { server.stop(0); }
    }
}
