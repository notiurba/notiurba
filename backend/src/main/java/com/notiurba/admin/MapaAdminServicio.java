package com.notiurba.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.admin.MapaAdminRepositorio.ConteoComuna;
import com.notiurba.reporte.ListadoReportesServicio;
import com.notiurba.reporte.ReporteDetalleResponse;
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

    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas(String estado, Integer idCategoria, Integer comuna, LocalDate desde,
            LocalDate hasta, String vigencia) {
        FiltroMapa f = filtro(estado, idCategoria, comuna, desde, hasta);
        FiltroVigencia v = vigencia(vigencia);
        return armar(v, repositorio.estadisticas(f, v), repositorio.categoriasActivas());
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

    static FiltroVigencia vigencia(String valor) {
        if (valor == null || valor.isBlank()) {
            return FiltroVigencia.TODOS;
        }
        for (FiltroVigencia v : FiltroVigencia.values()) {
            if (v.valor().equalsIgnoreCase(valor.trim())) {
                return v;
            }
        }
        throw error("La vigencia es vigentes, fuera_de_plazo o todos", "vigencia");
    }

    /**
     * Completa con ceros los 4 estados, las 10 comunas y las categorías activas, para que las tablas del panel
     * salgan completas. Las categorías van de más a menos reportes.
     */
    static EstadisticasResponse armar(FiltroVigencia vigencia, List<MapaAdminRepositorio.ConteoEstadistica> filas,
            List<ReporteDetalleResponse.Categoria> catalogo) {
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoReporte e : EstadoReporte.values()) {
            porEstado.put(e.nombre(), 0L);
        }
        Map<Integer, String> nombres = new LinkedHashMap<>();
        Map<Integer, Long> porCategoria = new HashMap<>();
        for (ReporteDetalleResponse.Categoria c : catalogo) {
            nombres.put(c.idCategoria(), c.nombre());
            porCategoria.put(c.idCategoria(), 0L);
        }
        long[] porComuna = new long[10];
        long total = 0;
        long sinComuna = 0;
        for (MapaAdminRepositorio.ConteoEstadistica fila : filas) {
            total += fila.total();
            porEstado.merge(fila.estado(), fila.total(), Long::sum);
            nombres.putIfAbsent(fila.idCategoria(), fila.categoria());
            porCategoria.merge(fila.idCategoria(), fila.total(), Long::sum);
            if (fila.comuna() == null) {
                sinComuna += fila.total();
            } else {
                porComuna[fila.comuna() - 1] += fila.total();
            }
        }

        long atendidos = porEstado.getOrDefault(EstadoReporte.RESUELTO.nombre(), 0L);
        BigDecimal porcentaje = total == 0 ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(atendidos * 100).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        List<EstadisticasResponse.PorEstado> estados = porEstado.entrySet().stream()
                .map(e -> new EstadisticasResponse.PorEstado(e.getKey(), e.getValue())).toList();
        List<EstadisticasResponse.PorCategoria> categorias = nombres.entrySet().stream()
                .map(c -> new EstadisticasResponse.PorCategoria(c.getKey(), c.getValue(), porCategoria.get(c.getKey())))
                .sorted(Comparator.comparingLong(EstadisticasResponse.PorCategoria::total).reversed()
                        .thenComparingInt(EstadisticasResponse.PorCategoria::idCategoria))
                .toList();
        List<EstadisticasResponse.PorComuna> comunas = new ArrayList<>();
        for (int i = 0; i < porComuna.length; i++) {
            comunas.add(new EstadisticasResponse.PorComuna(i + 1, porComuna[i]));
        }
        return new EstadisticasResponse(vigencia.valor(), total, atendidos, porcentaje, sinComuna, estados, categorias,
                comunas);
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
