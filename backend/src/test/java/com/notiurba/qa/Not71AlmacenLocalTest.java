package com.notiurba.qa;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
import com.notiurba.foto.AlmacenFotosNoDisponibleException;

/** HTTP y disco reales, sin Spring, credenciales, base ni servicios externos. */
class Not71AlmacenLocalTest {
    @TempDir Path root;

    @Test void conservaYRecuperaBytesSinDuplicarElArchivo() throws Exception {
        byte[] bytes = { (byte)0xff, (byte)0xd8, 1, 2, 3, (byte)0xff, (byte)0xd9 };
        try (var store = new Not71ServidorQA.LocalPhotoStore(root, 0)) {
            var foto = store.guardar(bytes, "image/jpeg");
            assertThat(store.guardar(bytes, "image/jpeg")).isEqualTo(foto);
            var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(foto.url())).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type")).contains("image/jpeg");
            assertThat(response.body()).isEqualTo(bytes);
            var name = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)) + ".jpg";
            assertThat(Files.readAllBytes(root.resolve(name))).isEqualTo(bytes);
            try (var files = Files.list(root)) { assertThat(files.filter(p -> p.toString().endsWith(".jpg")).count()).isEqualTo(1); }
        }
    }

    @Test void noExponeArchivosAjenosNiPermiteEscriturasHttp() throws Exception {
        Files.writeString(root.resolve("backend.log"), "diagnostico privado");
        try (var store = new Not71ServidorQA.LocalPhotoStore(root, 0)) {
            var foto = store.guardar(new byte[]{1,2,3}, "image/png");
            var base = URI.create(foto.url()).resolve("backend.log");
            var client = HttpClient.newHttpClient();
            assertThat(client.send(HttpRequest.newBuilder(base).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(404);
            assertThat(client.send(HttpRequest.newBuilder(URI.create(foto.url())).DELETE().build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(404);
            assertThatThrownBy(() -> store.guardar(new byte[]{1}, "text/html")).isInstanceOf(AlmacenFotosNoDisponibleException.class);
        }
    }
}
