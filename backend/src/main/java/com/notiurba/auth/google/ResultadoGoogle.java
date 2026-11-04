package com.notiurba.auth.google;

/** Respuesta de POST /api/auth/google: sesión iniciada o perfil por completar. */
public sealed interface ResultadoGoogle {

    /** Casos 1 y 2, y respuesta de /completar: igual que el login con contraseña. */
    record Sesion(String token, String rol) implements ResultadoGoogle {
    }

    /** Caso 3: no hay cuenta; el usuario completa el perfil con tokenRegistro (15 min). */
    record PerfilIncompleto(String estado, String tokenRegistro, String correo, String nombres, String apellidos)
            implements ResultadoGoogle {

        static final String ESTADO = "PERFIL_INCOMPLETO";
    }
}
