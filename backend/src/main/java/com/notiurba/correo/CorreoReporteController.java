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
    @PostMapping("/api/reportes/{codigo}/enviar")
    public ResponseEntity<EnvioReporteServicio.Estado> enviar(@PathVariable String codigo,Authentication sesion){
        return ResponseEntity.accepted().body(servicio.solicitar(sesion.getName(),codigo));
    }
    @GetMapping("/api/reportes/{codigo}/envio")
    public EnvioReporteServicio.Estado estado(@PathVariable String codigo,Authentication sesion){return servicio.consultar(sesion.getName(),codigo);}
}
