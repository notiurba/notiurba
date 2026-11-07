package com.notiurba.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.notiurba.catalogo.CatalogoController;
import com.notiurba.seguridad.JwtProveedor;

/**
 * CORS con el valor que se pone en Render: el sitio de producción y un comodín para las vistas previas de
 * Netlify (una por PR). Una vista previa entra; cualquier otro sitio, incluidos los que solo se le parecen, recibe 403.
 */
@WebMvcTest(CatalogoController.class)
@Import({ SecurityConfig.class, JwtProveedor.class })
@TestPropertySource(locations = "classpath:pruebas.properties", properties =
        "notiurba.cors.allowed-origins=https://notiurba.netlify.app,https://deploy-preview-*--notiurba.netlify.app")
class CorsOrigenesTest {

    private static final String RUTA = "/api/catalogos/tipos-documento";

    @Autowired private MockMvc mvc;
    @MockBean private JdbcTemplate jdbc;

    @SuppressWarnings("unchecked")
    private void listaVacia() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
    }

    private void permitido(String origen) throws Exception {
        listaVacia();
        mvc.perform(get(RUTA).header("Origin", origen)).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origen));
        mvc.perform(options(RUTA).header("Origin", origen).header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origen));
    }

    private void rechazado(String origen) throws Exception {
        listaVacia();
        mvc.perform(get(RUTA).header("Origin", origen)).andExpect(status().isForbidden());
        mvc.perform(options(RUTA).header("Origin", origen).header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elSitioDeProduccionEntra() throws Exception {
        permitido("https://notiurba.netlify.app");
    }

    @Test
    void cualquierVistaPreviaDeNotiurbaEntra() throws Exception {
        permitido("https://deploy-preview-20--notiurba.netlify.app");
        permitido("https://deploy-preview-7--notiurba.netlify.app");
        permitido("https://deploy-preview-123--notiurba.netlify.app");
    }

    @Test
    void unSitioAjenoRecibe403() throws Exception {
        rechazado("https://evil.com");
        rechazado("http://localhost:5173");                                   // no está en la lista
    }

    @Test
    void losSitiosQueSoloSeLeParecenRecibenTambien403() throws Exception {
        rechazado("https://deploy-preview-20--otrositio.netlify.app");        // vista previa de otro sitio
        rechazado("https://deploy-preview-20--notiurba.netlify.app.evil.com"); // el dominio real es evil.com
        rechazado("https://evil.com/deploy-preview-20--notiurba.netlify.app");
        rechazado("http://deploy-preview-20--notiurba.netlify.app");          // sin https
        rechazado("https://xnotiurba.netlify.app");
        rechazado("https://notiurba.netlify.app.evil.com");
        rechazado("https://otro.netlify.app");
    }
}
