package com.notiurba.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Un solo reloj para toda la app, para poder fijarlo en las pruebas. El «día» del negocio es el de Colombia. */
@Configuration
public class RelojConfig {

    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
