package com.notiurba.correo;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="notiurba.correo.reportes-habilitados",havingValue="true")
public class ColaCorreoConfig {
    private final EnvioReporteServicio servicio;
    public ColaCorreoConfig(EnvioReporteServicio servicio){this.servicio=servicio;}
    @Scheduled(fixedDelayString="${notiurba.correo.intervalo-ms:3000}")
    public void procesar(){servicio.procesarSiguiente();}
}
