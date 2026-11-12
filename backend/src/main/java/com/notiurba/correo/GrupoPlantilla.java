package com.notiurba.correo;

/**
 * Plantilla del correo: A, problema en un lugar; B, consulta o solicitud; C, falla de un servicio
 * público (va dirigida a la empresa y admite matrícula o cuenta).
 */
public enum GrupoPlantilla {
    A, B, C;

    public static GrupoPlantilla de(boolean esEmpresa, boolean requiereUbicacion) {
        if (esEmpresa) {
            return C;
        }
        return requiereUbicacion ? A : B;
    }
}
