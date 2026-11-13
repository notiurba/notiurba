package com.notiurba.reporte;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.notiurba.admin.ParametrosServicio;
import com.notiurba.config.RelojConfig;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Tope de reportes por cuenta y por día (el día es el de Colombia: se reinicia a las 00:00). El tope sale de la
 * base, así que un cambio del administrador aplica de inmediato. Cada cuenta tiene su propio cupo.
 */
@Service
public class LimiteDiarioServicio {

    private final ReporteRepositorio repositorio;
    private final ParametrosServicio parametros;
    private final Clock reloj;

    public LimiteDiarioServicio(ReporteRepositorio repositorio, ParametrosServicio parametros, Clock reloj) {
        this.repositorio = repositorio;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    /** Lanza 429 si la cuenta ya llegó al tope de hoy. Quien llama debe tener bloqueada la fila del usuario. */
    public void verificar(long idUsuario) {
        ZonedDateTime ahora = ZonedDateTime.now(reloj.withZone(RelojConfig.ZONA_NEGOCIO));
        OffsetDateTime inicioDelDia = ahora.toLocalDate().atStartOfDay(RelojConfig.ZONA_NEGOCIO).toOffsetDateTime();
        int maximo = parametros.reportesMaximosPorDia();
        if (repositorio.contarDesde(idUsuario, inicioDelDia) < maximo) {
            return;
        }
        OffsetDateTime manana = inicioDelDia.plusDays(1).atZoneSameInstant(RelojConfig.ZONA_NEGOCIO).toOffsetDateTime();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("maximo", maximo);
        datos.put("vuelveEn", manana.toString());
        throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, Codigos.LIMITE_DIARIO_REPORTES,
                "Hoy ya enviaste " + maximo + (maximo == 1 ? " reporte" : " reportes")
                        + ", que es el máximo por día. Podrás enviar otro desde mañana.", null, datos);
    }
}
