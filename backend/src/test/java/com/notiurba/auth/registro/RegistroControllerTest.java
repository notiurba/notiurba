package com.notiurba.auth.registro;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.config.SecurityConfig;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.web.error.ManejadorErrores;

@WebMvcTest(RegistroController.class)
@Import({ SecurityConfig.class, JwtProveedor.class, ManejadorErrores.class })
@TestPropertySource(locations = "classpath:pruebas.properties")
class RegistroControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private RegistroServicio registro;
    @MockBean private VerificacionServicio verificacion;

    private static final String VALIDO = """
            {"nombres":"Ana","apellidos":"Gómez","idTipoDocumento":1,"numeroDocumento":"1118456789",
             "idBarrio":1,"direccionCorrespondencia":"Cra 1","correo":"ana@x.com","password":"Clave1234",
             "aceptaPrivacidad":true}""";

    @Test
    void registroPublicoDa201SinTokenYConElMensaje() throws Exception {
        when(registro.registrar(any())).thenReturn(new MensajeResponse("ana@x.com", "Te enviamos un enlace"));
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana@x.com"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void sinAceptarPrivacidadDa400ConElCampoYElFormatoUnico() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDO.replace("\"aceptaPrivacidad\":true", "\"aceptaPrivacidad\":false")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("aceptaPrivacidad"));
    }

    @Test
    void elPrimerErrorEsElPrimerCampoDelFormulario() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"\",\"correo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("nombres"));
    }

    @Test
    void reenviarRespondeIgualYVerificarSinTokenDa400() throws Exception {
        when(verificacion.reenviar("a@x.com")).thenReturn("mensaje");
        mvc.perform(post("/api/auth/reenviar-verificacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"a@x.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").value("mensaje"));
        mvc.perform(post("/api/auth/verificar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("token"));
    }
}
