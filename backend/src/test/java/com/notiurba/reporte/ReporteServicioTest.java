package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.notiurba.geo.AsignadorDeComuna;
import com.notiurba.geo.AsignadorDeComuna.Asignacion;
import com.notiurba.geo.AsignadorDeComuna.Resultado;
import com.notiurba.reporte.ReporteRepositorio.DatosCategoria;
import com.notiurba.reporte.ReporteRepositorio.NuevoReporte;
import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.usuario.UsuariosDePrueba;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

class ReporteServicioTest {

    private final ReporteRepositorio repositorio = mock(ReporteRepositorio.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CatalogosRegistro catalogos = mock(CatalogosRegistro.class);
    private final AsignadorDeComuna asignador = mock(AsignadorDeComuna.class);
    private final LimiteDiarioServicio limite = mock(LimiteDiarioServicio.class);
    private final DuplicadoServicio duplicados = mock(DuplicadoServicio.class);
    private ReporteServicio servicio;

    @BeforeEach
    void preparar() {
        Usuario ana = mock(Usuario.class);
        when(ana.getIdUsuario()).thenReturn(7L);
        when(usuarios.findByCorreoIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));
        when(repositorio.categoriaActiva(1)).thenReturn(Optional.of(new DatosCategoria(true, false)));   // exige ubicación
        when(repositorio.categoriaActiva(6)).thenReturn(Optional.of(new DatosCategoria(false, false)));
        when(repositorio.categoriaActiva(13)).thenReturn(Optional.of(new DatosCategoria(true, true)));   // empresa
        when(repositorio.insertar(any())).thenReturn(
                new ReporteCreadoResponse("NU-000001", "Recibido", OffsetDateTime.now(), OffsetDateTime.now().plusDays(30)));
        when(catalogos.existeBarrio(1)).thenReturn(true);
        // Por defecto, cualquier punto cae dentro de la comuna 4
        when(asignador.asignar(anyDouble(), anyDouble()))
                .thenReturn(new Resultado(true, Optional.of(new Asignacion(4, "contiene", 0))));
        servicio = new ReporteServicio(repositorio, usuarios, catalogos, asignador, limite, duplicados);
    }

    private static CrearReporteRequest peticion(int categoria, Double lat, Double lon, Integer barrio,
            String descripcion, String matricula) {
        return new CrearReporteRequest(categoria, lat, lon, barrio, descripcion, "  ", matricula, null, true);
    }

    private void esperarError(CrearReporteRequest p, String campo) {
        assertThatThrownBy(() -> servicio.crear("ana@x.com", p)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo(Codigos.VALIDACION);
            assertThat(e.getCampo()).isEqualTo(campo);
        });
        verify(repositorio, never()).insertar(any());
    }

    @Test
    void devuelveLoQueLaBaseCalculoYGuardaLosDatosLimpios() {
        var respuesta = servicio.crear("ana@x.com", peticion(1, 4.08, -76.19, null, "  Hay un hueco enorme en la vía  ", null));

        assertThat(respuesta.codigoSeguimiento()).isEqualTo("NU-000001");
        var guardado = ArgumentCaptor.forClass(NuevoReporte.class);
        verify(repositorio).insertar(guardado.capture());
        assertThat(guardado.getValue().idUsuario()).isEqualTo(7L);
        assertThat(guardado.getValue().descripcion()).isEqualTo("Hay un hueco enorme en la vía");
        assertThat(guardado.getValue().direccionIncidente()).isNull();      // vacío pasa a nulo
    }

    @Test
    void unPuntoFueraDeLasComunasSeRechazaConFueraDeCoberturaEnLatitud() {
        when(asignador.asignar(4.5, -76.5)).thenReturn(new Resultado(true, Optional.empty()));

        assertThatThrownBy(() -> servicio.crear("ana@x.com",
                peticion(1, 4.5, -76.5, null, "Hay un hueco enorme en la vía", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(Codigos.FUERA_DE_COBERTURA);
                    assertThat(e.getCampo()).isEqualTo("latitud");
                    assertThat(e.getStatusCode().value()).isEqualTo(422);
                    assertThat(e.getReason()).contains("fuera de cobertura");
                });
        verify(repositorio, never()).insertar(any());
    }

    @Test
    void sinPoligonosCargadosNoSePuedeDecidirYElReporteSeGuarda() {
        when(asignador.asignar(anyDouble(), anyDouble())).thenReturn(new Resultado(false, Optional.empty()));

        servicio.crear("ana@x.com", peticion(1, 4.08, -76.19, null, "Hay un hueco enorme en la vía", null));

        verify(repositorio).insertar(any());
    }

    @Test
    void sinGpsNoSeConsultaLaCapaDeComunas() {
        servicio.crear("ana@x.com", peticion(1, null, null, 1, "Hay un hueco enorme en la vía", null));

        verify(asignador, never()).asignar(anyDouble(), anyDouble());
        verify(repositorio).insertar(any());
    }

    @Test
    void categoriaConUbicacionExigeGpsOBarrio() {
        esperarError(peticion(1, null, null, null, "Hay un hueco enorme en la vía", null), "idBarrio");
        servicio.crear("ana@x.com", peticion(1, null, null, 1, "Hay un hueco enorme en la vía", null));   // con barrio sí
    }

    @Test
    void gpsATantasMediasYBarrioInexistente() {
        esperarError(peticion(6, 4.08, null, null, "Hay un problema que necesita atención", null), "longitud");
        esperarError(peticion(6, null, -76.1, null, "Hay un problema que necesita atención", null), "latitud");
        esperarError(peticion(6, null, null, 99, "Hay un problema que necesita atención", null), "idBarrio");
    }

    @Test
    void categoriaInexistenteODescripcionFueraDeRango() {
        esperarError(peticion(999, null, null, null, "Hay un problema que necesita atención", null), "idCategoria");
        esperarError(peticion(6, null, null, null, "   corta   ", null), "descripcion");
        esperarError(peticion(6, null, null, null, "x".repeat(501), null), "descripcion");
    }

    @Test
    void laMatriculaSoloAplicaAEmpresasDeServiciosPublicos() {
        esperarError(peticion(6, null, null, null, "Hay un problema que necesita atención", "123"), "matriculaCuentaServicio");
        servicio.crear("ana@x.com", peticion(13, 4.08, -76.1, null, "Hay un problema que necesita atención", "123"));
    }

    @Test
    void sinUsuarioEnLaBaseEsNoAutenticado() {
        assertThatThrownBy(() -> servicio.crear("fantasma@x.com", peticion(6, null, null, null, "Hay un problema que necesita atención", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo(Codigos.NO_AUTENTICADO));
    }

    @Test
    void consultarNormalizaElCodigoYSoloBuscaEntreLosReportesDelUsuario() {
        var detalle = mock(ReporteDetalleResponse.class);
        when(repositorio.buscarPropio(7L, "NU-000123")).thenReturn(Optional.of(detalle));

        assertThat(servicio.consultar("ana@x.com", "  nu-000123 ")).isSameAs(detalle);
    }

    @Test
    void consultarConCodigoMalFormadoDa400EnElCampoCodigo() {
        for (String malo : new String[] { null, "", "123", "NU-12", "NU-ABCDEF", "XX-000001" }) {
            assertThatThrownBy(() -> servicio.consultar("ana@x.com", malo)).isInstanceOfSatisfying(ApiException.class, e -> {
                assertThat(e.getCodigo()).isEqualTo(Codigos.VALIDACION);
                assertThat(e.getCampo()).isEqualTo("codigo");
            });
        }
        verify(repositorio, never()).buscarPropio(org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void consultarSinReporteVisibleDa404SinCampo() {
        when(repositorio.buscarPropio(7L, "NU-000999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar("ana@x.com", "NU-000999")).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo(Codigos.NO_ENCONTRADO);
            assertThat(e.getCampo()).isNull();
        });
    }
}
