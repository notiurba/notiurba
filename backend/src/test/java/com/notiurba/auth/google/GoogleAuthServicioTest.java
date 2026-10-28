package com.notiurba.auth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.IdentidadExterna;
import com.notiurba.usuario.IdentidadExternaRepository;
import com.notiurba.usuario.ProveedorIdentidad;
import com.notiurba.usuario.ProveedorIdentidadRepository;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.RolRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServicioTest {

    private static final String CREDENTIAL = "id-token-de-google";
    private static final String SUB = "109876543210";
    private static final String CORREO = "ana.gomez@gmail.com";

    @Mock private GoogleTokenVerificador verificador;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private IdentidadExternaRepository identidadRepository;
    @Mock private ProveedorIdentidadRepository proveedorRepository;
    @Mock private RolRepository rolRepository;
    @Mock private CatalogosRegistro catalogos;

    private final JwtProveedor jwtProveedor =
            new JwtProveedor("secreto-de-pruebas-de-notiurba-con-mas-de-32-bytes", 60);
    private final ProveedorIdentidad google = mock(ProveedorIdentidad.class);
    private GoogleAuthServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new GoogleAuthServicio(verificador, jwtProveedor, usuarioRepository, identidadRepository,
                proveedorRepository, rolRepository, catalogos);
        lenient().when(proveedorRepository.findByNombre(ProveedorIdentidad.GOOGLE)).thenReturn(Optional.of(google));
    }

    private void googleDevuelve(String givenName, String familyName, String name) {
        when(verificador.verificar(CREDENTIAL)).thenReturn(new PerfilGoogle(SUB, CORREO, givenName, familyName, name));
    }

    // --- POST /api/auth/google ---

    @Test
    void caso1IdentidadYaVinculadaIniciaSesion() {
        googleDevuelve("Ana", "Gómez", "Ana Gómez");
        Usuario usuario = UsuariosDePrueba.soloGoogle(CORREO);
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB))
                .thenReturn(Optional.of(new IdentidadExterna(usuario, google, SUB, null)));

        var resultado = servicio.autenticar(CREDENTIAL);

        assertThat(resultado).isInstanceOf(ResultadoGoogle.Sesion.class);
        var sesion = (ResultadoGoogle.Sesion) resultado;
        assertThat(sesion.rol()).isEqualTo(Rol.CIUDADANO);
        assertThat(jwtProveedor.validarAcceso(sesion.token()).getSubject()).isEqualTo(CORREO);
        verify(identidadRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void caso2CuentaConElMismoCorreoSeVinculaYQuedaVerificada() {
        googleDevuelve("Ana", "Gómez", "Ana Gómez");
        Usuario existente = UsuariosDePrueba.conContrasena(CORREO, "$2a$10$hash", Rol.CIUDADANO);
        ReflectionTestUtils.setField(existente, "correoVerificadoEn", null);
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(existente));

        var resultado = servicio.autenticar(CREDENTIAL);

        assertThat(resultado).isInstanceOf(ResultadoGoogle.Sesion.class);
        ArgumentCaptor<IdentidadExterna> identidad = ArgumentCaptor.forClass(IdentidadExterna.class);
        verify(identidadRepository).save(identidad.capture());
        assertThat(identidad.getValue().getUsuario()).isSameAs(existente);
        assertThat(identidad.getValue().getIdentificadorExterno()).isEqualTo(SUB);
        assertThat(existente.getCorreoVerificadoEn()).isNotNull();
        assertThat(existente.getPasswordHash()).isEqualTo("$2a$10$hash");
    }

    @Test
    void caso3SinCuentaNoCreaUsuarioYDevuelvePerfilIncompleto() {
        googleDevuelve("Ana María", "Gómez Ruiz", "Ana María Gómez Ruiz");
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());

        var resultado = servicio.autenticar(CREDENTIAL);

        var perfil = (ResultadoGoogle.PerfilIncompleto) resultado;
        assertThat(perfil.estado()).isEqualTo("PERFIL_INCOMPLETO");
        assertThat(perfil.correo()).isEqualTo(CORREO);
        assertThat(perfil.nombres()).isEqualTo("Ana María");
        assertThat(perfil.apellidos()).isEqualTo("Gómez Ruiz");
        var claims = jwtProveedor.validarRegistroGoogle(perfil.tokenRegistro());
        assertThat(claims.getSubject()).isEqualTo(SUB);
        assertThat(claims.get("correo", String.class)).isEqualTo(CORREO);
        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(identidadRepository, never()).save(any());
    }

    @Test
    void caso3SinGivenNameUsaNameYApellidosVacios() {
        googleDevuelve(null, null, "Ana María Gómez Ruiz");
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());

        var perfil = (ResultadoGoogle.PerfilIncompleto) servicio.autenticar(CREDENTIAL);

        assertThat(perfil.nombres()).isEqualTo("Ana María Gómez Ruiz");
        assertThat(perfil.apellidos()).isEmpty();
    }

    @Test
    void tokenDeGoogleInvalidoResponde401() {
        when(verificador.verificar(CREDENTIAL)).thenThrow(new BadJwtException("firma inválida"));

        assertThatThrownBy(() -> servicio.autenticar(CREDENTIAL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void elTokenDeRegistroNoSirveComoTokenDeAcceso() {
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(SUB, CORREO);

        assertThatThrownBy(() -> jwtProveedor.validarAcceso(tokenRegistro))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    // --- POST /api/auth/google/completar ---

    private CompletarPerfilRequest peticion(String tokenRegistro, String nombres, String apellidos) {
        return new CompletarPerfilRequest(tokenRegistro, nombres, apellidos, (short) 1, " 1118456789 ", 1,
                " Cra 25 # 20-15 ", "  ", true);
    }

    private void catalogosValidos() {
        when(catalogos.existeTipoDocumento((short) 1)).thenReturn(true);
        when(catalogos.existeBarrio(1)).thenReturn(true);
    }

    @Test
    void completarCreaUsuarioSinContrasenaConSuIdentidad() {
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(SUB, CORREO);
        catalogosValidos();
        when(catalogos.avisoPrivacidadVigente()).thenReturn((short) 1);
        Rol ciudadano = UsuariosDePrueba.rol(Rol.CIUDADANO);
        when(rolRepository.findByNombre(Rol.CIUDADANO)).thenReturn(Optional.of(ciudadano));
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB)).thenReturn(Optional.empty());

        var sesion = servicio.completar(peticion(tokenRegistro, "  Ana María ", " Gómez Ruiz "));

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(usuario.capture());
        assertThat(usuario.getValue().getNombres()).isEqualTo("Ana María");
        assertThat(usuario.getValue().getApellidos()).isEqualTo("Gómez Ruiz");
        assertThat(usuario.getValue().getCorreo()).isEqualTo(CORREO);
        assertThat(usuario.getValue().getPasswordHash()).isNull();
        assertThat(usuario.getValue().getCorreoVerificadoEn()).isNotNull();
        ArgumentCaptor<IdentidadExterna> identidad = ArgumentCaptor.forClass(IdentidadExterna.class);
        verify(identidadRepository).saveAndFlush(identidad.capture());
        assertThat(identidad.getValue().getIdentificadorExterno()).isEqualTo(SUB);
        assertThat(identidad.getValue().getUsuario()).isSameAs(usuario.getValue());
        assertThat(sesion.rol()).isEqualTo(Rol.CIUDADANO);
        assertThat(jwtProveedor.validarAcceso(sesion.token()).getSubject()).isEqualTo(CORREO);
    }

    @Test
    void completarConTokenDeAccesoResponde401() {
        String tokenAcceso = jwtProveedor.generar(CORREO, Rol.CIUDADANO);

        assertThatThrownBy(() -> servicio.completar(peticion(tokenAcceso, "Ana", "Gómez")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void completarConNombresEnBlancoResponde400() {
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(SUB, CORREO);

        assertThatThrownBy(() -> servicio.completar(peticion(tokenRegistro, "   ", "Gómez")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void completarConNombresDeMasDe255Responde400() {
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(SUB, CORREO);

        assertThatThrownBy(() -> servicio.completar(peticion(tokenRegistro, "A".repeat(256), "Gómez")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void completarConDocumentoRepetidoResponde409() {
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(SUB, CORREO);
        catalogosValidos();
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByIdTipoDocumentoAndNumeroDocumento((short) 1, "1118456789")).thenReturn(true);

        assertThatThrownBy(() -> servicio.completar(peticion(tokenRegistro, "Ana", "Gómez")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getReason()).doesNotContain("1118456789");
                });
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void completarConCorreoRepetidoResponde409() {
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(SUB, CORREO);
        catalogosValidos();
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.existsByCorreoIgnoreCase(CORREO)).thenReturn(true);

        assertThatThrownBy(() -> servicio.completar(peticion(tokenRegistro, "Ana", "Gómez")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void completarEnCarreraConOtroRegistroResponde409() {
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(SUB, CORREO);
        catalogosValidos();
        when(catalogos.avisoPrivacidadVigente()).thenReturn((short) 1);
        Rol ciudadano = UsuariosDePrueba.rol(Rol.CIUDADANO);
        when(rolRepository.findByNombre(Rol.CIUDADANO)).thenReturn(Optional.of(ciudadano));
        when(identidadRepository.findByProveedorAndIdentificadorExterno(google, SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_usuario_documento"));

        assertThatThrownBy(() -> servicio.completar(peticion(tokenRegistro, "Ana", "Gómez")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
