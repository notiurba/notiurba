package com.notiurba.auth.google;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

@WebMvcTest(GoogleAuthController.class)
@Import({ SecurityConfig.class, JwtProveedor.class })
@TestPropertySource(locations = "classpath:pruebas.properties")
class GoogleAuthControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private GoogleAuthServicio servicio;

    @Test
    void perfilIncompletoDevuelveNombresYApellidos() throws Exception {
        when(servicio.autenticar("id-token")).thenReturn(new ResultadoGoogle.PerfilIncompleto(
                "PERFIL_INCOMPLETO", "token-registro", "ana@gmail.com", "Ana María", "Gómez Ruiz"));

        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PERFIL_INCOMPLETO"))
                .andExpect(jsonPath("$.tokenRegistro").value("token-registro"))
                .andExpect(jsonPath("$.correo").value("ana@gmail.com"))
                .andExpect(jsonPath("$.nombres").value("Ana María"))
                .andExpect(jsonPath("$.apellidos").value("Gómez Ruiz"));
    }

    @Test
    void conCuentaDevuelveTokenYRol() throws Exception {
        when(servicio.autenticar("id-token")).thenReturn(new ResultadoGoogle.Sesion("jwt", "CIUDADANO"));

        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.rol").value("CIUDADANO"));
    }

    @Test
    void completarConNombresEnBlancoResponde400() throws Exception {
        mvc.perform(post("/api/auth/google/completar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRegistro":"t","nombres":"   ","apellidos":"Gómez","idTipoDocumento":1,
                                 "numeroDocumento":"123","idBarrio":1,"direccionCorrespondencia":"Cra 1",
                                 "aceptaPrivacidad":true}"""))
                .andExpect(status().isBadRequest());
        verify(servicio, never()).completar(any());
    }

    @Test
    void completarSinAceptarPrivacidadResponde400() throws Exception {
        mvc.perform(post("/api/auth/google/completar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRegistro":"t","nombres":"Ana","apellidos":"Gómez","idTipoDocumento":1,
                                 "numeroDocumento":"123","idBarrio":1,"direccionCorrespondencia":"Cra 1",
                                 "aceptaPrivacidad":false}"""))
                .andExpect(status().isBadRequest());
        verify(servicio, never()).completar(any());
    }
}
