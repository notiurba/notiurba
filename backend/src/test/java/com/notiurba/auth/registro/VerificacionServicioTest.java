package com.notiurba.auth.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.notiurba.auth.google.ResultadoGoogle;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.TipoToken;
import com.notiurba.usuario.TipoTokenRepository;
import com.notiurba.usuario.TokenVerificacion;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

class VerificacionServicioTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TokenVerificacionRepository tokens = mock(TokenVerificacionRepository.class);
    private final TipoTokenRepository tipos = mock(TipoTokenRepository.class);
    private final ApplicationEventPublisher publicador = mock(ApplicationEventPublisher.class);
    private final JwtProveedor jwt = new JwtProveedor("secreto-de-pruebas-de-notiurba-con-mas-de-32-bytes", 60);
    private final TipoToken tipo = mock(TipoToken.class);
    private VerificacionServicio servicio;

    @BeforeEach
    void preparar() {
        when(tipos.findByNombre(TipoToken.VERIFICACION_CORREO)).thenReturn(Optional.of(tipo));
        servicio = new VerificacionServicio(usuarios, tokens, tipos, publicador, jwt, "https://app.test/verificar");
    }

    private static Usuario sinVerificar(String email) {
        return Usuario.nuevoConContrasena("Ana", "Gómez", (short) 1, "123", 1, "Cra 1", email, null,
                "$2a$hash", UsuariosDePrueba.rol(Rol.CIUDADANO), (short) 1, OffsetDateTime.now().minusDays(1));
    }

    @Test
    void emitirGuardaSoloElHashYPublicaElEnlaceConElToken() {
        Usuario ana = sinVerificar("ana@x.com");

        servicio.emitir(ana);

        var guardado = ArgumentCaptor.forClass(TokenVerificacion.class);
        verify(tokens).save(guardado.capture());
        var evento = ArgumentCaptor.forClass(CorreoVerificacionPendiente.class);
        verify(publicador).publishEvent(evento.capture());
        assertThat(evento.getValue().correo()).isEqualTo("ana@x.com");
        assertThat(evento.getValue().nombres()).isEqualTo("Ana");
        String enlace = evento.getValue().enlace();
        assertThat(enlace).startsWith("https://app.test/verificar?token=");
        String token = enlace.substring(enlace.indexOf("?token=") + 7);
        assertThat(guardado.getValue().getTokenHash()).isEqualTo(VerificacionServicio.hash(token)).isNotEqualTo(token);
        verify(tokens).invalidarVigentes(eq(ana), eq(tipo), any());
    }

    @Test
    void elEventoNoMuestraElEnlaceEnLosLogs() {
        assertThat(new CorreoVerificacionPendiente("a@x.com", "Ana", "https://x/?token=secreto").toString())
                .doesNotContain("secreto").doesNotContain("a@x.com");
    }

    @Test
    void verificarMarcaElCorreoYDevuelveLaSesion() {
        Usuario ana = sinVerificar("ana@x.com");
        OffsetDateTime ahora = OffsetDateTime.now();
        var token = new TokenVerificacion(ana, tipo, VerificacionServicio.hash("abc"), ahora.minusMinutes(5), ahora.plusHours(1));
        when(tokens.findByTokenHashAndTipoToken(VerificacionServicio.hash("abc"), tipo)).thenReturn(Optional.of(token));

        ResultadoGoogle.Sesion sesion = servicio.verificar("abc");

        assertThat(ana.getCorreoVerificadoEn()).isNotNull();
        assertThat(token.getUsadoEn()).isNotNull();
        assertThat(sesion.rol()).isEqualTo(Rol.CIUDADANO);
        assertThat(jwt.validarAcceso(sesion.token()).getSubject()).isEqualTo("ana@x.com");
    }

    @Test
    void tokenVencidoUsadoOInexistenteDaEnlaceInvalido() {
        Usuario ana = sinVerificar("ana@x.com");
        OffsetDateTime ahora = OffsetDateTime.now();
        var vencido = new TokenVerificacion(ana, tipo, VerificacionServicio.hash("viejo"), ahora.minusDays(2), ahora.minusDays(1));
        var usado = new TokenVerificacion(ana, tipo, VerificacionServicio.hash("usado"), ahora.minusHours(1), ahora.plusHours(1));
        usado.marcarUsado(ahora.minusMinutes(1));
        when(tokens.findByTokenHashAndTipoToken(VerificacionServicio.hash("viejo"), tipo)).thenReturn(Optional.of(vencido));
        when(tokens.findByTokenHashAndTipoToken(VerificacionServicio.hash("usado"), tipo)).thenReturn(Optional.of(usado));

        for (String t : new String[] { "viejo", "usado", "nunca-existio" }) {
            assertThatThrownBy(() -> servicio.verificar(t)).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.getCodigo()).isEqualTo(Codigos.ENLACE_INVALIDO);
                assertThat(e.getCampo()).isEqualTo("token");
            });
        }
        assertThat(ana.getCorreoVerificadoEn()).isNull();
    }

    @Test
    void reenviarSoloEnviaALaCuentaSinVerificarYRespondeSiempreLoMismo() {
        Usuario pendiente = sinVerificar("pendiente@x.com");
        Usuario yaVerificada = UsuariosDePrueba.conContrasena("ok@x.com", "$2a$hash", Rol.CIUDADANO);
        Usuario soloGoogle = UsuariosDePrueba.soloGoogle("g@x.com");
        when(usuarios.findByCorreoIgnoreCase("pendiente@x.com")).thenReturn(Optional.of(pendiente));
        when(usuarios.findByCorreoIgnoreCase("ok@x.com")).thenReturn(Optional.of(yaVerificada));
        when(usuarios.findByCorreoIgnoreCase("g@x.com")).thenReturn(Optional.of(soloGoogle));
        when(usuarios.findByCorreoIgnoreCase("nadie@x.com")).thenReturn(Optional.empty());
        when(tokens.countByUsuarioAndTipoTokenAndCreadoEnAfter(any(), any(), any())).thenReturn(0L);

        String a = servicio.reenviar("pendiente@x.com");
        String b = servicio.reenviar("ok@x.com");
        String c = servicio.reenviar("g@x.com");
        String d = servicio.reenviar("nadie@x.com");

        assertThat(a).isEqualTo(b).isEqualTo(c).isEqualTo(d);
        var evento = ArgumentCaptor.forClass(CorreoVerificacionPendiente.class);
        verify(publicador).publishEvent(evento.capture());      // solo uno de los cuatro
        assertThat(evento.getValue().correo()).isEqualTo("pendiente@x.com");
    }

    @Test
    void noReenviaSiYaSeEnvioUnoHaceMenosDeUnMinuto() {
        Usuario pendiente = sinVerificar("pendiente@x.com");
        when(usuarios.findByCorreoIgnoreCase("pendiente@x.com")).thenReturn(Optional.of(pendiente));
        when(tokens.countByUsuarioAndTipoTokenAndCreadoEnAfter(any(), any(), any())).thenReturn(1L);

        servicio.reenviar("pendiente@x.com");

        verify(publicador, never()).publishEvent(any(Object.class));
        verify(tokens, never()).save(any());
    }
}
