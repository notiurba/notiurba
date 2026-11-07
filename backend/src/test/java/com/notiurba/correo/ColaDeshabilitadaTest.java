package com.notiurba.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.*;

class ColaDeshabilitadaTest {
    @Test void llamadaDirectaNoAccedeABaseNiProveedorCuandoEstaApagada() {
        var jdbc=mock(JdbcTemplate.class);
        var tx=mock(TransactionTemplate.class);
        var rutas=mock(EnrutamientoServicio.class);
        var plantilla=mock(PlantillaReporte.class);
        var proveedor=mock(EnviadorCorreo.class);
        var json=mock(ObjectMapper.class);
        var servicio=new EnvioReporteServicio(jdbc,tx,rutas,plantilla,proveedor,json,false);
        servicio.procesarSiguiente();
        verifyNoInteractions(jdbc,tx,rutas,plantilla,proveedor,json);
    }
}
