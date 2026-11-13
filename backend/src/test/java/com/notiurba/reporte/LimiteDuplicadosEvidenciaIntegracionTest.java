package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.NotiUrbaApplication;
import com.notiurba.config.RelojConfig;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.seguridad.JwtProveedor;

/** Tope diario por cuenta, reportes parecidos de cualquier cuenta y evidencia cuando se repite un problema ya resuelto. */
@SpringBootTest(classes = NotiUrbaApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
@Import(LimiteDuplicadosEvidenciaIntegracionTest.Config.class)
class LimiteDuplicadosEvidenciaIntegracionTest {

    /** Reloj que sigue al real pero se puede adelantar (para probar el cambio de día sin esperar). */
    static class RelojDesplazable extends Clock {
        private final AtomicReference<Duration> desfase = new AtomicReference<>(Duration.ZERO);

        void desplazar(Duration d) {
            desfase.set(d);
        }

        @Override public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override public Clock withZone(ZoneId zone) {
            return Clock.offset(Clock.system(zone), desfase.get());
        }

        @Override public Instant instant() {
            return Instant.now().plus(desfase.get());
        }
    }

    @TestConfiguration
    static class Config {
        @Bean @Primary RelojDesplazable relojDePrueba() {
            return new RelojDesplazable();
        }
    }

    private static BaseDeDatosTemporal bd;

    @BeforeAll
    static void crearBase() {
        bd = BaseDeDatosTemporal.crearOOmitir();
    }

    @AfterAll
    static void borrarBase() throws SQLException {
        if (bd != null) {
            bd.borrar();
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        bd.registrar(registro);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtProveedor jwt;
    @Autowired RelojDesplazable reloj;
    @Autowired DuplicadoServicio servicioDuplicados;
    @MockBean EnviadorCorreo correoExterno;
    @MockBean AlmacenFotos fotosExternas;

    private static final double LAT = 4.0867;
    private static final double LON = -76.1950;
    private static final double METRO = 1 / 111_320.0;

    private int barrio;
    private int contador;

    @BeforeEach
    void preparar() {
        reloj.desplazar(Duration.ZERO);
        barrio = BarrioDePrueba.id(jdbc);
        jdbc.update("UPDATE configuracion SET valor = '2' WHERE clave = 'reportes_maximos_por_dia'");
        jdbc.update("UPDATE configuracion SET valor = '30' WHERE clave = 'duplicado_ventana_dias'");
        jdbc.update("UPDATE distancia_duplicado SET metros = 5 WHERE id_categoria = 1");
        jdbc.update("UPDATE distancia_duplicado SET metros = 10 WHERE id_categoria = 2");
        jdbc.update("DELETE FROM reporte");
    }

    private record Persona(long id, String correo, String token) {
    }

    private Persona persona(String nombre) {
        String correo = nombre + (++contador) + "-" + System.nanoTime() + "@example.test";
        long id = DatosDePrueba.usuario(jdbc, barrio, correo, DatosDePrueba.CIUDADANO);
        return new Persona(id, correo, "Bearer " + jwt.generar(correo, "CIUDADANO"));
    }

    private static double norte(double metros) {
        return LAT + metros * METRO;
    }

    private record Respuesta(int estado, String cuerpo) {
        boolean contiene(String texto) {
            return cuerpo.contains(texto);
        }
    }

    private Respuesta crear(Persona p, int categoria, double latitud, double longitud, String extra) throws Exception {
        String cuerpo = """
                {"idCategoria":%d,"latitud":%s,"longitud":%s,"descripcion":"Hay un hueco enorme frente a la panadería de la esquina",
                 "autorizaEnvio":true%s}""".formatted(categoria, latitud, longitud, extra);
        var r = mvc.perform(post("/api/reportes").header("Authorization", p.token()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn().getResponse();
        return new Respuesta(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private Respuesta crear(Persona p, int categoria, double latitud, String extra) throws Exception {
        return crear(p, categoria, latitud, LON, extra);
    }

    private Respuesta crear(Persona p, int categoria, double latitud) throws Exception {
        return crear(p, categoria, latitud, LON, "");
    }

    /** Un reporte ya guardado (de cualquier cuenta), hace {@code diasAtras} días, en el punto indicado. */
    private DatosDePrueba.Reporte existente(Persona p, int categoria, double latitud, int diasAtras, String descripcion) {
        return jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, direccion_incidente, ubicacion,
                                     autorizacion_envio_en, creado_en)
                VALUES (?, ?, ?, ?, 'Cra 5 # 10-20', ST_SetSRID(ST_MakePoint(?, ?), 4326),
                        now() - make_interval(days => ?), now() - make_interval(days => ?))
                RETURNING id_reporte, codigo_seguimiento""",
                (rs, i) -> new DatosDePrueba.Reporte(rs.getLong(1), rs.getString(2)),
                p.id(), categoria, barrio, descripcion, LON, latitud, diasAtras, diasAtras);
    }

    private DatosDePrueba.Reporte existente(Persona p, int categoria, double latitud, int diasAtras) {
        return existente(p, categoria, latitud, diasAtras, "Hueco grande frente a la panadería");
    }

    private void resolver(DatosDePrueba.Reporte r, int diasAtras) {
        jdbc.update("""
                INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion, cambiado_en)
                SELECT ?, id_estado_reporte, 'Resuelto de prueba', now() - make_interval(days => ?)
                  FROM estado_reporte WHERE nombre = 'Resuelto'""", r.id(), diasAtras);
    }

    private int totalDe(Persona p) {
        return jdbc.queryForObject("SELECT count(*) FROM reporte WHERE id_usuario = ?", Integer.class, p.id());
    }

    // ---------------------------------------------------------------- límite diario

    @Test
    void elTercerReporteDelDiaSeRechazaYNoSeGuarda() throws Exception {
        var ana = persona("ana");
        assertThat(crear(ana, 1, norte(0)).estado()).isEqualTo(201);
        assertThat(crear(ana, 1, norte(300)).estado()).isEqualTo(201);

        var tercero = crear(ana, 1, norte(600));

        assertThat(tercero.estado()).isEqualTo(429);
        assertThat(tercero.contiene("LIMITE_DIARIO_REPORTES")).isTrue();
        assertThat(tercero.contiene("\"maximo\":2")).isTrue();
        assertThat(tercero.contiene("Hoy ya enviaste 2 reportes")).isTrue();
        assertThat(tercero.cuerpo()).containsPattern("\"vuelveEn\":\"\\d{4}-\\d{2}-\\d{2}T00:00(:00)?-05:00\"");
        assertThat(totalDe(ana)).isEqualTo(2);
    }

    @Test
    void elTopeEsPorCuentaYOtraPersonaTieneSuCupoCompleto() throws Exception {
        var ana = persona("ana");
        var luis = persona("luis");
        crear(ana, 1, norte(0));
        crear(ana, 1, norte(300));
        assertThat(crear(ana, 1, norte(600)).estado()).isEqualTo(429);

        assertThat(crear(luis, 1, norte(900)).estado()).isEqualTo(201);
        assertThat(crear(luis, 1, norte(1200)).estado()).isEqualTo(201);
        assertThat(crear(luis, 1, norte(1500)).estado()).isEqualTo(429);
    }

    @Test
    void elAdministradorCambiaElTopeYAplicaDeInmediato() throws Exception {
        var ana = persona("ana");
        crear(ana, 1, norte(0));
        crear(ana, 1, norte(300));
        assertThat(crear(ana, 1, norte(600)).estado()).isEqualTo(429);

        jdbc.update("UPDATE configuracion SET valor = '3' WHERE clave = 'reportes_maximos_por_dia'");

        assertThat(crear(ana, 1, norte(600)).estado()).isEqualTo(201);
        assertThat(crear(ana, 1, norte(900)).estado()).isEqualTo(429);
    }

    @Test
    void alDiaSiguienteEnHoraDeColombiaElCupoEmpiezaDeCero() throws Exception {
        var ana = persona("ana");
        crear(ana, 1, norte(0));
        crear(ana, 1, norte(300));
        assertThat(crear(ana, 1, norte(600)).estado()).isEqualTo(429);

        reloj.desplazar(Duration.ofHours(24));

        assertThat(crear(ana, 1, norte(600)).estado()).isEqualTo(201);
    }

    @Test
    void laVistaPreviaNoCuentaComoReporte() throws Exception {
        var ana = persona("ana");
        for (int i = 0; i < 5; i++) {
            var previa = mvc.perform(post("/api/reportes/vista-previa").header("Authorization", ana.token()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idCategoria\":1,\"latitud\":" + norte(0) + ",\"longitud\":" + LON
                            + ",\"descripcion\":\"Hay un hueco enorme frente a la panadería\",\"totalFotos\":0}"))
                    .andReturn().getResponse();
            assertThat(previa.getStatus()).isEqualTo(200);
        }
        assertThat(crear(ana, 1, norte(0)).estado()).isEqualTo(201);
        assertThat(crear(ana, 1, norte(300)).estado()).isEqualTo(201);
    }

    @Test
    void unEnvioRechazadoPorParecidoNoCuentaContraElTope() throws Exception {
        var ana = persona("ana");
        assertThat(crear(ana, 1, norte(0)).estado()).isEqualTo(201);
        assertThat(crear(ana, 1, norte(2)).estado()).isEqualTo(409);

        assertThat(crear(ana, 1, norte(300)).estado()).isEqualTo(201);
        assertThat(crear(ana, 1, norte(600)).estado()).isEqualTo(429);
    }

    @Test
    void dosEnviosALaVezNoDejanPasarElTope() throws Exception {
        var ana = persona("ana");
        crear(ana, 1, norte(0));
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<Respuesta>> resultados = new ArrayList<>();
        resultados.add(hilos.submit(() -> crear(ana, 1, norte(300))));
        resultados.add(hilos.submit(() -> crear(ana, 1, norte(600))));
        int creados = 0;
        int rechazados = 0;
        for (var f : resultados) {
            int estado = f.get().estado();
            if (estado == 201) creados++;
            if (estado == 429) rechazados++;
        }
        hilos.shutdown();
        assertThat(creados).isEqualTo(1);
        assertThat(rechazados).isEqualTo(1);
        assertThat(totalDe(ana)).isEqualTo(2);
    }

    // ---------------------------------------------------------------- duplicados

    @Test
    void elParecidoDeOtraPersonaAvisaConDatosBaseSinIdentificarla() throws Exception {
        var ana = persona("ana");
        var luis = persona("luis");
        var deLuis = existente(luis, 1, norte(0), 2);

        var r = crear(ana, 1, norte(3));

        assertThat(r.estado()).isEqualTo(409);
        assertThat(r.contiene("POSIBLE_DUPLICADO")).isTrue();
        assertThat(r.contiene("\"esPropio\":false")).isTrue();
        assertThat(r.contiene("\"cantidad\":1")).isTrue();
        assertThat(r.contiene("\"estadoExistente\":\"Recibido\"")).isTrue();
        assertThat(r.contiene("\"creadoEn\":\"" + LocalDate.now(ZoneId.of("America/Bogota")).minusDays(2) + "\"")).isTrue();
        assertThat(r.contiene("\"descripcion\":\"Hueco grande frente a la panadería\"")).isTrue();
        assertThat(r.cuerpo()).doesNotContain("codigoExistente").doesNotContain(deLuis.codigo())
                .doesNotContain("Cra 5 # 10-20").doesNotContain(luis.correo()).doesNotContain("Gómez");
        assertThat(totalDe(ana)).isZero();
    }

    @Test
    void elParecidoPropioTraeSuCodigoYGanaSobreElDeOtraPersona() throws Exception {
        var ana = persona("ana");
        var luis = persona("luis");
        existente(luis, 1, norte(0), 0, "Reporte más reciente de otra persona");
        var propio = existente(ana, 1, norte(1), 5);

        var r = crear(ana, 1, norte(2));

        assertThat(r.estado()).isEqualTo(409);
        assertThat(r.contiene("\"esPropio\":true")).isTrue();
        assertThat(r.contiene("\"codigoExistente\":\"" + propio.codigo() + "\"")).isTrue();
        assertThat(r.contiene("\"cantidad\":2")).isTrue();
    }

    @Test
    void cuentaCuantosParecidosHay() throws Exception {
        var ana = persona("ana");
        for (String nombre : new String[] {"luis", "eva", "pepe"}) {
            existente(persona(nombre), 1, norte(0), 1);
        }
        var r = crear(ana, 1, norte(1));
        assertThat(r.estado()).isEqualTo(409);
        assertThat(r.contiene("\"cantidad\":3")).isTrue();
    }

    @Test
    void soloEsParecidoLoDeLaMismaCategoriaCercanoYDeLosUltimos30Dias() throws Exception {
        var luis = persona("luis");
        existente(luis, 1, norte(0), 1);
        assertThat(crear(persona("a"), 1, norte(20)).estado()).as("a 20 m con distancia de 5 m").isEqualTo(201);
        assertThat(crear(persona("b"), 3, norte(0)).estado()).as("otra categoría, mismo punto").isEqualTo(201);

        var antiguo = persona("antiguo");
        existente(antiguo, 1, norte(1000), 31);
        assertThat(crear(persona("c"), 1, norte(1000)).estado()).as("de hace 31 días").isEqualTo(201);
        existente(antiguo, 1, norte(2000), 29);
        assertThat(crear(persona("d"), 1, norte(2000)).estado()).as("de hace 29 días").isEqualTo(409);
    }

    @Test
    void cadaCategoriaUsaSuPropiaDistancia() throws Exception {
        var luis = persona("luis");
        existente(luis, 1, norte(0), 1);
        existente(luis, 2, norte(0), 1);

        assertThat(crear(persona("a"), 1, norte(8)).estado()).as("huecos: 5 m, a 8 m no es parecido").isEqualTo(201);
        assertThat(crear(persona("b"), 2, norte(8)).estado()).as("alumbrado: 10 m, a 8 m sí").isEqualTo(409);
    }

    @Test
    void sinGpsNoSeRevisaDuplicado() throws Exception {
        var luis = persona("luis");
        existente(luis, 1, norte(0), 1);
        var ana = persona("ana");
        var r = mvc.perform(post("/api/reportes").header("Authorization", ana.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idCategoria\":1,\"idBarrio\":" + barrio
                        + ",\"descripcion\":\"Hay un hueco enorme frente a la panadería\",\"autorizaEnvio\":true}"))
                .andReturn().getResponse();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void confirmarElDuplicadoLoGuardaYCuentaContraElTope() throws Exception {
        var luis = persona("luis");
        existente(luis, 1, norte(0), 1);
        var ana = persona("ana");
        assertThat(crear(ana, 1, norte(2)).estado()).isEqualTo(409);

        assertThat(crear(ana, 1, norte(2), ",\"confirmarDuplicado\":true").estado()).isEqualTo(201);
        assertThat(crear(ana, 1, norte(300)).estado()).isEqualTo(201);
        assertThat(crear(ana, 1, norte(600)).estado()).isEqualTo(429);
    }

    // ---------------------------------------------------------------- evidencia

    @Test
    void repetirUnProblemaResueltoSinFotoSeRechazaConEvidenciaRequerida() throws Exception {
        var luis = persona("luis");
        var resuelto = existente(luis, 1, norte(0), 10);
        resolver(resuelto, 3);
        var ana = persona("ana");

        var sinFoto = crear(ana, 1, norte(2));
        assertThat(sinFoto.estado()).isEqualTo(422);
        assertThat(sinFoto.contiene("EVIDENCIA_REQUERIDA")).isTrue();
        assertThat(sinFoto.contiene("foto de hoy")).isTrue();
        assertThat(totalDe(ana)).isZero();

        assertThat(crear(ana, 1, norte(2), ",\"totalFotos\":0").estado()).isEqualTo(422);

        var conFoto = crear(ana, 1, norte(2), ",\"totalFotos\":1");
        assertThat(conFoto.estado()).as("un resuelto no es duplicado").isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT requiere_evidencia FROM reporte WHERE id_usuario = ?", Boolean.class, ana.id())).isTrue();
    }

    @Test
    void sinFotoRealElReporteConEvidenciaNoSaleALaEntidad() throws Exception {
        var luis = persona("luis");
        resolver(existente(luis, 1, norte(0), 10), 3);
        var ana = persona("ana");
        var creado = crear(ana, 1, norte(2), ",\"totalFotos\":1");
        assertThat(creado.estado()).isEqualTo(201);
        String codigo = jdbc.queryForObject("SELECT codigo_seguimiento FROM reporte WHERE id_usuario = ?", String.class, ana.id());

        var sinFoto = mvc.perform(post("/api/reportes/" + codigo + "/enviar").header("Authorization", ana.token())).andReturn().getResponse();
        assertThat(sinFoto.getStatus()).isEqualTo(422);
        assertThat(sinFoto.getContentAsString(StandardCharsets.UTF_8)).contains("EVIDENCIA_REQUERIDA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_envio_correo", Integer.class)).isZero();

        long idReporte = jdbc.queryForObject("SELECT id_reporte FROM reporte WHERE codigo_seguimiento = ?", Long.class, codigo);
        DatosDePrueba.foto(jdbc, new DatosDePrueba.Reporte(idReporte, codigo), ana.id(), 1, "foto-de-prueba");
        var conFoto = mvc.perform(post("/api/reportes/" + codigo + "/enviar").header("Authorization", ana.token())).andReturn().getResponse();
        assertThat(conFoto.getStatus()).isEqualTo(202);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_envio_correo", Integer.class)).isEqualTo(1);
    }

    @Test
    void unResueltoDeHaceMasDeLaVentanaNoPideFoto() throws Exception {
        var luis = persona("luis");
        resolver(existente(luis, 1, norte(0), 60), 45);
        assertThat(crear(persona("ana"), 1, norte(2)).estado()).isEqualTo(201);
    }

    @Test
    void siHayUnoSinResolverYOtroResueltoGanaElAvisoDeDuplicado() throws Exception {
        var luis = persona("luis");
        resolver(existente(luis, 1, norte(0), 10), 3);
        existente(persona("eva"), 1, norte(1), 1);

        var r = crear(persona("ana"), 1, norte(2));
        assertThat(r.estado()).isEqualTo(409);
        assertThat(r.contiene("POSIBLE_DUPLICADO")).isTrue();
    }

    @Test
    void laVistaPreviaIndicaSiHaceFaltaEvidenciaYNoEscribeNada() throws Exception {
        var luis = persona("luis");
        resolver(existente(luis, 1, norte(0), 10), 3);
        var ana = persona("ana");
        String cuerpo = "{\"idCategoria\":1,\"latitud\":%s,\"longitud\":" + LON
                + ",\"descripcion\":\"Hay un hueco enorme frente a la panadería\",\"totalFotos\":0}";

        var cerca = mvc.perform(post("/api/reportes/vista-previa").header("Authorization", ana.token()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo.formatted(norte(2)))).andReturn().getResponse();
        var lejos = mvc.perform(post("/api/reportes/vista-previa").header("Authorization", ana.token()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo.formatted(norte(500)))).andReturn().getResponse();

        assertThat(cerca.getContentAsString(StandardCharsets.UTF_8)).contains("\"requiereEvidencia\":true");
        assertThat(lejos.getContentAsString(StandardCharsets.UTF_8)).contains("\"requiereEvidencia\":false");
        assertThat(totalDe(ana)).isZero();
    }

    @Test
    void conDiezMilReportesLaBusquedaUsaElIndiceEspacialYTardaPoco() {
        var luis = persona("luis");
        jdbc.update("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion, autorizacion_envio_en, creado_en)
                SELECT ?, 1, ?, 'Reporte de relleno para medir la búsqueda',
                       ST_SetSRID(ST_MakePoint(? + (random() - 0.5) * 0.05, ? + (random() - 0.5) * 0.05), 4326), now(), now()
                  FROM generate_series(1, 10000)""", luis.id(), barrio, LON, LAT);
        jdbc.execute("ANALYZE reporte");

        String plan = String.join("\n", jdbc.queryForList("""
                EXPLAIN SELECT r.id_usuario FROM reporte r
                 WHERE r.id_categoria = 1 AND r.ubicacion IS NOT NULL
                   AND r.ubicacion && ST_Expand(ST_SetSRID(ST_MakePoint(-76.19, 4.08), 4326), 0.00005)
                   AND ST_DWithin(r.ubicacion::geography, ST_SetSRID(ST_MakePoint(-76.19, 4.08), 4326)::geography, 5)""", String.class));
        assertThat(plan).contains("ix_reporte_ubicacion");

        var ana = persona("ana");
        long inicio = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            servicioDuplicados.buscarSinResolver(ana.id(), 1, norte(i * 7), LON);
        }
        long milisPorBusqueda = (System.nanoTime() - inicio) / 1_000_000 / 20;
        assertThat(milisPorBusqueda).as("ms por búsqueda con 10 000 reportes").isLessThan(100);
    }
}
