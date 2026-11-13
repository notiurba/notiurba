package com.notiurba.correo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Cada categoría usa la plantilla de su grupo (datos de V3, V8 y V16). */
class GrupoPlantillaTest {

    @ParameterizedTest(name = "categoría {0} → {3}")
    @CsvSource({
            "1, false, true, A", "2, false, true, A", "3, false, true, A", "4, false, true, A",
            "5, false, true, A", "8, false, true, A",
            "6, false, false, B", "7, false, false, B", "9, false, false, B", "10, false, false, B",
            "15, false, false, B",
            "11, true, true, C", "12, true, true, C", "13, true, true, C", "14, true, true, C"})
    void elGrupoSaleDeLaEntidadYDeSiExigeUbicacion(int categoria, boolean esEmpresa, boolean requiereUbicacion,
            GrupoPlantilla esperado) {
        assertThat(GrupoPlantilla.de(esEmpresa, requiereUbicacion)).as("categoría %d", categoria).isEqualTo(esperado);
    }
}
