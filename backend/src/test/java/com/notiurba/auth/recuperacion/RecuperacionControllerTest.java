package com.notiurba.auth.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.config.SecurityConfig;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.TipoToken;
import com.notiurba.usuario.TipoTokenRepository;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;

/** Con el servicio real: la respuesta HTTP no delata si la cuenta existe. */
@WebMvcTest(RecuperacionController.class)
@Import({ SecurityConfig.class, JwtProveedor.class, RecuperacionServicio.class })
@TestPropertySource(locations = "classpath:pruebas.properties")
class RecuperacionControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private TokenVerificacionRepository tokenRepository;
    @MockBean private TipoTokenRepository tipoTokenRepository;
    @MockBean private EnviadorEnlaceRecuperacion enviador;

    private MockHttpServletResponse recuperar(String correo) throws Exception {
        return mvc.perform(post("/api/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\"}"))
                .andReturn().getResponse();
    }

    @Test
    void respuestaIdenticaConCorreoExistenteEInexistente() throws Exception {
        when(tipoTokenRepository.findByNombre(TipoToken.RECUPERACION_CONTRASENA))
                .thenReturn(Optional.of(mock(TipoToken.class)));
        Usuario conClave = UsuariosDePrueba.conContrasena("existe@ejemplo.com", "$2a$10$hash", Rol.CIUDADANO);
        Usuario soloGoogleUsuario = UsuariosDePrueba.soloGoogle("google@ejemplo.com");
        when(usuarioRepository.findByCorreoIgnoreCase("existe@ejemplo.com")).thenReturn(Optional.of(conClave));
        when(usuarioRepository.findByCorreoIgnoreCase("google@ejemplo.com")).thenReturn(Optional.of(soloGoogleUsuario));
        when(usuarioRepository.findByCorreoIgnoreCase("nadie@ejemplo.com")).thenReturn(Optional.empty());

        MockHttpServletResponse existe = recuperar("existe@ejemplo.com");
        MockHttpServletResponse soloGoogle = recuperar("google@ejemplo.com");
        MockHttpServletResponse inexistente = recuperar("nadie@ejemplo.com");

        assertThat(existe.getStatus()).isEqualTo(200);
        for (MockHttpServletResponse otra : new MockHttpServletResponse[] { soloGoogle, inexistente }) {
            assertThat(otra.getStatus()).isEqualTo(existe.getStatus());
            assertThat(otra.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(existe.getContentAsString(StandardCharsets.UTF_8));
        }
        verify(enviador, times(1)).enviar(eq("existe@ejemplo.com"), anyString());
    }

    @Test
    void restablecerConEnlaceInvalidoResponde400() throws Exception {
        when(tipoTokenRepository.findByNombre(TipoToken.RECUPERACION_CONTRASENA))
                .thenReturn(Optional.of(mock(TipoToken.class)));

        var respuesta = mvc.perform(post("/api/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"inventado\",\"nuevaContrasena\":\"NuevaClave2026\"}"))
                .andReturn().getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(400);
        assertThat(respuesta.getContentAsString(StandardCharsets.UTF_8)).contains("El enlace no es válido o venció");
    }
}
