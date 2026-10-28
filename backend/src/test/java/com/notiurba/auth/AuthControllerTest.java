package com.notiurba.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.config.SecurityConfig;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtProveedor.class })
@TestPropertySource(locations = "classpath:pruebas.properties")
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private UsuarioRepository usuarioRepository;

    private String login(String correo, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void cuentaSoloGoogleRespondeIgualQueCredencialesInvalidas() throws Exception {
        Usuario soloGoogleUsuario = UsuariosDePrueba.soloGoogle("google@x.com");
        Usuario conClave = UsuariosDePrueba.conContrasena("clave@x.com", passwordEncoder.encode("Correcta123"), Rol.CIUDADANO);
        when(usuarioRepository.findByCorreoIgnoreCase("google@x.com")).thenReturn(Optional.of(soloGoogleUsuario));
        when(usuarioRepository.findByCorreoIgnoreCase("clave@x.com")).thenReturn(Optional.of(conClave));

        String soloGoogle = login("google@x.com", "Cualquiera123");
        String claveMala = login("clave@x.com", "Incorrecta123");

        assertThat(soloGoogle).isEqualTo(claveMala).contains("Credenciales inválidas");
    }
}
