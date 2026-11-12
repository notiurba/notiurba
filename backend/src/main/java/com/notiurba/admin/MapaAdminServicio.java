package com.notiurba.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.admin.MapaAdminRepositorio.ConteoComuna;
import com.notiurba.reporte.ListadoReportesServicio;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

@Service
public class MapaAdminServicio {

    static final int TAMANO_MAXIMO = 10;

    private final MapaAdminRepositorio repositorio;

    public MapaAdminServicio(MapaAdminRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public MapaResponse mapa(String estado, Integer idCategoria, Integer comuna, LocalDate desde, LocalDate hasta) {
        return repositorio.mapa(filtro(estado, idCategoria, comuna, desde, hasta));
    }

    @Transactional(readOnly = true)
    public CalorResponse calor(String estado, Integer idCategoria, Integer comuna, LocalDate desde, LocalDate hasta,
            int pagina, int tamano) {
        FiltroMapa f = filtro(estado, idCategoria, comuna, desde, hasta);
        validarPaginacion(pagina, tamano);
        MapaAdminRepositorio.DatosCalor datos = repositorio.calor(f);
        long sinComuna = datos.comunas().stream().filter(c -> c.comuna() == null).mapToLong(ConteoComuna::total).sum();
        return new CalorResponse(datos.totalReportes(), datos.totalPuntos(),
                datos.totalPuntos() > datos.puntos().size(), sinComuna, datos.puntos(),
                paginar(datos.comunas(), pagina, tamano));
    }

    /** Mismas reglas y mensajes que el listado del panel. Una categoría que no existe da una lista vacía. */
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

    static void validarPaginacion(int pagina, int tamano) {
        if (pagina < 1) {
            throw error("La página empieza en 1", "pagina");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw error("El tamaño de página es un número de 1 a " + TAMANO_MAXIMO, "tamano");
        }
    }

    /** En empate va primero la comuna de menor número. Una página fuera de rango queda vacía. */
    static CalorResponse.ComunasCriticas paginar(List<ConteoComuna> conteos, int pagina, int tamano) {
        List<ConteoComuna> ranking = conteos.stream()
                .filter(c -> Objects.nonNull(c.comuna()))
                .sorted(Comparator.comparingLong(ConteoComuna::total).reversed().thenComparing(ConteoComuna::comuna))
                .toList();
        int total = ranking.size();
        int totalPaginas = (total + tamano - 1) / tamano;
        long inicio = (long) (pagina - 1) * tamano;
        List<CalorResponse.ComunaCritica> items = new ArrayList<>();
        for (long i = inicio; i < Math.min(inicio + tamano, total); i++) {
            ConteoComuna c = ranking.get((int) i);
            items.add(new CalorResponse.ComunaCritica((int) i + 1, c.comuna(), c.total(), c.categoriaPrincipal()));
        }
        return new CalorResponse.ComunasCriticas(pagina, tamano, total, totalPaginas, items);
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
