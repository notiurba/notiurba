package com.notiurba.auth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;

import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.IdentidadExternaRepository;
import com.notiurba.usuario.ProveedorIdentidad;
import com.notiurba.usuario.ProveedorIdentidadRepository;
import com.notiurba.usuario.RolRepository;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Cuando «Continuar con Google» falla queda escrito en qué paso y de qué tipo, sin datos de la persona. */
class GoogleAuthRegistroDeFallosTest {

    private static final String CREDENTIAL = "eyJ.token-secreto.firma";
    private static final String SUB = "109876543210";
    private static final String CORREO = "ana.gomez@gmail.com";

    private final GoogleTokenVerificador verificador = mock(GoogleTokenVerificador.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final IdentidadExternaRepository identidades = mock(IdentidadExternaRepository.class);
    private final ProveedorIdentidadRepository proveedores = mock(ProveedorIdentidadRepository.class);
    private final ProveedorIdentidad google = mock(ProveedorIdentidad.class);
    private GoogleAuthServicio servicio;
    private ListAppender<ILoggingEvent> registro;
    private Logger logger;

    @BeforeEach
    void preparar() {
        servicio = new GoogleAuthServicio(verificador, new JwtProveedor("secreto-de-pruebas-de-notiurba-con-mas-de-32-bytes", 60),
                usuarios, identidades, proveedores, mock(RolRepository.class), mock(CatalogosRegistro.class));
        when(proveedores.findByNombre(ProveedorIdentidad.GOOGLE)).thenReturn(Optional.of(google));
        logger = (Logger) LoggerFactory.getLogger(GoogleAuthServicio.class);
        registro = new ListAppender<>();
        registro.start();
        logger.addAppender(registro);
    }

    @AfterEach
    void limpiar() {
        logger.detachAppender(registro);
    }

    private String log() {
        List<ILoggingEvent> eventos = registro.list;
        StringBuilder sb = new StringBuilder();
        for (ILoggingEvent e : eventos) {
            sb.append(e.getLevel()).append(' ').append(e.getFormattedMessage()).append('\n');
            assertThat(e.getThrowableProxy()).as("no se adjunta la excepción con su mensaje").isNull();
        }
        return sb.toString();
    }

    private void sinDatosPersonales(String texto) {
        assertThat(texto).doesNotContain(CORREO).doesNotContain("ana").doesNotContain(SUB)
                .doesNotContain("token-secreto").doesNotContain(CREDENTIAL);
    }

    @Test
    void tokenConAudienciaDistintaQuedaEscritoComoTalYDa401() {
        when(verificador.verificar(CREDENTIAL)).thenThrow(new BadJwtException("The aud claim is not valid for ana.gomez@gmail.com"));

        assertThatThrownBy(() -> servicio.autenticar(CREDENTIAL)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        String texto = log();
        assertThat(texto).contains("verificar").contains("BadJwtException").contains("audiencia distinta");
        sinDatosPersonales(texto);
    }

    @Test
    void laCausaSeDecidePorPalabrasCompletasNoPorPedazos() {
        assertThat(GoogleAuthServicio.causaDelRechazo(new JwtException("The iss claim is not valid"))).contains("emisor no válido");
        assertThat(GoogleAuthServicio.causaDelRechazo(new JwtException("Missing required claim: sub"))).doesNotContain("emisor");
        assertThat(GoogleAuthServicio.causaDelRechazo(new JwtException("Unsupported audit header"))).doesNotContain("audiencia");
    }

    @Test
    void noPoderObtenerLasClavesDeGoogleSeDistingueDeUnTokenMalo() {
        when(verificador.verificar(CREDENTIAL))
                .thenThrow(new JwtException("An error occurred while attempting to decode the Jwt: Couldn't retrieve remote JWK set: timed out"));

        assertThatThrownBy(() -> servicio.autenticar(CREDENTIAL)).isInstanceOf(ApiException.class);

        assertThat(log()).contains("no se pudieron obtener las claves de Google");
    }

    @Test
    void unErrorInesperadoSoloDejaSuTipoNoSuMensaje() {
        when(verificador.verificar(CREDENTIAL)).thenThrow(new IllegalStateException("falló con " + CORREO + " y " + SUB));

        assertThatThrownBy(() -> servicio.autenticar(CREDENTIAL)).isInstanceOf(IllegalStateException.class);

        String texto = log();
        assertThat(texto).contains("verificar").contains("IllegalStateException");
        sinDatosPersonales(texto);
    }

    @Test
    void siFallaLaBaseEnUnPasoSeEscribeCualPasoFue() {
        when(verificador.verificar(CREDENTIAL)).thenReturn(new PerfilGoogle(SUB, CORREO, "Ana", "Gómez", "Ana Gómez"));
        when(identidades.findByProveedorAndIdentificadorExterno(any(), any())).thenReturn(Optional.empty());
        when(usuarios.findByCorreoIgnoreCase(any())).thenThrow(new DataAccessResourceFailureException("conexión caída: " + CORREO));

        assertThatThrownBy(() -> servicio.autenticar(CREDENTIAL)).isInstanceOf(DataAccessResourceFailureException.class);

        String texto = log();
        assertThat(texto).contains("buscar-cuenta").contains("DataAccessResourceFailureException");
        sinDatosPersonales(texto);
    }

    @Test
    void siGoogleNoTieneRegistradoElProveedorSeEscribeElPaso() {
        when(verificador.verificar(CREDENTIAL)).thenReturn(new PerfilGoogle(SUB, CORREO, "Ana", "Gómez", "Ana Gómez"));
        when(proveedores.findByNombre(ProveedorIdentidad.GOOGLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.autenticar(CREDENTIAL)).isInstanceOf(IllegalStateException.class);

        assertThat(log()).contains("proveedor").contains("IllegalStateException");
    }
}
