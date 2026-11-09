package com.notiurba.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.notiurba.web.error.ApiException;

/** Contraseña: de 8 a 72 caracteres (72 bytes), con letras y números. El motivo del rechazo es concreto. */
class ReglaContrasenaTest {

    private static void rechaza(String contrasena, String mensaje) {
        assertThatThrownBy(() -> ReglaContrasena.validar(contrasena, "password"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCampo()).isEqualTo("password");
                    assertThat(e.getReason()).isEqualTo(mensaje);
                });
    }

    @Test
    void aceptaContrasenasValidas() {
        assertThat(ReglaContrasena.cumple("Abcdef12")).isTrue();
        assertThat(ReglaContrasena.cumple("clave segura 2026")).isTrue();
        assertThat(ReglaContrasena.cumple("Contraseña1ñ")).isTrue();            // letras con tilde cuentan
        assertThat(ReglaContrasena.cumple("a1" + "x".repeat(70))).isTrue();     // 72 bytes justos
    }

    @Test
    void rechazaCortasYSinLetrasONumeros() {
        rechaza(null, ReglaContrasena.MENSAJE);
        rechaza("Ab1", ReglaContrasena.MENSAJE);
        rechaza("soloLetrasAqui", ReglaContrasena.MENSAJE);
        rechaza("1234567890", ReglaContrasena.MENSAJE);
        rechaza("        ", ReglaContrasena.MENSAJE);
    }

    @Test
    void siEsMuyLargaDiceQueEsMuyLargaNoQueEsSimple() {
        rechaza("a1" + "x".repeat(71), ReglaContrasena.MENSAJE_LARGA);          // 73 caracteres
        rechaza("a1" + "ñ".repeat(40), ReglaContrasena.MENSAJE_LARGA);          // 42 caracteres pero 82 bytes
    }
}
