package com.notiurba.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.admin.ParametrosServicio;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Límite de intentos de ingreso por correo: cada fallo se cuenta dentro de una ventana y, al llegar al máximo,
 * ese correo queda bloqueado un rato. Cuenta igual los correos que no existen, así no se revela qué cuentas hay.
 * Los valores salen de la base (ParametrosServicio) y la fila de cada correo se bloquea mientras se actualiza,
 * de modo que intentos simultáneos no se saltan el tope.
 */
@Service
public class IntentosLoginServicio {

    /** Resultado de contar un fallo: o quedó bloqueado hasta cierta hora, o le quedan intentos. */
    public record Fallo(OffsetDateTime bloqueadoHasta, int restantes) {
        public boolean bloqueado() {
            return bloqueadoHasta != null;
        }
    }

    private final JdbcTemplate jdbc;
    private final ParametrosServicio parametros;
    private final Clock reloj;

    public IntentosLoginServicio(JdbcTemplate jdbc, ParametrosServicio parametros, Clock reloj) {
        this.jdbc = jdbc;
        this.parametros = parametros;
        this.reloj = reloj;
    }

    /** Minúsculas y sin espacios; acotado al tamaño de la columna. */
    static String normalizar(String correo) {
        String limpio = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        return limpio.length() > 255 ? limpio.substring(0, 255) : limpio;
    }

    /** 429 si el correo está bloqueado ahora, aunque la contraseña sea la correcta. */
    @Transactional(readOnly = true)
    public void verificarNoBloqueado(String correo) {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        OffsetDateTime hasta = jdbc.query("SELECT bloqueado_hasta FROM intento_login WHERE correo_normalizado = ?",
                rs -> rs.next() ? rs.getObject(1, OffsetDateTime.class) : null, normalizar(correo));
        if (hasta != null && hasta.isAfter(ahora)) {
            throw bloqueo(hasta, ahora);
        }
    }

    /** Cuenta un fallo. Si con él se llega al máximo (o ya había bloqueo), devuelve la hora hasta la que queda bloqueado. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Fallo registrarFallo(String correo) {
        String clave = normalizar(correo);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        int maximo = parametros.intentosMaximos();
        Duration ventana = Duration.ofMinutes(parametros.ventanaLoginMinutos());

        jdbc.update("INSERT INTO intento_login (correo_normalizado) VALUES (?) ON CONFLICT DO NOTHING", clave);
        record Fila(int fallidos, OffsetDateTime primero, OffsetDateTime bloqueado) {
        }
        Fila fila = jdbc.queryForObject("""
                SELECT fallidos, primer_fallo_en, bloqueado_hasta FROM intento_login
                 WHERE correo_normalizado = ? FOR UPDATE""",
                (rs, i) -> new Fila(rs.getInt(1), rs.getObject(2, OffsetDateTime.class), rs.getObject(3, OffsetDateTime.class)), clave);
        int fallidos = fila.fallidos();
        OffsetDateTime primero = fila.primero();
        OffsetDateTime bloqueado = fila.bloqueado();

        if (bloqueado != null && bloqueado.isAfter(ahora)) {
            return new Fallo(bloqueado, 0);
        }
        if (bloqueado != null) {
            fallidos = 0;
            primero = null;
        }
        if (primero == null || !primero.plus(ventana).isAfter(ahora)) {
            fallidos = 1;
            primero = ahora;
        } else {
            fallidos++;
        }

        Fallo resultado;
        if (fallidos >= maximo) {
            OffsetDateTime hasta = ahora.plusMinutes(parametros.bloqueoLoginMinutos());
            jdbc.update("""
                    UPDATE intento_login SET fallidos = 0, primer_fallo_en = NULL, bloqueado_hasta = ?, actualizado_en = ?
                     WHERE correo_normalizado = ?""", hasta, ahora, clave);
            resultado = new Fallo(hasta, 0);
        } else {
            jdbc.update("""
                    UPDATE intento_login SET fallidos = ?, primer_fallo_en = ?, bloqueado_hasta = NULL, actualizado_en = ?
                     WHERE correo_normalizado = ?""", fallidos, primero, ahora, clave);
            resultado = new Fallo(null, maximo - fallidos);
        }
        // Que correos inventados no engorden la tabla: se borra lo que lleva más de un día sin moverse
        jdbc.update("DELETE FROM intento_login WHERE actualizado_en < ?", ahora.minusDays(1));
        return resultado;
    }

    /** Un ingreso correcto, o restablecer la contraseña con el enlace del correo, empieza de cero. */
    @Transactional
    public void limpiar(String correo) {
        jdbc.update("DELETE FROM intento_login WHERE correo_normalizado = ?", normalizar(correo));
    }

    /** El 429 con los minutos que faltan (se sube al minuto entero) y los segundos exactos para el cliente. */
    public ApiException bloqueo(OffsetDateTime hasta, OffsetDateTime ahora) {
        long segundos = Math.max(1, Duration.between(ahora, hasta).toSeconds() + (Duration.between(ahora, hasta).toNanosPart() > 0 ? 1 : 0));
        long minutos = (segundos + 59) / 60;
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("esperarSegundos", segundos);
        datos.put("bloqueadoHasta", hasta.toInstant().toString());
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, Codigos.INTENTOS_EXCEDIDOS,
                "Demasiados intentos. Espera " + minutos + (minutos == 1 ? " minuto" : " minutos") + " para volver a intentar.",
                null, datos);
    }

    /** Para los que ya tienen el resultado de registrarFallo y quieren el mismo 429. */
    public ApiException bloqueo(Fallo fallo) {
        return bloqueo(fallo.bloqueadoHasta(), OffsetDateTime.now(reloj));
    }
}
