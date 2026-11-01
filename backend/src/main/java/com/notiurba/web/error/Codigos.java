package com.notiurba.web.error;

/** Códigos de error y mensajes fijos del contrato (docs/contexto/11-contrato-api.md). */
public final class Codigos {

    public static final String VALIDACION = "VALIDACION";
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS";
    public static final String SIN_PERMISO = "SIN_PERMISO";
    public static final String CORREO_NO_VERIFICADO = "CORREO_NO_VERIFICADO";
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";
    public static final String CORREO_YA_REGISTRADO = "CORREO_YA_REGISTRADO";
    public static final String DOCUMENTO_YA_REGISTRADO = "DOCUMENTO_YA_REGISTRADO";
    public static final String CUENTA_GOOGLE_YA_REGISTRADA = "CUENTA_GOOGLE_YA_REGISTRADA";
    public static final String CONFLICTO = "CONFLICTO";
    public static final String FOTOS_MAXIMO = "FOTOS_MAXIMO";
    public static final String ENLACE_INVALIDO = "ENLACE_INVALIDO";
    public static final String ARCHIVO_MUY_GRANDE = "ARCHIVO_MUY_GRANDE";
    public static final String FORMATO_NO_SOPORTADO = "FORMATO_NO_SOPORTADO";
    public static final String ALMACEN_FOTOS_NO_DISPONIBLE = "ALMACEN_FOTOS_NO_DISPONIBLE";
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    public static final String MSG_NO_AUTENTICADO = "Tu sesión no es válida o venció. Inicia sesión de nuevo.";
    public static final String MSG_SIN_PERMISO = "No tienes permiso para esta acción.";
    public static final String MSG_CREDENCIALES = "Correo o contraseña incorrectos.";
    public static final String MSG_CORREO_NO_VERIFICADO =
            "Confirma tu correo para ingresar. Revisa tu bandeja de entrada o pide un nuevo enlace.";

    private Codigos() {
    }
}
