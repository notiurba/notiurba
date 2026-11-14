package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Del reporte de otra persona solo se muestra un resumen corto: una línea, palabras completas, con «…» si se acortó. */
class ResumenDeDescripcionTest {

    @Test
    void unTextoCortoQuedaIgualPeroEnUnaSolaLinea() {
        assertThat(DuplicadoServicio.resumir("  Hueco grande\n\n frente a la   panadería  ")).isEqualTo("Hueco grande frente a la panadería");
    }

    @Test
    void unTextoLargoSeCortaEnUnaPalabraCompletaYTerminaConPuntosSuspensivos() {
        String largo = "Hay un hueco muy grande frente a la panadería de la esquina que daña las llantas de los carros y las motos "
                + "cada vez que pasan por la calle principal del barrio, especialmente de noche cuando casi no se ve nada";
        String resumen = DuplicadoServicio.resumir(largo);

        assertThat(resumen.length()).isLessThanOrEqualTo(141);
        assertThat(resumen).endsWith("…").doesNotContain("\n");
        String sinPuntos = resumen.substring(0, resumen.length() - 1);
        assertThat(largo).startsWith(sinPuntos);
        assertThat(largo.charAt(sinPuntos.length())).as("corta en una palabra completa").isEqualTo(' ');
    }

    @Test
    void sinEspaciosParaCortarCorta140CaracteresExactos() {
        String sinEspacios = "x".repeat(300);
        assertThat(DuplicadoServicio.resumir(sinEspacios)).isEqualTo("x".repeat(140) + "…");
    }

    @Test
    void nuloOVacioDaVacio() {
        assertThat(DuplicadoServicio.resumir(null)).isEmpty();
        assertThat(DuplicadoServicio.resumir("   ")).isEmpty();
    }
}
