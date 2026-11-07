package com.notiurba.correo;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value;

/** Cola persistente: crear/enviar son transacciones separadas; nunca se borra un reporte por fallo de correo. */
@Service
public class EnvioReporteServicio {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final EnrutamientoServicio rutas;
    private final PlantillaReporte plantilla;
    private final EnviadorCorreo enviador;
    private final ObjectMapper json;
    private final boolean habilitado;
    /** Si tiene valor (RESEND_PRUEBA_DESTINO), el correo sale a esa dirección y NO a la entidad real. */
    @Value("${notiurba.correo.destino-prueba:}")
    private String destinoPrueba = "";
    public EnvioReporteServicio(JdbcTemplate jdbc, TransactionTemplate tx, EnrutamientoServicio rutas,
            PlantillaReporte plantilla, EnviadorCorreo enviador, ObjectMapper json,
            @Value("${notiurba.correo.reportes-habilitados:false}") boolean habilitado) {
        this.jdbc=jdbc; this.tx=tx; this.rutas=rutas; this.plantilla=plantilla; this.enviador=enviador; this.json=json;
        this.habilitado=habilitado;
    }
    public record Estado(String estado, String motivo, boolean reintentable, boolean rebote,
            EnrutamientoServicio.Ruta ruta) { }

    public Estado solicitar(String usuario, String codigo) {
        return tx.execute(status -> {
            long id = propio(usuario,codigo,true);
            Map<String,Object> reporte=jdbc.queryForMap("SELECT id_categoria, emergencia, autorizacion_envio_en FROM reporte WHERE id_reporte=?",id);
            if(reporte.get("autorizacion_envio_en")==null) throw error("Debes autorizar la remisión del reporte");
            var ruta=rutas.resolver(((Number)reporte.get("id_categoria")).intValue(), (Boolean)reporte.get("emergencia"));
            List<Map<String,Object>> anteriores=jdbc.queryForList("""
                SELECT e.*,s.nombre AS estado FROM registro_envio_correo e JOIN estado_envio s USING(id_estado_envio)
                WHERE id_reporte=? ORDER BY creado_en DESC,id_registro_envio DESC
                """,id);
            if(!anteriores.isEmpty()) {
                var ultimo=anteriores.get(0);
                if(!"FALLIDO".equals(ultimo.get("estado"))) return estado(id,ruta);
                if(!Boolean.TRUE.equals(ultimo.get("reintentable")) || Boolean.TRUE.equals(ultimo.get("rebote")))
                    throw error("Este envío requiere revisar el canal antes de reintentar");
                OffsetDateTime primero=jdbc.queryForObject("SELECT min(creado_en) FROM registro_envio_correo WHERE id_reporte=?",OffsetDateTime.class,id);
                if(Duration.between(primero,OffsetDateTime.now()).compareTo(Duration.ofHours(23))>=0)
                    throw error("Consulta el resultado del proveedor antes de reenviar: venció la ventana segura de reintento");
                if(!ruta.permiteCorreo() || !ruta.acceso().equals(ultimo.get("destino")))
                    throw error("La configuración del canal cambió. Revisa el destino antes de reenviar");
                insertar(id, ((Number)ultimo.get("id_canal_atencion")).intValue(),"RESEND","PENDIENTE",
                    (String)ultimo.get("destino"),(String)ultimo.get("clave_idempotencia"),(String)ultimo.get("mensaje_json"));
            } else if(!ruta.permiteCorreo()) {
                insertar(id,ruta.idCanal(),"EXTERNO","NO_ENVIADO_CORREO",ruta.acceso(),null,null);
            } else {
                var detalle=datos(id,ruta);
                String clave="reporte-"+UUID.randomUUID();
                CorreoSaliente correo=plantilla.crear(detalle,ruta.acceso(),clave);
                try { insertar(id,ruta.idCanal(),"RESEND","PENDIENTE",ruta.acceso(),clave,json.writeValueAsString(correo)); }
                catch(com.fasterxml.jackson.core.JsonProcessingException e){ throw new IllegalStateException("No se pudo preparar el correo",e); }
            }
            return estado(id,ruta);
        });
    }

    public Estado consultar(String usuario,String codigo) {
        long id=propio(usuario,codigo,false);
        var r=jdbc.queryForMap("SELECT id_categoria, emergencia FROM reporte WHERE id_reporte=?",id);
        return estado(id,rutas.resolver(((Number)r.get("id_categoria")).intValue(),(Boolean)r.get("emergencia")));
    }
    private long propio(String usuario,String codigo,boolean bloquear) {
        if(codigo==null || !codigo.trim().toUpperCase(java.util.Locale.ROOT).matches("NU-[0-9]{6,}")) throw error("Código de seguimiento no válido");
        var ids=jdbc.queryForList("""
            SELECT r.id_reporte FROM reporte r JOIN usuario u USING(id_usuario)
            WHERE lower(u.correo)=lower(?) AND r.codigo_seguimiento=?
            """+(bloquear?" FOR UPDATE OF r":""),Long.class,usuario,codigo.trim().toUpperCase(java.util.Locale.ROOT));
        if(ids.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"NO_ENCONTRADO","No encontramos ese reporte");
        return ids.get(0);
    }
    private void insertar(long id,int canal,String modo,String estado,String destino,String clave,String mensaje) {
        jdbc.update("""
            INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio,destino,clave_idempotencia,mensaje_json)
            VALUES (?,?,(SELECT id_modo_envio FROM modo_envio WHERE nombre=?),(SELECT id_estado_envio FROM estado_envio WHERE nombre=?),?,?,?)
            """,id,canal,modo,estado,destino,clave,mensaje);
    }
    private Estado estado(long id,EnrutamientoServicio.Ruta ruta) {
        var rows=jdbc.query("""
            SELECT s.nombre,e.reintentable,e.rebote FROM registro_envio_correo e JOIN estado_envio s USING(id_estado_envio)
            WHERE id_reporte=? ORDER BY creado_en DESC,id_registro_envio DESC LIMIT 1
            """,(rs,i)->new Estado(rs.getString(1),"FALLIDO".equals(rs.getString(1))?"No pudimos entregar el correo a la entidad.":null,
                rs.getBoolean(2),rs.getBoolean(3),ruta),id);
        return rows.isEmpty()?new Estado("SIN_SOLICITAR",null,false,false,ruta):rows.get(0);
    }
    private PlantillaReporte.Datos datos(long id,EnrutamientoServicio.Ruta ruta) {
        List<String> fotos=jdbc.queryForList("SELECT url FROM evidencia_reporte WHERE id_reporte=? ORDER BY orden",String.class,id);
        return jdbc.queryForObject("""
            SELECT r.*,u.nombres,u.apellidos,u.numero_documento,u.direccion_correspondencia,u.correo,u.telefono,
             t.codigo AS tipo_documento,b.nombre AS barrio,v.id_comuna,ST_Y(r.ubicacion) AS lat,ST_X(r.ubicacion) AS lon
            FROM reporte r JOIN usuario u USING(id_usuario) JOIN tipo_documento t USING(id_tipo_documento)
            LEFT JOIN barrio b ON b.id_barrio=r.id_barrio LEFT JOIN v_reporte_comuna v USING(id_reporte)
            WHERE r.id_reporte=?
            """,(rs,i)->new PlantillaReporte.Datos(rs.getString("codigo_seguimiento"),ruta.entidad(),ruta.dependencia(),ruta.categoria(),
             rs.getString("nombres")+" "+rs.getString("apellidos"),rs.getString("tipo_documento")+" "+rs.getString("numero_documento"),
             rs.getString("direccion_correspondencia"),rs.getString("correo"),rs.getString("telefono"),rs.getString("descripcion"),
             rs.getString("direccion_incidente"),rs.getString("barrio"),rs.getString("id_comuna"),(Double)rs.getObject("lat"),(Double)rs.getObject("lon"),
             rs.getObject("creado_en",OffsetDateTime.class),rs.getObject("autorizacion_envio_en",OffsetDateTime.class),rs.getString("matricula_cuenta_servicio"),fotos),id);
    }

    /** Reclamo corto con SKIP LOCKED; ninguna transacción de BD permanece abierta durante la llamada HTTP. */
    public void procesarSiguiente() {
        if (!habilitado) return;
        Map<String,Object> intento=tx.execute(status->{
            var rows=jdbc.queryForList("""
                SELECT e.* FROM registro_envio_correo e JOIN estado_envio s USING(id_estado_envio)
                WHERE s.nombre='PENDIENTE' AND NOT e.procesando
                 AND e.clave_idempotencia IS NOT NULL AND e.mensaje_json IS NOT NULL
                ORDER BY e.creado_en,e.id_registro_envio LIMIT 1 FOR UPDATE OF e SKIP LOCKED
                """);
            if(rows.isEmpty()) return null;
            var e=rows.get(0);
            jdbc.update("UPDATE registro_envio_correo SET procesando=TRUE WHERE id_registro_envio=?",e.get("id_registro_envio"));
            return e;
        });
        if(intento==null) return;
        long id=((Number)intento.get("id_registro_envio")).longValue();
        String proveedor;
        try {
            var reporte=jdbc.queryForMap("SELECT id_categoria,emergencia FROM reporte WHERE id_reporte=?",intento.get("id_reporte"));
            var actual=rutas.resolver(((Number)reporte.get("id_categoria")).intValue(),(Boolean)reporte.get("emergencia"));
            if(!actual.permiteCorreo() || !actual.acceso().equals(intento.get("destino")))
                throw new CorreoNoEnviadoException("El canal ya no permite envío",false);
            Integer previos=jdbc.queryForObject("SELECT count(*) FROM registro_envio_correo WHERE id_reporte=? AND id_registro_envio<?",Integer.class,intento.get("id_reporte"),id);
            OffsetDateTime primero=jdbc.queryForObject("SELECT min(creado_en) FROM registro_envio_correo WHERE id_reporte=?",OffsetDateTime.class,intento.get("id_reporte"));
            if(previos!=null && previos>0 && Duration.between(primero,OffsetDateTime.now()).compareTo(Duration.ofHours(23))>=0)
                throw new CorreoNoEnviadoException("Ventana de idempotencia vencida; requiere conciliación",false);
            CorreoSaliente mensaje=aDestinoDePrueba(json.readValue((String)intento.get("mensaje_json"),CorreoSaliente.class),destinoPrueba);
            proveedor=enviador.enviar(mensaje);
            if(proveedor==null || proveedor.isBlank()) throw new CorreoNoEnviadoException("Proveedor sin confirmación",true);
        } catch(Exception fallo) {
            boolean reintentable=fallo instanceof CorreoNoEnviadoException c && c.reintentable();
            tx.executeWithoutResult(status->jdbc.update("""
                UPDATE registro_envio_correo SET id_estado_envio=(SELECT id_estado_envio FROM estado_envio WHERE nombre='FALLIDO'),
                error='El proveedor no confirmó el envío',reintentable=?,procesando=FALSE WHERE id_registro_envio=?
                """,reintentable,id));
            return;
        }
        final String aceptado=proveedor;
        // Un fallo al persistir la aceptación deja el intento bloqueado; NO se reenvía a ciegas.
        tx.executeWithoutResult(status->{
            jdbc.queryForObject("SELECT id_reporte FROM reporte WHERE id_reporte=? FOR UPDATE",Long.class,intento.get("id_reporte"));
            jdbc.update("""
                UPDATE registro_envio_correo SET id_estado_envio=(SELECT id_estado_envio FROM estado_envio WHERE nombre='ENVIADO'),
                enviado_en=now(),id_envio_proveedor=?,procesando=FALSE WHERE id_registro_envio=?
                """,aceptado,id);
            // tg_historial_sincroniza (V2) actualiza reporte al insertar el historial: no duplicar el UPDATE.
            jdbc.update("""
                INSERT INTO historial_estado(id_reporte,id_estado_reporte,observacion)
                SELECT r.id_reporte,n.id_estado_reporte,'Correo aceptado por el proveedor; no acredita entrega ni lectura.'
                FROM reporte r JOIN estado_reporte actual ON actual.id_estado_reporte=r.id_estado_reporte
                JOIN estado_reporte n ON n.nombre='Enviado' WHERE r.id_reporte=? AND actual.orden<n.orden
                """,intento.get("id_reporte"));
            jdbc.update("""
                UPDATE registro_envio_correo SET id_estado_envio=(SELECT id_estado_envio FROM estado_envio WHERE nombre='FALLIDO'),
                error='El proveedor notificó un fallo posterior',rebote=TRUE,reintentable=FALSE
                WHERE id_registro_envio=? AND EXISTS(SELECT 1 FROM evento_correo WHERE id_envio_proveedor=?)
                """,id,aceptado);
        });
    }
    /**
     * Con destino de prueba, el correo no sale a la entidad real: va a esa dirección, con «[PRUEBA]» en el
     * asunto y una línea que dice a quién habría llegado. Sin destino de prueba devuelve el mismo correo.
     */
    static CorreoSaliente aDestinoDePrueba(CorreoSaliente c, String destinoPrueba) {
        if (destinoPrueba == null || destinoPrueba.isBlank()) return c;
        String aviso = "CORREO DE PRUEBA: el destino real habría sido " + String.join(", ", c.para()) + ".";
        String html = c.html() == null || c.html().isBlank() ? c.html()
                : "<p><strong>" + org.springframework.web.util.HtmlUtils.htmlEscape(aviso) + "</strong></p>" + c.html();
        return new CorreoSaliente(List.of(destinoPrueba.trim()), "[PRUEBA] " + c.asunto(), aviso + "\n\n" + c.texto(),
                html, c.nombreRemitente(), c.responderA(), c.claveIdempotencia());
    }

    private static ApiException error(String m){ return new ApiException(HttpStatus.CONFLICT,"CONFLICTO",m); }
}
