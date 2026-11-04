package com.notiurba.foto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TipoImagenTest {

    @Test
    void reconoceJpegYPngPorSusPrimerosBytes() {
        assertThat(TipoImagen.detectar(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16 }))
                .contains(TipoImagen.JPEG);
        assertThat(TipoImagen.detectar(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0 }))
                .contains(TipoImagen.PNG);
    }

    @Test
    void rechazaTodoLoDemasAunqueSeaMuyParecido() {
        assertThat(TipoImagen.detectar("GIF89a......".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(TipoImagen.detectar("<html>soy una página</html>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(TipoImagen.detectar("%PDF-1.7".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(TipoImagen.detectar(new byte[] { (byte) 0xFF, (byte) 0xD8 })).isEmpty();          // JPEG cortado
        assertThat(TipoImagen.detectar(new byte[] { (byte) 0x89, 'P', 'N', 'G' })).isEmpty();        // PNG cortado
        assertThat(TipoImagen.detectar(new byte[0])).isEmpty();
    }
}
