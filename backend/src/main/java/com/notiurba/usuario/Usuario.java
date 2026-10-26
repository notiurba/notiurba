package com.notiurba.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Mapea solo las columnas que necesita hoy el login (correo + password_hash + rol).
 * El resto de columnas de la tabla (nombre, documento, barrio, etc.) no se tocan
 * desde JPA todavía; esta entidad es de solo lectura para autenticación.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    protected Usuario() {
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }
}
