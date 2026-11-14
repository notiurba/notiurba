package com.notiurba.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Trabajo que no debe hacer esperar al ciudadano (hoy, el correo de verificación). Pocos hilos y una cola
 * corta: si se llena, la tarea se descarta con un aviso en el log y la persona puede pedir el reenvío.
 * Con notiurba.segundo-plano.sincrono=true (pruebas) todo corre en el mismo hilo.
 */
@Configuration
@EnableAsync
public class SegundoPlanoConfig {

    public static final String EJECUTOR = "tareasSegundoPlano";

    private static final Logger log = LoggerFactory.getLogger(SegundoPlanoConfig.class);

    @Bean(EJECUTOR)
    TaskExecutor tareasSegundoPlano(@Value("${notiurba.segundo-plano.sincrono:false}") boolean sincrono) {
        if (sincrono) {
            return new SyncTaskExecutor();
        }
        var ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(2);
        ejecutor.setQueueCapacity(50);
        ejecutor.setThreadNamePrefix("segundo-plano-");
        ejecutor.setRejectedExecutionHandler((tarea, pool) -> log.warn("Tarea en segundo plano descartada: la cola está llena"));
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(10);
        return ejecutor;
    }
}
