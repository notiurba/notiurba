package com.notiurba.auth.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.notiurba.auth.IntentosLoginServicio;
import com.notiurba.auth.registro.CorreoVerificacionPendiente;
import com.notiurba.auth.registro.EnvioCorreoVerificacion;
import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.TipoToken;
import com.notiurba.usuario.TipoTokenRepository;
import com.notiurba.usuario.TokenVerificacion;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class RecuperacionServicioTest {

    private static final String ENLACE = "http://localhost:5173/app/nueva-contrasena";
    private static final String CORREO = "maria.gomez@ejemplo.com";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenVerificacionRepository tokenRepository;
    @Mock private TipoTokenRepository tipoTokenRepository;
    @Mock private EnviadorEnlaceRecuperacion enviador;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final IntentosLoginServicio intentos = mock(IntentosLoginServicio.class);
    private final TipoToken recuperacion = mock(TipoToken.class);
    private PersistenciaRecuperacion persistencia;
    private EnvioRecuperacionPendiente envioPendiente;
    private RecuperacionServicio servicio;

    @BeforeEach
    void preparar() {
        persistencia = new PersistenciaRecuperacion(usuarioRepository, tokenRepository, tipoTokenRepository);
        envioPendiente = new EnvioRecuperacionPendiente(enviador, persistencia);
        servicio = new RecuperacionServicio(tokenRepository, tipoTokenRepository,
                passwordEncoder, intentos, envioPendiente, ENLACE);
        lenient().when(tipoTokenRepository.findByNombre(TipoToken.RECUPERACION_CONTRASENA))
                .thenReturn(Optional.of(recuperacion));
        lenient().when(tokenRepository.confirmarPendiente(
                any(), any(), anyString(), any(), any()))
                .thenReturn(1);
    }

    private Usuario conContrasena() {
        return UsuariosDePrueba.conContrasena(CORREO, passwordEncoder.encode("Anterior123"), Rol.CIUDADANO);
    }

    // --- solicitar ---

    /** /recuperar es público: si compartiera la cola, un abuso dejaría sin correo de verificación a los registros. */
    @Test
    void laRecuperacionUsaUnaColaDistintaALaDelCorreoDeVerificacion() throws Exception {
        String colaRecuperacion = EnvioRecuperacionPendiente.class
                .getMethod("procesar", String.class, String.class, String.class).getAnnotation(Async.class).value();
        String colaVerificacion = EnvioCorreoVerificacion.class
                .getMethod("enviar", CorreoVerificacionPendiente.class).getAnnotation(Async.class).value();

        assertThat(colaRecuperacion).isNotBlank().isNotEqualTo(colaVerificacion);
    }

    /** Cualquier consulta en el hilo de la petición haría que una cuenta elegible tarde más en responder. */
    @Test
    void laPeticionSoloEncolaYNoConsultaLaBaseParaNingunCorreo() {
        EnvioRecuperacionPendiente cola = mock(EnvioRecuperacionPendiente.class);
        RecuperacionServicio soloEncola = new RecuperacionServicio(tokenRepository, tipoTokenRepository,
                passwordEncoder, intentos, cola, ENLACE);

        soloEncola.solicitar(CORREO);
        soloEncola.solicitar("nadie@ejemplo.com");

        verifyNoInteractions(usuarioRepository, tokenRepository, tipoTokenRepository, enviador);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(cola).procesar(eq(CORREO), hash.capture(), enlace.capture());
        verify(cola).procesar(eq("nadie@ejemplo.com"), anyString(), startsWith(ENLACE + "?token="));
        String token = enlace.getValue().substring((ENLACE + "?token=").length());
        assertThat(hash.getValue()).isEqualTo(RecuperacionServicio.hash(token)).doesNotContain(token);
    }

    @Test
    void cuentaConContrasenaRecibeEnlaceYSoloSeGuardaElHash() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));

        servicio.solicitar(" " + CORREO + " ");

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(enviador).enviar(eq(CORREO), enlace.capture());
        assertThat(enlace.getValue()).startsWith(ENLACE + "?token=");
        String token = enlace.getValue().substring((ENLACE + "?token=").length());

        ArgumentCaptor<TokenVerificacion> guardado = ArgumentCaptor.forClass(TokenVerificacion.class);
        verify(tokenRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getTokenHash())
                .isEqualTo(RecuperacionServicio.hash(token))
                .hasSize(64)
                .doesNotContain(token);
        assertThat(Duration.between(OffsetDateTime.now(), guardado.getValue().getExpiraEn()))
                .isBetween(Duration.ofMinutes(59), Duration.ofMinutes(60));
        assertThat(guardado.getValue().getUsadoEn()).isEqualTo(guardado.getValue().getExpiraEn());
        verify(tokenRepository).confirmarPendiente(eq(usuario), eq(recuperacion),
                eq(guardado.getValue().getTokenHash()), eq(guardado.getValue().getExpiraEn()), any());
        verify(tokenRepository).invalidarVigentesExcepto(
                eq(usuario), eq(recuperacion), eq(guardado.getValue().getTokenHash()), any());
    }

    @Test
    void correoInexistenteNoGeneraNada() {
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate("nadie@ejemplo.com")).thenReturn(Optional.empty());

        servicio.solicitar("nadie@ejemplo.com");

        verifyNoInteractions(tokenRepository, enviador);
    }

    @Test
    void cuentaSoloGoogleNoRecibeEnlace() {
        Usuario soloGoogle = UsuariosDePrueba.soloGoogle(CORREO);
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(soloGoogle));

        servicio.solicitar(CORREO);

        verifyNoInteractions(tokenRepository, enviador);
    }

    @Test
    void masDeTresSolicitudesEnUnaHoraNoGeneranOtroEnlace() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));
        when(tokenRepository.countByUsuarioAndTipoTokenAndCreadoEnAfter(
                eq(usuario), eq(recuperacion), any()))
                .thenReturn(3L);

        servicio.solicitar(CORREO);

        verify(tokenRepository, never()).saveAndFlush(any());
        verifyNoInteractions(enviador);
    }

    @Test
    void unFalloDelEnvioNoCambiaLaRespuesta() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));
        doThrow(new IllegalStateException("SMTP caído")).when(enviador).enviar(anyString(), anyString());

        assertThatCode(() -> servicio.solicitar(CORREO)).doesNotThrowAnyException();

        ArgumentCaptor<TokenVerificacion> creado = ArgumentCaptor.forClass(TokenVerificacion.class);
        verify(tokenRepository).saveAndFlush(creado.capture());
        verify(tokenRepository).eliminarPendiente(eq(usuario), eq(recuperacion),
                eq(creado.getValue().getTokenHash()), eq(creado.getValue().getExpiraEn()));
        verify(tokenRepository, never()).confirmarPendiente(
                eq(usuario), eq(recuperacion), anyString(), any(), any());
    }

    @Test
    void siEncolarLanzaUnErrorNoQuedaNadaGuardadoYLaRespuestaNoCambia() {
        EnvioRecuperacionPendiente cola = mock(EnvioRecuperacionPendiente.class);
        doThrow(new TaskRejectedException("cola llena")).when(cola).procesar(anyString(), anyString(), anyString());
        RecuperacionServicio conColaLlena = new RecuperacionServicio(tokenRepository, tipoTokenRepository,
                passwordEncoder, intentos, cola, ENLACE);

        assertThatCode(() -> conColaLlena.solicitar(CORREO)).doesNotThrowAnyException();

        verifyNoInteractions(usuarioRepository, tokenRepository, enviador);
    }

    @Test
    void siPrepararFallaNoSeLlamaAlProveedorYSeRegistraLaCausa() {
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenThrow(
                new DataAccessResourceFailureException("base caída", new SQLTransientConnectionException("sin conexión")));

        String mensajes = registrar(() -> assertThatCode(() -> servicio.solicitar(CORREO)).doesNotThrowAnyException());

        verifyNoInteractions(enviador);
        verify(tokenRepository, never()).saveAndFlush(any());
        assertThat(mensajes).contains("No se pudo preparar la recuperación (SQLTransientConnectionException)");
    }

    @Test
    void unFalloPasajeroAlConfirmarSeReintentaYElEnlaceQuedaActivo() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));
        when(tokenRepository.invalidarVigentesExcepto(eq(usuario), eq(recuperacion), anyString(), any()))
                .thenThrow(new IllegalStateException("bloqueo pasajero"))
                .thenReturn(1);

        String mensajes = registrar(() -> servicio.solicitar(CORREO));

        verify(tokenRepository, times(2)).confirmarPendiente(eq(usuario), eq(recuperacion), anyString(), any(), any());
        verify(tokenRepository, never()).eliminarPendiente(any(), any(), anyString(), any());
        assertThat(mensajes).contains("Enlace de recuperación activo").doesNotContain("no quedó activo");
    }

    @Test
    void siElPendienteYaNoExisteAlConfirmarSeAvisaQueElEnlaceNoQuedoActivo() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));
        when(tokenRepository.confirmarPendiente(any(), any(), anyString(), any(), any())).thenReturn(0);

        String mensajes = registrar(() -> servicio.solicitar(CORREO));

        verify(enviador).enviar(eq(CORREO), anyString());
        verify(tokenRepository, never()).invalidarVigentesExcepto(any(), any(), anyString(), any());
        assertThat(mensajes).contains("El correo de recuperación salió, pero su enlace no quedó activo");
    }

    @Test
    void unRechazoDeResendRegistraSiEsReintentableYElCodigoSinElCuerpoDeLaRespuesta() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));
        doThrow(new CorreoNoEnviadoException("Resend rechazó el envío (HTTP 403): solo puede enviar a " + CORREO,
                false, 403, null)).when(enviador).enviar(anyString(), anyString());

        String mensajes = registrar(() -> servicio.solicitar(CORREO));

        assertThat(mensajes).contains("CorreoNoEnviadoException, reintentable=false, http=403")
                .doesNotContain(CORREO, "solo puede enviar");
    }

    /** Ejecuta la acción y devuelve lo que escribió el envío en segundo plano en el log. */
    private static String registrar(Runnable accion) {
        var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EnvioRecuperacionPendiente.class);
        var registro = new ListAppender<ILoggingEvent>();
        registro.start();
        logger.addAppender(registro);
        try {
            accion.run();
        } finally {
            logger.detachAppender(registro);
        }
        return registro.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
    }

    @Test
    void siFallaLaInvalidacionDespuesDeAceptarElCorreoElTokenQuedaPendienteYNoSeBorra() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));
        doThrow(new IllegalStateException("base temporalmente no disponible"))
                .when(tokenRepository).invalidarVigentesExcepto(
                        eq(usuario), eq(recuperacion), anyString(), any());

        assertThatCode(() -> servicio.solicitar(CORREO)).doesNotThrowAnyException();

        verify(enviador).enviar(eq(CORREO), anyString());
        verify(tokenRepository, never()).eliminarPendiente(any(), any(), anyString(), any());
    }

    @Test
    void unFalloSeRegistraSinCorreoTokenNiMensajeDelProveedor() {
        Usuario usuario = conContrasena();
        when(usuarioRepository.findByCorreoIgnoreCaseForUpdate(CORREO)).thenReturn(Optional.of(usuario));
        doThrow(new IllegalStateException("rechazo para " + CORREO + " ?token=secreto"))
                .when(enviador).enviar(anyString(), anyString());

        String mensajes = registrar(() -> servicio.solicitar(CORREO));

        assertThat(mensajes).contains("Envío de recuperación fallido", "IllegalStateException")
                .doesNotContain(CORREO, "?token=secreto", "rechazo para");
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
        verify(intentos).limpiar(usuario.getCorreo());
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
