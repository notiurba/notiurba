package com.notiurba.reporte;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

import jakarta.annotation.PreDestroy;

/**
 * POST /api/reportes/{codigo}/enviar: manda el reporte a la entidad por correo (RF-14).
 *
 * <p>Responde rápido (202) y el correo sale en un hilo aparte (RNF-04). Es idempotente: si el envío
 * ya salió o está en curso no se repite; si falló (o se interrumpió) se puede volver a pedir.
 * El reporte pasa a «Enviado» cuando Resend acepta el correo (decisión 3 del contrato).
 */
@Service
public class EnvioReporteServicio {

    private static final Logger log = LoggerFactory.getLogger(EnvioReporteServicio.class);
    private static final String MENSAJE_NO_ENCONTRADO = "No encontramos ese reporte.";

    private record Iniciado(ReporteDetalleResponse.Envio envio, Long idRegistroNuevo) {
    }

    private final ReporteRepositorio reportes;
    private final EnvioReporteRepositorio envios;
    private final UsuarioRepository usuarios;
    private final EnviadorCorreo enviador;
    private final TransactionTemplate tx;
    private final String destinoPrueba;
    private final boolean sincrono;
    private final ExecutorService ejecutor;

    public EnvioReporteServicio(ReporteRepositorio reportes, EnvioReporteRepositorio envios, UsuarioRepository usuarios,
            EnviadorCorreo enviador, PlatformTransactionManager transacciones,
            @Value("${notiurba.correo.destino-prueba:}") String destinoPrueba,
            @Value("${notiurba.correo.envio-sincrono:false}") boolean sincrono) {
        this.reportes = reportes;
        this.envios = envios;
        this.usuarios = usuarios;
        this.enviador = enviador;
        this.tx = new TransactionTemplate(transacciones);
        this.destinoPrueba = destinoPrueba;
        this.sincrono = sincrono;
        this.ejecutor = sincrono ? null : Executors.newFixedThreadPool(2, r -> {
            Thread hilo = new Thread(r, "envio-correo-reporte");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    @PreDestroy
    void cerrar() {
        if (ejecutor != null) {
            ejecutor.shutdown();
        }
    }

    /** @return el estado del envío tras esta llamada (PENDIENTE si apenas se inició, ENVIADO si ya había salido) */
    public ReporteDetalleResponse.Envio enviar(String correoUsuario, String codigo) {
        String normalizado = ReporteServicio.normalizarCodigo(codigo);
        long idUsuario = usuarios.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO))
                .getIdUsuario();

        Iniciado iniciado = tx.execute(s -> iniciar(idUsuario, normalizado));

        if (iniciado.idRegistroNuevo() != null) {
            long id = iniciado.idRegistroNuevo();
            if (sincrono) {
                procesar(id);
            } else {
                ejecutor.execute(() -> procesar(id));
            }
        }
        return iniciado.envio();
    }

    /** Dentro de una transacción con el reporte bloqueado: dos llamadas a la vez no crean dos envíos. */
    private Iniciado iniciar(long idUsuario, String codigo) {
        long idReporte = reportes.bloquearReportePropio(idUsuario, codigo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Codigos.NO_ENCONTRADO, MENSAJE_NO_ENCONTRADO));

        var ultimo = envios.ultimoEnvio(idReporte);
        if (ultimo.isPresent()) {
            var u = ultimo.get();
            if ("ENVIADO".equals(u.estado())) {
                return new Iniciado(new ReporteDetalleResponse.Envio("ENVIADO", null), null);
            }
            if ("PENDIENTE".equals(u.estado())) {
                if (!u.vencido()) {
                    return new Iniciado(new ReporteDetalleResponse.Envio("PENDIENTE", null), null);
                }
                envios.marcarFallido(u.idRegistro(), "Se interrumpió el envío anterior.");
            }
        }

        int canal = envios.canalPrincipal(idReporte).orElseThrow(() -> new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY, Codigos.SIN_DESTINO_DE_CORREO, Codigos.MSG_SIN_DESTINO_DE_CORREO));
        long idRegistro = envios.crearPendiente(idReporte, canal);
        return new Iniciado(new ReporteDetalleResponse.Envio("PENDIENTE", null), idRegistro);
    }

    /** Arma y manda el correo; deja el registro en ENVIADO o FALLIDO. Nunca lanza: corre en segundo plano. */
    void procesar(long idRegistro) {
        try {
            DatosEnvio datos = envios.cargar(idRegistro).orElse(null);
            if (datos == null) {
                return;
            }
            try {
                String idProveedor = enviador.enviar(CorreoDelReporte.armar(datos, destinoPrueba));
                tx.executeWithoutResult(s -> {
                    envios.marcarEnviado(idRegistro, idProveedor);
                    envios.idReporteDe(idRegistro).ifPresent(envios::pasarReporteAEnviado);
                });
            } catch (CorreoNoEnviadoException e) {
                log.warn("No se pudo enviar el reporte {} (reintentable={}): {}", datos.codigoSeguimiento(),
                        e.reintentable(), e.getMessage());
                envios.marcarFallido(idRegistro, e.getMessage());
            }
        } catch (RuntimeException e) {
            log.error("Error inesperado enviando el registro {}", idRegistro, e);
            try {
                envios.marcarFallido(idRegistro, "Error inesperado al enviar el correo.");
            } catch (RuntimeException ignorada) {
                log.error("Tampoco se pudo marcar como fallido el registro {}", idRegistro, ignorada);
            }
        }
    }
}
