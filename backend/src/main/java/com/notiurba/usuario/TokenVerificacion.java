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

/** Token de un solo uso: se guarda solo su hash SHA-256, nunca el token. */
@Entity
@Table(name = "token_verificacion")
public class TokenVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token_verificacion")
    private Long idTokenVerificacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_token", nullable = false)
    private TipoToken tipoToken;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "usado_en")
    private OffsetDateTime usadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected TokenVerificacion() {
    }

    public TokenVerificacion(Usuario usuario, TipoToken tipoToken, String tokenHash,
            OffsetDateTime creadoEn, OffsetDateTime expiraEn) {
        this.usuario = usuario;
        this.tipoToken = tipoToken;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    /** Vigente = sin usar y antes de expira_en. */
    public boolean estaVigente(OffsetDateTime ahora) {
        return usadoEn == null && ahora.isBefore(expiraEn);
    }

    public void marcarUsado(OffsetDateTime ahora) {
        this.usadoEn = ahora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public OffsetDateTime getUsadoEn() {
        return usadoEn;
    }
}
