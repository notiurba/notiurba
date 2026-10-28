package com.notiurba.usuario;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.OffsetDateTime;

import org.mockito.quality.Strictness;

/** Usuarios en memoria para las pruebas unitarias (sin base de datos). */
public final class UsuariosDePrueba {

    private UsuariosDePrueba() {
    }

    public static Rol rol(String nombre) {
        Rol rol = mock(Rol.class, withSettings().strictness(Strictness.LENIENT));
        when(rol.getNombre()).thenReturn(nombre);
        return rol;
    }

    /** Cuenta que solo entra con Google (password_hash NULL). */
    public static Usuario soloGoogle(String correo) {
        return Usuario.nuevoConGoogle("Ana", "Gómez", (short) 1, "123", 1, "Cra 1", correo, null,
                rol(Rol.CIUDADANO), (short) 1, OffsetDateTime.now().minusDays(1));
    }

    public static Usuario conContrasena(String correo, String passwordHash, String rol) {
        Usuario usuario = Usuario.nuevoConGoogle("Ana", "Gómez", (short) 1, "123", 1, "Cra 1", correo, null,
                rol(rol), (short) 1, OffsetDateTime.now().minusDays(1));
        usuario.cambiarPasswordHash(passwordHash);
        return usuario;
    }
}
