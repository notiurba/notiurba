package com.notiurba.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class SegundoPlanoConfigTest {

    private final SegundoPlanoConfig config = new SegundoPlanoConfig();

    /** El registro y el reenvío de verificación usan este ejecutor: una cola llena no puede lanzarles un error. */
    @Test
    void conLaColaLlenaLaTareaSeDescartaConUnAvisoSinLanzarError() throws Exception {
        ThreadPoolTaskExecutor ejecutor = (ThreadPoolTaskExecutor) config.tareasSegundoPlano(false);
        ejecutor.initialize();
        var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SegundoPlanoConfig.class);
        var registro = new ListAppender<ILoggingEvent>();
        registro.start();
        logger.addAppender(registro);
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicBoolean descartadaCorrio = new AtomicBoolean();
        try {
            llenar(ejecutor, liberar);

            assertThatCode(() -> ejecutor.execute(() -> descartadaCorrio.set(true))).doesNotThrowAnyException();
            assertThat(registro.list).extracting(ILoggingEvent::getFormattedMessage)
                    .contains("Tarea en segundo plano descartada: la cola está llena (segundo-plano-)");
        } finally {
            logger.detachAppender(registro);
            liberar.countDown();
            ejecutor.shutdown();
        }
        assertThat(descartadaCorrio).isFalse();
    }

    @Test
    void conLaColaDeRecuperacionLlenaElCorreoDeVerificacionSigueSaliendo() throws Exception {
        ThreadPoolTaskExecutor recuperacion = (ThreadPoolTaskExecutor) config.tareasRecuperacion(false);
        ThreadPoolTaskExecutor verificacion = (ThreadPoolTaskExecutor) config.tareasSegundoPlano(false);
        recuperacion.initialize();
        verificacion.initialize();
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch verificacionEnviada = new CountDownLatch(1);
        try {
            llenar(recuperacion, liberar);
            assertThatCode(() -> recuperacion.execute(() -> { })).doesNotThrowAnyException();

            verificacion.execute(verificacionEnviada::countDown);

            assertThat(verificacionEnviada.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            liberar.countDown();
            recuperacion.shutdown();
            verificacion.shutdown();
        }
    }

    /** Ocupa todos los hilos con tareas bloqueadas y completa la cola. */
    private static void llenar(ThreadPoolTaskExecutor ejecutor, CountDownLatch liberar) throws InterruptedException {
        int hilos = ejecutor.getMaxPoolSize();
        CountDownLatch ocupados = new CountDownLatch(hilos);
        for (int i = 0; i < hilos; i++) {
            ejecutor.execute(() -> {
                ocupados.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException interrumpida) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertThat(ocupados.await(2, TimeUnit.SECONDS)).isTrue();
        for (int i = 0; i < ejecutor.getQueueCapacity(); i++) {
            ejecutor.execute(() -> { });
        }
    }
}
