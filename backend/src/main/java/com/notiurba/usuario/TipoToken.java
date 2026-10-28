package com.notiurba.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de tipos de token de un solo uso (verificar correo, recuperar contraseña). */
@Entity
@Table(name = "tipo_token")
public class TipoToken {

    public static final String RECUPERACION_CONTRASENA = "Recuperación de contraseña";

    @Id
    @Column(name = "id_tipo_token")
    private Short idTipoToken;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    protected TipoToken() {
    }

    public Short getIdTipoToken() {
        return idTipoToken;
    }

    public String getNombre() {
        return nombre;
    }
}
