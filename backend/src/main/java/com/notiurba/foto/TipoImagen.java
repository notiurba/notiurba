package com.notiurba.foto;

import java.util.Optional;

/**
 * Detecta el tipo REAL de una imagen por sus primeros bytes, sin fiarse de la extensión ni del
 * Content-Type que declara quien sube el archivo. Solo JPG y PNG (RF-06).
 */
public final class TipoImagen {

    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";

    private static final byte[] FIRMA_PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    private TipoImagen() {
    }

    public static Optional<String> detectar(byte[] contenido) {
        if (contenido.length >= 3 && (contenido[0] & 0xFF) == 0xFF && (contenido[1] & 0xFF) == 0xD8
                && (contenido[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if (contenido.length >= FIRMA_PNG.length) {
            for (int i = 0; i < FIRMA_PNG.length; i++) {
                if (contenido[i] != FIRMA_PNG[i]) {
                    return Optional.empty();
                }
            }
            return Optional.of(PNG);
        }
        return Optional.empty();
    }
}
