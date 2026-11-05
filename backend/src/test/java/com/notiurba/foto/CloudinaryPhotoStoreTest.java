package com.notiurba.foto;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** SDK contract and privacy failure paths. These are NOT provider integration tests. */
class CloudinaryPhotoStoreTest {
    private final Cloudinary sdk = mock(Cloudinary.class);
    private final Uploader uploader = mock(Uploader.class);
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
    private CloudinaryPhotoStore store() {
        when(sdk.uploader()).thenReturn(uploader);
        return new CloudinaryPhotoStore(sdk, clock);
    }
    private static byte[] jpeg() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(32,24,BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }
    @Test void uploadsSanitizedJpegAsAuthenticatedAndDisablesBackups() throws Exception {
        var store = store();
        when(uploader.upload(any(), anyMap())).thenAnswer(invocation -> {
            Map<?,?> options = invocation.getArgument(1);
            assertThat(options.get("type")).isEqualTo("authenticated");
            assertThat(options.get("resource_type")).isEqualTo("image");
            assertThat(options.get("backup")).isEqualTo(false);
            assertThat(options.get("overwrite")).isEqualTo(false);
            assertThat(options.get("allowed_formats")).isEqualTo(List.of("jpg"));
            byte[] body = invocation.getArgument(0);
            assertThat(ImageIO.read(new java.io.ByteArrayInputStream(body))).isNotNull();
            return Map.of("public_id", options.get("public_id"), "type", "authenticated",
                "resource_type", "image", "format", "jpg");
        });
        UUID id = store.upload(jpeg());
        assertThat(id.version()).isEqualTo(4);
    }
    @Test void invalidBytesNeverReachTheProvider() {
        var store = store();
        assertThatThrownBy(() -> store.upload(new byte[1_000_001])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.upload("not-a-jpeg".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(uploader);
    }
    @Test void uncertainUploadAttemptsCleanupWithoutLeakingSdkErrors() throws Exception {
        var store = store();
        when(uploader.upload(any(),anyMap())).thenThrow(new java.io.IOException("sensitive-request"));
        when(uploader.destroy(anyString(),anyMap())).thenReturn(Map.of("result","not found"));
        assertThatThrownBy(() -> store.upload(jpeg())).hasMessage("No se confirmo la carga privada").hasNoCause();
        verify(uploader).destroy(startsWith("notiurba/edt51/"),argThat(options ->
            "authenticated".equals(options.get("type"))));
    }
    @Test void rejectsPublicResponseAndCleansOnlyItsOwnRandomId() throws Exception {
        var store = store();
        when(uploader.upload(any(), anyMap())).thenAnswer(invocation -> {
            Map<?,?> options = invocation.getArgument(1);
            return Map.of("public_id", options.get("public_id"), "type", "upload",
                "resource_type", "image", "format", "jpg");
        });
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result","ok"));
        assertThatThrownBy(() -> store.upload(jpeg())).isInstanceOf(IllegalStateException.class)
            .hasMessage("El proveedor no confirmo almacenamiento authenticated");
        var ids = ArgumentCaptor.forClass(String.class);
        var options = ArgumentCaptor.forClass(Map.class);
        verify(uploader, times(2)).destroy(ids.capture(), options.capture());
        assertThat(ids.getAllValues().get(0)).isEqualTo(ids.getAllValues().get(1));
        assertThat(ids.getValue()).startsWith("notiurba/edt51/");
        assertThat(options.getAllValues().get(0).get("type")).isEqualTo("authenticated");
        assertThat(options.getAllValues().get(1).get("type")).isEqualTo("upload");
    }
    @Test void deletionTargetsAuthenticatedAndAcceptsRepeatedDeletion() throws Exception {
        var store = store();
        UUID id = UUID.randomUUID();
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result","not found"));
        store.delete(id);
        verify(uploader).destroy(eq("notiurba/edt51/"+id), argThat(options ->
            "authenticated".equals(options.get("type")) && "image".equals(options.get("resource_type"))
                && Boolean.TRUE.equals(options.get("invalidate"))));
    }
    @Test void temporaryLinkDelegatesSigningTypeAndSixtySecondExpiryToSdk() throws Exception {
        var store = store();
        UUID id = UUID.randomUUID();
        when(sdk.privateDownload(anyString(),eq("jpg"),anyMap()))
            .thenReturn("https://api.cloudinary.com/v1_1/test/image/download?fixture=true");
        assertThat(store.recoveryLink(id).getScheme()).isEqualTo("https");
        verify(sdk).privateDownload(eq("notiurba/edt51/"+id),eq("jpg"),argThat(options ->
            "authenticated".equals(options.get("type")) && Boolean.FALSE.equals(options.get("attachment"))
                && Long.valueOf(clock.instant().plusSeconds(60).getEpochSecond()).equals(options.get("expires_at"))));
    }
    @Test void providerErrorsDoNotExposeCredentialsOrCapabilityUrls() throws Exception {
        var store = store();
        when(uploader.destroy(anyString(),anyMap())).thenThrow(new java.io.IOException("sensitive-vendor-data"));
        assertThatThrownBy(() -> store.delete(UUID.randomUUID()))
            .hasMessage("No se confirmo la eliminacion").hasNoCause();
        when(sdk.privateDownload(anyString(),anyString(),anyMap())).thenThrow(new Exception("sensitive-url"));
        assertThatThrownBy(() -> store.recoveryLink(UUID.randomUUID()))
            .hasMessage("No se pudo generar el enlace temporal").hasNoCause();
    }
    @Test void realSdkBuildsAuthenticatedUploadParamsAndSignedPrivateDownloadWithoutNetwork() throws Exception {
        var real = new Cloudinary(new HashMap<>(Map.of("cloud_name","fixture", "api_key","fixture-key",
            "api_secret","fixture-secret", "secure",true,"signature_algorithm","SHA256")));
        var options = new HashMap<String,Object>(Map.of("type","authenticated","resource_type","image",
            "public_id","notiurba/edt51/fixture", "allowed_formats",List.of("jpg"),"overwrite",false));
        Map<String,Object> params = real.uploader().buildUploadParams(options);
        assertThat(params.get("type")).isEqualTo("authenticated");
        assertThat(real.cloudinaryApiUrl("upload",options)).endsWith("/image/upload");
        URI uri = new CloudinaryPhotoStore(real,clock).recoveryLink(UUID.randomUUID());
        var query = new HashMap<String,String>();
        for (String part : uri.getRawQuery().split("&")) {
            String[] pair = part.split("=",2);
            query.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),URLDecoder.decode(pair[1],StandardCharsets.UTF_8));
        }
        assertThat(query.get("type")).isEqualTo("authenticated");
        assertThat(query.get("expires_at")).isEqualTo(Long.toString(clock.instant().plusSeconds(60).getEpochSecond()));
        String signature = query.remove("signature");
        query.remove("api_key");
        assertThat(signature).isEqualTo(CloudinarySignature.sha256(query,"fixture-secret"));
        assertThat(signature).matches("[0-9a-f]{64}");
    }
}