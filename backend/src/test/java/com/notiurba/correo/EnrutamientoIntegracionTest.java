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
        servicio=new EnvioReporteServicio(jdbc,new TransactionTemplate(new DataSourceTransactionManager(ds)),rutas,new PlantillaReporte(),proveedor,new ObjectMapper().findAndRegisterModules(),true);
        jdbc.update("DELETE FROM registro_envio_correo WHERE id_estado_envio=(SELECT id_estado_envio FROM estado_envio WHERE nombre='PENDIENTE')");
        correo="prueba-"+UUID.randomUUID()+"@example.test";
        usuario=jdbc.queryForObject("""
            INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,
            correo,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en)
            VALUES ('Ana','Prueba',1,?,(SELECT min(id_barrio) FROM barrio),'Dirección sintética',?,'no-login',1,1,now()) RETURNING id_usuario
            """,Long.class,UUID.randomUUID().toString().substring(0,25),correo);
    }
    @AfterEach void limpiarSoloDatosSinteticos(){
        if(jdbc==null) return;
        jdbc.update("DELETE FROM evento_correo WHERE id_evento LIKE 'evt-%'");
        if(usuario>0)jdbc.update("DELETE FROM usuario WHERE id_usuario=?",usuario);
    }
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
        assertThat(rutas.resolver(11,false).correoReferencia()).isEqualTo("info@centroaguas.com");
        assertThat(rutas.resolver(11,false).permiteCorreo()).isTrue();
        assertThat(rutas.resolver(12,false).correoReferencia()).isEqualTo("inquietudes@gdo.com.co");
        assertThat(rutas.resolver(13,false).correoReferencia()).isEqualToIgnoringCase("servicioalcliente@celsia.com");
        assertThat(rutas.resolver(14,false).correoReferencia()).isEqualTo("pqr.tulua@veolia.com");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM categoria WHERE id_categoria=15",Integer.class)).isEqualTo(1);
    }
    @Test void gasEmergenteYCanalesAlternosNoLlamanProveedor() {
        assertThat(rutas.resolver(12,true).mensaje()).isEqualTo("Emergencia de gas: llame al 164");
        assertThat(rutas.resolver(12,true).permiteCorreo()).isFalse();
        for(int id:new int[]{12,13,14})assertThat(rutas.resolver(id,false).permiteCorreo()).isFalse();
        assertThat(rutas.resolver(13,false).canal()).isEqualTo("Web");
        for(int id:new int[]{12,13,14}) {
            String codigo=reporte(id,id==12);
            assertThat(servicio.solicitar(correo,codigo).estado()).isEqualTo("NO_ENVIADO_CORREO");
        }
        servicio.procesarSiguiente();verifyNoInteractions(proveedor);
    }
    @Test void elCorreoEncoladoEsLaCartaFormalSinNotiUrbaNiCodigo() throws Exception {
        String codigo=reporte(1,false);
        servicio.solicitar(correo,codigo);
        var fila=jdbc.queryForMap("""
            SELECT e.destino,e.mensaje_json FROM registro_envio_correo e JOIN reporte r USING(id_reporte)
            WHERE r.codigo_seguimiento=?""",codigo);
        var mensaje=new ObjectMapper().readValue((String)fila.get("mensaje_json"),CorreoSaliente.class);
        assertThat(fila.get("destino")).isEqualTo("servicioalciudadano@tulua.gov.co");
        assertThat(mensaje.asunto()).startsWith("Petición ciudadana – "+rutas.resolver(1,false).categoria()+" – ");
        assertThat(mensaje.texto()).startsWith("Respetados señores:").contains("Yo, Ana Prueba, con cédula de ciudadanía No. ",
            "Hechos: Un incidente sintético para verificar el correo","Atentamente,\nAna Prueba");
        for(String cuerpo:new String[]{mensaje.asunto(),mensaje.texto(),mensaje.html()})
            assertThat(cuerpo).doesNotContain("NotiUrba",codigo,"Hábitat");
        assertThat(mensaje.responderA()).isEqualTo(correo);
    }
    @Test void centroaguasEncolaElCorreoHaciaInfo() {
        String codigo=reporte(11,false);
        assertThat(servicio.solicitar(correo,codigo).estado()).isEqualTo("PENDIENTE");
        assertThat(jdbc.queryForMap("""
            SELECT e.destino,e.id_canal_atencion FROM registro_envio_correo e JOIN reporte r USING(id_reporte)
            WHERE r.codigo_seguimiento=?""",codigo)).containsEntry("destino","info@centroaguas.com")
            .containsEntry("id_canal_atencion",13);
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
        assertThat(jdbc.queryForObject("""
            SELECT count(*) FROM historial_estado h JOIN reporte r USING(id_reporte)
             JOIN estado_reporte e ON e.id_estado_reporte=h.id_estado_reporte
            WHERE r.codigo_seguimiento=? AND e.nombre='Enviado'
            """,Integer.class,codigo)).isEqualTo(1);
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
        String id="evt-"+UUID.randomUUID(),ts=Long.toString(java.time.Instant.now().getEpochSecond()),body="{\"type\":\"email.bounced\",\"data\":{\"email_id\":\""+idProveedor+"\"}}";
        String firma=firmar(id,ts,body);
        var webhook=new ReboteController(SECRETO_WEBHOOK,jdbc,new ObjectMapper());webhook.recibir(id,ts,firma,body);webhook.recibir(id,ts,firma,body);
        assertThat(estadoGeneral(codigo)).isEqualTo("Enviado");assertThat(servicio.consultar(correo,codigo).rebote()).isTrue();
        assertThat(servicio.consultar(correo,codigo).estado()).isEqualTo("FALLIDO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_correo WHERE id_evento=?",Integer.class,id)).isEqualTo(1);
        jdbc.update("DELETE FROM evento_correo WHERE id_evento=?",id);
    }
    @Test void conDestinoDePruebaLaColaEnviaSoloAEsaDireccionConPrueba() {
        String codigo=reporte(1,false);
        org.springframework.test.util.ReflectionTestUtils.setField(servicio,"destinoPrueba","bandeja@example.test");
        when(proveedor.enviar(any())).thenReturn("fake-"+UUID.randomUUID());
        servicio.solicitar(correo,codigo);servicio.procesarSiguiente();
        var enviado=org.mockito.ArgumentCaptor.forClass(CorreoSaliente.class);verify(proveedor).enviar(enviado.capture());
        assertThat(enviado.getValue().para()).containsExactly("bandeja@example.test");
        assertThat(enviado.getValue().asunto()).startsWith("[PRUEBA] ");
        assertThat(enviado.getValue().texto()).contains("servicioalciudadano@tulua.gov.co");
        assertThat(servicio.consultar(correo,codigo).estado()).isEqualTo("ENVIADO");
    }
    @Test void sinDestinoDePruebaLaColaEnviaAlDestinoDeLaRutaYMarcaEnviado() {
        String codigo=reporte(1,false);
        when(proveedor.enviar(any())).thenReturn("fake-"+UUID.randomUUID());
        servicio.solicitar(correo,codigo);servicio.procesarSiguiente();
        var enviado=org.mockito.ArgumentCaptor.forClass(CorreoSaliente.class);verify(proveedor).enviar(enviado.capture());
        assertThat(enviado.getValue().para()).containsExactly("servicioalciudadano@tulua.gov.co");
        assertThat(enviado.getValue().asunto()).doesNotContain("[PRUEBA]");
        assertThat(enviado.getValue().texto()).doesNotContain("CORREO DE PRUEBA");
        assertThat(servicio.consultar(correo,codigo).estado()).isEqualTo("ENVIADO");
        assertThat(estadoGeneral(codigo)).isEqualTo("Enviado");
    }

    static final String CLAVE_WEBHOOK="secreto-sintetico";
    static final String SECRETO_WEBHOOK="whsec_"+java.util.Base64.getEncoder().encodeToString(CLAVE_WEBHOOK.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    static String firmar(String id,String ts,String cuerpo) throws Exception {
        var mac=javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(CLAVE_WEBHOOK.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
        return "v1,"+java.util.Base64.getEncoder().encodeToString(mac.doFinal((id+"."+ts+"."+cuerpo).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    String ahora(){return Long.toString(java.time.Instant.now().getEpochSecond());}
    record ReporteEnviado(String codigo,String idProveedor) { }
    /** Un reporte ya aceptado por el proveedor. */
    ReporteEnviado reporteEnviado() {
        String codigo=reporte(1,false),idProveedor="fake-"+UUID.randomUUID();
        when(proveedor.enviar(any())).thenReturn(idProveedor);servicio.solicitar(correo,codigo);servicio.procesarSiguiente();
        return new ReporteEnviado(codigo,idProveedor);
    }
    void esperaError(org.springframework.http.HttpStatus estado,String codigoError,org.junit.jupiter.api.function.Executable llamada) {
        var e=Assertions.assertThrows(com.notiurba.web.error.ApiException.class,llamada);
        assertThat(e.getStatusCode()).isEqualTo(estado);assertThat(e.getCodigo()).isEqualTo(codigoError);
    }
    @Test void avisoSinFirmaConFirmaFalsaOVencidaSeRechazaYNoCambiaNada() throws Exception {
        var enviado=reporteEnviado();
        String id="evt-"+UUID.randomUUID(),ts=ahora(),body="{\"type\":\"email.bounced\",\"data\":{\"email_id\":\""+enviado.idProveedor()+"\"}}";
        var webhook=new ReboteController(SECRETO_WEBHOOK,jdbc,new ObjectMapper());
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->webhook.recibir(id,ts,null,body));
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->webhook.recibir(id,ts,"v1,AAAA",body));
        String vieja=Long.toString(java.time.Instant.now().getEpochSecond()-3600);
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->webhook.recibir(id,vieja,firmar(id,vieja,body),body));
        String alterado=body.replace("bounced","delivered");
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->webhook.recibir(id,ts,firmar(id,ts,body),alterado));
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->webhook.recibir(id,"no-es-numero",firmar(id,"no-es-numero",body),body));
        String futuro=Long.toString(java.time.Instant.now().getEpochSecond()+3600);
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->webhook.recibir(id,futuro,firmar(id,futuro,body),body));
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->webhook.recibir(null,ts,firmar(id,ts,body),body));
        var sinSecreto=new ReboteController("",jdbc,new ObjectMapper());
        esperaError(org.springframework.http.HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA",()->sinSecreto.recibir(id,ts,firmar(id,ts,body),body));
        assertThat(servicio.consultar(correo,enviado.codigo()).estado()).isEqualTo("ENVIADO");
        assertThat(servicio.consultar(correo,enviado.codigo()).rebote()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_correo WHERE id_evento=?",Integer.class,id)).isZero();
    }
    @Test void quejaFirmadaTambienMarcaReboteYOtrosEventosSeIgnoran() throws Exception {
        var enviado=reporteEnviado();
        var webhook=new ReboteController(SECRETO_WEBHOOK,jdbc,new ObjectMapper());
        String entregado="evt-"+UUID.randomUUID(),ts=ahora(),cuerpoEntregado="{\"type\":\"email.delivered\",\"data\":{\"email_id\":\""+enviado.idProveedor()+"\"}}";
        webhook.recibir(entregado,ts,firmar(entregado,ts,cuerpoEntregado),cuerpoEntregado);
        assertThat(servicio.consultar(correo,enviado.codigo()).rebote()).isFalse();
        String queja="evt-"+UUID.randomUUID(),cuerpoQueja="{\"type\":\"email.complained\",\"data\":{\"email_id\":\""+enviado.idProveedor()+"\"}}";
        webhook.recibir(queja,ts,firmar(queja,ts,cuerpoQueja),cuerpoQueja);
        var estado=servicio.consultar(correo,enviado.codigo());
        assertThat(estado.rebote()).isTrue();assertThat(estado.estado()).isEqualTo("FALLIDO");assertThat(estado.reintentable()).isFalse();
        assertThat(estado.motivo()).isEqualTo("No pudimos entregar el correo a la entidad.");
        assertThat(estadoGeneral(enviado.codigo())).isEqualTo("Enviado");
        jdbc.update("DELETE FROM evento_correo WHERE id_evento=?",queja);
    }
    @Test void elAvisoQueLlegaAntesDeGuardarLaAceptacionTambienMarcaElRebote() {
        String codigo=reporte(1,false),idProveedor="fake-"+UUID.randomUUID();
        jdbc.update("INSERT INTO evento_correo(id_evento,id_envio_proveedor) VALUES (?,?)","evt-"+UUID.randomUUID(),idProveedor);
        when(proveedor.enviar(any())).thenReturn(idProveedor);
        servicio.solicitar(correo,codigo);servicio.procesarSiguiente();
        var estado=servicio.consultar(correo,codigo);
        assertThat(estado.estado()).isEqualTo("FALLIDO");assertThat(estado.rebote()).isTrue();
        assertThat(estadoGeneral(codigo)).isEqualTo("Enviado");
    }
    @Test void avisoFirmadoConIdentificadorVacioOExcesivoEsInvalido() throws Exception {
        var webhook=new ReboteController(SECRETO_WEBHOOK,jdbc,new ObjectMapper());
        for(String email:new String[]{"","x".repeat(101)}) {
            String id="evt-"+UUID.randomUUID(),ts=ahora(),body="{\"type\":\"email.bounced\",\"data\":{\"email_id\":\""+email+"\"}}";
            esperaError(org.springframework.http.HttpStatus.BAD_REQUEST,"VALIDACION",()->webhook.recibir(id,ts,firmar(id,ts,body),body));
        }
    }
    @Test void falloNoReintentableQuedaFallidoConMotivoAmableYNoSeReintentaSolo() {
        String codigo=reporte(1,false);
        when(proveedor.enviar(any())).thenThrow(new CorreoNoEnviadoException("datos inválidos",false));
        servicio.solicitar(correo,codigo);servicio.procesarSiguiente();servicio.procesarSiguiente();
        verify(proveedor,times(1)).enviar(any());
        var estado=servicio.consultar(correo,codigo);
        assertThat(estado.estado()).isEqualTo("FALLIDO");assertThat(estado.reintentable()).isFalse();
        assertThat(estado.motivo()).isEqualTo("No pudimos entregar el correo a la entidad.");
        assertThat(estadoGeneral(codigo)).isEqualTo("Recibido");
        assertThatThrownBy(()->servicio.solicitar(correo,codigo)).isInstanceOf(com.notiurba.web.error.ApiException.class);
    }
    @Test void estadoYMensajeDeUnReporteAjenoDanNoEncontrado() {
        String codigo=reporte(1,false);
        for(org.junit.jupiter.api.function.Executable llamada:new org.junit.jupiter.api.function.Executable[]{
            ()->servicio.consultar("otra@example.test",codigo),()->servicio.mensaje("otra@example.test",codigo)})
            esperaError(org.springframework.http.HttpStatus.NOT_FOUND,"NO_ENCONTRADO",llamada);
    }
    String estadoGeneral(String codigo){return jdbc.queryForObject("SELECT e.nombre FROM reporte r JOIN estado_reporte e USING(id_estado_reporte) WHERE codigo_seguimiento=?",String.class,codigo);}

    @Test void colaDeshabilitadaConservaPendienteSinLlamarAlProveedor() {
        String codigo=reporte(1,false);
        servicio.solicitar(correo,codigo);
        var apagado=new EnvioReporteServicio(jdbc,new TransactionTemplate(new DataSourceTransactionManager(
            new DriverManagerDataSource(System.getenv("NOTIURBA_TEST_DB_URL"),System.getenv("NOTIURBA_TEST_DB_USER"),System.getenv("NOTIURBA_TEST_DB_PASSWORD")))),
            rutas,new PlantillaReporte(),proveedor,new ObjectMapper().findAndRegisterModules(),false);
        apagado.procesarSiguiente();
        verifyNoInteractions(proveedor);
        assertThat(servicio.consultar(correo,codigo).estado()).isEqualTo("PENDIENTE");
        assertThat(estadoGeneral(codigo)).isEqualTo("Recibido");
    }
}
