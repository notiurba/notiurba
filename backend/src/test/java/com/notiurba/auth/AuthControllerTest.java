package com.notiurba.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
    @MockBean private IntentosLoginServicio intentos;

    @BeforeEach
    void sinBloqueo() {
        when(intentos.registrarFallo(any())).thenReturn(new IntentosLoginServicio.Fallo(null, 2));
    }

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

        assertThat(soloGoogle).isEqualTo(claveMala).contains("Correo o contraseña incorrectos.");
    }

    @Test
    void conContrasenaCorrectaPeroCorreoSinVerificarDa403() throws Exception {
        Usuario pendiente = Usuario.nuevoConContrasena("Ana", "Gómez", (short) 1, "123", 1, "Cra 1", "p@x.com", null,
                passwordEncoder.encode("Correcta123"), UsuariosDePrueba.rol(Rol.CIUDADANO), (short) 1,
                java.time.OffsetDateTime.now().minusDays(1));
        when(usuarioRepository.findByCorreoIgnoreCase("p@x.com")).thenReturn(Optional.of(pendiente));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"p@x.com\",\"password\":\"Correcta123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CORREO_NO_VERIFICADO"));

        // con la contraseña mala sigue siendo 401: no se revela que la cuenta existe
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"p@x.com\",\"password\":\"Mala12345\"}"))
                .andExpect(status().isUnauthorized());
    }
}
