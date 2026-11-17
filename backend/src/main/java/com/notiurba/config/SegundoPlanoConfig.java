package com.notiurba.config;

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
 * Trabajo que no debe hacer esperar al ciudadano. Pocos hilos y una cola corta: si se llena, la tarea se descarta
 * con un aviso en el log y la persona puede volver a pedirlo. La recuperación de contraseña tiene su propia cola:
 * su endpoint es público y un abuso no debe dejar sin correo de verificación a quien se registra.
 * Todo @Async debe nombrar su cola: sin nombre, Spring usaría un ejecutor sin límite de hilos.
 * Con notiurba.segundo-plano.sincrono=true (pruebas) todo corre en el mismo hilo.
 */
@Configuration
@EnableAsync
public class SegundoPlanoConfig {

    public static final String EJECUTOR = "tareasSegundoPlano";
    public static final String EJECUTOR_RECUPERACION = "tareasRecuperacion";

    private static final Logger log = LoggerFactory.getLogger(SegundoPlanoConfig.class);

    @Bean(EJECUTOR)
    TaskExecutor tareasSegundoPlano(@Value("${notiurba.segundo-plano.sincrono:false}") boolean sincrono) {
        return crear(sincrono, 2, 50, "segundo-plano-");
    }

    @Bean(EJECUTOR_RECUPERACION)
    TaskExecutor tareasRecuperacion(@Value("${notiurba.segundo-plano.sincrono:false}") boolean sincrono) {
        return crear(sincrono, 2, 20, "recuperacion-");
    }

    private static TaskExecutor crear(boolean sincrono, int hilos, int cola, String prefijo) {
        if (sincrono) {
            return new SyncTaskExecutor();
        }
        var ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(hilos);
        ejecutor.setMaxPoolSize(hilos);
        ejecutor.setQueueCapacity(cola);
        ejecutor.setThreadNamePrefix(prefijo);
        ejecutor.setRejectedExecutionHandler(
                (tarea, pool) -> log.warn("Tarea en segundo plano descartada: la cola está llena ({})", prefijo));
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(10);
        return ejecutor;
    }
}
