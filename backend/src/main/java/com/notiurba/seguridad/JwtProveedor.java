package com.notiurba.seguridad;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProveedor {

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
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
