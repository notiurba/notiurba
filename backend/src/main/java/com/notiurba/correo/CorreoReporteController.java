package com.notiurba.correo;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
public class CorreoReporteController {
    private final EnrutamientoServicio rutas;
    private final EnvioReporteServicio servicio;
    private final JdbcTemplate jdbc;
    public CorreoReporteController(EnrutamientoServicio rutas,EnvioReporteServicio servicio,JdbcTemplate jdbc){
        this.rutas=rutas;this.servicio=servicio;this.jdbc=jdbc;
    }
    @GetMapping("/api/enrutamiento/categorias")
    public List<EnrutamientoServicio.Ruta> categorias(){return rutas.listar();}
    @GetMapping("/api/enrutamiento/categorias/{id}")
    public EnrutamientoServicio.Ruta resolver(@PathVariable int id,@RequestParam(defaultValue="false") boolean emergencia){return rutas.resolver(id,emergencia);}
    @GetMapping("/api/reportes/opciones/barrios")
    public List<Map<String,Object>> barrios(){return jdbc.queryForList("SELECT id_barrio AS \"idBarrio\", nombre, id_comuna AS \"idComuna\" FROM barrio WHERE NOT interno ORDER BY nombre");}
    @GetMapping("/api/reportes")
    public List<Map<String,Object>> propios(Authentication sesion){return jdbc.queryForList("""
        SELECT r.codigo_seguimiento AS "codigoSeguimiento",c.nombre AS categoria,s.nombre AS estado,
         (SELECT se.nombre FROM registro_envio_correo ec JOIN estado_envio se USING(id_estado_envio)
          WHERE ec.id_reporte=r.id_reporte ORDER BY ec.creado_en DESC,ec.id_registro_envio DESC LIMIT 1) AS correo
        FROM reporte r JOIN usuario u USING(id_usuario) JOIN categoria c USING(id_categoria)
        JOIN estado_reporte s USING(id_estado_reporte)
        WHERE lower(u.correo)=lower(?) ORDER BY r.creado_en DESC LIMIT 100
        """,sesion.getName());}
    @PostMapping("/api/reportes/{codigo}/enviar")
    public ResponseEntity<EnvioReporteServicio.Estado> enviar(@PathVariable String codigo,Authentication sesion){
        return ResponseEntity.accepted().body(servicio.solicitar(sesion.getName(),codigo));
    }
    @GetMapping("/api/reportes/{codigo}/envio")
    public EnvioReporteServicio.Estado estado(@PathVariable String codigo,Authentication sesion){return servicio.consultar(sesion.getName(),codigo);}
}
