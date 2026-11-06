package com.notiurba.correo;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

/** Solo en una BD PostGIS desechable suministrada expresamente; jamás usa la configuración de producción. */
@EnabledIfEnvironmentVariable(named="NOTIURBA_TEST_DB_URL",matches="jdbc:postgresql:.*")
class EnrutamientoIntegracionTest {
    JdbcTemplate jdbc;
    EnrutamientoServicio rutas;
    EnvioReporteServicio servicio;
    EnviadorCorreo proveedor;
    String correo;
    long usuario;
    @BeforeEach void preparar() {
        String url=System.getenv("NOTIURBA_TEST_DB_URL");
        if(!url.matches(".*[/]notiurba_test[^/?]*(\\?.*)?")) throw new IllegalArgumentException("Se exige una BD desechable con prefijo notiurba_test");
        var ds=new DriverManagerDataSource(url,System.getenv("NOTIURBA_TEST_DB_USER"),System.getenv("NOTIURBA_TEST_DB_PASSWORD"));
        Flyway.configure().dataSource(ds).placeholders(java.util.Map.of("adminPassword","PruebaSintetica2026"))
            .load().migrate();
        jdbc=new JdbcTemplate(ds);rutas=new EnrutamientoServicio(jdbc);proveedor=mock(EnviadorCorreo.class);
        servicio=new EnvioReporteServicio(jdbc,new TransactionTemplate(new DataSourceTransactionManager(ds)),rutas,new PlantillaReporte(),proveedor,new ObjectMapper().findAndRegisterModules());
        correo="prueba-"+UUID.randomUUID()+"@example.test";
        usuario=jdbc.queryForObject("""
            INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,
            correo,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en)
            VALUES ('Ana','Prueba',1,?,(SELECT min(id_barrio) FROM barrio),'Dirección sintética',?,'no-login',1,1,now()) RETURNING id_usuario
            """,Long.class,UUID.randomUUID().toString().substring(0,25),correo);
    }
    @AfterEach void limpiarSoloDatosSinteticos(){if(jdbc!=null && usuario>0)jdbc.update("DELETE FROM usuario WHERE id_usuario=?",usuario);}
    String reporte(int categoria,boolean emergencia) {
        return jdbc.queryForObject("""
            INSERT INTO reporte(id_usuario,id_categoria,id_barrio,descripcion,autorizacion_envio_en,emergencia)
            VALUES (?, ?,(SELECT min(id_barrio) FROM barrio),'Un incidente sintético para verificar el correo',now(),?) RETURNING codigo_seguimiento
            """,String.class,usuario,categoria,emergencia);
    }
    @Test void catorceRutasConservanIdsYMunicipalesVanSoloAlCaic() {
        assertThat(rutas.listar()).hasSize(14);
        for(int id=1;id<=10;id++) {
            var r=rutas.resolver(id,false);
            assertThat(r.acceso()).isEqualTo("servicioalciudadano@tulua.gov.co");assertThat(r.permiteCorreo()).isTrue();
        }
        assertThat(rutas.resolver(8,false).competenciaConfirmada()).isFalse();
        assertThat(rutas.resolver(9,false).competenciaConfirmada()).isFalse();
        assertThat(rutas.resolver(10,false).revisionClasificacion()).isTrue();
        assertThat(rutas.resolver(11,false).correoReferencia()).isEqualTo("notificacionespqr@centroaguas.com");
        assertThat(rutas.resolver(12,false).correoReferencia()).isEqualTo("inquietudes@gdo.com.co");
        assertThat(rutas.resolver(13,false).correoReferencia()).isEqualToIgnoringCase("servicioalcliente@celsia.com");
        assertThat(rutas.resolver(14,false).correoReferencia()).isEqualTo("pqr.tulua@veolia.com");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM categoria WHERE id_categoria=15",Integer.class)).isEqualTo(1);
    }
    @Test void gasEmergenteYCanalesAlternosNoLlamanProveedor() {
        assertThat(rutas.resolver(12,true).mensaje()).isEqualTo("Emergencia de gas: llama al 164");
        assertThat(rutas.resolver(12,true).permiteCorreo()).isFalse();
        for(int id:new int[]{11,13,14})assertThat(rutas.resolver(id,false).permiteCorreo()).isFalse();
        assertThat(rutas.resolver(13,false).canal()).isEqualTo("Web");
        for(int id:new int[]{11,12,13,14}) {
            String codigo=reporte(id,id==12);
            assertThat(servicio.solicitar(correo,codigo).estado()).isEqualTo("NO_ENVIADO_CORREO");
        }
        servicio.procesarSiguiente();verifyNoInteractions(proveedor);
    }
    @Test void pendienteAceptadoYReintentoNoCreanReportesNiDuplicanCorreos() {
        String codigo=reporte(1,false);
        when(proveedor.enviar(any())).thenReturn("fake-"+UUID.randomUUID());
        assertThat(servicio.solicitar(correo,codigo).estado()).isEqualTo("PENDIENTE");
        servicio.solicitar(correo,codigo);
        assertThat(estadoGeneral(codigo)).isEqualTo("Recibido");
        servicio.procesarSiguiente();
        assertThat(servicio.consultar(correo,codigo).estado()).isEqualTo("ENVIADO");
        assertThat(estadoGeneral(codigo)).isEqualTo("Enviado");
        servicio.solicitar(correo,codigo);servicio.procesarSiguiente();verify(proveedor,times(1)).enviar(any());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporte WHERE codigo_seguimiento=?",Integer.class,codigo)).isEqualTo(1);
    }
    @Test void falloConservaReporteYReintentoReutilizaMensajeYClave() {
        String codigo=reporte(1,false);
        when(proveedor.enviar(any())).thenThrow(new CorreoNoEnviadoException("timeout simulado",true)).thenReturn("fake-"+UUID.randomUUID());
        servicio.solicitar(correo,codigo);servicio.procesarSiguiente();
        assertThat(estadoGeneral(codigo)).isEqualTo("Recibido");assertThat(servicio.consultar(correo,codigo).estado()).isEqualTo("FALLIDO");
        servicio.solicitar(correo,codigo);servicio.procesarSiguiente();
        var mensajes=org.mockito.ArgumentCaptor.forClass(CorreoSaliente.class);verify(proveedor,times(2)).enviar(mensajes.capture());
        assertThat(mensajes.getAllValues().get(0)).isEqualTo(mensajes.getAllValues().get(1));
        assertThat(estadoGeneral(codigo)).isEqualTo("Enviado");
    }
    @Test void propiedadImpideConsultarOEnviarReporteAjeno(){
        String codigo=reporte(1,false);
        assertThatThrownBy(()->servicio.solicitar("otra@example.test",codigo)).isInstanceOf(com.notiurba.web.error.ApiException.class);
        verifyNoInteractions(proveedor);
    }
    @Test void reboteFirmadoNoRetrocedeHistorialYEsIdempotente() throws Exception {
        String codigo=reporte(1,false),idProveedor="fake-"+UUID.randomUUID();
        when(proveedor.enviar(any())).thenReturn(idProveedor);servicio.solicitar(correo,codigo);servicio.procesarSiguiente();
        String clave="secreto-sintetico",secret="whsec_"+java.util.Base64.getEncoder().encodeToString(clave.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String id="evt-"+UUID.randomUUID(),ts=Long.toString(java.time.Instant.now().getEpochSecond()),body="{\"type\":\"email.bounced\",\"data\":{\"email_id\":\""+idProveedor+"\"}}";
        var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(clave.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
        String firma="v1,"+java.util.Base64.getEncoder().encodeToString(mac.doFinal((id+"."+ts+"."+body).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var webhook=new ReboteController(secret,jdbc,new ObjectMapper());webhook.recibir(id,ts,firma,body);webhook.recibir(id,ts,firma,body);
        assertThat(estadoGeneral(codigo)).isEqualTo("Enviado");assertThat(servicio.consultar(correo,codigo).rebote()).isTrue();
        assertThat(servicio.consultar(correo,codigo).estado()).isEqualTo("FALLIDO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_correo WHERE id_evento=?",Integer.class,id)).isEqualTo(1);
        jdbc.update("DELETE FROM evento_correo WHERE id_evento=?",id);
    }
    String estadoGeneral(String codigo){return jdbc.queryForObject("SELECT e.nombre FROM reporte r JOIN estado_reporte e USING(id_estado_reporte) WHERE codigo_seguimiento=?",String.class,codigo);}
}
