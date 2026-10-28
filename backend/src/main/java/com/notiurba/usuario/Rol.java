package com.notiurba.usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol")
public class Rol {

    public static final String CIUDADANO = "CIUDADANO";

    @Id
    private Short idRol;

    private String nombre;

    protected Rol() {
    }

    public Short getIdRol() {
        return idRol;
    }

    public String getNombre() {
        return nombre;
    }
}
