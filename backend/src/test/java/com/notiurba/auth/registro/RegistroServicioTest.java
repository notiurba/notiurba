package com.notiurba.auth.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.RolRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

class RegistroServicioTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final CatalogosRegistro catalogos = mock(CatalogosRegistro.class);
    private final VerificacionServicio verificacion = mock(VerificacionServicio.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private RegistroServicio servicio;

    @BeforeEach
    void preparar() {
        when(catalogos.existeTipoDocumento(anyShort())).thenReturn(true);
        when(catalogos.existeBarrio(any())).thenReturn(true);
        when(catalogos.avisoPrivacidadVigente()).thenReturn((short) 1);
        Rol ciudadano = UsuariosDePrueba.rol(Rol.CIUDADANO);
        when(roles.findByNombre(Rol.CIUDADANO)).thenReturn(Optional.of(ciudadano));
        servicio = new RegistroServicio(usuarios, roles, catalogos, encoder, verificacion);
    }

    private static RegistroRequest peticion(String correo, String password) {
        return new RegistroRequest(" Ana ", "Gómez", (short) 1, "1118456789", 1, "Cra 1 # 2-3", correo, null,
                password, true, true);
    }

    @Test
    void creaLaCuentaSinVerificarConLaContrasenaEncriptadaYEnviaElEnlace() {
        var respuesta = servicio.registrar(peticion(" ana@x.com ", "Clave1234"));

        var guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(guardado.capture());
        Usuario u = guardado.getValue();
        assertThat(u.getCorreo()).isEqualTo("ana@x.com");
        assertThat(u.getNombres()).isEqualTo("Ana");
        assertThat(u.getCorreoVerificadoEn()).isNull();
        assertThat(u.getPasswordHash()).isNotEqualTo("Clave1234");
        assertThat(encoder.matches("Clave1234", u.getPasswordHash())).isTrue();
        assertThat(u.getRol().getNombre()).isEqualTo(Rol.CIUDADANO);
        verify(verificacion).emitir(u);
        assertThat(respuesta.correo()).isEqualTo("ana@x.com");
    }

    @Test
    void contrasenaDebilDa400EnElCampoPassword() {
        assertThatThrownBy(() -> servicio.registrar(peticion("ana@x.com", "abc")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(Codigos.VALIDACION);
                    assertThat(e.getCampo()).isEqualTo("password");
                });
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void correoYDocumentoRepetidosDan409ConSuCodigo() {
        when(usuarios.existsByCorreoIgnoreCase("ana@x.com")).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrar(peticion("ana@x.com", "Clave1234")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(Codigos.CORREO_YA_REGISTRADO);
                    assertThat(e.getCampo()).isEqualTo("correo");
                });

        when(usuarios.existsByCorreoIgnoreCase("otra@x.com")).thenReturn(false);
        when(usuarios.existsByIdTipoDocumentoAndNumeroDocumento((short) 1, "1118456789")).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrar(peticion("otra@x.com", "Clave1234")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(Codigos.DOCUMENTO_YA_REGISTRADO);
                    assertThat(e.getCampo()).isEqualTo("numeroDocumento");
                });
    }

    @Test
    void barrioOTipoDeDocumentoInexistentesDan400() {
        when(catalogos.existeBarrio(any())).thenReturn(false);
        assertThatThrownBy(() -> servicio.registrar(peticion("ana@x.com", "Clave1234")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampo()).isEqualTo("idBarrio"));
    }
}
