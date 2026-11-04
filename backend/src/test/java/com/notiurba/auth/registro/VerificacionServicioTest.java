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

import com.notiurba.auth.google.ResultadoGoogle;
import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;
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
    private final EnviadorCorreo correo = mock(EnviadorCorreo.class);
    private final JwtProveedor jwt = new JwtProveedor("secreto-de-pruebas-de-notiurba-con-mas-de-32-bytes", 60);
    private final TipoToken tipo = mock(TipoToken.class);
    private VerificacionServicio servicio;

    @BeforeEach
    void preparar() {
        when(tipos.findByNombre(TipoToken.VERIFICACION_CORREO)).thenReturn(Optional.of(tipo));
        servicio = new VerificacionServicio(usuarios, tokens, tipos, correo, jwt, "https://app.test/verificar");
    }

    private static Usuario sinVerificar(String email) {
        return Usuario.nuevoConContrasena("Ana", "Gómez", (short) 1, "123", 1, "Cra 1", email, null,
                "$2a$hash", UsuariosDePrueba.rol(Rol.CIUDADANO), (short) 1, OffsetDateTime.now().minusDays(1));
    }

    @Test
    void emitirGuardaSoloElHashYEnviaElEnlaceConElToken() {
        Usuario ana = sinVerificar("ana@x.com");

        servicio.emitirYEnviar(ana);

        var guardado = ArgumentCaptor.forClass(TokenVerificacion.class);
        verify(tokens).save(guardado.capture());
        var enviado = ArgumentCaptor.forClass(CorreoSaliente.class);
        verify(correo).enviar(enviado.capture());
        assertThat(enviado.getValue().para()).containsExactly("ana@x.com");
        String texto = enviado.getValue().texto();
        String token = texto.substring(texto.indexOf("?token=") + 7).split("\\s")[0];
        assertThat(texto).contains("https://app.test/verificar?token=");
        assertThat(guardado.getValue().getTokenHash()).isEqualTo(VerificacionServicio.hash(token)).isNotEqualTo(token);
        verify(tokens).invalidarVigentes(eq(ana), eq(tipo), any());
    }

    @Test
    void siFallaElEnvioLaCuentaSigueYNoSeLanzaError() {
        when(correo.enviar(any())).thenThrow(new CorreoNoEnviadoException("caído", true));
        servicio.emitirYEnviar(sinVerificar("ana@x.com"));
        verify(tokens).save(any());
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
        var enviado = ArgumentCaptor.forClass(CorreoSaliente.class);
        verify(correo).enviar(enviado.capture());           // solo uno de los cuatro
        assertThat(enviado.getValue().para()).containsExactly("pendiente@x.com");
    }

    @Test
    void noReenviaSiYaSeEnvioUnoHaceMenosDeUnMinuto() {
        Usuario pendiente = sinVerificar("pendiente@x.com");
        when(usuarios.findByCorreoIgnoreCase("pendiente@x.com")).thenReturn(Optional.of(pendiente));
        when(tokens.countByUsuarioAndTipoTokenAndCreadoEnAfter(any(), any(), any())).thenReturn(1L);

        servicio.reenviar("pendiente@x.com");

        verify(correo, never()).enviar(any());
        verify(tokens, never()).save(any());
    }
}
