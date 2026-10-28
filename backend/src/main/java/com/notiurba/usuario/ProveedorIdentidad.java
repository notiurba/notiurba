package com.notiurba.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de proveedores de identidad externa (hoy solo GOOGLE). */
@Entity
@Table(name = "proveedor_identidad")
public class ProveedorIdentidad {

    public static final String GOOGLE = "GOOGLE";

    @Id
    @Column(name = "id_proveedor_identidad")
    private Short idProveedorIdentidad;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    protected ProveedorIdentidad() {
    }

    public Short getIdProveedorIdentidad() {
        return idProveedorIdentidad;
    }

    public String getNombre() {
        return nombre;
    }
}
