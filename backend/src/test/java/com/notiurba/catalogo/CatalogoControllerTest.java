package com.notiurba.catalogo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.config.SecurityConfig;
import com.notiurba.seguridad.JwtProveedor;

/** NOT-55: los catálogos son públicos (sin token) y devuelven la forma del contrato. */
@WebMvcTest(CatalogoController.class)
@Import({ SecurityConfig.class, JwtProveedor.class })
@TestPropertySource(locations = "classpath:pruebas.properties")
class CatalogoControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private JdbcTemplate jdbc;

    @Test
    @SuppressWarnings("unchecked")
    void tiposDocumentoEsPublicoYDevuelveLaForma() throws Exception {
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(new CatalogoController.TipoDocumentoResponse((short) 1, "Cédula de ciudadanía", "CC")));

        mvc.perform(get("/api/catalogos/tipos-documento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTipoDocumento").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Cédula de ciudadanía"))
                .andExpect(jsonPath("$[0].codigo").value("CC"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void barriosEsPublicoYDevuelveLaForma() throws Exception {
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(new CatalogoController.BarrioResponse(7, (short) 3, "Centro")));

        mvc.perform(get("/api/catalogos/barrios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idBarrio").value(7))
                .andExpect(jsonPath("$[0].idComuna").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Centro"));
    }
}
