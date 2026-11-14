package com.notiurba.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.annotation.Propagation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.reporte.ReporteRepositorio;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.correo.EnrutamientoServicio;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.correo.EnvioReporteServicio;
import com.notiurba.correo.PlantillaReporte;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.NotiUrbaApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.*;

/** Reutiliza una BD nueva por clase y Flyway; cada caso revierte sus datos, incluida la configuración. */
@SpringBootTest(classes = NotiUrbaApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:postgresql:.*")
@Transactional
class AdminIntegracionTest {
    private static BaseDeDatosTemporal bd;
    @BeforeAll static void crearBase() {
        String url = System.getenv("DB_URL");
        // Se omite (no falla) si DB_URL apunta a otra base: así no rompe `mvn test` a quien tenga su propia base local
        org.junit.jupiter.api.Assumptions.assumeTrue(
                url != null && url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/notiurba_test_admin_bootstrap"),
                "Se omite: usa el contenedor local desechable de docs/admin-estados-validar-local.ps1");
        bd = BaseDeDatosTemporal.crearOOmitir();
    }
    @AfterAll static void borrarBase() throws SQLException { if (bd != null) bd.borrar(); }
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry registro) { bd.registrar(registro); }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtProveedor jwt;
    @Autowired AdminRepositorio admin;
    @Autowired ReporteRepositorio reportes;
    @Autowired EnrutamientoServicio rutas;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired ObjectMapper json;
    // Se prueba JDBC real; los límites externos no pueden contactar Resend ni Cloudinary.
    @MockBean EnviadorCorreo correoExterno;
    @MockBean AlmacenFotos fotosExternas;
    @AfterEach void sinAccesoAProveedoresReales() { verifyNoInteractions(correoExterno, fotosExternas); }
    private long ciudadano;
    private long administrador;
    private String token;

    @BeforeEach void preparar() {
        administrador = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE correo='admin.dev@notiurba.local'", Long.class);
        token = jwt.generar("admin.dev@notiurba.local", "ADMIN");
        ciudadano = jdbc.queryForObject("""
                INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,
                  direccion_correspondencia,correo,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en)
                VALUES ('Ana','Prueba',1,?,(SELECT min(id_barrio) FROM barrio),'Dirección sintética',?,
                  'no-login',1,1,now()) RETURNING id_usuario
                """, Long.class, UUID.randomUUID().toString().substring(0,25), UUID.randomUUID()+"@example.test");
    }

    private String crear(boolean vencido) {
        return jdbc.queryForObject("""
                INSERT INTO reporte(id_usuario,id_categoria,id_barrio,descripcion,ubicacion,creado_en,autorizacion_envio_en)
                VALUES (?,1,(SELECT min(id_barrio) FROM barrio),'Incidente sintético para probar vigencia',
                  ST_SetSRID(ST_Point(-76.195,4.085),4326),
                  now()-make_interval(days => ?),now()-make_interval(days => ?))
                RETURNING codigo_seguimiento
                """, String.class, ciudadano, vencido ? 31 : 0, vencido ? 31 : 0);
    }

    private org.springframework.test.web.servlet.ResultActions cambiar(String codigo, String estado, String observacion) throws Exception {
        return mvc.perform(patch("/api/admin/reportes/"+codigo+"/estado").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\""+estado+"\",\"observacion\":\""+observacion+"\",\"idUsuario\":999999}"));
    }
    private long historial(String codigo) {
        return jdbc.queryForObject("SELECT count(*) FROM historial_estado h JOIN reporte r USING(id_reporte) WHERE r.codigo_seguimiento=?",Long.class,codigo);
    }
    private OffsetDateTime vencimiento(String codigo) {
        return jdbc.queryForObject("SELECT vigente_hasta FROM reporte WHERE codigo_seguimiento=?",OffsetDateTime.class,codigo);
    }
    private long visibles(String codigo) {
        return jdbc.queryForObject("SELECT count(*) FROM v_reporte_mapa WHERE codigo_seguimiento=?",Long.class,codigo);
    }

    @Test void guardaHistorialConActorFechaObservacionYEvitaDuplicados() throws Exception {
        String codigo=crear(false);
        var fecha=vencimiento(codigo);
        cambiar(codigo,"EN_TRAMITE","Se revisó la evidencia").andExpect(status().isOk())
                .andExpect(jsonPath("cambiadoPor").value(administrador));
        assertThat(historial(codigo)).isEqualTo(2);
        var asiento=jdbc.queryForMap("""
                SELECT h.id_usuario_cambia,h.observacion,h.cambiado_en,e.nombre FROM historial_estado h
                JOIN reporte r USING(id_reporte) JOIN estado_reporte e ON e.id_estado_reporte=h.id_estado_reporte
                WHERE r.codigo_seguimiento=? ORDER BY h.cambiado_en DESC,h.id_historial_estado DESC LIMIT 1
                """,codigo);
        assertThat(asiento.get("id_usuario_cambia")).isEqualTo(administrador);
        assertThat(asiento.get("observacion")).isEqualTo("Se revisó la evidencia");
        assertThat(asiento.get("cambiado_en")).isNotNull();
        assertThat(asiento.get("nombre")).isEqualTo("En trámite");
        assertThat(jdbc.queryForObject("SELECT id_estado_reporte FROM reporte WHERE codigo_seguimiento=?",Integer.class,codigo)).isEqualTo(3);
        cambiar(codigo,"EN_TRAMITE","Se revisó la evidencia").andExpect(status().isOk());
        assertThat(historial(codigo)).isEqualTo(2);
        cambiar(codigo,"EN_TRAMITE","Otra observación").andExpect(status().isOk());
        assertThat(historial(codigo)).isEqualTo(3);
        assertThat(vencimiento(codigo)).isEqualTo(fecha);
    }

    @ParameterizedTest @CsvSource({"1,EN_TRAMITE", "1,RESUELTO", "2,EN_TRAMITE", "2,RESUELTO", "3,RESUELTO"})
    void avancesYSaltosValidos(int inicial,String destino) throws Exception {
        String codigo=crear(false);
        if(inicial>1) jdbc.update("INSERT INTO historial_estado(id_reporte,id_estado_reporte,observacion) SELECT id_reporte,?,'Preparación sintética' FROM reporte WHERE codigo_seguimiento=?",inicial,codigo);
        long antes=historial(codigo);
        cambiar(codigo,destino,"Confirmación revisada").andExpect(status().isOk());
        assertThat(historial(codigo)).isEqualTo(antes+1);
    }

    @Test void rechazaRetrocesoEnvioManualYResueltoTerminalSinAlterarHistorial() throws Exception {
        String codigo=crear(false);
        cambiar(codigo,"ENVIADO","No es aceptación Resend").andExpect(status().isConflict());
        assertThat(historial(codigo)).isEqualTo(1);
        cambiar(codigo,"EN_TRAMITE","Verificado").andExpect(status().isOk());
        cambiar(codigo,"RECIBIDO","Retroceso").andExpect(status().isConflict());
        assertThat(historial(codigo)).isEqualTo(2);
        cambiar(codigo,"RESUELTO","Solución confirmada").andExpect(status().isOk());
        for(String destino:new String[]{"RECIBIDO","ENVIADO","EN_TRAMITE","RESUELTO"}) {
            cambiar(codigo,destino,"Nuevo cambio").andExpect(status().isConflict());
        }
        assertThat(historial(codigo)).isEqualTo(3);
    }

    @Test void sinSesionCiudadanoInexistenteYObservacionVacia() throws Exception {
        String codigo=crear(false);
        mvc.perform(patch("/api/admin/reportes/"+codigo+"/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"RESUELTO\",\"observacion\":\"Confirmado\"}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/reportes/"+codigo+"/estado").header("Authorization","Bearer "+jwt.generar("persona@example.test","CIUDADANO"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"RESUELTO\",\"observacion\":\"Confirmado\"}"))
                .andExpect(status().isForbidden());
        cambiar("NU-999999999999","RESUELTO","Confirmado").andExpect(status().isNotFound());
        cambiar(codigo,"RESUELTO","x".repeat(1001)).andExpect(status().isBadRequest());
        assertThat(historial(codigo)).isEqualTo(1);
    }

    @Test void treintaDiasPorDefectoYConfiguracionSoloAfectaNuevos() throws Exception {
        mvc.perform(get("/api/admin/configuracion/vigencia").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("vigenciaDias").value(30));
        String antiguo=crear(false);
        var fecha=vencimiento(antiguo);
        assertThat(jdbc.queryForObject("SELECT vigente_hasta=creado_en+interval '30 days' FROM reporte WHERE codigo_seguimiento=?",Boolean.class,antiguo)).isTrue();
        mvc.perform(put("/api/admin/configuracion/vigencia").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"vigenciaDias\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("vigenciaDias").value(1));
        String nuevo=crear(false);
        assertThat(vencimiento(antiguo)).isEqualTo(fecha);
        assertThat(jdbc.queryForObject("SELECT vigente_hasta=creado_en+interval '1 day' FROM reporte WHERE codigo_seguimiento=?",Boolean.class,nuevo)).isTrue();
        assertThat(jdbc.queryForObject("SELECT id_usuario_actualiza FROM configuracion WHERE clave='vigencia_dias'",Long.class)).isEqualTo(administrador);
    }

    @Test void vencidoDesapareceDelMapaPeroSigueConsultableYAdministrable() throws Exception {
        String codigo=crear(true);
        var fecha=vencimiento(codigo);
        assertThat(visibles(codigo)).isZero();
        assertThat(jdbc.queryForList("SELECT id_categoria,count(*) FROM v_reporte_mapa WHERE codigo_seguimiento=? GROUP BY id_categoria",codigo)).isEmpty();
        assertThat(admin.bloquear(codigo)).isPresent();
        assertThat(reportes.buscarPropio(ciudadano,codigo)).isPresent();
        cambiar(codigo,"EN_TRAMITE","Seguimiento de reporte vencido").andExpect(status().isOk());
        assertThat(vencimiento(codigo)).isEqualTo(fecha);
        assertThat(visibles(codigo)).isZero();
    }

    @Test void limiteExactoDeVencimientoYConservacionDeEstado() throws Exception {
        String codigo=crear(true);
        jdbc.update("UPDATE reporte SET vigente_hasta=CURRENT_TIMESTAMP+interval '1 second' WHERE codigo_seguimiento=?",codigo);
        assertThat(visibles(codigo)).isEqualTo(1);
        jdbc.update("UPDATE reporte SET vigente_hasta=CURRENT_TIMESTAMP WHERE codigo_seguimiento=?",codigo);
        assertThat(visibles(codigo)).isZero();
        assertThat(reportes.buscarPropio(ciudadano,codigo)).isPresent();
        assertThat(admin.bloquear(codigo)).isPresent();
        assertThat(historial(codigo)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id_estado_reporte FROM reporte WHERE codigo_seguimiento=?",Integer.class,codigo)).isEqualTo(1);
        var fecha=vencimiento(codigo);
        cambiar(codigo,"EN_TRAMITE","Seguimiento de reporte vencido").andExpect(status().isOk());
        assertThat(vencimiento(codigo)).isEqualTo(fecha);
        assertThat(visibles(codigo)).isZero();
    }

    @Test void reintentoDeCorreoNoReiniciaVigenciaYSoloAceptacionMarcaEnviado() {
        String codigo=crear(true);
        var fecha=vencimiento(codigo);
        String correo=jdbc.queryForObject("SELECT correo FROM usuario WHERE id_usuario=?",String.class,ciudadano);
        var proveedor=mock(EnviadorCorreo.class);
        when(proveedor.enviar(any())).thenThrow(new CorreoNoEnviadoException("Fallo sintético",true))
                .thenReturn("sintetico-"+UUID.randomUUID());
        var envios=new EnvioReporteServicio(jdbc,new TransactionTemplate(transacciones),rutas,
                new PlantillaReporte(),proveedor,json,true);
        envios.solicitar(correo,codigo);
        envios.procesarSiguiente();
        assertThat(envios.consultar(correo,codigo).estado()).isEqualTo("FALLIDO");
        assertThat(historial(codigo)).isEqualTo(1);
        assertThat(vencimiento(codigo)).isEqualTo(fecha);
        envios.solicitar(correo,codigo);
        envios.procesarSiguiente();
        assertThat(envios.consultar(correo,codigo).estado()).isEqualTo("ENVIADO");
        assertThat(historial(codigo)).isEqualTo(2);
        assertThat(admin.bloquear(codigo).orElseThrow().estado()).isEqualTo("Enviado");
        assertThat(vencimiento(codigo)).isEqualTo(fecha);
        assertThat(visibles(codigo)).isZero();
        verify(proveedor,times(2)).enviar(any());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void solicitudesConcurrentesIdenticasCreanUnSoloAsiento() throws Exception {
        String codigo=crear(false);
        var iniciar=new CountDownLatch(1);
        var ejecutor=Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> solicitud=() -> {
                iniciar.await();
                return cambiar(codigo,"EN_TRAMITE","Confirmación concurrente").andReturn().getResponse().getStatus();
            };
            var primera=ejecutor.submit(solicitud);
            var segunda=ejecutor.submit(solicitud);
            iniciar.countDown();
            assertThat(primera.get(20,TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(segunda.get(20,TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(historial(codigo)).isEqualTo(2);
        } finally {
            ejecutor.shutdownNow();
            ejecutor.awaitTermination(20,TimeUnit.SECONDS);
            jdbc.update("DELETE FROM usuario WHERE id_usuario=?",ciudadano);
        }
    }
}
