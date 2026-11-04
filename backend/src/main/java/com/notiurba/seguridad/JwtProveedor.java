package com.notiurba.seguridad;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Firma y valida los JWT propios de NotiUrba. Hay dos tipos (claim «tipo»):
 * «acceso» autentica peticiones; «registro-google» solo sirve para completar
 * el perfil tras «Continuar con Google» y nunca autentica nada más.
 */
@Component
public class JwtProveedor {

    public static final String TIPO_ACCESO = "acceso";
    public static final String TIPO_REGISTRO_GOOGLE = "registro-google";
    static final long MINUTOS_REGISTRO_GOOGLE = 15;

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtProveedor(
            @Value("${notiurba.jwt.secreto}") String secreto,
            @Value("${notiurba.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes());
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(String correo, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim("tipo", TIPO_ACCESO)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /** Token de 15 minutos para POST /api/auth/google/completar: sub de Google + correo verificado. */
    public String generarRegistroGoogle(String subGoogle, String correo) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(subGoogle)
                .claim("tipo", TIPO_REGISTRO_GOOGLE)
                .claim("correo", correo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(MINUTOS_REGISTRO_GOOGLE, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /** Valida firma, vigencia y que sea un token de acceso (con rol). */
    public Claims validarAcceso(String token) {
        Claims claims = validarTipo(token, TIPO_ACCESO);
        if (claims.get("rol", String.class) == null) {
            throw new JwtException("El token de acceso no tiene rol");
        }
        return claims;
    }

    /** Valida firma, vigencia y que sea un token de registro con Google. */
    public Claims validarRegistroGoogle(String token) {
        return validarTipo(token, TIPO_REGISTRO_GOOGLE);
    }

    private Claims validarTipo(String token, String tipo) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!tipo.equals(claims.get("tipo", String.class))) {
            throw new JwtException("Tipo de token no permitido aquí");
        }
        return claims;
    }
}
