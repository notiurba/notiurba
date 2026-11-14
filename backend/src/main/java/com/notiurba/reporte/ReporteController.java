package com.notiurba.reporte;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Reportes del ciudadano (exige sesión: lo cubre anyRequest().authenticated()). */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteServicio servicio;
    private final FotoServicio fotos;
    private final ActualizacionReporteServicio actualizaciones;

    public ReporteController(ReporteServicio servicio, FotoServicio fotos,
            ActualizacionReporteServicio actualizaciones) {
        this.servicio = servicio;
        this.fotos = fotos;
        this.actualizaciones = actualizaciones;
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

    /** Sube una foto (JPG o PNG, hasta 1 MB, máximo 3 por reporte). Solo el autor del reporte. */
    @PostMapping(value = "/{codigo}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoSubidaResponse> subirFoto(
            @PathVariable String codigo, @RequestPart(name = "foto", required = true) MultipartFile foto,
            Authentication sesion) {
        return ResponseEntity.status(201).body(fotos.subir(sesion.getName(), codigo, foto));
    }

    /**
     * «Actualizar mi reporte»: avisa que le respondieron o que se resolvió, y puede anotar el radicado.
     * No cambia el estado oficial (lo confirma el administrador). Solo el autor.
     */
    @PatchMapping("/{codigo}/actualizacion")
    public ReporteDetalleResponse actualizar(@PathVariable String codigo,
            @Valid @RequestBody ActualizarReporteRequest peticion, Authentication sesion) {
        return actualizaciones.actualizar(sesion.getName(), codigo, peticion);
    }
}
