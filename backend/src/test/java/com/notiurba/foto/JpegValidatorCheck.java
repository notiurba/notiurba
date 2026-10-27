package com.notiurba.foto;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Runs with plain Java 21 when Maven dependencies cannot be downloaded. */
public final class JpegValidatorCheck {
    static int checks;
    public static void main(String[] args) throws Exception {
        run();
        System.out.println("UTC=" + java.time.Instant.now() + " passed=" + checks + " failed=0");
    }
    static void run() throws Exception {
        checks = 0;
        var validator = new JpegValidator();
        var image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        byte[] jpg = output.toByteArray();
        byte[] clean = validator.validateAndSanitize(jpg);
        require(ImageIO.read(new ByteArrayInputStream(clean)).getWidth() == 32, "JPEG decoding");
        require(ImageIO.read(new ByteArrayInputStream(clean)).getHeight() == 24, "dimensions preserved");
        reject(() -> validator.validateAndSanitize(null), "null");
        reject(() -> validator.validateAndSanitize(new byte[0]), "empty");
        reject(() -> validator.validateAndSanitize(new byte[1_000_001]), "over 1 MB");
        reject(() -> validator.validateAndSanitize("fake.jpg".getBytes()), "fake extension");
        output.reset(); ImageIO.write(image, "png", output);
        byte[] png = output.toByteArray();
        reject(() -> validator.validateAndSanitize(png), "PNG rejected");
        reject(() -> validator.validateAndSanitize(new byte[]{(byte)255,(byte)216,(byte)255,(byte)217}), "markers only");
        reject(() -> validator.validateAndSanitize(Arrays.copyOf(jpg,jpg.length-2)), "truncated");
        // Insert JPEG APP1 metadata containing a synthetic GPS marker, then confirm removal.
        byte[] metadata = "SYNTHETIC-GPS-DO-NOT-KEEP".getBytes();
        var withMetadata = new ByteArrayOutputStream();
        withMetadata.write(jpg,0,2); withMetadata.write(255); withMetadata.write(225);
        withMetadata.write(0); withMetadata.write(metadata.length+2); withMetadata.write(metadata);
        withMetadata.write(jpg,2,jpg.length-2);
        byte[] stripped = validator.validateAndSanitize(withMetadata.toByteArray());
        require(!new String(stripped,java.nio.charset.StandardCharsets.ISO_8859_1).contains("SYNTHETIC-GPS"), "metadata stripped");
        var tooBig = new BufferedImage(4001,3000,BufferedImage.TYPE_INT_RGB);
        output.reset(); ImageIO.write(tooBig,"jpg",output); byte[] large = output.toByteArray();
        reject(() -> validator.validateAndSanitize(large), "pixel limit before decode");
        require(ImageIO.read(new ByteArrayInputStream(stripped)) != null, "sanitized image decodable");
        if (checks != 12) throw new AssertionError("Unexpected test count");
    }
    static void reject(Runnable action,String name) {
        try { action.run(); } catch(IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Accepted invalid input: "+name);
    }
    static void require(boolean condition,String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
