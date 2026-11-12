package com.notiurba.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/** NOT-129: validación de los filtros comunes del mapa, del calor y de las estadísticas (CA-10). */
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
}
