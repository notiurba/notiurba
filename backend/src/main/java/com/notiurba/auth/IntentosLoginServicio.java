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
 * de modo que intentos simultáneos no se saltan el tope ni prueban más contraseñas que el máximo.
 */
@Service
public class IntentosLoginServicio {

    /**
     * Un intento ya contado como fallo antes de mirar la contraseña. Si la contraseña resulta mala, quedan
     * {@code restantes} intentos o, si con este se llegó al máximo, el correo queda bloqueado hasta {@code bloqueadoHasta}.
     */
    public record Reserva(OffsetDateTime bloqueadoHasta, int restantes) {
        public boolean bloquea() {
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

    /**
     * Cuenta el intento como fallo ANTES de comparar la contraseña, con la fila del correo bloqueada: así una
     * ráfaga de peticiones a la vez no puede probar más contraseñas que el máximo. Si el correo ya está
     * bloqueado responde 429 sin mirar la contraseña. Si luego la contraseña es correcta, {@link #limpiar}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reserva reservar(String correo) {
        String clave = normalizar(correo);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        int maximo = parametros.intentosMaximos();
        Duration ventana = Duration.ofMinutes(parametros.ventanaLoginMinutos());

        record Fila(int fallidos, OffsetDateTime primero, OffsetDateTime bloqueado) {
        }
        // Inserta o bloquea la fila existente en una sola sentencia: siempre devuelve una fila
        Fila fila = jdbc.queryForObject("""
                INSERT INTO intento_login (correo_normalizado, actualizado_en) VALUES (?, ?)
                ON CONFLICT (correo_normalizado) DO UPDATE SET actualizado_en = intento_login.actualizado_en
                RETURNING fallidos, primer_fallo_en, bloqueado_hasta""",
                (rs, i) -> new Fila(rs.getInt(1), rs.getObject(2, OffsetDateTime.class), rs.getObject(3, OffsetDateTime.class)),
                clave, ahora);
        if (fila.bloqueado() != null && fila.bloqueado().isAfter(ahora)) {
            throw bloqueo(fila.bloqueado());
        }

        boolean ventanaNueva = fila.bloqueado() != null || fila.primero() == null || !fila.primero().plus(ventana).isAfter(ahora);
        int fallidos = ventanaNueva ? 1 : fila.fallidos() + 1;
        OffsetDateTime primero = ventanaNueva ? ahora : fila.primero();

        Reserva reserva;
        if (fallidos >= maximo) {
            OffsetDateTime hasta = ahora.plusMinutes(parametros.bloqueoLoginMinutos());
            jdbc.update("""
                    UPDATE intento_login SET fallidos = 0, primer_fallo_en = NULL, bloqueado_hasta = ?, actualizado_en = ?
                     WHERE correo_normalizado = ?""", hasta, ahora, clave);
            reserva = new Reserva(hasta, 0);
        } else {
            jdbc.update("""
                    UPDATE intento_login SET fallidos = ?, primer_fallo_en = ?, bloqueado_hasta = NULL, actualizado_en = ?
                     WHERE correo_normalizado = ?""", fallidos, primero, ahora, clave);
            reserva = new Reserva(null, maximo - fallidos);
        }
        // Que correos inventados no engorden la tabla; las filas que otro está usando se dejan para después
        jdbc.update("""
                DELETE FROM intento_login WHERE correo_normalizado IN (
                  SELECT correo_normalizado FROM intento_login WHERE actualizado_en < ? FOR UPDATE SKIP LOCKED)""",
                ahora.minusDays(1));
        return reserva;
    }

    /** Un ingreso correcto, o restablecer la contraseña con el enlace del correo, empieza de cero. */
    @Transactional
    public void limpiar(String correo) {
        jdbc.update("DELETE FROM intento_login WHERE correo_normalizado = ?", normalizar(correo));
    }

    /** El 429 con los minutos que faltan y los segundos exactos para el cliente, ambos redondeados hacia arriba. */
    public ApiException bloqueo(OffsetDateTime hasta) {
        Duration falta = Duration.between(OffsetDateTime.now(reloj), hasta);
        long segundos = Math.max(1, falta.plusNanos(999_999_999).getSeconds());
        long minutos = (segundos + 59) / 60;
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("esperarSegundos", segundos);
        datos.put("bloqueadoHasta", hasta.toInstant().toString());
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, Codigos.INTENTOS_EXCEDIDOS,
                "Demasiados intentos. Espera " + minutos + (minutos == 1 ? " minuto" : " minutos") + " para volver a intentar.",
                null, datos);
    }
}
