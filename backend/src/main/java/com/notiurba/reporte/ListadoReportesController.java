package com.notiurba.reporte;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listados de reportes. «Mis reportes» exige sesión; el del panel lo restringe al rol ADMIN
 * SecurityConfig (/api/admin/**).
 */
@RestController
public class ListadoReportesController {

    private final ListadoReportesServicio servicio;

    public ListadoReportesController(ListadoReportesServicio servicio) {
        this.servicio = servicio;
    }

    /** Los reportes del ciudadano con sesión, los más recientes primero (hasta 100). Filtro opcional {@code estado}. */
    @GetMapping("/api/reportes")
    public List<ReporteResumenResponse> misReportes(@RequestParam(required = false) String estado,
            Authentication sesion) {
        return servicio.misReportes(sesion.getName(), estado);
    }

    /** Todos los reportes para el panel (hasta 500), con filtros opcionales por estado, categoría, comuna y fechas. */
    @GetMapping("/api/admin/reportes")
    public List<ReporteDetalleResponse> todos(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer comuna,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.todos(estado, idCategoria, comuna, desde, hasta);
    }
}
