package com.notiurba;

import java.util.*;
import java.net.URI;
import com.notiurba.web.HealthController;
import com.notiurba.admin.SpatialDiagnosticsController;
import com.notiurba.foto.*;
import com.notiurba.geo.SpatialProbe;
import com.notiurba.config.SecurityConfig;
import com.notiurba.seguridad.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers={HealthController.class,SpatialDiagnosticsController.class,PhotoDiagnosticsController.class},properties={
    "notiurba.diagnostics.enabled=true","notiurba.diagnostics.photos-enabled=true",
    "notiurba.cors.allowed-origins=http://localhost:5173",
    "notiurba.jwt.secreto=only-test-fixture-key-not-for-production-123456",
    "notiurba.jwt.expiracion-minutos=1"})
@Import({SecurityConfig.class,JwtFiltro.class,JwtProveedor.class})
class DiagnosticsSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProveedor jwt;
    @MockBean SpatialProbe probe;
    @MockBean CloudinaryPhotoStore photos;
    String admin() { return "Bearer " + jwt.generar("synthetic@example.invalid","ADMIN"); }
    @Test void healthRemainsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ok"));
        verifyNoInteractions(probe);
    }
    @Test void citizenCannotUploadPhotos() throws Exception {
        mvc.perform(multipart("/api/admin/diagnostics/photos")
            .file(new MockMultipartFile("file","synthetic.jpg","image/jpeg",new byte[]{1}))
            .header("Authorization","Bearer "+jwt.generar("test@example.invalid","CIUDADANO")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(photos);
    }
    @Test void adminCanUploadRetrieveLinkAndDelete() throws Exception {
        UUID id=UUID.randomUUID();
        when(photos.upload(any())).thenReturn(id);
        when(photos.recoveryLink(id)).thenReturn(URI.create("https://api.cloudinary.com/v1_1/example/image/download"));
        mvc.perform(multipart("/api/admin/diagnostics/photos")
            .file(new MockMultipartFile("file","synthetic.jpg","image/jpeg",new byte[]{1}))
            .header("Authorization",admin()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/api/admin/diagnostics/photos/"+id+"/link").header("Authorization",admin()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.expiresInSeconds").value("60"));
        mvc.perform(delete("/api/admin/diagnostics/photos/"+id).header("Authorization",admin()))
            .andExpect(status().isNoContent());
        verify(photos).delete(id);
    }
    @Test void rejectsAnonymous() throws Exception {
        mvc.perform(get("/api/admin/diagnostics/spatial")).andExpect(status().isUnauthorized());
        verifyNoInteractions(probe);
    }
    @Test void rejectsInvalidToken() throws Exception {
        mvc.perform(get("/api/admin/diagnostics/spatial").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
    }
    @Test void rejectsExpiredToken() throws Exception {
        var expired = new JwtProveedor("only-test-fixture-key-not-for-production-123456",-1);
        mvc.perform(get("/api/admin/diagnostics/spatial").header("Authorization","Bearer "+expired.generar("test@example.invalid","ADMIN")))
            .andExpect(status().isUnauthorized());
    }
    @Test void rejectsCitizen() throws Exception {
        mvc.perform(get("/api/admin/diagnostics/spatial").header("Authorization","Bearer "+jwt.generar("test@example.invalid","CIUDADANO")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(probe);
    }
    @Test void acceptsAdmin() throws Exception {
        when(probe.check()).thenReturn(Map.of("contains_inside",true));
        mvc.perform(get("/api/admin/diagnostics/spatial").header("Authorization",admin()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contains_inside").value(true));
    }
    @Test void sanitizesDatabaseError() throws Exception {
        when(probe.check()).thenThrow(new IllegalStateException("internal sensitive details"));
        mvc.perform(get("/api/admin/diagnostics/spatial").header("Authorization",admin()))
            .andExpect(status().isServiceUnavailable());
    }
    @Test void requiresAdminForDeletion() throws Exception {
        mvc.perform(delete("/api/admin/diagnostics/photos/"+UUID.randomUUID())).andExpect(status().isUnauthorized());
        verifyNoInteractions(photos);
    }
    @Test void oversizedFileDoesNotReachProvider() throws Exception {
        mvc.perform(multipart("/api/admin/diagnostics/photos")
            .file(new MockMultipartFile("file","test.jpg","image/jpeg",new byte[1_000_001]))
            .header("Authorization",admin())).andExpect(status().isBadRequest());
        verifyNoInteractions(photos);
    }
    @Test void invalidImageReturns400() throws Exception {
        when(photos.upload(any())).thenThrow(new IllegalArgumentException("JPEG inválido"));
        mvc.perform(multipart("/api/admin/diagnostics/photos")
            .file(new MockMultipartFile("file","test.jpg","image/jpeg",new byte[]{1}))
            .header("Authorization",admin())).andExpect(status().isBadRequest());
    }
    @Test void providerFailureReturns502() throws Exception {
        when(photos.upload(any())).thenThrow(new IllegalStateException("sensitive vendor details"));
        mvc.perform(multipart("/api/admin/diagnostics/photos")
            .file(new MockMultipartFile("file","test.jpg","image/jpeg",new byte[]{1}))
            .header("Authorization",admin())).andExpect(status().isBadGateway());
    }
    @Test void linkProviderFailureReturnsGeneric502WithoutVendorDetails() throws Exception {
        UUID id = UUID.randomUUID();
        when(photos.recoveryLink(id)).thenThrow(new IllegalStateException("sensitive-provider-url"));
        mvc.perform(get("/api/admin/diagnostics/photos/"+id+"/link").header("Authorization",admin()))
            .andExpect(status().isBadGateway())
            .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getErrorMessage())
                .isEqualTo("Recuperacion no disponible"));
    }
}
