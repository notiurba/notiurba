package com.notiurba.reporte;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Reportes del ciudadano (exige sesión: lo cubre anyRequest().authenticated()). */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteServicio servicio;

    public ReporteController(ReporteServicio servicio) {
        this.servicio = servicio;
    }

    /** 201 con Location: /api/reportes/{codigo} y el código leído de la base tras guardar (NOT-69). */
    @PostMapping
    public ResponseEntity<ReporteCreadoResponse> crear(
            @Valid @RequestBody CrearReporteRequest peticion, Authentication sesion) {
        ReporteCreadoResponse creado = servicio.crear(sesion.getName(), peticion);
        return ResponseEntity.created(URI.create("/api/reportes/" + creado.codigoSeguimiento())).body(creado);
    }

    /** El reporte completo, solo si es del usuario con sesión (404 si no existe o es de otra persona). */
    @GetMapping("/{codigo}")
    public ReporteDetalleResponse consultar(@PathVariable String codigo, Authentication sesion) {
        return servicio.consultar(sesion.getName(), codigo);
    }
}
