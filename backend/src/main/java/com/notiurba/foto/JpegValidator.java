package com.notiurba.foto;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Pure Java validation: client MIME/filename are never proof of image content. */
public final class JpegValidator {
    public static final int MAX_BYTES = 1_000_000;
    public static final long MAX_PIXELS = 12_000_000;

    public byte[] validateAndSanitize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("Fotografía vacía");
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Máximo 1 MB (1000000 bytes)");
        if (bytes.length < 4 || (bytes[0] & 255) != 255 || (bytes[1] & 255) != 216
                || (bytes[bytes.length - 2] & 255) != 255 || (bytes[bytes.length - 1] & 255) != 217)
            throw new IllegalArgumentException("Se requiere un JPEG completo");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Imagen no decodificable");
            ImageReader reader = readers.next();
            try {
                if (!"JPEG".equalsIgnoreCase(reader.getFormatName()))
                    throw new IllegalArgumentException("Solo se admite JPG");
                var warning = new java.util.concurrent.atomic.AtomicBoolean();
                reader.addIIOReadWarningListener((source, message) -> warning.set(true));
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS)
                    throw new IllegalArgumentException("Dimensiones de imagen no permitidas");
                BufferedImage decoded = reader.read(0);
                if (decoded == null || warning.get()) throw new IllegalArgumentException("JPEG inválido");
                // A fresh image and encode discard EXIF/GPS, comments and appended content.
                BufferedImage clean = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                var graphics = clean.createGraphics();
                try { graphics.drawImage(decoded, 0, 0, null); } finally { graphics.dispose(); }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(clean, "jpg", output)) throw new IOException("JPEG encoder unavailable");
                if (output.size() > MAX_BYTES) throw new IllegalArgumentException("JPEG procesado supera 1 MB");
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException e) {
            throw new IllegalArgumentException("Imagen no decodificable");
        }
    }
}
