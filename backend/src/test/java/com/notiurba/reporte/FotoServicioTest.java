package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.notiurba.foto.AlmacenFotos;
import com.notiurba.foto.AlmacenFotosNoDisponibleException;
import com.notiurba.foto.FotoGuardada;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

class FotoServicioTest {

    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3 };

    private final ReporteRepositorio repositorio = mock(ReporteRepositorio.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AlmacenFotos almacen = mock(AlmacenFotos.class);
    private FotoServicio servicio;

    @BeforeEach
    void preparar() {
        Usuario ana = mock(Usuario.class);
        when(ana.getIdUsuario()).thenReturn(7L);
        when(usuarios.findByCorreoIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));
        when(repositorio.bloquearReportePropio(7L, "NU-000010")).thenReturn(Optional.of(10L));
        when(repositorio.ordenesDeFotos(10L)).thenReturn(List.of());
        when(almacen.guardar(any(), anyString())).thenReturn(new FotoGuardada("https://res.test/a.jpg", "notiurba/reportes/a"));
        servicio = new FotoServicio(repositorio, usuarios, almacen);
    }

    private static MockMultipartFile archivo(byte[] bytes) {
        return new MockMultipartFile("foto", "foto.jpg", "image/jpeg", bytes);
    }

    private void esperar(Runnable accion, HttpStatus estado, String codigo, String campo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(estado);
            assertThat(e.getCodigo()).isEqualTo(codigo);
            assertThat(e.getCampo()).isEqualTo(campo);
        });
    }

    @Test
    void guardaLaFotoYRegistraLaUrlYElIdentificadorDelAlmacen() {
        var respuesta = servicio.subir("ana@x.com", " nu-000010 ", archivo(JPEG));

        assertThat(respuesta.codigoSeguimiento()).isEqualTo("NU-000010");
        assertThat(respuesta.foto().orden()).isEqualTo(1);
        assertThat(respuesta.foto().url()).isEqualTo("https://res.test/a.jpg");
        verify(almacen).guardar(JPEG, "image/jpeg");
        verify(repositorio).insertarFoto(10L, 7L, "https://res.test/a.jpg", "notiurba/reportes/a", 1);
    }

    @Test
    void usaElPrimerOrdenLibreYConTresFotosRespondeConflictoSinSubir() {
        when(repositorio.ordenesDeFotos(10L)).thenReturn(List.of(1, 3));
        assertThat(servicio.subir("ana@x.com", "NU-000010", archivo(JPEG)).foto().orden()).isEqualTo(2);

        when(repositorio.ordenesDeFotos(10L)).thenReturn(List.of(1, 2, 3));
        esperar(() -> servicio.subir("ana@x.com", "NU-000010", archivo(JPEG)),
                HttpStatus.CONFLICT, Codigos.FOTOS_MAXIMO, "foto");
        verify(almacen).guardar(any(), anyString());           // solo la primera: la cuarta no sube nada
    }

    @Test
    void elTipoSeDecidePorElContenidoNoPorElNombreNiElContentType() {
        byte[] texto = "esto no es una imagen".getBytes();
        esperar(() -> servicio.subir("ana@x.com", "NU-000010", archivo(texto)),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, Codigos.FORMATO_NO_SOPORTADO, "foto");
        verify(almacen, never()).guardar(any(), anyString());
    }

    @Test
    void sinArchivoOVacioDa400YMasDeUnMegaDa413() {
        esperar(() -> servicio.subir("ana@x.com", "NU-000010", null), HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "foto");
        esperar(() -> servicio.subir("ana@x.com", "NU-000010", archivo(new byte[0])),
                HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "foto");
        byte[] grande = new byte[1_048_577];
        grande[0] = (byte) 0xFF; grande[1] = (byte) 0xD8; grande[2] = (byte) 0xFF;
        esperar(() -> servicio.subir("ana@x.com", "NU-000010", archivo(grande)),
                HttpStatus.PAYLOAD_TOO_LARGE, Codigos.ARCHIVO_MUY_GRANDE, "foto");
        verifyNoInteractions(almacen);
    }

    @Test
    void unMegaExactoSiSeAcepta() {
        byte[] exacto = new byte[1_048_576];
        exacto[0] = (byte) 0xFF; exacto[1] = (byte) 0xD8; exacto[2] = (byte) 0xFF;
        assertThat(servicio.subir("ana@x.com", "NU-000010", archivo(exacto)).foto().orden()).isEqualTo(1);
    }

    @Test
    void elReporteAjenoOInexistenteDa404SinTocarElAlmacen() {
        when(repositorio.bloquearReportePropio(7L, "NU-000099")).thenReturn(Optional.empty());

        esperar(() -> servicio.subir("ana@x.com", "NU-000099", archivo(JPEG)),
                HttpStatus.NOT_FOUND, Codigos.NO_ENCONTRADO, null);
        verifyNoInteractions(almacen);
        verify(repositorio, never()).insertarFoto(anyLong(), anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    void codigoMalFormadoDa400EnElCampoCodigo() {
        esperar(() -> servicio.subir("ana@x.com", "123", archivo(JPEG)), HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "codigo");
    }

    @Test
    void siElAlmacenFallaNoQuedaNadaRegistrado() {
        when(almacen.guardar(any(), anyString())).thenThrow(new AlmacenFotosNoDisponibleException("caído"));

        assertThatThrownBy(() -> servicio.subir("ana@x.com", "NU-000010", archivo(JPEG)))
                .isInstanceOf(AlmacenFotosNoDisponibleException.class);
        verify(repositorio, never()).insertarFoto(anyLong(), anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    void siFallaElRegistroSeBorraLaFotoSubidaParaNoDejarlaHuerfana() {
        doThrow(new IllegalStateException("la base falló")).when(repositorio)
                .insertarFoto(eq(10L), eq(7L), anyString(), anyString(), eq(1));

        assertThatThrownBy(() -> servicio.subir("ana@x.com", "NU-000010", archivo(JPEG)))
                .isInstanceOf(IllegalStateException.class).hasMessage("la base falló");
        verify(almacen).borrar("notiurba/reportes/a");
    }
}
