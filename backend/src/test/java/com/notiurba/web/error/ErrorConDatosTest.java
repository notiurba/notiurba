package com.notiurba.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** El error lleva `datos` solo cuando existen, y la espera de un bloqueo sale también en Retry-After. */
class ErrorConDatosTest {

    @RestController
    static class Prueba {
        @GetMapping("/bloqueo")
        void bloqueo() {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, Codigos.INTENTOS_EXCEDIDOS, "Espera 4 minutos.", null,
                    Map.of("esperarSegundos", 240));
        }

        @GetMapping("/sin-datos")
        void sinDatos() {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "Revisa el campo.", "correo");
        }

        @GetMapping("/otros-datos")
        void otrosDatos() {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.POSIBLE_DUPLICADO, "Hay uno parecido.", null,
                    Map.of("cantidad", 2, "esPropio", false));
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Prueba()).setControllerAdvice(new ManejadorErrores()).build();

    @Test
    void conDatosSalenEnElCuerpoYLaEsperaEnRetryAfter() throws Exception {
        var respuesta = mvc.perform(get("/bloqueo"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("INTENTOS_EXCEDIDOS"))
                .andExpect(jsonPath("$.mensaje").value("Espera 4 minutos."))
                .andExpect(jsonPath("$.datos.esperarSegundos").value(240))
                .andReturn().getResponse();
        assertThat(respuesta.getHeader("Retry-After")).isEqualTo("240");
    }

    @Test
    void sinDatosLaLlaveNoAparece() throws Exception {
        var cuerpo = mvc.perform(get("/sin-datos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("correo"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).doesNotContain("datos");
    }

    @Test
    void otrosDatosNoAgreganRetryAfter() throws Exception {
        var respuesta = mvc.perform(get("/otros-datos"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.datos.cantidad").value(2))
                .andExpect(jsonPath("$.datos.esPropio").value(false))
                .andReturn().getResponse();
        assertThat(respuesta.getHeader("Retry-After")).isNull();
    }
}
