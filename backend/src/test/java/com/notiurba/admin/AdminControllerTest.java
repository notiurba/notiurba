package com.notiurba.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.notiurba.config.SecurityConfig;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;

/** MVC, JWT y servicio reales; solo se sustituye el acceso a persistencia. */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtProveedor.class, AdminServicio.class})
@TestPropertySource(locations = "classpath:pruebas.properties")
class AdminControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProveedor jwt;
    @MockBean AdminRepositorio repositorio;
    @MockBean UsuarioRepository usuarios;
    private String token;
    private final OffsetDateTime fecha = OffsetDateTime.parse("2026-11-06T12:00:00Z");

    @BeforeEach void preparar() {
        token = jwt.generar("admin@example.test", "ADMIN");
        var usuario = mock(Usuario.class);
        var rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn("ADMIN");
        when(usuario.getRol()).thenReturn(rol);
        when(usuario.getIdUsuario()).thenReturn(7L);
        when(usuarios.findByCorreoIgnoreCase("admin@example.test")).thenReturn(Optional.of(usuario));
        actual("Recibido", 1);
        when(repositorio.destino(any())).thenAnswer(i -> {
            EstadoReporte e = i.getArgument(0);
            return new AdminRepositorio.EstadoDestino((short) (e.ordinal()+1), e.ordinal()+1);
        });
        when(repositorio.ultimo(1)).thenReturn(Optional.empty());
        when(repositorio.insertar(anyLong(), anyShort(), anyString(), anyLong())).thenReturn(fecha);
        when(repositorio.vigencia()).thenReturn(30);
    }

    private void actual(String nombre, int orden) {
        when(repositorio.bloquear("NU-000001")).thenReturn(Optional.of(
                new AdminRepositorio.ReporteBloqueado(1, nombre, orden, fecha.plusDays(30))));
    }

    private org.springframework.test.web.servlet.ResultActions cambiar(String estado, String observacion) throws Exception {
        return mvc.perform(patch("/api/admin/reportes/NU-000001/estado").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\",\"observacion\":\"" + observacion + "\",\"idUsuario\":999}"));
    }

    @ParameterizedTest
    @CsvSource({"Recibido,1,EN_TRAMITE", "Recibido,1,RESUELTO", "Enviado,2,EN_TRAMITE",
            "Enviado,2,RESUELTO", "En trámite,3,RESUELTO"})
    void aceptaTodosLosAvancesManuales(String origen, int orden, String destino) throws Exception {
        actual(origen, orden);
        cambiar(destino, "  Evidencia revisada  ").andExpect(status().isOk())
                .andExpect(jsonPath("estado").value(destino)).andExpect(jsonPath("cambiadoPor").value(7))
                .andExpect(jsonPath("observacion").value("Evidencia revisada"))
                .andExpect(jsonPath("cambiadoEn").exists()).andExpect(jsonPath("vigenteHasta").exists());
        verify(repositorio).insertar(1, (short)(EstadoReporte.valueOf(destino).ordinal()+1), "Evidencia revisada", 7);
    }

    @ParameterizedTest
    @CsvSource({"Enviado,2,RECIBIDO", "En trámite,3,RECIBIDO", "En trámite,3,ENVIADO",
            "Resuelto,4,RECIBIDO", "Resuelto,4,ENVIADO", "Resuelto,4,EN_TRAMITE", "Resuelto,4,RESUELTO",
            "Recibido,1,ENVIADO"})
    void rechazaRetrocesosTerminalYEnvioManual(String origen, int orden, String destino) throws Exception {
        actual(origen, orden);
        cambiar(destino, "Motivo").andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("CONFLICTO"));
        verify(repositorio, never()).insertar(anyLong(), anyShort(), anyString(), anyLong());
    }

    @Test void inexistenteDa404() throws Exception {
        when(repositorio.bloquear("NU-000001")).thenReturn(Optional.empty());
        cambiar("EN_TRAMITE", "Motivo").andExpect(status().isNotFound()).andExpect(jsonPath("codigo").value("NO_ENCONTRADO"));
    }

    @Test void evitaDuplicadoYPermiteNuevaObservacionSinAvanzar() throws Exception {
        actual("En trámite", 3);
        when(repositorio.ultimo(1)).thenReturn(Optional.of(new AdminRepositorio.Asiento("Motivo", 7L, fecha, (short)3)));
        cambiar("EN_TRAMITE", " Motivo ").andExpect(status().isOk());
        verify(repositorio, never()).insertar(anyLong(), anyShort(), anyString(), anyLong());
        cambiar("EN_TRAMITE", "Información adicional").andExpect(status().isOk());
        verify(repositorio).insertar(1, (short)3, "Información adicional", 7);
    }

    @ParameterizedTest @ValueSource(strings = {"", "   "})
    void observacionVaciaDa400(String texto) throws Exception {
        cambiar("EN_TRAMITE", texto).andExpect(status().isBadRequest()).andExpect(jsonPath("campo").value("observacion"));
        verifyNoInteractions(repositorio);
    }

    @Test void validaEstadoLongitudYCodigo() throws Exception {
        cambiar("NUEVO", "Motivo").andExpect(status().isBadRequest());
        cambiar("EN_TRAMITE", "a".repeat(1001)).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/reportes/incorrecto/estado").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"RESUELTO\",\"observacion\":\"Motivo\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("campo").value("codigo"));
    }

    @Test void estadoNumericoNoSeConvierteEnOrdinal() throws Exception {
        mvc.perform(patch("/api/admin/reportes/NU-000001/estado").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":3,\"observacion\":\"Motivo\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repositorio);
    }

    @Test void administradorEliminadoORolRevocadoNoModificaDatos() throws Exception {
        when(usuarios.findByCorreoIgnoreCase("admin@example.test")).thenReturn(Optional.empty());
        cambiar("EN_TRAMITE", "Motivo").andExpect(status().isUnauthorized());
        var usuario = mock(Usuario.class);
        var rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn("CIUDADANO");
        when(usuario.getRol()).thenReturn(rol);
        when(usuarios.findByCorreoIgnoreCase("admin@example.test")).thenReturn(Optional.of(usuario));
        cambiar("EN_TRAMITE", "Motivo").andExpect(status().isForbidden());
        verifyNoInteractions(repositorio);
    }

    @ParameterizedTest @ValueSource(strings = {"ADMIN", "CIUDADANO", "SIN_TOKEN"})
    void autorizaLosTresEndpoints(String rol) throws Exception {
        String credencial = rol.equals("SIN_TOKEN") ? "" : "Bearer " + jwt.generar("admin@example.test", rol);
        int esperado = rol.equals("ADMIN") ? 200 : rol.equals("CIUDADANO") ? 403 : 401;
        var resultado = mvc.perform(get("/api/admin/configuracion/vigencia").header("Authorization", credencial))
                .andExpect(status().is(esperado)).andReturn();
        if (esperado == 200) assertThat(resultado.getResponse().getContentAsString()).contains("30");
        mvc.perform(put("/api/admin/configuracion/vigencia").header("Authorization", credencial)
                .contentType(MediaType.APPLICATION_JSON).content("{\"vigenciaDias\":15}"))
                .andExpect(status().is(esperado));
        mvc.perform(patch("/api/admin/reportes/NU-000001/estado").header("Authorization", credencial)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_TRAMITE\",\"observacion\":\"Motivo\"}"))
                .andExpect(status().is(esperado));
        if (esperado != 200) verifyNoInteractions(repositorio, usuarios);
        else verify(repositorio).guardarVigencia(15, 7);
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.5", "\"15\"", "true", "null", "2147483648", "{}", "[]"})
    void vigenciaExigeEnteroPositivoSinCoercion(String valor) throws Exception {
        mvc.perform(put("/api/admin/configuracion/vigencia").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"vigenciaDias\":" + valor + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("codigo").value("VALIDACION"));
        verify(repositorio, never()).guardarVigencia(anyInt(), anyLong());
    }
}
