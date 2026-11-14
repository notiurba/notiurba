package com.notiurba.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.notiurba.reporte.FiltroPanel;

@RestController
@RequestMapping("/api/admin")
public class MapaAdminController {

    private final MapaAdminServicio servicio;

    public MapaAdminController(MapaAdminServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/mapa")
    public MapaResponse mapa(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer comuna,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.mapa(FiltroPanel.de(estado, idCategoria, comuna, desde, hasta));
    }

    /** {@code pagina} y {@code tamano} paginan solo el ranking de comunas críticas. */
    @GetMapping("/mapa/calor")
    public CalorResponse calor(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer comuna,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "5") int tamano) {
        return servicio.calor(FiltroPanel.de(estado, idCategoria, comuna, desde, hasta), pagina, tamano);
    }

    /** {@code vigencia}: vigentes, fuera_de_plazo o todos (por defecto). */
    @GetMapping("/estadisticas")
    public EstadisticasResponse estadisticas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer comuna,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String vigencia) {
        return servicio.estadisticas(FiltroPanel.de(estado, idCategoria, comuna, desde, hasta), vigencia);
    }
}
