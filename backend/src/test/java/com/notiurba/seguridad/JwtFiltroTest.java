package com.notiurba.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.admin.AdminPingController;
import com.notiurba.config.SecurityConfig;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Solo un token de acceso autentica: el de registro con Google o uno sin «tipo» no. */
@WebMvcTest(AdminPingController.class)
@Import({ SecurityConfig.class, JwtProveedor.class })
@TestPropertySource(locations = "classpath:pruebas.properties")
class JwtFiltroTest {

    private static final String SECRETO = "secreto-de-pruebas-de-notiurba-con-mas-de-32-bytes";

    @Autowired private MockMvc mvc;
    @Autowired private JwtProveedor jwtProveedor;

    @Test
    void tokenDeAccesoAdminEntra() throws Exception {
        mvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + jwtProveedor.generar("admin@x.com", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void tokenDeRegistroGoogleNoAutentica() throws Exception {
        mvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + jwtProveedor.generarRegistroGoogle("sub", "a@gmail.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoSinTipoNoAutentica() throws Exception {
        String sinTipo = Jwts.builder()
                .subject("admin@x.com")
                .claim("rol", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes()))
                .compact();

        mvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + sinTipo))
                .andExpect(status().isUnauthorized());
    }
}
