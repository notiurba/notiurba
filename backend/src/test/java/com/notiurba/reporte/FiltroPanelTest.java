package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

class FiltroPanelTest {

    private static void rechaza(Runnable accion, String campo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatusCode().value()).isEqualTo(400);
            assertThat(e.getCodigo()).isEqualTo(Codigos.VALIDACION);
            assertThat(e.getCampo()).isEqualTo(campo);
            assertThat(e.getReason()).isNotBlank();
        });
    }

    @Test
    void sinFiltrosTodoQuedaVacioYNoAgregaCondiciones() {
        FiltroPanel f = FiltroPanel.de(null, null, null, null, null);
        var p = new MapSqlParameterSource();

        assertThat(f).isEqualTo(new FiltroPanel(null, null, null, null, null));
        assertThat(f.condiciones(p)).isEmpty();
        assertThat(p.getParameterNames()).isEmpty();
    }

    @Test
    void elEstadoSeAceptaPorNombreOPorIdentificadorYSeGuardaConElNombreDelCatalogo() {
        assertThat(FiltroPanel.de("EN_TRAMITE", null, null, null, null).estado()).isEqualTo("En trámite");
        assertThat(FiltroPanel.de(" en trámite ", null, null, null, null).estado()).isEqualTo("En trámite");
        assertThat(FiltroPanel.de("  ", null, null, null, null).estado()).isNull();
    }

    @Test
    void cadaFiltroAgregaSuCondicionConParametros() {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 10);
        var p = new MapSqlParameterSource();

        String sql = FiltroPanel.de("Resuelto", 3, 10, desde, hasta).condiciones(p);

        assertThat(sql).contains("est.nombre = :estado", "c.id_categoria = :categoria", "r.id_comuna = :comuna",
                ">= :desde", "<= :hasta", "America/Bogota");
        assertThat(p.getValue("estado")).isEqualTo("Resuelto");
        assertThat(p.getValue("categoria")).isEqualTo(3);
        assertThat(p.getValue("comuna")).isEqualTo(10);
        assertThat(p.getValue("desde")).isEqualTo(desde);
        assertThat(p.getValue("hasta")).isEqualTo(hasta);
        assertThat(FiltroPanel.de(null, null, 1, desde, desde).comuna()).isEqualTo(1);
    }

    @Test
    void rechazaUnEstadoDesconocido() {
        rechaza(() -> FiltroPanel.de("Cancelado", null, null, null, null), "estado");
        rechaza(() -> FiltroPanel.nombreDeEstado("Cancelado"), "estado");
    }

    @Test
    void rechazaUnaComunaFueraDe1A10() {
        rechaza(() -> FiltroPanel.de(null, null, 0, null, null), "comuna");
        rechaza(() -> FiltroPanel.de(null, null, 11, null, null), "comuna");
        rechaza(() -> FiltroPanel.de(null, null, -3, null, null), "comuna");
    }

    @Test
    void rechazaDesdePosteriorAHasta() {
        rechaza(() -> FiltroPanel.de(null, null, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)), "desde");
    }
}
