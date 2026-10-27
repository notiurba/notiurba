package com.notiurba;

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
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers={SpatialDiagnosticsController.class,PhotoDiagnosticsController.class},properties={
    "notiurba.diagnostics.enabled=false","notiurba.diagnostics.photos-enabled=false",
    "notiurba.cors.allowed-origins=http://localhost:5173",
    "notiurba.jwt.secreto=only-test-fixture-key-not-for-production-123456",
    "notiurba.jwt.expiracion-minutos=1"})
@Import({SecurityConfig.class,JwtFiltro.class,JwtProveedor.class})
class DiagnosticsDisabledTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProveedor jwt;
    @MockBean SpatialProbe probe;
    @MockBean CloudinaryPhotoStore photos;
    @Test void noDiagnosticRoutesWhenDisabledEvenForAdmin() throws Exception {
        String token="Bearer "+jwt.generar("test@example.invalid","ADMIN");
        mvc.perform(get("/api/admin/diagnostics/spatial").header("Authorization",token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/diagnostics/photos/"+java.util.UUID.randomUUID()+"/link")
            .header("Authorization",token)).andExpect(status().isNotFound());
        verifyNoInteractions(probe,photos);
    }
}
