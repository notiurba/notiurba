package com.notiurba.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.notiurba.web.error.ApiException;

/** Nombres y apellidos: la misma regla que el frontend (letras, hasta 60). */
class ReglasPerfilTest {

    private static String limpiar(String valor) {
        return ReglasPerfil.limpiarNombre(valor, "Los nombres", "nombres");
    }

    private static void rechaza(String valor, String fragmentoDelMensaje) {
        assertThatThrownBy(() -> limpiar(valor))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCampo()).isEqualTo("nombres");
                    assertThat(e.getMessage()).contains(fragmentoDelMensaje);
                });
    }

    @Test
    void aceptaNombresRealesConTildesEnieYSignosComunes() {
        assertThat(limpiar("María José")).isEqualTo("María José");
        assertThat(limpiar("Peña Núñez")).isEqualTo("Peña Núñez");
        assertThat(limpiar("D'Alessandro")).isEqualTo("D'Alessandro");
        assertThat(limpiar("De la Cruz-Pérez")).isEqualTo("De la Cruz-Pérez");
        assertThat(limpiar("Jr. Ríos")).isEqualTo("Jr. Ríos");
        assertThat(limpiar("Jo")).isEqualTo("Jo");
    }

    @Test
    void recortaYUneLosEspaciosRepetidos() {
        assertThat(limpiar("  Ana    María  ")).isEqualTo("Ana María");
    }

    @Test
    void rechazaVacioMuyCortoYMuyLargo() {
        rechaza(null, "son obligatorios");
        rechaza("   ", "son obligatorios");
        rechaza("A", "al menos 2");
        rechaza("A".repeat(61), "no pueden pasar de 60");
        assertThat(limpiar("A".repeat(60))).hasSize(60);
    }

    @Test
    void rechazaNumerosSimbolosYTextoRaro() {
        rechaza("Ana123", "solo pueden llevar letras");
        rechaza("12345", "solo pueden llevar letras");
        rechaza("Ana <script>", "solo pueden llevar letras");
        rechaza("Ana@correo.com", "solo pueden llevar letras");
        rechaza("-Ana", "solo pueden llevar letras");           // debe empezar con una letra
        rechaza("😀😀", "solo pueden llevar letras");
        rechaza("Ana\u0000María", "solo pueden llevar letras");
    }
}
