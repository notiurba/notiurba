package com.notiurba.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.notiurba.reporte.ReporteDetalleResponse;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

class MapaAdminServicioTest {

    private static void rechaza(Runnable accion, String campo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatusCode().value()).isEqualTo(400);
            assertThat(e.getCodigo()).isEqualTo(Codigos.VALIDACION);
            assertThat(e.getCampo()).isEqualTo(campo);
            assertThat(e.getReason()).isNotBlank();
        });
    }

    @Test
    void sinFiltrosTodoQuedaVacio() {
        FiltroMapa f = MapaAdminServicio.filtro(null, null, null, null, null);

        assertThat(f).isEqualTo(new FiltroMapa(null, null, null, null, null));
    }

    @Test
    void elEstadoSeAceptaPorNombreOPorIdentificadorYSeGuardaConElNombreDelCatalogo() {
        assertThat(MapaAdminServicio.filtro("EN_TRAMITE", null, null, null, null).estado()).isEqualTo("En trámite");
        assertThat(MapaAdminServicio.filtro("en trámite", null, null, null, null).estado()).isEqualTo("En trámite");
        assertThat(MapaAdminServicio.filtro("  ", null, null, null, null).estado()).isNull();
    }

    @Test
    void conservaLosFiltrosValidos() {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 10);

        FiltroMapa f = MapaAdminServicio.filtro("Resuelto", 3, 10, desde, hasta);

        assertThat(f).isEqualTo(new FiltroMapa("Resuelto", 3, 10, desde, hasta));
        assertThat(MapaAdminServicio.filtro(null, null, 1, desde, desde).comuna()).isEqualTo(1);
    }

    @Test
    void rechazaUnEstadoDesconocido() {
        rechaza(() -> MapaAdminServicio.filtro("Cancelado", null, null, null, null), "estado");
    }

    @Test
    void rechazaUnaComunaFueraDe1A10() {
        rechaza(() -> MapaAdminServicio.filtro(null, null, 0, null, null), "comuna");
        rechaza(() -> MapaAdminServicio.filtro(null, null, 11, null, null), "comuna");
        rechaza(() -> MapaAdminServicio.filtro(null, null, -3, null, null), "comuna");
    }

    @Test
    void rechazaDesdePosteriorAHasta() {
        rechaza(() -> MapaAdminServicio.filtro(null, null, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)),
                "desde");
    }

    private static MapaAdminRepositorio.ConteoComuna conteo(Integer comuna, long total) {
        return new MapaAdminRepositorio.ConteoComuna(comuna, total, new ReporteDetalleResponse.Categoria(1, "Vías"));
    }

    @Test
    void ordenaDeMasAMenosYEnEmpateVaPrimeroLaComunaDeMenorNumero() {
        var conteos = List.of(conteo(7, 1), conteo(3, 2), conteo(5, 3), conteo(2, 2), conteo(1, 1));

        CalorResponse.ComunasCriticas p = MapaAdminServicio.paginar(conteos, 1, 5);

        assertThat(p.items()).extracting(CalorResponse.ComunaCritica::comuna).containsExactly(5, 2, 3, 1, 7);
        assertThat(p.items()).extracting(CalorResponse.ComunaCritica::posicion).containsExactly(1, 2, 3, 4, 5);
        assertThat(p.totalComunas()).isEqualTo(5);
        assertThat(p.totalPaginas()).isEqualTo(1);
    }

    @Test
    void laSegundaPaginaSigueLaPosicionDelRanking() {
        var conteos = List.of(conteo(1, 6), conteo(2, 5), conteo(3, 4), conteo(4, 3), conteo(5, 2), conteo(6, 1));

        CalorResponse.ComunasCriticas p = MapaAdminServicio.paginar(conteos, 2, 5);

        assertThat(p.pagina()).isEqualTo(2);
        assertThat(p.tamano()).isEqualTo(5);
        assertThat(p.totalPaginas()).isEqualTo(2);
        assertThat(p.items()).extracting(CalorResponse.ComunaCritica::comuna).containsExactly(6);
        assertThat(p.items()).extracting(CalorResponse.ComunaCritica::posicion).containsExactly(6);
    }

    @Test
    void unaPaginaFueraDeRangoYUnaListaVaciaNoFallan() {
        assertThat(MapaAdminServicio.paginar(List.of(conteo(1, 1)), 4, 5).items()).isEmpty();
        CalorResponse.ComunasCriticas vacia = MapaAdminServicio.paginar(List.of(), 1, 5);
        assertThat(vacia.items()).isEmpty();
        assertThat(vacia.totalComunas()).isZero();
        assertThat(vacia.totalPaginas()).isZero();
    }

    @Test
    void validaPaginaYTamano() {
        rechaza(() -> MapaAdminServicio.validarPaginacion(0, 5), "pagina");
        rechaza(() -> MapaAdminServicio.validarPaginacion(-1, 5), "pagina");
        rechaza(() -> MapaAdminServicio.validarPaginacion(1, 0), "tamano");
        rechaza(() -> MapaAdminServicio.validarPaginacion(1, 11), "tamano");
        MapaAdminServicio.validarPaginacion(1, 1);
        MapaAdminServicio.validarPaginacion(3, 10);
    }

    // estadísticas

    @Test
    void laVigenciaPorDefectoEsTodosYAceptaMayusculas() {
        assertThat(MapaAdminServicio.vigencia(null)).isEqualTo(FiltroVigencia.TODOS);
        assertThat(MapaAdminServicio.vigencia(" ")).isEqualTo(FiltroVigencia.TODOS);
        assertThat(MapaAdminServicio.vigencia("vigentes")).isEqualTo(FiltroVigencia.VIGENTES);
        assertThat(MapaAdminServicio.vigencia("FUERA_DE_PLAZO")).isEqualTo(FiltroVigencia.FUERA_DE_PLAZO);
        assertThat(FiltroVigencia.FUERA_DE_PLAZO.valor()).isEqualTo("fuera_de_plazo");
        rechaza(() -> MapaAdminServicio.vigencia("vencidos"), "vigencia");
    }

    private static MapaAdminRepositorio.ConteoEstadistica fila(String estado, int idCategoria, Integer comuna,
            long total) {
        return new MapaAdminRepositorio.ConteoEstadistica(estado, idCategoria, "Categoría " + idCategoria, comuna,
                total);
    }

    @Test
    void sumaPorEstadoCategoriaYComunaYCompletaConCeros() {
        var filas = List.of(fila("Recibido", 1, 5, 4), fila("Resuelto", 1, 5, 2), fila("Resuelto", 2, 3, 1),
                fila("En trámite", 2, null, 2), fila("Recibido", 9, 3, 3));
        var catalogo = List.of(new ReporteDetalleResponse.Categoria(1, "Categoría 1"),
                new ReporteDetalleResponse.Categoria(2, "Categoría 2"),
                new ReporteDetalleResponse.Categoria(3, "Categoría 3"));

        EstadisticasResponse e = MapaAdminServicio.armar(FiltroVigencia.TODOS, filas, catalogo);

        assertThat(e.vigencia()).isEqualTo("todos");
        assertThat(e.total()).isEqualTo(12);
        assertThat(e.atendidos()).isEqualTo(3);
        assertThat(e.porcentajeAtendidos()).isEqualByComparingTo("25.0");
        assertThat(e.sinComuna()).isEqualTo(2);
        assertThat(e.porEstado()).extracting(EstadisticasResponse.PorEstado::estado)
                .containsExactly("Recibido", "Enviado", "En trámite", "Resuelto");
        assertThat(e.porEstado()).extracting(EstadisticasResponse.PorEstado::total).containsExactly(7L, 0L, 2L, 3L);
        assertThat(e.porComuna()).hasSize(10);
        assertThat(e.porComuna()).extracting(EstadisticasResponse.PorComuna::comuna)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(e.porComuna().get(4).total()).isEqualTo(6);
        assertThat(e.porComuna().get(2).total()).isEqualTo(4);
        assertThat(e.porComuna().get(0).total()).isZero();
        // de más a menos; la 9 no está en el catálogo activo pero tiene reportes y aparece igual
        assertThat(e.porCategoria()).extracting(EstadisticasResponse.PorCategoria::idCategoria)
                .containsExactly(1, 2, 9, 3);
        assertThat(e.porCategoria()).extracting(EstadisticasResponse.PorCategoria::total)
                .containsExactly(6L, 3L, 3L, 0L);
    }

    @Test
    void elPorcentajeDeAtendidosLlevaUnDecimalYEsCeroSinReportes() {
        var catalogo = List.of(new ReporteDetalleResponse.Categoria(1, "Categoría 1"));

        assertThat(MapaAdminServicio.armar(FiltroVigencia.TODOS,
                List.of(fila("Resuelto", 1, 1, 1), fila("Recibido", 1, 1, 2)), catalogo).porcentajeAtendidos())
                .isEqualByComparingTo("33.3");
        assertThat(MapaAdminServicio.armar(FiltroVigencia.TODOS,
                List.of(fila("Resuelto", 1, 1, 2), fila("Recibido", 1, 1, 1)), catalogo).porcentajeAtendidos())
                .isEqualByComparingTo("66.7");

        EstadisticasResponse vacia = MapaAdminServicio.armar(FiltroVigencia.VIGENTES, List.of(), catalogo);
        assertThat(vacia.total()).isZero();
        assertThat(vacia.porcentajeAtendidos()).isEqualByComparingTo("0");
        assertThat(vacia.porEstado()).extracting(EstadisticasResponse.PorEstado::total).containsOnly(0L);
        assertThat(vacia.porCategoria()).extracting(EstadisticasResponse.PorCategoria::total).containsExactly(0L);
        assertThat(vacia.vigencia()).isEqualTo("vigentes");
    }
}
