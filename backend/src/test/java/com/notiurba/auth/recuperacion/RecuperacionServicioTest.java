package com.notiurba.auth.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.notiurba.usuario.Rol;
import com.notiurba.usuario.TipoToken;
import com.notiurba.usuario.TipoTokenRepository;
import com.notiurba.usuario.TokenVerificacion;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;

@ExtendWith(MockitoExtension.class)
class RecuperacionServicioTest {

    private static final String ENLACE = "http://localhost:5173/app/nueva-contrasena";
    private static final String CORREO = "maria.gomez@ejemplo.com";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenVerificacionRepository tokenRepository;
    @Mock private TipoTokenRepository tipoTokenRepository;
    @Mock private EnviadorEnlaceRecuperacion enviador;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final TipoToken recuperacion = mock(TipoToken.class);
    private RecuperacionServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new RecuperacionServicio(usuarioRepository, tokenRepository, tipoTokenRepository,
                passwordEncoder, enviador, ENLACE);
        lenient().when(tipoTokenRepository.findByNombre(TipoToken.RECUPERACION_CONTRASENA))
                .thenReturn(Optional.of(recuperacion));
    }

    private Usuario conContrasena() {
        return UsuariosDePrueba.conContrasena(CORREO, passwordEncoder.encode("Anterior123"), Rol.CIUDADANO);
    }

    // --- solicitar ---

    @Test
    void cuentaConContrasenaRecibeEnlaceYSoloSeGuardaElHash() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));

        servicio.solicitar(" " + CORREO + " ");

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(eq(CORREO), enlace.capture());
        assertThat(enlace.getValue()).startsWith(ENLACE + "?token=");
        String token = enlace.getValue().substring((ENLACE + "?token=").length());

        ArgumentCaptor<TokenVerificacion> guardado = ArgumentCaptor.forClass(TokenVerificacion.class);
        verify(tokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getTokenHash())
                .isEqualTo(RecuperacionServicio.hash(token))
                .hasSize(64)
                .doesNotContain(token);
        assertThat(Duration.between(OffsetDateTime.now(), guardado.getValue().getExpiraEn()))
                .isBetween(Duration.ofMinutes(59), Duration.ofMinutes(60));
        verify(tokenRepository).invalidarVigentes(eq(usuario), eq(recuperacion), any());
    }

    @Test
    void correoInexistenteNoGeneraNada() {
        when(usuarioRepository.findByCorreoIgnoreCase("nadie@ejemplo.com")).thenReturn(Optional.empty());

        servicio.solicitar("nadie@ejemplo.com");

        verifyNoInteractions(tokenRepository, enviador);
    }

    @Test
    void cuentaSoloGoogleNoRecibeEnlace() {
        Usuario soloGoogle = UsuariosDePrueba.soloGoogle(CORREO);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(soloGoogle));

        servicio.solicitar(CORREO);

        verifyNoInteractions(tokenRepository, enviador);
    }

    @Test
    void masDeTresSolicitudesEnUnaHoraNoGeneranOtroEnlace() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(tokenRepository.countByUsuarioAndTipoTokenAndCreadoEnAfter(eq(usuario), eq(recuperacion), any()))
                .thenReturn(3L);

        servicio.solicitar(CORREO);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(enviador);
    }

    @Test
    void unFalloDelEnvioNoCambiaLaRespuesta() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        doThrow(new IllegalStateException("SMTP caído")).when(enviador).enviar(anyString(), anyString());

        assertThatCode(() -> servicio.solicitar(CORREO)).doesNotThrowAnyException();
    }

    // --- restablecer ---

    private TokenVerificacion token(Usuario usuario, String token, OffsetDateTime expiraEn) {
        TokenVerificacion encontrado = new TokenVerificacion(usuario, recuperacion, RecuperacionServicio.hash(token),
                expiraEn.minusHours(1), expiraEn);
        when(tokenRepository.findByTokenHashAndTipoToken(RecuperacionServicio.hash(token), recuperacion))
                .thenReturn(Optional.of(encontrado));
        return encontrado;
    }

    private static void esEnlaceInvalido(Throwable error) {
        assertThat(error).isInstanceOfSatisfying(ResponseStatusException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getReason()).isEqualTo("El enlace no es válido o venció");
        });
    }

    @Test
    void tokenVigenteCambiaLaContrasenaYQuedaUsado() {
        Usuario usuario = conContrasena();
        TokenVerificacion vigente = token(usuario, "token-vigente", OffsetDateTime.now().plusMinutes(30));

        servicio.restablecer("token-vigente", "NuevaClave2026");

        assertThat(passwordEncoder.matches("NuevaClave2026", usuario.getPasswordHash())).isTrue();
        assertThat(vigente.getUsadoEn()).isNotNull();
    }

    @Test
    void tokenVencidoResponde400YNoCambiaNada() {
        Usuario usuario = conContrasena();
        String hashAnterior = usuario.getPasswordHash();
        token(usuario, "token-vencido", OffsetDateTime.now().minusMinutes(1));

        Throwable error = org.assertj.core.api.Assertions.catchThrowable(
                () -> servicio.restablecer("token-vencido", "NuevaClave2026"));

        esEnlaceInvalido(error);
        assertThat(usuario.getPasswordHash()).isEqualTo(hashAnterior);
    }

    @Test
    void elMismoTokenNoSirveDosVeces() {
        Usuario usuario = conContrasena();
        token(usuario, "token-unico", OffsetDateTime.now().plusMinutes(30));

        servicio.restablecer("token-unico", "Primera2026");
        Throwable error = org.assertj.core.api.Assertions.catchThrowable(
                () -> servicio.restablecer("token-unico", "Segunda2026"));

        esEnlaceInvalido(error);
        assertThat(passwordEncoder.matches("Primera2026", usuario.getPasswordHash())).isTrue();
    }

    @Test
    void tokenInexistenteResponde400() {
        when(tokenRepository.findByTokenHashAndTipoToken(anyString(), eq(recuperacion))).thenReturn(Optional.empty());

        esEnlaceInvalido(org.assertj.core.api.Assertions.catchThrowable(
                () -> servicio.restablecer("inventado", "NuevaClave2026")));
    }

    @Test
    void contrasenaQueNoCumpleLaReglaResponde400SinConsultarElToken() {
        for (String debil : new String[] { "corta1", "solamenteletras", "1234567890" }) {
            assertThatThrownBy(() -> servicio.restablecer("token", debil))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verifyNoInteractions(tokenRepository);
    }
}
