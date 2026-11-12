package com.notiurba.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mapa del panel (NOT-129). SecurityConfig restringe /api/admin/** al rol ADMIN: 401 sin sesión y 403 a un
 * ciudadano, con el formato de error del contrato.
 */
@RestController
@RequestMapping("/api/admin")
public class MapaAdminController {

    private final MapaAdminServicio servicio;

    public MapaAdminController(MapaAdminServicio servicio) {
        this.servicio = servicio;
    }

    /** Reportes vigentes con ubicación (RF-22), con los filtros comunes del panel. */
    @GetMapping("/mapa")
    public MapaResponse mapa(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer comuna,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.mapa(estado, idCategoria, comuna, desde, hasta);
    }
}
