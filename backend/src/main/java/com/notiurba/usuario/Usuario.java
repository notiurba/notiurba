package com.notiurba.usuario;

import java.time.OffsetDateTime;

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
 * Usuario de NotiUrba. Los catálogos (tipo de documento, barrio, aviso de privacidad)
 * se mapean como ids simples: el login y el alta con Google no necesitan cargarlos.
 * password_hash es NULL en las cuentas que solo entran con Google (regla B13 en la base).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "id_tipo_documento", nullable = false)
    private Short idTipoDocumento;

    @Column(name = "numero_documento", nullable = false)
    private String numeroDocumento;

    @Column(name = "id_barrio", nullable = false)
    private Integer idBarrio;

    @Column(name = "direccion_correspondencia", nullable = false)
    private String direccionCorrespondencia;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "correo_verificado_en")
    private OffsetDateTime correoVerificadoEn;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "password_hash", nullable = true)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "id_aviso_privacidad", nullable = false)
    private Short idAvisoPrivacidad;

    @Column(name = "acepto_privacidad_en", nullable = false)
    private OffsetDateTime aceptoPrivacidadEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Usuario() {
    }

    /**
     * Ciudadano que se registra con Google: sin contraseña y con el correo ya verificado
     * por Google. creado_en, correo_verificado_en y acepto_privacidad_en usan el mismo
     * instante para cumplir ck_usuario_fechas sin depender del reloj de la base.
     */
    public static Usuario nuevoConGoogle(
            String nombres, String apellidos, Short idTipoDocumento, String numeroDocumento,
            Integer idBarrio, String direccionCorrespondencia, String correo, String telefono,
            Rol rol, Short idAvisoPrivacidad, OffsetDateTime ahora) {
        Usuario usuario = new Usuario();
        usuario.nombres = nombres;
        usuario.apellidos = apellidos;
        usuario.idTipoDocumento = idTipoDocumento;
        usuario.numeroDocumento = numeroDocumento;
        usuario.idBarrio = idBarrio;
        usuario.direccionCorrespondencia = direccionCorrespondencia;
        usuario.correo = correo;
        usuario.telefono = telefono;
        usuario.rol = rol;
        usuario.idAvisoPrivacidad = idAvisoPrivacidad;
        usuario.creadoEn = ahora;
        usuario.correoVerificadoEn = ahora;
        usuario.aceptoPrivacidadEn = ahora;
        return usuario;
    }

    /** B8: el correo queda verificado (al usar el token o al entrar con Google). */
    public void marcarCorreoVerificado(OffsetDateTime ahora) {
        if (correoVerificadoEn == null) {
            correoVerificadoEn = ahora;
        }
    }

    public void cambiarPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public OffsetDateTime getCorreoVerificadoEn() {
        return correoVerificadoEn;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }
}
