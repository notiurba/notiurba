package com.notiurba.admin;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.reporte.ListadoReportesServicio;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/** Mapa, mapa de calor y estadísticas del panel de administración (NOT-129, RF-22 a RF-24). */
@Service
public class MapaAdminServicio {

    private final MapaAdminRepositorio repositorio;

    public MapaAdminServicio(MapaAdminRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public MapaResponse mapa(String estado, Integer idCategoria, Integer comuna, LocalDate desde, LocalDate hasta) {
        return repositorio.mapa(filtro(estado, idCategoria, comuna, desde, hasta));
    }

    /**
     * Valida los filtros comunes con las mismas reglas y mensajes que el listado del panel
     * ({@code GET /api/admin/reportes}). La categoría no se valida: una que no existe da una lista vacía.
     */
    static FiltroMapa filtro(String estado, Integer idCategoria, Integer comuna, LocalDate desde, LocalDate hasta) {
        if (comuna != null && (comuna < 1 || comuna > 10)) {
            throw error("La comuna es un número de 1 a 10", "comuna");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw error("La fecha «desde» no puede ser posterior a «hasta»", "desde");
        }
        return new FiltroMapa(ListadoReportesServicio.nombreDeEstado(estado).orElse(null), idCategoria, comuna,
                desde, hasta);
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
