package com.notiurba;

import com.notiurba.config.SecurityConfig;
import com.cloudinary.Cloudinary;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import com.notiurba.foto.*;
import com.notiurba.seguridad.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual provider calls through Spring MVC, only explicit opt-in and synthetic images. */
@WebMvcTest(controllers=PhotoDiagnosticsController.class,properties={
    "notiurba.diagnostics.photos-enabled=true",
    "notiurba.cors.allowed-origins=http://localhost:5173",
    "notiurba.jwt.secreto=only-test-fixture-key-not-for-production-123456",
    "notiurba.jwt.expiracion-minutos=10"})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Import({CloudinaryPhotoStore.class,SecurityConfig.class,JwtFiltro.class,JwtProveedor.class})
class CloudinaryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProveedor jwt;
    @Autowired ObjectMapper mapper;
    @BeforeAll static void requireAuthorizedSecrets() {
        for (String name : new String[]{"CLOUDINARY_CLOUD_NAME","CLOUDINARY_API_KEY","CLOUDINARY_API_SECRET"})
            if (System.getenv(name) == null || System.getenv(name).isBlank())
                throw new IllegalStateException("Missing authorized test configuration: " + name);
    }
    @Test void actualUploadRecoverDelete() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(32,24,BufferedImage.TYPE_INT_RGB),"jpg",output);
        String token = "Bearer " + jwt.generar("synthetic@example.invalid","ADMIN");
        var upload = mvc.perform(multipart("/api/admin/diagnostics/photos")
                .file(new MockMultipartFile("file","synthetic.jpg","image/jpeg",output.toByteArray()))
                .header("Authorization",token)).andExpect(status().isCreated()).andReturn();
        String id = mapper.readTree(upload.getResponse().getContentAsString()).path("id").asText();
        URI recovery = null;
        Cloudinary sdk = testClient();
        try {
            verifyAuthenticatedMetadata(sdk, id);
            var link = mvc.perform(get("/api/admin/diagnostics/photos/"+id+"/link").header("Authorization",token))
                .andExpect(status().isOk()).andReturn();
            URI uri = URI.create(mapper.readTree(link.getResponse().getContentAsString()).path("url").asText());
            recovery = uri;
            assertThat(uri.getScheme()).isEqualTo("https");
            assertThat(uri.getHost()).isEqualTo("api.cloudinary.com");
            String expires = java.util.Arrays.stream(uri.getRawQuery().split("&"))
                .filter(part -> part.startsWith("expires_at=")).findFirst().orElseThrow().split("=",2)[1];
            long remaining = Long.parseLong(expires) - java.time.Instant.now().getEpochSecond();
            assertThat(remaining).isBetween(1L,60L);
            var response = download(uri);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("image/jpeg");
            var decoded = ImageIO.read(new ByteArrayInputStream(response.body()));
            assertThat(decoded).isNotNull();
            assertThat(decoded.getWidth()).isEqualTo(32);
            assertThat(decoded.getHeight()).isEqualTo(24);
            var options = new HashMap<String,Object>(Map.of("resource_type","image","type","authenticated",
                "attachment",false,"expires_at",java.time.Instant.now().minusSeconds(3600).getEpochSecond()));
            URI expired;
            try { expired = URI.create(sdk.privateDownload("notiurba/edt51/"+id,"jpg",options)); }
            catch (Exception e) { throw new AssertionError("Expired SDK URL generation failed; sensitive details omitted"); }
            assertDenied(download(expired).statusCode());
            assertDenied(download(URI.create(uri.toString().replaceFirst("signature=[^&]+","signature=invalid"))).statusCode());
            String cloud = System.getenv("CLOUDINARY_CLOUD_NAME");
            for (String path : new String[]{"authenticated/", "upload/", "authenticated/w_16/"}) {
                assertDenied(download(URI.create("https://res.cloudinary.com/"+cloud+
                    "/image/"+path+"notiurba/edt51/"+id+".jpg")).statusCode());
            }
        } finally {
            mvc.perform(delete("/api/admin/diagnostics/photos/"+id).header("Authorization",token))
                .andExpect(status().isNoContent());
            verifyRemoved(sdk, id);
        }
        if (recovery != null) assertDenied(download(recovery).statusCode());
        System.out.println("UTC=" + java.time.Instant.now() + " Cloudinary synthetic lifecycle completed");
    }
    private static Cloudinary testClient() {
        return new Cloudinary(new HashMap<>(Map.of("cloud_name",System.getenv("CLOUDINARY_CLOUD_NAME"),
            "api_key",System.getenv("CLOUDINARY_API_KEY"),"api_secret",System.getenv("CLOUDINARY_API_SECRET"),
            "secure",true,"signature_algorithm","SHA256","timeout",30)));
    }
    private static Map<String,Object> options() {
        return new HashMap<>(Map.of("resource_type","image","type","authenticated"));
    }
    private static void verifyAuthenticatedMetadata(Cloudinary sdk, String id) {
        Map<?,?> resource;
        try { resource = sdk.api().resource("notiurba/edt51/"+id, options()); }
        catch (Exception e) { throw new AssertionError("Provider metadata check failed; sensitive details omitted"); }
        assertThat(resource.get("type")).isEqualTo("authenticated");
        assertThat(resource.get("resource_type")).isEqualTo("image");
        assertThat(resource.get("format")).isEqualTo("jpg");
        assertThat(((Number)resource.get("width")).intValue()).isEqualTo(32);
        assertThat(((Number)resource.get("height")).intValue()).isEqualTo(24);
        assertThat(((Number)resource.get("bytes")).longValue()).isBetween(1L,1_000_000L);
        System.out.println("UTC="+java.time.Instant.now()+" synthetic="+id+
            " provider_metadata_type=authenticated format=jpg dimensions=32x24");
    }
    private static void verifyRemoved(Cloudinary sdk, String id) {
        try { sdk.api().resource("notiurba/edt51/"+id, options()); }
        catch (Exception e) {
            // Distinguish confirmed absence from network/auth failures.
            assertThat(e.getClass().getSimpleName()).isEqualTo("NotFound");
            return;
        }
        throw new AssertionError("Provider still reports the synthetic resource after deletion");
    }
    // Never let HttpClient exceptions include capability URLs in test reports.
    private static HttpResponse<byte[]> download(URI uri) throws Exception {
        try {
            return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                .send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new AssertionError("HTTPS transport failed; signed URL omitted");
        }
    }
    private static void assertDenied(int status) {
        assertThat(status).isIn(400,401,403,404,410);
    }
}
