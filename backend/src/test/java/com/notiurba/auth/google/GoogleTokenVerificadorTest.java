package com.notiurba.auth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Firma ID tokens con una clave RSA propia (en lugar de la de Google) y comprueba que
 * el verificador aplica las mismas validaciones: firma, exp, iss, aud y email_verified.
 */
class GoogleTokenVerificadorTest {

    private static final String CLIENT_ID = "cliente-de-prueba.apps.googleusercontent.com";

    private static KeyPair claves;
    private static KeyPair otrasClaves;

    @BeforeAll
    static void generarClaves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        claves = generador.generateKeyPair();
        otrasClaves = generador.generateKeyPair();
    }

    private GoogleTokenVerificador verificador() {
        return new GoogleTokenVerificador(
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) claves.getPublic()).build(), CLIENT_ID);
    }

    private static String token(KeyPair firma, Consumer<JWTClaimsSet.Builder> ajuste) throws Exception {
        Instant ahora = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject("109876543210")
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plusSeconds(3600)))
                .claim("email", "ana.gomez@gmail.com")
                .claim("email_verified", true)
                .claim("given_name", "Ana María")
                .claim("family_name", "Gómez Ruiz")
                .claim("name", "Ana María Gómez Ruiz");
        ajuste.accept(claims);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner((RSAPrivateKey) firma.getPrivate()));
        return jwt.serialize();
    }

    private static String token(Consumer<JWTClaimsSet.Builder> ajuste) throws Exception {
        return token(claves, ajuste);
    }

    @Test
    void tokenValidoDevuelveElPerfil() throws Exception {
        PerfilGoogle perfil = verificador().verificar(token(c -> { }));

        assertThat(perfil.sub()).isEqualTo("109876543210");
        assertThat(perfil.correo()).isEqualTo("ana.gomez@gmail.com");
        assertThat(perfil.givenName()).isEqualTo("Ana María");
        assertThat(perfil.familyName()).isEqualTo("Gómez Ruiz");
        assertThat(perfil.nombreCompleto()).isEqualTo("Ana María Gómez Ruiz");
    }

    @Test
    void aceptaElEmisorSinHttps() throws Exception {
        assertThat(verificador().verificar(token(c -> c.issuer("accounts.google.com"))).sub())
                .isEqualTo("109876543210");
    }

    @Test
    void aceptaEmailVerifiedComoTexto() throws Exception {
        assertThat(verificador().verificar(token(c -> c.claim("email_verified", "true"))).correo())
                .isEqualTo("ana.gomez@gmail.com");
    }

    @Test
    void rechazaEmailNoVerificado() throws Exception {
        String token = token(c -> c.claim("email_verified", false));
        assertThatThrownBy(() -> verificador().verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaSinEmailVerified() throws Exception {
        String token = token(c -> c.claim("email_verified", null));
        assertThatThrownBy(() -> verificador().verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaAudienciaDeOtraAplicacion() throws Exception {
        String token = token(c -> c.audience("otra-app.apps.googleusercontent.com"));
        assertThatThrownBy(() -> verificador().verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaOtroEmisor() throws Exception {
        String token = token(c -> c.issuer("https://accounts.evil.com"));
        assertThatThrownBy(() -> verificador().verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenVencido() throws Exception {
        String token = token(c -> c.expirationTime(Date.from(Instant.now().minusSeconds(600))));
        assertThatThrownBy(() -> verificador().verificar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaFirmaDeOtraClave() throws Exception {
        String token = token(otrasClaves, c -> { });
        assertThatThrownBy(() -> verificador().verificar(token)).isInstanceOf(JwtException.class);
    }
}
