package com.notiurba.reporte;

/** Lo que el ciudadano llena en el formulario; lo comparten la creación y la vista previa del mensaje. */
public interface BorradorDeReporte {
    Integer idCategoria();
    Double latitud();
    Double longitud();
    Integer idBarrio();
    String descripcion();
    String direccionIncidente();
    String matriculaCuentaServicio();
    Boolean emergencia();
}
